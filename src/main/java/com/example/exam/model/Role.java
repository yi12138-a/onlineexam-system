package com.example.exam.model;

public enum Role {
    STUDENT("学生"),
    TEACHER("教师"),
    ADMIN("管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
