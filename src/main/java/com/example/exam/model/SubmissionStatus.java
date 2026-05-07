package com.example.exam.model;

public enum SubmissionStatus {
    SUBMITTED("待阅卷"),
    GRADED("已评分");

    private final String label;

    SubmissionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
