package com.example.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1200)
    private String description;

    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status = ExamStatus.DRAFT;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(optional = false)
    private UserAccount teacher;

    @ManyToMany
    @OrderColumn(name = "question_order")
    private List<Question> questions = new ArrayList<>();

    protected Exam() {
    }

    public Exam(String title, String description, int durationMinutes, UserAccount teacher, List<Question> questions) {
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.teacher = teacher;
        this.questions = questions;
    }

    public int getTotalScore() {
        return questions.stream().mapToInt(Question::getScore).sum();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserAccount getTeacher() {
        return teacher;
    }

    public void setTeacher(UserAccount teacher) {
        this.teacher = teacher;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}
