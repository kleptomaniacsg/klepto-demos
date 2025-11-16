package com.example.demoproject.model;



public class ItemFieldMapping {
    private String source;
    private String targetPrefix;
    private String targetSuffix;
    private Object transform;
    private Condition condition;
    private String defaultValue;
    private CollectionMapping collection;

    @Override
    public String toString() {
        return "ItemFieldMapping [source=" + source + ", targetPrefix=" + targetPrefix + ", targetSuffix="
                + targetSuffix + ", transform=" + transform + ", condition=" + condition + ", defaultValue="
                + defaultValue + ", collection=" + collection + "]";
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setTargetPrefix(String targetPrefix) {
        this.targetPrefix = targetPrefix;
    }

    public void setTargetSuffix(String targetSuffix) {
        this.targetSuffix = targetSuffix;
    }

    public void setTransform(Object transform) {
        this.transform = transform;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setCollection(CollectionMapping collection) {
        this.collection = collection;
    }

    public boolean isNestedCollection() {
        return collection != null;
    }

    // Getters
    public String getSource() { return source; }
    public String getTargetPrefix() { return targetPrefix; }
    public String getTargetSuffix() { return targetSuffix; }
    public Object getTransform() { return transform; }
    public Condition getCondition() { return condition; }
    public String getDefaultValue() { return defaultValue; }
    public CollectionMapping getCollection() { return collection; }
}