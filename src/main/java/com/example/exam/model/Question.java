package com.example.exam.model;

import jakarta.persistence.*;

@Entity
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String groupTitle;

    private Integer groupOrder;

    @Column(length = 3000)
    private String imageUrls;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    @Column(columnDefinition = "TEXT")
    private String correctAnswer;

    private int score;

    @ManyToOne(optional = false)
    private UserAccount creator;

    protected Question() {
    }

    public Question(QuestionType type, String title, String optionA, String optionB, String optionC,
                    String optionD, String correctAnswer, int score, UserAccount creator) {
        this(type, title, null, null, null, optionA, optionB, optionC, optionD, correctAnswer, score, creator);
    }

    public Question(QuestionType type, String title, String groupTitle, Integer groupOrder, String imageUrls,
                    String optionA, String optionB, String optionC, String optionD, String correctAnswer,
                    int score, UserAccount creator) {
        this.type = type;
        this.title = title;
        this.groupTitle = groupTitle;
        this.groupOrder = groupOrder;
        this.imageUrls = imageUrls;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.score = score;
        this.creator = creator;
    }

    public boolean isObjective() {
        return type == QuestionType.SINGLE_CHOICE || type == QuestionType.TRUE_FALSE || type == QuestionType.FILL_BLANK;
    }

    public Long getId() {
        return id;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    public Integer getGroupOrder() {
        return groupOrder;
    }

    public void setGroupOrder(Integer groupOrder) {
        this.groupOrder = groupOrder;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public UserAccount getCreator() {
        return creator;
    }

    public void setCreator(UserAccount creator) {
        this.creator = creator;
    }
}
