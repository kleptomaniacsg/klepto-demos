package com.example.demoproject.model;

public class ContextDef {
    private String source;

    @Override
    public String toString() {
        return "ContextDef [source=" + source + "]";
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}