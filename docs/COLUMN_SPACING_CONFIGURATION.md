# Column Spacing Configuration - Usage Guide

## Overview

You can now configure the number of columns between plan columns using the YAML template configuration. This allows you to adjust the spacing in the Excel comparison table without changing code.

## Configuration Method: YAML Template

The column spacing is configured in the template's `config` section:

> **Tip:** the same `config` map may also contain other plan-comparison
> options such as `valuesOnly` (see plan comparison documentation) which tells
> the transformer to omit the benefit column because the workbook already
> contains it.  That flag is ignored by the spacing logic.


```yaml
templateId: plan-comparison
description: Plan comparison table showing benefits across multiple plans

# Configuration for plan comparison transformation
config:
  columnSpacing: 1  # Number of columns between plan columns

sections:
  - sectionId: plan_comparison
    type: EXCEL
    templatePath: comparison-template.xlsx
    mappingType: JSONPATH
    fieldMappings:
      "A1:G6": "$.comparisonMatrix"
```

## Spacing Examples

### Default: 1 Column Spacing

```yaml
config:
  columnSpacing: 1
```

**Result:**
```
Benefit | [space] | Plan A | [space] | Plan B | [space] | Plan C
```

### No Spacing: 0 Columns

```yaml
config:
  columnSpacing: 0
```

**Result:**
```
Benefit | Plan A | Plan B | Plan C
```

**Note:** Requires updated template with adjusted column range (e.g., A1:D6 instead of A1:G6)

### Wide Spacing: 2 Columns

```yaml
config:
  columnSpacing: 2
```

**Result:**
```
Benefit | [space] | [space] | Plan A | [space] | [space] | Plan B | [space] | [space] | Plan C
```

**Note:** Requires larger template range (e.g., A1:J6)

## How It Works

### Implementation Flow

1. **Server receives Excel generation request**
   ```json
   {
     "templateId": "plan-comparison",
     "data": { "plans": [...] }
   }
   ```

2. **DocumentComposer loads template**
   - Extracts `config.columnSpacing` from YAML
   - Defaults to `1` if not specified or invalid

3. **Auto-transformation uses spacing**
   ```java
   PlanComparisonTransformer.injectComparisonMatrix(
       data, 
       plans,
       "name",        // Benefit name field
       "value",       // Benefit value field
       columnSpacing  // From template config
   )
   ```

4. **Matrix is rendered to Excel**
   - All cells filled according to spacing configuration

### Code Reference

**DocumentComposer.java (lines ~360-380):**
```java
// Extract columnSpacing config from template (default to 1 if not specified)
int columnSpacing = getColumnSpacingFromTemplate(template);

// Auto-transform plan data if needed (using configured spacing)
transformPlanDataIfNeeded(request, columnSpacing);
```

**getColumnSpacingFromTemplate():**
- Looks for `template.getConfig().get("columnSpacing")`
- Validates it's a non-negative Integer or Number
- Returns configured value or defaults to `1`
- Logs warnings if config is invalid

## Customizing Spacing

### To Change Spacing

1. **Edit the YAML template:**
   ```yaml
   # src/main/resources/common-templates/templates/plan-comparison.yaml
   config:
     columnSpacing: 2  # Change from 1 to 2
   ```

2. **Update Excel template if needed:**
   - If increasing spacing, create a larger template
   - Example: 2-column spacing needs A1:J6 (10 columns) instead of A1:G6 (7 columns)

3. **Deploy changes**

### Validation

Column spacing must be:
- ✅ Non-negative integer (0, 1, 2, ...)
- ✅ Less than available columns in template
- ❌ Not negative
- ❌ Not a decimal

Invalid values fall back to default (`1`).

## Examples

### Example 1: Standard Spacing (1 column)

**Config:**
```yaml
config:
  columnSpacing: 1
```

**Template Range:** A1:G6 (7 columns)

**Result:**
```
   A              B   C         D   E         F   G
1: Benefit        -   Plan A    -   Plan B    -   Plan C
2: Doctor Visits  -   $20       -   $10       -   Free
3: ER Visit       -   $250      -   $150      -   Covered 100%
```

### Example 2: No Spacing (0 columns)

**Config:**
```yaml
config:
  columnSpacing: 0
```

**Template Range:** A1:D6 (4 columns)

**Result:**
```
   A              B         C         D
1: Benefit        Plan A    Plan B    Plan C
2: Doctor Visits  $20       $10       Free
3: ER Visit       $250      $150      Covered 100%
```

**Note:** Requires new template file with 4 columns

### Example 3: Wide Spacing (2 columns)

**Config:**
```yaml
config:
  columnSpacing: 2
```

**Template Range:** A1:J6 (10 columns)

**Result:**
```
   A              B   C    D         E   F    G         H   I    J
1: Benefit        -   -    Plan A    -   -    Plan B    -   -    Plan C
2: Doctor Visits  -   -    $20       -   -    $10       -   -    Free
3: ER Visit       -   -    $250      -   -    $150      -   -    Covered 100%
```

## API Usage

### Default Configuration (spacing = 1)

**Request:**
```bash
curl -X POST http://localhost:8080/api/documents/generate/excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "plan-comparison",
    "data": {
      "plans": [
        {
          "planName": "Plan A",
          "benefits": [
            {"name": "Doctor Visits", "value": "$20"}
          ]
        }
      ]
    }
  }' --output comparison.xlsx
```

**Server logs:**
```
DEBUG: Using configured column spacing: 1
INFO: Auto-transforming plan data into comparison matrix for plan-comparison template (spacing: 1)
```

### Custom Configuration (via YAML)

Edit `plan-comparison.yaml`:
```yaml
config:
  columnSpacing: 2
```

**Request:** (same as above)

**Server logs:**
```
DEBUG: Using configured column spacing: 2
INFO: Auto-transforming plan data into comparison matrix for plan-comparison template (spacing: 2)
```

## Troubleshooting

### Problem: Spacing configuration not being used

**Check:**
1. YAML syntax is valid
2. `columnSpacing` is under `config:` section
3. Template has been redeployed (in target/classes)
4. Check server logs for "Using configured column spacing"

### Problem: Matrix shows truncated data

**Cause:** Template too small for chosen spacing

**Solution:** Extend template range in YAML mapping
```yaml
fieldMappings:
  "A1:J6": "$.comparisonMatrix"  # Use 10 columns for 2-column spacing
```

### Problem: Config value ignored, using default

**Cause:** Invalid columnSpacing value

**Check:**
- Must be integer (not string, not decimal)
- Must be non-negative
- Check for typos in YAML

**Example of INVALID config:**
```yaml
config:
  columnSpacing: "1"     # String, not integer
  columnSpacing: 1.5     # Decimal, not integer
  columnSpacing: -1      # Negative number
```

**Example of VALID config:**
```yaml
config:
  columnSpacing: 0       # Valid
  columnSpacing: 1       # Valid
  columnSpacing: 2       # Valid
```

## Performance Considerations

Column spacing configuration has **zero performance impact**:
- ✅ No additional processing time
- ✅ No memory overhead
- ✅ Simple integer configuration lookup
- ✅ Applied during transformation (already happening)

## Future Options

Currently, spacing is configured via YAML template. Future options could include:

1. **Request-level override:**
   ```json
   {
     "data": {
       "columnSpacing": 2,
       "plans": [...]
     }
   }
   ```

2. **Environment configuration:**
   ```properties
   docgen.plan-comparison.default-column-spacing=1
   ```

3. **Named spacing presets:**
   ```yaml
   config:
     spacing: "wide"  # Converts to columnSpacing=2
   ```

Contact the team if these options are needed.

## Summary

✅ **Configured via:** YAML template `config.columnSpacing`  
✅ **Default value:** 1 (one column between plans)  
✅ **Valid values:** Non-negative integers (0, 1, 2, ...)  
✅ **No code changes needed:** Pure configuration  
✅ **Logged:** Debug logs show configured spacing  
✅ **Flexible:** Easily change without deployment  

---

**Documentation**: Column Spacing Configuration  
**Created**: 2026-02-17  
**Status**: Complete ✅
