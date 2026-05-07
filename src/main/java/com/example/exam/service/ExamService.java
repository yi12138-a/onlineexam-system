package com.example.exam.service;

import com.example.exam.model.*;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamSubmissionRepository;
import com.example.exam.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExamService {
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamSubmissionRepository submissionRepository;

    public ExamService(ExamRepository examRepository, QuestionRepository questionRepository,
                       ExamSubmissionRepository submissionRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public Exam createExam(String title, String description, int durationMinutes,
                           List<Long> questionIds, UserAccount teacher) {
        String cleanTitle = required(title, "考试名称不能为空");
        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalStateException("请至少选择一道题目");
        }
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(questionIds));
        Map<Long, Question> questionMap = questionRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<Question> questions = distinctIds.stream()
                .map(questionMap::get)
                .peek(question -> {
                    if (question == null || !question.getCreator().getId().equals(teacher.getId())) {
                        throw new IllegalStateException("试卷只能选择自己题库中的题目");
                    }
                })
                .toList();
        if (questions.isEmpty()) {
            throw new IllegalStateException("请至少选择一道题目");
        }
        Exam exam = new Exam(cleanTitle, clean(description), Math.max(durationMinutes, 10), teacher, questions);
        return examRepository.save(exam);
    }

    @Transactional
    public ExamSubmission submitExam(Long examId, UserAccount student, Map<String, String> answerMap) {
        Exam exam = examRepository.findById(examId).orElseThrow();
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new IllegalStateException("该考试尚未发布或已关闭");
        }
        if (submissionRepository.existsByExamAndStudent(exam, student)) {
            throw new IllegalStateException("该考试已提交，不能重复交卷");
        }

        List<Answer> answers = new ArrayList<>();
        int objectiveScore = 0;
        for (Question question : exam.getQuestions()) {
            String value = limit(clean(answerMap.get("question_" + question.getId())), 3000);
            Integer score = null;
            if (question.isObjective()) {
                score = matches(question, value) ? question.getScore() : 0;
                objectiveScore += score;
            }
            answers.add(new Answer(question, value, score));
        }

        ExamSubmission submission = new ExamSubmission(exam, student, answers, objectiveScore);
        if (!submission.hasEssayQuestion()) {
            submission.setStatus(SubmissionStatus.GRADED);
        }
        return submissionRepository.save(submission);
    }

    @Transactional
    public void gradeSubmission(Long submissionId, UserAccount teacher, Map<String, String> scoreMap, Map<String, String> commentMap) {
        ExamSubmission submission = submissionRepository.findById(submissionId).orElseThrow();
        if (!submission.getExam().getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalStateException("只能批阅自己试卷的答卷");
        }
        int objectiveScore = 0;
        int subjectiveScore = 0;
        for (Answer answer : submission.getAnswers()) {
            Question question = answer.getQuestion();
            if (question.isObjective()) {
                objectiveScore += answer.getScore() == null ? 0 : answer.getScore();
                continue;
            }

            int score = parseScore(scoreMap.get("score_" + answer.getId()), question.getScore());
            answer.setScore(score);
            answer.setTeacherComment(limit(clean(commentMap.get("comment_" + answer.getId())), 1000));
            subjectiveScore += score;
        }
        submission.setObjectiveScore(objectiveScore);
        submission.setSubjectiveScore(subjectiveScore);
        submission.setTotalScore(objectiveScore + subjectiveScore);
        submission.setStatus(SubmissionStatus.GRADED);
        submissionRepository.save(submission);
    }

    private boolean matches(Question question, String value) {
        String expected = normalize(question.getCorrectAnswer());
        String actual = normalize(value);
        return switch (question.getType()) {
            case SINGLE_CHOICE -> expected.equalsIgnoreCase(actual);
            case TRUE_FALSE -> normalizeTruth(expected).equals(normalizeTruth(actual));
            case FILL_BLANK -> expected.equals(actual);
            case ESSAY -> false;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeTruth(String value) {
        return switch (value.toLowerCase()) {
            case "true", "t", "yes", "正确", "对", "是" -> "true";
            case "false", "f", "no", "错误", "错", "否" -> "false";
            default -> value.toLowerCase();
        };
    }

    private int parseScore(String raw, int max) {
        try {
            int score = Integer.parseInt(raw);
            return Math.max(0, Math.min(max, score));
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private String required(String value, String message) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            throw new IllegalStateException(message);
        }
        return cleaned;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
