package com.example.demoproject.service;

import com.example.demoproject.SensitiveFieldDetector;
import com.example.demoproject.model.*;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PdfFieldMapper {
    private boolean dryRun = false;

    public PdfFieldMapper dryRun(boolean enabled) {
        this.dryRun = enabled;
        return this;
    }

    public void mapJsonToPdf(String yamlPath, String jsonInput, String pdfPath, String outputPath) throws Exception {
        // Load config
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(
            new org.yaml.snakeyaml.constructor.Constructor(MappingConfig.class)
        );
        try (InputStream in = new FileInputStream(yamlPath)) {
            MappingConfig config = yaml.load(in);
            DocumentContext rootJson = JsonPath.parse(jsonInput);

            // Pre-evaluate all contexts
            Map<String, Object> contextCache = new HashMap<>();
            if (config.getContexts() != null) {
                for (Map.Entry<String, ContextDef> entry : config.getContexts().entrySet()) {
                    String name = entry.getKey();
                    String source = entry.getValue().getSource();
                    try {
                        Object value = resolveValue(source, rootJson, contextCache);
                        contextCache.put(name, value);
                    } catch (Exception e) {
                        contextCache.put(name, null);
                    }
                }
            }

            PDDocument doc = null;
            PDAcroForm form = null;
         

            // Process all mappings
            for (FieldMapping mapping : config.getMappings()) {
                if (mapping.isScalar()) {
                    processScalar(mapping, rootJson, contextCache, form);
                } else if (mapping.isCollection()) {
                    processCollection(
                        mapping.getCollection(),
                        rootJson,
                        contextCache,
                        form,
                        "", null, 0
                    );
                }
            }

            if (!dryRun) {
                doc.save(outputPath);
                doc.close();
                System.out.println("✅ PDF saved: " + outputPath);
            } else {
                System.out.println("🎯 Dry-run complete.");
            }
        }
    }

    private Object resolveValue(String sourcePath, DocumentContext rootCtx, Map<String, Object> contextCache) {
        if (sourcePath.startsWith("@")) {
            String[] parts = sourcePath.substring(1).split("\.", 2);
            String contextName = parts[0];
            String subPath = parts.length > 1 ? parts[1] : null;

            Object contextValue = contextCache.get(contextName);
            if (contextValue == null) return null;

            if (subPath == null) {
                return contextValue;
            }

            try {
                String jsonPath = subPath.startsWith("$") ? subPath : "$." + subPath;
                return JsonPath.parse(contextValue).read(jsonPath);
            } catch (Exception e) {
                return null;
            }
        } else {
            try {
                String jsonPath = sourcePath.startsWith("$") ? sourcePath : "$." + sourcePath;
                return rootCtx.read(jsonPath);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private void processScalar(FieldMapping mapping, DocumentContext rootCtx,
                              Map<String, Object> contextCache, PDAcroForm form) throws IOException {
        Object rawValue = resolveValue(mapping.getSource(), rootCtx, contextCache);
        boolean conditionPassed = ConditionEvaluator.evaluate(
            mapping.getCondition(), 
            rootCtx, 
            rawValue
        );

        if (!conditionPassed) {
            if (dryRun) {
                System.out.println("⏭️  Skipped (condition): " + mapping.getTarget());
            }
            return;
        }

        Object transformed = DataTransformer.applyTransform(rawValue, mapping.getTransform());
        String finalValue = (transformed != null) ? transformed.toString() : "";
        if (finalValue.trim().isEmpty() && mapping.getDefaultValue() != null) {
            finalValue = mapping.getDefaultValue();
        }

        if (dryRun) {
            String safeVal = SensitiveFieldDetector.isSensitive(mapping.getTarget()) ?
                SensitiveFieldDetector.maskValue(finalValue, mapping.getTarget().contains("email")) :
                finalValue;
            System.out.println("✅ " + mapping.getTarget() + " = '" + safeVal + "'");
        } else if (form != null) {
            PDField field = form.getField(mapping.getTarget());
            if (field != null) {
                field.setValue(finalValue);
            }
        }
    }

    private void processCollection(
            CollectionMapping coll,
            DocumentContext rootJson,
            Map<String, Object> contextCache,
            PDAcroForm form,
            String currentPrefix,
            Object parentItem,
            int outerIndex
    ) throws Exception {
        String resolvedPrefix = coll.getTargetPrefix() != null ?
            coll.getTargetPrefix().replace("${index}", String.valueOf(outerIndex)) :
            currentPrefix;

        List<?> items;
        if (coll.getSource().startsWith("@")) {
            String contextName = coll.getSource().substring(1);
            Object cached = contextCache.get(contextName);
            items = (cached instanceof List) ? (List<?>) cached : Collections.emptyList();
        } else {
            Object raw = resolveValue(coll.getSource(), rootJson, contextCache);
            items = (raw instanceof List) ? (List<?>) raw : Collections.emptyList();
        }

        if (items == null) items = Collections.emptyList();

        // Apply collection-level filter
        List<Object> filteredItems = new ArrayList<>();
        for (Object item : items) {
            boolean passes = ConditionEvaluator.evaluate(
                coll.getCondition(),
                JsonPath.parse(item),
                item
            );
            if (passes) {
                filteredItems.add(item);
            }
        }

        int limit = coll.getMaxItems() != null ?
            Math.min(filteredItems.size(), coll.getMaxItems()) :
            filteredItems.size();

        for (int i = 0; i < limit; i++) {
            Object item = filteredItems.get(i);
            int innerIndex = i + 1;

            for (ItemFieldMapping itemMap : coll.getItemMappings()) {
                if (itemMap.isNestedCollection()) {
                    String innerPrefix = resolvedPrefix;
                    if (itemMap.getCollection().getTargetPrefix() != null) {
                        innerPrefix = itemMap.getCollection().getTargetPrefix()
                            .replace("${index}", String.valueOf(innerIndex));
                    }
                    processCollection(
                        itemMap.getCollection(),
                        rootJson,
                        contextCache,
                        form,
                        innerPrefix,
                        item,
                        innerIndex
                    );
                } else {
                    Object rawValue = null;
                    try {
                        rawValue = JsonPath.parse(item).read(itemMap.getSource());
                    } catch (Exception e) {
                        rawValue = null;
                    }

                    boolean fieldConditionPassed = ConditionEvaluator.evaluate(
                        itemMap.getCondition(),
                        JsonPath.parse(item),
                        rawValue
                    );

                    if (!fieldConditionPassed) continue;

                    Object transformed = DataTransformer.applyTransform(rawValue, itemMap.getTransform());
                    String finalValue = (transformed != null) ? transformed.toString() : "";
                    if (finalValue.trim().isEmpty() && itemMap.getDefaultValue() != null) {
                        finalValue = itemMap.getDefaultValue();
                    }

                    String suffix = itemMap.getTargetSuffix() != null ? itemMap.getTargetSuffix() : "";
                    String targetField = resolvedPrefix + innerIndex + suffix;

                    if (dryRun) {
                        String safeVal = SensitiveFieldDetector.isSensitive(targetField) ?
                            SensitiveFieldDetector.maskValue(finalValue, targetField.contains("email")) :
                            finalValue;
                        System.out.println("✅ " + targetField + " = '" + safeVal + "'");
                    } else if (form != null) {
                        PDField field = form.getField(targetField);
                        if (field != null) field.setValue(finalValue);
                    }
                }
            }
        }
    }
}