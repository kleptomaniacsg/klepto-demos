package com.example.demoproject.model;

import java.util.List;


public class CollectionMapping {
    private String source;
    public void setSource(String source) {
        this.source = source;
    }
    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }
    public void setTargetPrefix(String targetPrefix) {
        this.targetPrefix = targetPrefix;
    }
    public void setTargetSuffix(String targetSuffix) {
        this.targetSuffix = targetSuffix;
    }
    public void setItemMappings(List<ItemFieldMapping> itemMappings) {
        this.itemMappings = itemMappings;
    }
    public void setCondition(Condition condition) {
        this.condition = condition;
    }
    private Integer maxItems;
    private String targetPrefix;
    private String targetSuffix;
    private List<ItemFieldMapping> itemMappings;
    private Condition condition;

    @Override
    public String toString() {
        return "CollectionMapping [source=" + source + ", maxItems=" + maxItems + ", targetPrefix=" + targetPrefix
                + ", targetSuffix=" + targetSuffix + ", itemMappings=" + itemMappings + ", condition=" + condition
                + "]";
    }
    // Getters
    public String getSource() { return source; }
    public Integer getMaxItems() { return maxItems; }
    public String getTargetPrefix() { return targetPrefix; }
    public String getTargetSuffix() { return targetSuffix; }
    public List<ItemFieldMapping> getItemMappings() { return itemMappings; }
    public Condition getCondition() { return condition; }
}