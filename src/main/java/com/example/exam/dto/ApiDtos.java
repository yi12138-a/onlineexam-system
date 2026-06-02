package com.example.exam.dto;

import com.example.exam.model.Answer;
import com.example.exam.model.Exam;
import com.example.exam.model.ExamStatus;
import com.example.exam.model.ExamSubmission;
import com.example.exam.model.Question;
import com.example.exam.model.QuestionType;
import com.example.exam.model.Role;
import com.example.exam.model.SubmissionStatus;
import com.example.exam.model.UserAccount;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record OptionDto(String value, String label) {
    }

    public record UserDto(Long id, String username, String displayName, Role role, String roleLabel, boolean enabled) {
        public static UserDto from(UserAccount user) {
            return new UserDto(user.getId(), user.getUsername(), user.getDisplayName(),
                    user.getRole(), user.getRole().getLabel(), user.isEnabled());
        }
    }

    public record QuestionDto(Long id, QuestionType type, String typeLabel, String title, String optionA,
                              String optionB, String optionC, String optionD, String correctAnswer,
                              int score, Long creatorId, String groupTitle, Integer groupOrder,
                              List<String> imageUrls) {
        public static QuestionDto from(Question question, boolean includeAnswer) {
            return new QuestionDto(question.getId(), question.getType(), question.getType().getLabel(),
                    question.getTitle(), question.getOptionA(), question.getOptionB(), question.getOptionC(),
                    question.getOptionD(), includeAnswer ? question.getCorrectAnswer() : null,
                    question.getScore(), question.getCreator().getId(), question.getGroupTitle(),
                    question.getGroupOrder(), imageUrls(question.getImageUrls()));
        }

        private static List<String> imageUrls(String raw) {
            if (raw == null || raw.isBlank()) {
                return List.of();
            }
            return Arrays.stream(raw.split("\\R"))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .toList();
        }
    }

    public record ExamSummaryDto(Long id, String title, String description, int durationMinutes,
                                 ExamStatus status, String statusLabel, LocalDateTime createdAt,
                                 String teacherName, int questionCount, int totalScore) {
        public static ExamSummaryDto from(Exam exam) {
            return new ExamSummaryDto(exam.getId(), exam.getTitle(), exam.getDescription(),
                    exam.getDurationMinutes(), exam.getStatus(), exam.getStatus().getLabel(),
                    exam.getCreatedAt(), exam.getTeacher().getDisplayName(),
                    exam.getQuestions().size(), exam.getTotalScore());
        }
    }

    public record ExamDetailDto(Long id, String title, String description, int durationMinutes,
                                ExamStatus status, String statusLabel, LocalDateTime createdAt,
                                String teacherName, int totalScore, List<QuestionDto> questions) {
        public static ExamDetailDto from(Exam exam, boolean includeAnswers) {
            return new ExamDetailDto(exam.getId(), exam.getTitle(), exam.getDescription(),
                    exam.getDurationMinutes(), exam.getStatus(), exam.getStatus().getLabel(),
                    exam.getCreatedAt(), exam.getTeacher().getDisplayName(), exam.getTotalScore(),
                    exam.getQuestions().stream()
                            .map(question -> QuestionDto.from(question, includeAnswers))
                            .toList());
        }
    }

    public record AnswerDto(Long id, QuestionDto question, String answerText, Integer score,
                            String teacherComment) {
        public static AnswerDto from(Answer answer, boolean includeCorrectAnswer) {
            return new AnswerDto(answer.getId(), QuestionDto.from(answer.getQuestion(), includeCorrectAnswer),
                    answer.getAnswerText(), answer.getScore(), answer.getTeacherComment());
        }
    }

    public record SubmissionDto(Long id, ExamSummaryDto exam, UserDto student, LocalDateTime submittedAt,
                                int objectiveScore, int subjectiveScore, int totalScore,
                                SubmissionStatus status, String statusLabel, List<AnswerDto> answers) {
        public static SubmissionDto from(ExamSubmission submission, boolean includeAnswers,
                                         boolean includeCorrectAnswers) {
            List<AnswerDto> answerDtos = includeAnswers
                    ? submission.getAnswers().stream()
                            .map(answer -> AnswerDto.from(answer, includeCorrectAnswers))
                            .toList()
                    : List.of();
            return new SubmissionDto(submission.getId(), ExamSummaryDto.from(submission.getExam()),
                    UserDto.from(submission.getStudent()), submission.getSubmittedAt(),
                    submission.getObjectiveScore(), submission.getSubjectiveScore(),
                    submission.getTotalScore(), submission.getStatus(), submission.getStatus().getLabel(),
                    answerDtos);
        }
    }

    public record StudentDashboardDto(List<ExamSummaryDto> exams, List<SubmissionDto> submissions,
                                      Map<Long, Long> submittedResultIds) {
    }

    public record TeacherDashboardDto(List<ExamSummaryDto> exams, int questionCount, long submissionCount) {
    }

    public record AdminOverviewDto(List<UserDto> users, List<ExamSummaryDto> exams, long totalUsers,
                                   long studentCount, long teacherCount, long publishedExamCount,
                                   long pendingGradeCount) {
    }

    public record MetaDto(List<OptionDto> roles, List<OptionDto> questionTypes,
                          List<OptionDto> examStatuses, List<OptionDto> submissionStatuses) {
        public static MetaDto current() {
            return new MetaDto(options(Role.values()), options(QuestionType.values()),
                    options(ExamStatus.values()), options(SubmissionStatus.values()));
        }

        private static <E extends Enum<E>> List<OptionDto> options(E[] values) {
            return Arrays.stream(values)
                    .map(value -> new OptionDto(value.name(), label(value)))
                    .toList();
        }

        private static String label(Enum<?> value) {
            if (value instanceof Role role) {
                return role.getLabel();
            }
            if (value instanceof QuestionType type) {
                return type.getLabel();
            }
            if (value instanceof ExamStatus status) {
                return status.getLabel();
            }
            if (value instanceof SubmissionStatus status) {
                return status.getLabel();
            }
            return value.name();
        }
    }

    public record LoginRequest(String username, String password, Role role) {
    }

    public record RegisterStudentRequest(String username, String displayName, String password) {
    }

    public record CreateUserRequest(String username, String password, String displayName, Role role) {
    }

    public record UpdateEnabledRequest(boolean enabled) {
    }

    public record ResetPasswordRequest(String newPassword) {
    }

    public record CreateQuestionRequest(QuestionType type, String title, String groupTitle, Integer groupOrder,
                                        List<String> imageUrls, String optionA, String optionB,
                                        String optionC, String optionD, String correctAnswer, int score) {
    }

    public record ImportQuestionsRequest(String payload) {
    }

    public record ImportQuestionsResponse(int imported) {
    }

    public record BatchDeleteQuestionsRequest(List<Long> ids) {
    }

    public record BatchDeleteQuestionsResponse(int deleted) {
    }

    public record CreateExamRequest(String title, String description, int durationMinutes, List<Long> questionIds) {
    }

    public record UpdateExamStatusRequest(ExamStatus status) {
    }

    public record SubmitExamRequest(Map<Long, String> answers) {
    }

    public record GradeSubmissionRequest(Map<Long, Integer> scores, Map<Long, String> comments) {
    }

    public record ErrorResponse(int status, String error, String message, String path) {
    }
}
