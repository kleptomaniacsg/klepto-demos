# Plan Comparison Excel - Quick Reference (Updated)

## 🎯 NEW: Auto-Transformation

Server automatically transforms raw plan data → 2D matrix → Excel comparison table.

**You just send raw data. Server handles everything.**

## One-Liner Usage

**No client-side code needed anymore!** Just:

```bash
curl -X POST http://localhost:8080/api/documents/generate/excel \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "plan-comparison",
    "data": { "plans": [...] }
  }'
```

Server auto-transforms and generates Excel.

## What Happens Automatically

1. **Client sends**: Raw nested plans data
2. **Server detects**: Template ID = "plan-comparison"
3. **Server checks**: Data has "plans" field
4. **Server transforms**: Calls `PlanComparisonTransformer.injectComparisonMatrix()`
5. **Server renders**: ExcelSectionRenderer fills template with 2D matrix
6. **Client receives**: Excel file with comparison table ✓

## Input Data Format

```json
{
  "templateId": "plan-comparison",
  "namespace": "common-templates",
  "data": {
    "plans": [
      {
        "planName": "Plan Name",
        "benefits": [
          {"name": "Benefit 1", "value": "Value"},
          {"name": "Benefit 2", "value": "Value"}
        ]
      }
    ]
  }
}
```

## Output: 2D Matrix

What server generates internally:

```
[
  ["Benefit", "", "Plan 1", "", "Plan 2", "", "Plan 3"],
  ["Benefit A", "", "Value", "", "Value", "", "Value"],
  ["Benefit B", "", "Value", "", "Value", "", "Value"]
]
```

- **Column A**: Benefit names
- **Columns B, D, F**: Spacing (empty)
- **Columns C, E, G**: Plan values

## Did I Mention: You Don't Need Code?

### ✅ Old Way (BEFORE)
1. Extract plans from request
2. Call `PlanComparisonTransformer.injectComparisonMatrix()`
3. Manually enrich data
4. Send to API
5. 🔧 Client-side boilerplate

### ✅ New Way (NOW)
1. Send API request with `templateId: "plan-comparison"`
2. Server does steps 1-4 automatically
3. 🎉 No client code needed

## Template Configuration

```yaml
# plan-comparison.yaml
templateId: plan-comparison
sections:
  - sectionId: comparison
    type: EXCEL
    templatePath: comparison-template.xlsx
    fieldMappings:
      "A1:G6": "$.comparisonMatrix"
```

## Features Supported

| Feature | Status |
|---------|--------|
| Auto-transformation | ✅ Yes |
| Different benefit orders | ✅ Yes (normalized) |
| Missing benefits | ✅ Yes (empty cells) |
| Case-insensitive matching | ✅ Yes |
| Preserves original data | ✅ Yes |
| 1-column spacing | ✅ Yes (default) |
| Custom spacing | ✅ Yes (configurable) |
| Multiple plans | ✅ Yes (template capacity: 3) |
| Special characters | ✅ Yes |
| Null handling | ✅ Yes (→ empty string) |

## Manual Transformation (Optional)

If you need to preprocess or use anywhere else:

```java
import com.example.demo.docgen.util.PlanComparisonTransformer;

// Manually transform
Map<String, Object> enriched = PlanComparisonTransformer.injectComparisonMatrix(
    data,     // Original data (preserved)
    plans,    // List with planName and benefits[]
    "name",   // Benefit name field
    "value",  // Benefit value field
    1         // Spacing width
);

// enriched now has:
// - Original fields + new "comparisonMatrix" field
```

## Testing

All scenarios covered by 13 comprehensive tests:

```bash
# Run all tests
mvn test -Dtest=ExcelGenerationComprehensiveTest

# Result: BUILD SUCCESS - 13/13 tests PASSED ✓
```

**Test Coverage:**
- ✅ Basic 3-plan comparison
- ✅ Different benefit orders
- ✅ Missing benefits
- ✅ Auto-transformation
- ✅ Edge cases (empty, null, single plan)
- ✅ Case-insensitive matching
- ✅ Special characters

## Files Created

| File | Purpose |
|------|---------|
| `PlanComparisonTransformer.java` | Transformation logic (src/main/java/...) |
| `DocumentComposer.java` | Auto-transformation integration (line 415-423) |
| `ExcelSectionRenderer.java` | 2D array rendering (line 515-527) |
| `ExcelGenerationComprehensiveTest.java` | 13 test cases |
| `plan-comparison.yaml` | Template config |
| `comparison-template.xlsx` | Excel template (7 cols × 6 rows) |

## Curl Examples

### Basic 3-Plan Comparison

```bash
curl -X POST http://localhost:8080/api/documents/generate/excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "plan-comparison",
    "data": {
      "plans": [
        {
          "planName": "Basic",
          "benefits": [
            {"name": "Doctor Visits", "value": "$20 copay"},
            {"name": "ER Visit", "value": "$250 copay"},
            {"name": "Prescription", "value": "$10 copay"}
          ]
        },
        {
          "planName": "Premium",
          "benefits": [
            {"name": "Doctor Visits", "value": "Covered 100%"},
            {"name": "ER Visit", "value": "$150 copay"},
            {"name": "Prescription", "value": "$5 copay"}
          ]
        },
        {
          "planName": "Elite",
          "benefits": [
            {"name": "Doctor Visits", "value": "Covered 100%"},
            {"name": "ER Visit", "value": "Covered 100%"},
            {"name": "Prescription", "value": "Free"}
          ]
        }
      ]
    }
  }' --output comparison.xlsx
```

### With Custom Title and Date

```bash
curl -X POST http://localhost:8080/api/documents/generate/excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "plan-comparison",
    "data": {
      "comparisonTitle": "2026 Benefit Plans",
      "effectiveDate": "January 1, 2026",
      "plans": [
        { "planName": "Plan A", "benefits": [...] },
        { "planName": "Plan B", "benefits": [...] }
      ]
    }
  }' --output comparison.xlsx
```

## What's Transformed?

```
Input:  Nested plans + benefits (any order)
        ↓
Transform: Normalize benefits → Build matrix
        ↓
Output: 2D array (benefits × plans)
        ↓
Render: Fill Excel template
        ↓
Result: Professional comparison table ✓
```

## Architecture Calls

1. API endpoint receives request
2. `DocumentComposer.generateExcel()` called
3. `transformPlanDataIfNeeded()` auto-transforms if needed
4. `TemplateLoader.loadTemplate()` loads YAML config
5. `ExcelSectionRenderer.render()` fills cells with 2D array
6. `ExcelOutputService.toBytes()` converts to XLSX bytes
7. Response sent to client

## Limitations & Notes

- **Template Capacity**: Current template supports 3 plans × 5 benefits (A1:G6)
- **Plan Count**: More plans need larger template with extended column range
- **Benefit Count**: More benefits need extended row range in mapping
- **Spacing**: Default 1-column between plans (configurable in transformer)
- **Only for "plan-comparison"**: Auto-transformation is template-specific

## Status

✅ **Auto-transformation**: Fully implemented and tested  
✅ **13 test cases**: All passing  
✅ **Production ready**: Deployed and verified  
✅ **Documentation**: Complete and updated  

---

See `PLAN_COMPARISON_GUIDE.md` for complete walkthrough with architecture diagrams and advanced examples.
