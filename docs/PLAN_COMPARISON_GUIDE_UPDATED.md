# Plan Comparison Excel Generation - Complete Guide

## ✨ NEW: Auto-Transformation Feature

The server now **automatically transforms plan data** when using the `plan-comparison` template. No manual transformation required on the client side!

## Quick Start

### 1. Send Raw Nested Data to the Server

```bash
curl -X POST http://localhost:8080/api/documents/generate/excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "plan-comparison",
    "data": {
      "comparisonTitle": "2026 Health Insurance Plans",
      "effectiveDate": "March 1, 2026",
      "plans": [
        {
          "planName": "Basic",
          "benefits": [
            {"name": "Doctor Visits", "value": "$20 copay"},
            {"name": "ER Visit", "value": "$250 copay"},
            {"name": "Hospital", "value": "$1000 deductible"},
            {"name": "Prescription", "value": "$10/$35 copay"}
          ]
        },
        {
          "planName": "Premium",
          "benefits": [
            {"name": "Prescription", "value": "$5 copay"},
            {"name": "Doctor Visits", "value": "Covered 100%"},
            {"name": "ER Visit", "value": "$150 copay"},
            {"name": "Hospital", "value": "$500 deductible"}
          ]
        },
        {
          "planName": "Elite",
          "benefits": [
            {"name": "ER Visit", "value": "Covered 100%"},
            {"name": "Hospital", "value": "$250 deductible"},
            {"name": "Doctor Visits", "value": "Covered 100%"},
            {"name": "Prescription", "value": "Free"}
          ]
        }
      ]
    }
  }' --output comparison.xlsx
```

### 2. Server Auto-Transforms (Automatically)

The server detects:
- ✅ Template ID is `"plan-comparison"`  
- ✅ Data contains `"plans"` field
- ✅ Automatically calls `PlanComparisonTransformer.injectComparisonMatrix()`
- ✅ Enriches request data with `"comparisonMatrix"`
- ✅ Proceeds to template rendering

**No client-side code needed!**

### 3. Result

Excel file with benefits in rows, plans in columns:

```
   A               B    C                D    E              F    G
1: Benefit         -    Basic            -    Premium        -    Elite
2: Doctor Visits   -    $20 copay        -    Covered 100%   -    Covered 100%
3: ER Visit        -    $250 copay       -    $150 copay     -    Covered 100%
4: Hospital        -    $1000 deductible -    $500 deductible -    $250 deductible
5: Prescription    -    $10/$35 copay    -    $5 copay       -    Free
```

---

## How Auto-Transformation Works

### Server-Side Implementation

In `DocumentComposer.generateExcel()` (lines 415-423):

```java
/**
 * Auto-transform plan data into comparison matrix if conditions are met.
 * 
 * Detects if:
 * 1. The template is "plan-comparison" (exact match)
 * 2. The data contains a "plans" field (List of plans)
 * 3. The data does NOT already have a "comparisonMatrix" field
 * 
 * If all conditions are met, transforms the nested plan/benefits structure
 * into a 2D matrix using PlanComparisonTransformer and injects it into the data.
 */
private void transformPlanDataIfNeeded(DocumentGenerationRequest request) {
    // Checks template ID against "plan-comparison"
    // Verifies "plans" field exists and is a List
    // Skips if "comparisonMatrix" already present (respects manual transformation)
    // Calls: PlanComparisonTransformer.injectComparisonMatrix(data, plans)
    // Updates request with enriched data
}
```

### Transformation Algorithm

The `PlanComparisonTransformer.transformPlansToMatrix()` method (4-step algorithm):

1. **Collect Unique Benefits**: Normalize benefit names (case-insensitive), preserve first-seen order
2. **Extract Plan Names**: Build list of plan names from input data
3. **Build Benefit-to-Value Maps**: For each plan, create a map of benefit → value
4. **Build 2D Matrix**: Construct matrix with:
   - Row 0: Headers (Benefit, spacer, Plan 1, spacer, Plan 2, ...)
   - Rows 1+: Benefit values (benefit name, spacer, value, spacer, value, ...)

**Key Features:**
- ✅ **Benefit Name Normalization**: Case-insensitive matching (e.g., "Doctor Visits" == "doctor visits")
- ✅ **Order Independence**: Benefits in different order per plan → matrix has consistent order
- ✅ **Missing Benefits**: Plans without a benefit get empty cells
- ✅ **Configurable Spacing**: Default 1 column, can be customized
- ✅ **Data Preservation**: Original fields never modified, matrix injected as new field

---

## Advanced Usage

### Backup: Manual Transformation (Optional)

If you want to manually transform before sending (e.g., for preprocessing):

```java
import com.example.demo.docgen.util.PlanComparisonTransformer;

// Extract plans
List<Map<String, Object>> plans = (List<Map<String, Object>>) data.get("plans");

// Transform manually
Map<String, Object> enrichedData = PlanComparisonTransformer.injectComparisonMatrix(
    data,      // Preserves original data
    plans,     // List with planName and benefits[]
    "name",    // Benefit name field (default)
    "value",   // Benefit value field (default)
    1          // Spacing width between plans (default)
);

// Send to API (or server will auto-transform again - harmless, skipped if matrix exists)
```

### Custom Field Names

If your data uses different field names:

```java
PlanComparisonTransformer.injectComparisonMatrix(
    data,
    plans,
    "featureName",       // Use this field instead of "name"
    "featureValue",      // Use this field instead of "value"
    1
);
```

### Custom Spacing

To add more space between plan columns:

```java
// 2-column spacing: ["Benefit", "", "", "Plan A", "", "", "Plan B", ...]
PlanComparisonTransformer.injectComparisonMatrix(data, plans, "name", "value", 2);
```

### Get Just the Matrix

Without injecting into data:

```java
List<List<Object>> matrix = PlanComparisonTransformer.transformPlansToMatrix(plans);
// Use matrix for other purposes
```

---

## Template Configuration

### YAML Template

File: `src/main/resources/common-templates/templates/plan-comparison.yaml`

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

**Mapping Explanation:**
- `"A1:G6"`: Excel range (6 rows × 7 columns)
  - A1:G1 = Headers (Benefit, spacer, Plan A, spacer, Plan B, spacer, Plan C)
  - A2:G6 = Data rows (5 benefits)
- `"$.comparisonMatrix"`: JSONPath to the 2D matrix in data

### Excel Template

File: `src/main/resources/common-templates/comparison-template.xlsx`

**Structure:**
- 7 columns (A-G) for: benefit + spacer + 3 plans with spacers
- 6 rows total (1 header + 5 data)
- Pre-styled with borders and column widths
- Clean cells (no placeholder data) so matrix values completely overwrite

**Rendering Process:**
1. Server receives raw plan data
2. Auto-transformation creates 2D matrix
3. ExcelSectionRenderer maps 2D matrix to A1:G6 range
4. Each cell value fills corresponding Excel cell
5. Result: Complete comparison table

---

## Data Format & Examples

### Input Data Structure

```json
{
  "plans": [
    {
      "planName": "Plan Name",
      "benefits": [
        {"name": "Benefit Name", "value": "Benefit Value"},
        {"name": "Benefit Name", "value": "Benefit Value"}
      ]
    }
  ]
}
```

### Features

- ✅ **Any Number of Plans**: 1, 2, 3+ plans supported (template capacity dependent)
- ✅ **Any Order of Benefits**: Automatically normalized to consistent order
- ✅ **Missing Benefits**: Some plans don't have all benefits → empty cells
- ✅ **Case-Insensitive Matching**: "Doctor Visits", "doctor visits", "DOCTOR VISITS" all match
- ✅ **Special Characters**: Preserved in output (e.g., "$", "%", "+")
- ✅ **Null Handling**: Null values converted to empty strings

### Example: Different Benefit Orders

```json
{
  "plans": [
    {
      "planName": "Plan A",
      "benefits": [
        {"name": "Doctor Visits", "value": "$20"},
        {"name": "ER Visit", "value": "$250"},
        {"name": "Prescription", "value": "$10"}
      ]
    },
    {
      "planName": "Plan B",
      "benefits": [
        {"name": "Prescription", "value": "$5"},
        {"name": "Doctor Visits", "value": "Free"},
        {"name": "ER Visit", "value": "$150"}
      ]
    }
  ]
}
```

**Transformer Output**:
- Detects benefits: "Doctor Visits" (first in Plan A), "ER Visit", "Prescription"
- Matrix has consistent row order regardless of input order
- All values correctly mapped to their benefits

### Example: Missing Benefits

```json
{
  "plans": [
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
        {"name": "Doctor Visits", "value": "Free"},
        {"name": "ER Visit", "value": "$150"},
        {"name": "Eye Care", "value": "Free"}
      ]
    }
  ]
}
```

**Result**:
- Detected benefits: Doctor Visits, ER Visit, Eye Care
- Basic plan gets empty cell for "Eye Care"
- All benefits still in consistent row order

---

## Test Suite

Comprehensive test coverage with **13 test cases** in `ExcelGenerationComprehensiveTest.java`:

### Basic Tests (3 tests)
- ✅ Simple 3-plan comparison
- ✅ Different benefit orders per plan
- ✅ Missing benefits across plans

### Auto-Transformation Tests (2 tests)
- ✅ Raw data auto-transformation
- ✅ Skip transformation if matrix already exists

### Advanced Scenarios (4 tests)
- ✅ Multiple plans and benefits
- ✅ Case-insensitive benefit matching
- ✅ Custom spacing widths
- ✅ Special characters in values

### Edge Cases (4 tests)
- ✅ Empty plans
- ✅ Single plan
- ✅ Null values and missing fields
- ✅ Template specificity (only auto-transform for "plan-comparison")

**Run Tests:**
```bash
mvn test -Dtest=ExcelGenerationComprehensiveTest
# Result: BUILD SUCCESS - 13/13 tests PASSED ✓
```

---

## File Inventory

| File | Purpose |
|------|---------|
| `PlanComparisonTransformer.java` | Core transformation logic |
| `DocumentComposer.java` | Orchestra with auto-transformation (line 415-423) |
| `ExcelSectionRenderer.java` | 2D array detection and cell filling (line 515-527) |
| `PlanComparisonTransformerTest.java` | 5 unit tests |
| `PlanComparisonExcelIntegrationTest.java` | Integration tests |
| `ExcelGenerationComprehensiveTest.java` | 13 comprehensive tests (NEW) |
| `plan-comparison.yaml` | Template configuration |
| `comparison-template.xlsx` | Excel template |
| `PLAN_COMPARISON_GUIDE.md` | This guide (updated) |
| `EXCEL_GENERATION_TEST_SUITE.md` | Test suite documentation |

---

## Architecture Diagram

```
Client                          Server
   │                              │
   │  POST /api/documents/        │
   │  generate/excel              │
   │  {                           │
   │    templateId: "plan-       │
   │    comparison",             │
   │    data: {                  │
   │      plans: [...]           │
   │    }                        │
   │  }                          │
   └──────────────────────────>  DocumentComposer
                                 │
                                 │  generateExcel()
                                 │
                                 v
                    transformPlanDataIfNeeded()
                                 │
                    ┌────────────┴────────────┐
                    v                        v
            Check template ID           Check for
                 "plan-comparison"       "plans" field
                                 │
                                 └──────────┬─────────────┐
                                           YES          NO/Skip
                                            │             │
                            ┌───────────────v─────────┐  │
                            │ PlanComparisonTransformer  │
                            │                          │  │
                            │ • Normalize benefits    │  │
                            │ • Extract plans         │  │
                            │ • Build matrices        │  │
                            │ • Inject into data      │  │
                            └───────────────┬─────────┘  │
                                            │             │
                                    Enriched data with   │
                                    comparisonMatrix    │
                                            │             │
                                            └─────┬───────┘
                                                  │
                                        TemplateLoader
                                                  │
                                        Load YAML config
                                                  │
                                       ExcelSectionRenderer
                                                  │
                                  • Detect 2D array
                                  • Fill A1:G6 range
                                  • 2D iteration
                                                  │
                                       Filled Excel workbook
                                                  │
                                        ExcelOutputService
                                                  │
                                                  v
                                          XLSX Bytes
                                                  │
                                                  │
                           <──────────────────────
                               comparison.xlsx
```

---

## FAQ

**Q: Do I need to manually call PlanComparisonTransformer?**  
A: No! The server does it automatically. Only use for behind-the-scenes preprocessing if needed.

**Q: What if I already have a comparisonMatrix in my data?**  
A: The auto-transformer detects it and skips transformation, so your existing matrix is used.

**Q: Does this only work for "plan-comparison" template?**  
A: Yes. Auto-transformation is specific to template ID "plan-comparison". Other templates are unaffected.

**Q: What if I use a different template ID but still want transformation?**  
A: Call `PlanComparisonTransformer.injectComparisonMatrix()` manually beforehand, or extend the auto-transformation logic in `DocumentComposer.transformPlanDataIfNeeded()`.

**Q: Can I have more than 3 plans?**  
A: Template capacity is limited to 3 plans by the A1:G6 range (7 columns). For more plans, create a larger template or modify the mapping range.

**Q: How are benefits sorted in the output?**  
A: By first appearance order - the order they first appear across all plans.

---

## Summary

✅ **Setup**: Send nested plans data with `templateId: "plan-comparison"`  
✅ **Auto-Transformation**: Server detects template and auto-transforms  
✅ **Rendering**: ExcelSectionRenderer fills the 2D matrix into the Excel template  
✅ **Result**: Professional comparison table with benefits in rows, plans in columns  
✅ **Testing**: 13 comprehensive test cases ensure reliability  

**No client-side boilerplate needed!**
