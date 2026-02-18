# Plan Comparison Auto-Transformation Test

## Overview
The `DocumentComposer.generateExcel()` method now automatically transforms raw plan data into a comparison matrix when generating Excel documents using the "plan-comparison" template.

## How It Works Now

### Before (Manual Transformation)
```java
// CLIENT had to manually call the transformer
Map<String, Object> data = new HashMap<>();
data.put("plans", [...]);

Map<String, Object> enrichedData = PlanComparisonTransformer.injectComparisonMatrix(data, plans);

curl -X POST http://localhost:8080/api/documents/generate/excel \
  -H "Content-Type: application/json" \
  -d {
    "namespace": "common-templates",
    "templateId": "plan-comparison",
    "data": enrichedData  // Had to pre-transform
  }
```

### After (Automatic Transformation) ✨
```java
// CLIENT just sends raw data, server handles transformation
curl -X POST http://localhost:8080/api/documents/generate/excel \
  -H "Content-Type: application/json" \
  -d {
    "namespace": "common-templates",
    "templateId": "plan-comparison",
    "data": {
      "plans": [
        {
          "planName": "Plan A",
          "benefits": [
            {"name": "Doctor Visits", "value": "Yes"},
            {"name": "ER Visit", "value": "$20"}
          ]
        },
        {
          "planName": "Plan B",
          "benefits": [
            {"name": "Doctor Visits", "value": "$15"},
            {"name": "ER Visit", "value": "Free"}
          ]
        }
      ]
    }
  }
```

## What Changed

### In DocumentComposer.java

1. **Added Import**
   ```java
   import com.example.demo.docgen.util.PlanComparisonTransformer;
   ```

2. **Enhanced generateExcel() method**
   - Now calls `transformPlanDataIfNeeded(request)` before template loading
   - Automatically detects plan-comparison templates with raw plan data
   - Injects comparison matrix into request data before rendering

3. **New Helper Method: transformPlanDataIfNeeded()**
   ```
   Detects when to auto-transform:
   - Template ID equals "plan-comparison"
   - Data contains "plans" field (List)
   - Data does NOT already have "comparisonMatrix"
   
   Then calls PlanComparisonTransformer.injectComparisonMatrix()
   and updates the request with enriched data
   ```

## Test Scenario

**Input Request:**
```json
{
  "namespace": "common-templates",
  "templateId": "plan-comparison",
  "data": {
    "plans": [
      {
        "planName": "Basic",
        "benefits": [
          {"name": "Doctor Visits", "value": "$20 copay"},
          {"name": "ER Visit", "value": "$250 copay"},
          {"name": "Hospital Stay", "value": "$1,000 deductible"}
        ]
      },
      {
        "planName": "Premium",
        "benefits": [
          {"name": "Prescription", "value": "$5 copay"},
          {"name": "Doctor Visits", "value": "Covered 100%"},
          {"name": "ER Visit", "value": "$150 copay"},
          {"name": "Hospital Stay", "value": "$500 deductible"}
        ]
      }
    ]
  }
}
```

**Server Processing:**
1. DocumentController receives request
2. DocumentController.generateExcel() calls DocumentComposer.generateExcel()
3. DocumentComposer detects "plan-comparison" template with "plans" data
4. DocumentComposer calls transformPlanDataIfNeeded()
5. transformPlanDataIfNeeded() injects comparisonMatrix into data
6. Data now contains:
   ```json
   {
     "plans": [...],
     "comparisonMatrix": [
       ["Benefit", "", "Basic", "", "Premium"],
       ["Doctor Visits", "", "$20 copay", "", "Covered 100%"],
       ["ER Visit", "", "$250 copay", "", "$150 copay"],
       ["Hospital Stay", "", "$1,000 deductible", "", "$500 deductible"],
       ["Prescription", "", "", "", "$5 copay"]
     ]
   }
   ```
7. ExcelSectionRenderer renders normally using the injected matrix
8. Client receives properly formatted Excel file

## Key Benefits

✅ **Seamless Integration** - Clients don't need to know about PlanComparisonTransformer
✅ **Backward Compatible** - If comparisonMatrix already exists, transformation is skipped
✅ **Template-Specific** - Only activates for "plan-comparison" templates
✅ **Error Handling** - Gracefully handles transformation failures without breaking generation
✅ **Transparent** - Works automatically when conditions are met

## To Test

```bash
# Start the application
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Send raw plan data (no manual transformation needed!)
curl -X POST http://localhost:8080/api/documents/generate/excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "plan-comparison",
    "data": {
      "plans": [
        {"planName": "Plan A", "benefits": [{"name": "Doctor Visits", "value": "Yes"}, {"name": "ER Visit", "value": "$20"}]},
        {"planName": "Plan B", "benefits": [{"name": "Doctor Visits", "value": "$15"}, {"name": "ER Visit", "value": "Free"}]}
      ]
    }
  }' -o comparison.xlsx

# Open comparison.xlsx - all benefits and values rendered correctly!
```

## Comparison

| Aspect | Before | After |
|--------|--------|-------|
| Client knows about Transformer? | ✓ Yes | ✗ No |
| Manual transformation needed? | ✓ Yes | ✗ No |
| API is user-friendly? | Partially | ✓ Yes, fully transparent |
| Backward compatible? | N/A | ✓ Yes |
| Works without plans field? | ✓ Yes | ✓ Yes (no transformation) |
| Works if comparisonMatrix already exists? | N/A | ✓ Yes (skips) |

