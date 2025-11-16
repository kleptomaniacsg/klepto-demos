package com.example.demoproject.model;

import java.util.List;
import java.util.Map;

public class MappingConfig {
    private Map<String, ContextDef> contexts;
    private List<FieldMapping> mappings;

    public Map<String, ContextDef> getContexts() { return contexts; }
    public List<FieldMapping> getMappings() { return mappings; }
}