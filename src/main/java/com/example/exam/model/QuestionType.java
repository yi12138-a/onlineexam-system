package com.example.exam.model;

public enum QuestionType {
    SINGLE_CHOICE("选择题"),
    TRUE_FALSE("判断题"),
    FILL_BLANK("填空题"),
    ESSAY("大题");

    private final String label;

    QuestionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
