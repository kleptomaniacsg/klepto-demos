package com.example.demoproject.model;

import java.util.*;

public class DryRunReport {
    private boolean dryRun = true;
    private long timestamp = System.currentTimeMillis();
    private String configUsed;
    @Override
    public String toString() {
        return "DryRunReport [dryRun=" + dryRun + ", timestamp=" + timestamp + ", configUsed=" + configUsed
                + ", mappings=" + mappings + ", coverage=" + coverage + "]";
    }

    private List<MappingEntryReport> mappings = new ArrayList<>();
    private CoverageSummary coverage = new CoverageSummary();

    // Getters & setters
    public static class MappingEntryReport {
        private String sourcePath;
        private String targetField;
        private Object rawValue;
        public String getSourcePath() {
            return sourcePath;
        }
        public void setSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
        }
        public String getTargetField() {
            return targetField;
        }
        public void setTargetField(String targetField) {
            this.targetField = targetField;
        }
        public Object getRawValue() {
            return rawValue;
        }
        public void setRawValue(Object rawValue) {
            this.rawValue = rawValue;
        }
        public String getTransformedValue() {
            return transformedValue;
        }
        public void setTransformedValue(String transformedValue) {
            this.transformedValue = transformedValue;
        }
        public boolean isConditionPassed() {
            return conditionPassed;
        }
        public void setConditionPassed(boolean conditionPassed) {
            this.conditionPassed = conditionPassed;
        }
        public String getReasonIfSkipped() {
            return reasonIfSkipped;
        }
        public void setReasonIfSkipped(String reasonIfSkipped) {
            this.reasonIfSkipped = reasonIfSkipped;
        }
        public String getAction() {
            return action;
        }
        public void setAction(String action) {
            this.action = action;
        }
        public boolean isSensitive() {
            return isSensitive;
        }
        public void setSensitive(boolean isSensitive) {
            this.isSensitive = isSensitive;
        }
        private String transformedValue;
        @Override
        public String toString() {
            return "MappingEntryReport [sourcePath=" + sourcePath + ", targetField=" + targetField + ", rawValue="
                    + rawValue + ", transformedValue=" + transformedValue + ", conditionPassed=" + conditionPassed
                    + ", reasonIfSkipped=" + reasonIfSkipped + ", action=" + action + ", isSensitive=" + isSensitive
                    + "]";
        }
        private boolean conditionPassed;
        private String reasonIfSkipped;
        private String action; // "SET", "SKIPPED", "ERROR"
        private boolean isSensitive;

        // Getters/setters
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getConfigUsed() {
        return configUsed;
    }

    public void setConfigUsed(String configUsed) {
        this.configUsed = configUsed;
    }

    public List<MappingEntryReport> getMappings() {
        return mappings;
    }

    public void setMappings(List<MappingEntryReport> mappings) {
        this.mappings = mappings;
    }

    public CoverageSummary getCoverage() {
        return coverage;
    }

    public void setCoverage(CoverageSummary coverage) {
        this.coverage = coverage;
    }

    public static class CoverageSummary {
        private int totalMappings;
        private int applied;
        private int skipped;
        public int getTotalMappings() {
            return totalMappings;
        }
        public void setTotalMappings(int totalMappings) {
            this.totalMappings = totalMappings;
        }
        public int getApplied() {
            return applied;
        }
        public void setApplied(int applied) {
            this.applied = applied;
        }
        @Override
        public String toString() {
            return "CoverageSummary [totalMappings=" + totalMappings + ", applied=" + applied + ", skipped=" + skipped
                    + ", coveragePercent=" + coveragePercent + "]";
        }
        public int getSkipped() {
            return skipped;
        }
        public void setSkipped(int skipped) {
            this.skipped = skipped;
        }
        public double getCoveragePercent() {
            return coveragePercent;
        }
        public void setCoveragePercent(double coveragePercent) {
            this.coveragePercent = coveragePercent;
        }
        private double coveragePercent;

        // Getters/setters
    }

    // Getters/setters for top-level fields
}