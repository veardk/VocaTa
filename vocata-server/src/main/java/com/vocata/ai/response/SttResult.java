package com.vocata.ai.response;

/**
 * STT结果封装类
 */
public class SttResult {
    private String text;
    private boolean isFinal;
    private double confidence;

    public SttResult(String text, boolean isFinal, double confidence) {
        this.text = text;
        this.isFinal = isFinal;
        this.confidence = confidence;
    }

    // Getters
    public String getText() {
        return text;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public double getConfidence() {
        return confidence;
    }
}