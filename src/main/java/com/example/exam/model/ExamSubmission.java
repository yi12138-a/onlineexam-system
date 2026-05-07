package com.example.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ExamSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Exam exam;

    @ManyToOne(optional = false)
    private UserAccount student;

    private LocalDateTime submittedAt = LocalDateTime.now();

    private int objectiveScore;
    private int subjectiveScore;
    private int totalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "submission_id")
    @OrderColumn(name = "answer_order")
    private List<Answer> answers = new ArrayList<>();

    protected ExamSubmission() {
    }

    public ExamSubmission(Exam exam, UserAccount student, List<Answer> answers, int objectiveScore) {
        this.exam = exam;
        this.student = student;
        this.answers = answers;
        this.objectiveScore = objectiveScore;
        this.totalScore = objectiveScore;
    }

    public boolean hasEssayQuestion() {
        return answers.stream().anyMatch(answer -> !answer.getQuestion().isObjective());
    }

    public Long getId() {
        return id;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public UserAccount getStudent() {
        return student;
    }

    public void setStudent(UserAccount student) {
        this.student = student;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public int getObjectiveScore() {
        return objectiveScore;
    }

    public void setObjectiveScore(int objectiveScore) {
        this.objectiveScore = objectiveScore;
    }

    public int getSubjectiveScore() {
        return subjectiveScore;
    }

    public void setSubjectiveScore(int subjectiveScore) {
        this.subjectiveScore = subjectiveScore;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }
}
