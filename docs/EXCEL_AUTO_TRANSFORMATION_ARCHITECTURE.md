# Plan Comparison Auto-Transformation Architecture

## Overview

The server implements **automatic data transformation** for Excel plan comparison generation. When a client sends nested plan/benefits data with `templateId: "plan-comparison"`, the server:

1. Detects the plan-comparison template
2. Transforms nested data into a 2D comparison matrix
3. Injects matrix into request data
4. Renders matrix into Excel template

**Result**: Simplified client-side API - no manual transformation code needed.

---

## Architecture Layers

```
┌─────────────────────────────────────────────────────┐
│ REST API Layer                                      │
│ POST /api/documents/generate/excel                  │
├─────────────────────────────────────────────────────┤
│ DocumentComposer.generateExcel()                    │
│ • Entry point for Excel generation                  │
│ • Calls transformPlanDataIfNeeded()                 │
│ • Orchestrates template loading & rendering         │
├─────────────────────────────────────────────────────┤
│ transformPlanDataIfNeeded() [NEW]                   │
│ • Detects template ID = "plan-comparison"           │
│ • Checks for "plans" field in data                  │
│ • Skips if "comparisonMatrix" already exists        │
│ • Delegates transformation to PlanComparisonTransformer
├─────────────────────────────────────────────────────┤
│ PlanComparisonTransformer                           │
│ • 4-step algorithm for matrix generation            │
│ • Handles benefit normalization & deduplication     │
│ • Injects matrix into request data                  │
├─────────────────────────────────────────────────────┤
│ TemplateLoader → ExcelSectionRenderer               │
│ • Loads template config (YAML)                      │
│ • Detects 2D array in data                          │
│ • Fills Excel cells with matrix values              │
├─────────────────────────────────────────────────────┤
│ ExcelOutputService                                  │
│ • Converts Apache POI Workbook → XLSX bytes         │
└─────────────────────────────────────────────────────┘
```

---

## Component Descriptions

### 1. DocumentComposer.generateExcel()

**Location**: `src/main/java/com/example/demo/docgen/service/DocumentComposer.java` (lines 405-423)

**Responsibility**: Main orchestrator for Excel generation with integrated auto-transformation

**Code Flow**:
```java
public byte[] generateExcel(DocumentGenerationRequest request) {
    // NEW: Auto-transform plan data if needed
    transformPlanDataIfNeeded(request);  // Line 417
    
    // Existing flow: Load template, render, convert to bytes
    DocumentTemplate template = templateLoader.loadTemplate(...);
    RenderContext context = new RenderContext(template, request.getData());
    // ... render sections ...
    byte[] bytes = excelOutputService.toBytes(workbook);
}
```

**Key Decision**: 
- Transformation happens **before** template loading
- Enriched data is passed to renderer
- No changes to existing rendering pipeline

### 2. transformPlanDataIfNeeded()

**Location**: `src/main/java/com/example/demo/docgen/service/DocumentComposer.java` (lines 425-467)

**Responsibility**: Conditional auto-transformation trigger

**Algorithm**:
```
INPUT: DocumentGenerationRequest
  CHECK 1: request.getTemplateId() == "plan-comparison" ?
    |-- NO:  Return (skip transformation)
    |-- YES: Continue
  
  CHECK 2: request.getData().containsKey("plans") ?
    |-- NO:  Return (skip transformation)
    |-- YES: Continue
  
  CHECK 3: Determine if we should inject a full or values-only matrix
    |-- Read template.config.valuesOnly (default false)
    |-- If valuesOnly == false:
        • CHECK 3a: data.containsKey("comparisonMatrix") ?
            |-- YES: Return (already transformed, skip)
            |-- NO:  Continue
    |-- If valuesOnly == true:
        • CHECK 3b: data.containsKey("comparisonMatrixValues") ?
            |-- YES: Return (already transformed, skip)
            |-- NO: Continue
  
  ACTION: Transform & Inject
    → Extract plans from data
    → If valuesOnly==false call
         PlanComparisonTransformer.injectComparisonMatrix(data, plans)
         (populates `comparisonMatrix`)
      Else call
         PlanComparisonTransformer.injectComparisonMatrixValuesOnly(data, plans)
         (populates `comparisonMatrixValues`)
    → Update request.setData(enrichedData)
    → Log matrix dimensions (key depends on branch)
    → RETURN

EXCEPTION: Log warning, don't fail generation (graceful degradation)
```

**Design Rationale**:
- **Exact template match**: Only "plan-comparison" triggers auto-transformation
- **Plans field check**: Respects data structure (optional field)
- **Skip if matrix exists**: Allows manual pre-transformation if needed
- **Graceful error handling**: Warnings don't break generation
- **Dimension logging**: Helps debug matrix structure

### 3. PlanComparisonTransformer

**Location**: `src/main/java/com/example/demo/docgen/util/PlanComparisonTransformer.java`

**Public API**:
```java
// Main transformation with overloads
static List<List<Object>> transformPlansToMatrix(List<Map<String, Object>> plans)
static List<List<Object>> transformPlansToMatrix(
    List<Map<String, Object>> plans,
    String benefitNameField,
    String benefitValueField,
    int spacingWidth
)

// Injection method
static Map<String, Object> injectComparisonMatrix(
    Map<String, Object> data,
    List<Map<String, Object>> plans
)
static Map<String, Object> injectComparisonMatrix(
    Map<String, Object> data,
    List<Map<String, Object>> plans,
    String benefitNameField,
    String benefitValueField,
    int spacingWidth
)
```

**Transformation Algorithm** (4 steps):

#### Step 1: Collect Unique Benefits
```
For each plan:
  For each benefit:
    Normalize name: benefitName.toLowerCase().trim()
    Add to set (deduplicates)
    Remember original casing
Result: LinkedSet<String> preserves order
```

#### Step 2: Extract Plan Names
```
For each plan:
  Extract plan.get("planName")
Result: List<String> planNames (in order)
```

#### Step 3: Build Benefit-to-Value Maps
```
For each plan:
  Create map: normalizedBenefitName → benefitValue
  Store in list
Result: List<Map<benefitName, value>> (one map per plan)
```

#### Step 4: Build 2D Matrix
```
Row 0: BuildHeaders
  ["Benefit", spacer, planName₁, spacer, planName₂, ...]
  
Rows 1+: BuildDataRows
  For each benefit:
    [originalBenefitName, spacer, value₁, spacer, value₂, ...]
    where value = benefitMap.get(benefit) OR empty

Result: List<List<Object>> matrix
```

**Example Transformation**:
```
INPUT:
[
  {
    "planName": "Basic",
    "benefits": [
      {"name": "Doctor Visits", "value": "$20"},
      {"name": "ER Visit", "value": "$250"}
    ]
  },
  {
    "planName": "Premium",
    "benefits": [
      {"name": "ER Visit", "value": "$150"},
      {"name": "Doctor Visits", "value": "Free"}
    ]
  }
]

STEP 1 - Unique Benefits (normalized):
["doctor visits", "er visit"]

STEP 2 - Plan Names:
["Basic", "Premium"]

STEP 3 - Benefit Maps:
[
  {"doctor visits": "$20", "er visit": "$250"},
  {"er visit": "$150", "doctor visits": "Free"}
]

STEP 4 - Matrix (with 1-col spacing):
[
  ["Benefit", "", "Basic", "", "Premium"],
  ["Doctor Visits", "", "$20", "", "Free"],
  ["ER Visit", "", "$250", "", "$150"]
]
```

### 4. ExcelSectionRenderer - 2D Array Detection

**Location**: `src/main/java/.../ExcelSectionRenderer.java` (lines 515-527)

**Code**:
```java
// Detect 2D array pattern
if (value instanceof List && !((List<?>) value).isEmpty() 
    && ((List<?>) value).get(0) instanceof List) {
    
    List<List<?>> grid = (List<List<?>>) value;
    
    // Fill matrix into template range
    for (int r = 0; r < Math.min(grid.size(), rows); r++) {
        List<?> row = grid.get(r);
        for (int c = 0; c < Math.min(row.size(), cols); c++) {
            Object cellValue = row.get(c);
            setCellValueAt(sheet, 
                startRef.getRow() + r, 
                startRef.getCol() + c,
                cellValue == null ? "" : cellValue.toString()
            );
        }
    }
}
```

**Design Rationale**:
- **Nested List check**: `List<List<?>>` pattern detection
- **Bounds safety**: `Math.min()` prevents overflow
- **Row-major iteration**: Natural for spreadsheets
- **Null handling**: Empty string instead of nulls

### 5. Template Configuration

**File**: `src/main/resources/common-templates/templates/plan-comparison.yaml`

```yaml
templateId: plan-comparison
description: Plan comparison table with benefits vs plans

sections:
  - sectionId: plan_comparison
    type: EXCEL
    templatePath: comparison-template.xlsx
    mappingType: JSONPATH
    fieldMappings:
      "A1:G6": "$.comparisonMatrix"
```

**Mapping Explanation**:
- **Range `A1:G6`**: 7 columns (A-G) × 6 rows (1-6)
  - Column A: Benefit names
  - Column B: Spacing
  - Column C: Plan 1 values
  - Column D: Spacing
  - Column E: Plan 2 values
  - Column F: Spacing
  - Column G: Plan 3 values
  - Rows 1-6: Headers (row 1) + 5 data rows

- **JSONPath `$.comparisonMatrix`**: Points to 2D array in request.data

### 6. Excel Template Structure

**File**: `src/main/resources/common-templates/comparison-template.xlsx`

**Physical Layout** (7 cols × 6 rows):
```
A1: Benefit
B1: [spacing]
C1: Plan 1
D1: [spacing]
E1: Plan 2
F1: [spacing]
G1: Plan 3

A2-G2: [benefit 1 row - will be overwritten]
A3-G3: [benefit 2 row - will be overwritten]
A4-G4: [benefit 3 row - will be overwritten]
A5-G5: [benefit 4 row - will be overwritten]
A6-G6: [benefit 5 row - will be overwritten]
```

**Key Property**: All cells are **empty** (None, not empty string)
- Allows complete overwrite by matrix values
- Preserves styling (borders, fonts) from template

---

## Data Flow Diagram

```
1. Client Request
   │
   ├─ POST /api/documents/generate/excel
   │
   └─> DocumentGenerationRequest {
       templateId: "plan-comparison",
       data: {
         plans: [
           {planName: "Plan A", benefits: [...]},
           {planName: "Plan B", benefits: [...]}
         ]
       }
   }

2. DocumentComposer.generateExcel()
   │
   └─> transformPlanDataIfNeeded(request)
       │
       ├─ Is templateId == "plan-comparison"? YES
       │
       ├─ Does data have "plans" field? YES
       │
       ├─ Already has "comparisonMatrix"? NO
       │
       └─> PlanComparisonTransformer.injectComparisonMatrix()
           │
           ├─ Collect unique benefits (normalized)
           │
           ├─ Extract plan names
           │
           ├─ Build benefit→value maps
           │
           └─> Create 2D matrix
               │
               └─> Return enriched data {
                       plans: [...],
                       comparisonMatrix: [
                         ["Benefit", "", "Plan A", "", "Plan B"],
                         ["Benefit 1", "", "Value", "", "Value"],
                         ...
                       ]
                   }

3. Updated request.data → TemplateLoader
   │
   └─> Load plan-comparison.yaml

4. TemplateLoader → ExcelSectionRenderer
   │
   └─> Render section with fieldMappings
       │
       ├─ Detect 2D array in $.comparisonMatrix
       │
       └─> Fill A1:G6 range

5. ExcelSectionRenderer → ExcelOutputService
   │
   └─> workbook.save() → XLSX bytes

6. Return bytes
   │
   └─> Client receives comparison.xlsx
```

---

## Design Decisions

### 1. Server-Side vs Client-Side Transformation
**Decision**: Server-side  
**Rationale**:
- Simpler client API (no transformer imports needed)
- Single source of truth (transformation logic centralized)
- Consistent transformation across all clients
- Optional: Clients can pre-transform if needed (skipped by transformer)

### 2. Exact Template Match
**Decision**: Only `"plan-comparison"` template triggers auto-transformation  
**Rationale**:
- Prevents accidental transformation of other templates
- Explicit control over which templates get auto-transformation
- Easy to add other auto-transforming templates in future

### 3. Graceful Degradation
**Decision**: Transformation errors don't break generation  
**Rationale**:
- Rendering might work with raw data
- More specific errors reported during rendering
- Better user experience (attempt render before failing)

### 4. 2D Array Detection
**Decision**: `instanceof List && get(0) instanceof List`  
**Rationale**:
- Simple, efficient runtime check
- Distinguishes 2D from 1D arrays
- No schema/metadata needed

### 5. Template Capacity
**Decision**: Fixed 7×6 template (3 plans × 5 benefits)  
**Rationale**:
- Practical limit for readable Excel comparison
- Easy to extend by modifying template
- Current design supports typical use cases

---

## Testing Strategy

**13 Comprehensive Test Cases** (ExcelGenerationComprehensiveTest.java):

### Coverage Matrix

| Scenario | Test Count | Validation |
|----------|-----------|------------|
| Basic comparison | 3 | Matrix structure, values, different orders |
| Auto-transformation | 2 | Raw data → matrix, skip if matrix exists |
| Advanced cases | 4 | Multiple plans, case-insensitivity, spacing, special chars |
| Edge cases | 4 | Empty plans, single plan, nulls, template specificity |
| **Total** | **13** | 100% pass rate |

### Test Execution
```bash
mvn test -Dtest=ExcelGenerationComprehensiveTest
# BUILD SUCCESS - 13/13 tests PASSED ✓
```

---

## Future Enhancements

### 1. Dynamic Template Sizing
- Accept `{"maxPlans": N, "maxBenefits": M}` in config
- Auto-generate template range in mapping

### 2. Multiple Transformation Strategies
- Configurable benefit sorting (alphabetical, custom order)
- Multiple value format support (enum → label)
- Custom spacing per-plan-pair

### 3. Styling Configuration
- Header styling (colors, fonts)
- Benefit name styling
- Plan column styling
- Spacing column styling

### 4. Performance Optimization
- Cache transformation results
- Parallel rendering for multiple requests
- Streaming large Excel files

### 5. Additional Templates
- "plan-summary" for single plan
- "competitor-analysis" with more advanced layouts
- "cost-breakdown" with calculated columns

---

## Error Handling

### Graceful Degradation Path

```
transformPlanDataIfNeeded() Error
    ↓
Log warning: "Error during auto-transformation..."
    ↓
Continue with original request data
    ↓
TemplateLoader.loadTemplate()
    ↓
ExcelSectionRenderer attempts to render
    ↓
If 2D array not found in data:
  → Fallback to value rendering (might show object reference)
  → More specific error at this point
```

### Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| Matrix not visible in Excel | Template cells have data | Use clean template (cells = empty) |
| Only 3 plan columns | Template limited to A1:G6 | Extend template and mapping |
| Benefits in wrong order | Input order varies | Normalized by transformer (first-appearance order) |
| Missing benefits → missing columns | Expected behavior | Check data structure |
| Auto-transform not triggering | Wrong template ID | Must be exactly "plan-comparison" |

---

## Summary

The auto-transformation architecture provides:

✅ **Simplicity**: Clients send raw data, server handles transformation  
✅ **Reliability**: 13 test cases ensure correctness  
✅ **Flexibility**: Transformer reusable outside auto-transformation  
✅ **Extensibility**: Easy to add more auto-transforming templates  
✅ **Robustness**: Graceful error handling, non-breaking changes  

**Key Files**:
- [DocumentComposer.java](src/main/java/com/example/demo/docgen/service/DocumentComposer.java) - Auto-transformation integration
- [PlanComparisonTransformer.java](src/main/java/com/example/demo/docgen/util/PlanComparisonTransformer.java) - Transformation algorithm
- [ExcelSectionRenderer.java](src/main/java/.../ExcelSectionRenderer.java) - 2D array rendering
- [plan-comparison.yaml](src/main/resources/common-templates/templates/plan-comparison.yaml) - Template config
- [comparison-template.xlsx](src/main/resources/common-templates/comparison-template.xlsx) - Excel template

**Documentation References**:
- [PLAN_COMPARISON_GUIDE_UPDATED.md](PLAN_COMPARISON_GUIDE_UPDATED.md) - Complete end-to-end guide
- [PLAN_COMPARISON_QUICK_REF_UPDATED.md](PLAN_COMPARISON_QUICK_REF_UPDATED.md) - Quick reference
- [EXCEL_GENERATION_TEST_SUITE.md](EXCEL_GENERATION_TEST_SUITE.md) - Test documentation
