package com.logai.deploy.analysis;

public class ErrorSummary {

    private final String signature;
    private final String sampleLine;
    private final String firstOccurrence;

    private int count;
    private String lastOccurrence;

    public ErrorSummary(
            String signature,
            String sampleLine,
            String firstOccurrence) {

        this.signature = signature;
        this.sampleLine = sampleLine;
        this.firstOccurrence = firstOccurrence;
        this.lastOccurrence = firstOccurrence;
        this.count = 1;
    }

    public void addOccurrence(String timestamp) {
        count++;
        lastOccurrence = timestamp;
    }

    public String getSignature() {
        return signature;
    }

    public String getSampleLine() {
        return sampleLine;
    }

    public String getFirstOccurrence() {
        return firstOccurrence;
    }

    public String getLastOccurrence() {
        return lastOccurrence;
    }

    public int getCount() {
        return count;
    }
}