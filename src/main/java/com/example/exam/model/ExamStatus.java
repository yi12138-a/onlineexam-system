package com.example.exam.model;

public enum ExamStatus {
    DRAFT("草稿"),
    PUBLISHED("已发布"),
    CLOSED("已关闭");

    private final String label;

    ExamStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
