package com.example.exam.controller;

import com.example.exam.dto.ApiDtos.CreateExamRequest;
import com.example.exam.dto.ApiDtos.CreateQuestionRequest;
import com.example.exam.dto.ApiDtos.ExamDetailDto;
import com.example.exam.dto.ApiDtos.ExamSummaryDto;
import com.example.exam.dto.ApiDtos.GradeSubmissionRequest;
import com.example.exam.dto.ApiDtos.ImportQuestionsRequest;
import com.example.exam.dto.ApiDtos.ImportQuestionsResponse;
import com.example.exam.dto.ApiDtos.QuestionDto;
import com.example.exam.dto.ApiDtos.SubmissionDto;
import com.example.exam.dto.ApiDtos.TeacherDashboardDto;
import com.example.exam.dto.ApiDtos.UpdateExamStatusRequest;
import com.example.exam.model.Exam;
import com.example.exam.model.ExamStatus;
import com.example.exam.model.ExamSubmission;
import com.example.exam.model.Question;
import com.example.exam.model.QuestionType;
import com.example.exam.model.Role;
import com.example.exam.model.UserAccount;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamSubmissionRepository;
import com.example.exam.repository.QuestionRepository;
import com.example.exam.service.AuthService;
import com.example.exam.service.ExamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherApiController {
    private final AuthService authService;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final ExamSubmissionRepository submissionRepository;
    private final ExamService examService;

    public TeacherApiController(AuthService authService, QuestionRepository questionRepository,
                                ExamRepository examRepository, ExamSubmissionRepository submissionRepository,
                                ExamService examService) {
        this.authService = authService;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.submissionRepository = submissionRepository;
        this.examService = examService;
    }

    @GetMapping("/dashboard")
    public TeacherDashboardDto dashboard(HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        List<Exam> exams = examRepository.findByTeacherOrderByCreatedAtDesc(teacher);
        long submissionCount = exams.stream()
                .mapToLong(exam -> submissionRepository.findByExamOrderBySubmittedAtDesc(exam).size())
                .sum();
        return new TeacherDashboardDto(exams.stream().map(ExamSummaryDto::from).toList(),
                questionRepository.findByCreatorOrderByIdDesc(teacher).size(), submissionCount);
    }

    @GetMapping("/questions")
    public List<QuestionDto> questions(HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        return questionRepository.findByCreatorOrderByIdDesc(teacher).stream()
                .map(question -> QuestionDto.from(question, true))
                .toList();
    }

    @PostMapping("/questions")
    public QuestionDto createQuestion(@RequestBody CreateQuestionRequest request, HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        validateQuestion(request.type(), request.title(), request.correctAnswer());
        Question question = questionRepository.save(new Question(request.type(), clean(request.title()),
                emptyToNull(request.optionA()), emptyToNull(request.optionB()), emptyToNull(request.optionC()),
                emptyToNull(request.optionD()), clean(request.correctAnswer()), Math.max(request.score(), 1),
                teacher));
        return QuestionDto.from(question, true);
    }

    @PostMapping("/questions/import")
    public ImportQuestionsResponse importQuestions(@RequestBody ImportQuestionsRequest request, HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        int imported = 0;
        for (String line : clean(request.payload()).split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] cells = parseImportLine(trimmed);
            if (cells.length < 8) {
                continue;
            }
            if (isImportHeader(cells)) {
                continue;
            }
            try {
                QuestionType type = parseType(cells[0]);
                validateQuestion(type, cells[1], cells[6]);
                questionRepository.save(new Question(type, clean(cells[1]), emptyToNull(cells[2]),
                        emptyToNull(cells[3]), emptyToNull(cells[4]), emptyToNull(cells[5]),
                        clean(cells[6]), parseInt(cells[7], 5), teacher));
                imported++;
            } catch (IllegalStateException ignored) {
                // Skip malformed rows so one bad line does not block a full paper import.
            }
        }
        return new ImportQuestionsResponse(imported);
    }

    @DeleteMapping("/questions/{id}")
    public void deleteQuestion(@PathVariable Long id, HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        Question question = questionRepository.findById(id).orElseThrow();
        if (!question.getCreator().getId().equals(teacher.getId())) {
            throw new IllegalStateException("只能删除自己创建的题目");
        }
        boolean usedByExam = examRepository.findByTeacherOrderByCreatedAtDesc(teacher).stream()
                .anyMatch(exam -> exam.getQuestions().stream().anyMatch(item -> item.getId().equals(id)));
        if (usedByExam) {
            throw new IllegalStateException("题目已被试卷使用，不能删除");
        }
        questionRepository.delete(question);
    }

    @GetMapping("/exams")
    public List<ExamSummaryDto> exams(HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        return examRepository.findByTeacherOrderByCreatedAtDesc(teacher).stream()
                .map(ExamSummaryDto::from)
                .toList();
    }

    @PostMapping("/exams")
    public ExamDetailDto createExam(@RequestBody CreateExamRequest request, HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        Exam exam = examService.createExam(request.title(), request.description(),
                Math.max(request.durationMinutes(), 10), request.questionIds(), teacher);
        return ExamDetailDto.from(exam, true);
    }

    @PatchMapping("/exams/{id}/status")
    public ExamSummaryDto updateStatus(@PathVariable Long id, @RequestBody UpdateExamStatusRequest request,
                                       HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        Exam exam = examRepository.findById(id).orElseThrow();
        if (!exam.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalStateException("只能管理自己创建的试卷");
        }
        ExamStatus status = request.status();
        if (status == null) {
            throw new IllegalStateException("试卷状态不能为空");
        }
        exam.setStatus(status);
        return ExamSummaryDto.from(examRepository.save(exam));
    }

    @GetMapping("/exams/{id}")
    public ExamDetailDto exam(@PathVariable Long id, HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        Exam exam = teacherExam(id, teacher);
        return ExamDetailDto.from(exam, true);
    }

    @GetMapping("/exams/{id}/submissions")
    public List<SubmissionDto> submissions(@PathVariable Long id, HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        Exam exam = teacherExam(id, teacher);
        return submissionRepository.findByExamOrderBySubmittedAtDesc(exam).stream()
                .map(submission -> SubmissionDto.from(submission, false, false))
                .toList();
    }

    @GetMapping("/submissions/{id}")
    public SubmissionDto submission(@PathVariable Long id, HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        ExamSubmission submission = submissionRepository.findById(id).orElseThrow();
        if (!submission.getExam().getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalStateException("只能查看自己试卷的答卷");
        }
        return SubmissionDto.from(submission, true, true);
    }

    @PostMapping("/submissions/{id}/grade")
    public SubmissionDto grade(@PathVariable Long id, @RequestBody GradeSubmissionRequest request,
                               HttpSession session) {
        UserAccount teacher = authService.requireRole(session, Role.TEACHER);
        Map<String, String> scoreMap = new HashMap<>();
        Map<String, String> commentMap = new HashMap<>();
        Map<Long, Integer> scores = request.scores() == null ? Map.of() : request.scores();
        Map<Long, String> comments = request.comments() == null ? Map.of() : request.comments();
        scores.forEach((answerId, score) -> scoreMap.put("score_" + answerId, String.valueOf(score)));
        comments.forEach((answerId, comment) -> commentMap.put("comment_" + answerId, comment));
        examService.gradeSubmission(id, teacher, scoreMap, commentMap);
        return SubmissionDto.from(submissionRepository.findById(id).orElseThrow(), true, true);
    }

    private Exam teacherExam(Long id, UserAccount teacher) {
        Exam exam = examRepository.findById(id).orElseThrow();
        if (!exam.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalStateException("只能查看自己创建的试卷");
        }
        return exam;
    }

    private void validateQuestion(QuestionType type, String title, String correctAnswer) {
        if (type == null) {
            throw new IllegalStateException("题型不能为空");
        }
        if (clean(title).isEmpty()) {
            throw new IllegalStateException("题干不能为空");
        }
        if (clean(correctAnswer).isEmpty()) {
            throw new IllegalStateException("标准答案不能为空");
        }
        if (type == QuestionType.SINGLE_CHOICE && !List.of("A", "B", "C", "D").contains(clean(correctAnswer).toUpperCase())) {
            throw new IllegalStateException("选择题标准答案必须是 A、B、C 或 D");
        }
    }

    private QuestionType parseType(String raw) {
        return switch (clean(raw)) {
            case "选择题", "选择", "SINGLE_CHOICE" -> QuestionType.SINGLE_CHOICE;
            case "判断题", "判断", "TRUE_FALSE" -> QuestionType.TRUE_FALSE;
            case "填空题", "填空", "FILL_BLANK" -> QuestionType.FILL_BLANK;
            case "大题", "问答题", "ESSAY" -> QuestionType.ESSAY;
            default -> QuestionType.SINGLE_CHOICE;
        };
    }

    private String[] parseImportLine(String line) {
        if (line.contains("|")) {
            return line.split("\\|", -1);
        }
        return parseCsvLine(line);
    }

    private String[] parseCsvLine(String line) {
        List<String> cells = new java.util.ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (ch == ',' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
                continue;
            }
            cell.append(ch);
        }
        cells.add(cell.toString());
        return cells.toArray(String[]::new);
    }

    private boolean isImportHeader(String[] cells) {
        return stripBom(clean(cells[0])).equals("题型") || clean(cells[6]).equals("答案");
    }

    private String emptyToNull(String value) {
        String cleaned = clean(value);
        return cleaned.isEmpty() ? null : cleaned;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(clean(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
