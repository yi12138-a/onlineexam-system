package com.example.exam.controller;

import com.example.exam.dto.ApiDtos.ExamDetailDto;
import com.example.exam.dto.ApiDtos.ExamSummaryDto;
import com.example.exam.dto.ApiDtos.StudentDashboardDto;
import com.example.exam.dto.ApiDtos.SubmissionDto;
import com.example.exam.dto.ApiDtos.SubmitExamRequest;
import com.example.exam.model.Exam;
import com.example.exam.model.ExamStatus;
import com.example.exam.model.ExamSubmission;
import com.example.exam.model.Role;
import com.example.exam.model.UserAccount;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamSubmissionRepository;
import com.example.exam.service.AuthService;
import com.example.exam.service.ExamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentApiController {
    private final AuthService authService;
    private final ExamRepository examRepository;
    private final ExamSubmissionRepository submissionRepository;
    private final ExamService examService;

    public StudentApiController(AuthService authService, ExamRepository examRepository,
                                ExamSubmissionRepository submissionRepository, ExamService examService) {
        this.authService = authService;
        this.examRepository = examRepository;
        this.submissionRepository = submissionRepository;
        this.examService = examService;
    }

    @GetMapping("/dashboard")
    public StudentDashboardDto dashboard(HttpSession session) {
        UserAccount student = authService.requireRole(session, Role.STUDENT);
        var submissions = submissionRepository.findByStudentOrderBySubmittedAtDesc(student);
        Map<Long, Long> submittedResultIds = submissions.stream()
                .collect(Collectors.toMap(
                        submission -> submission.getExam().getId(),
                        ExamSubmission::getId,
                        (first, ignored) -> first));
        return new StudentDashboardDto(
                examRepository.findByStatusOrderByCreatedAtDesc(ExamStatus.PUBLISHED).stream()
                        .map(ExamSummaryDto::from)
                        .toList(),
                submissions.stream()
                        .map(submission -> SubmissionDto.from(submission, false, false))
                        .toList(),
                submittedResultIds);
    }

    @GetMapping("/exams/{id}")
    public ExamDetailDto exam(@PathVariable Long id, HttpSession session) {
        UserAccount student = authService.requireRole(session, Role.STUDENT);
        Exam exam = examRepository.findById(id).orElseThrow();
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new IllegalStateException("该考试暂不可参加");
        }
        if (submissionRepository.existsByExamAndStudent(exam, student)) {
            throw new IllegalStateException("该考试已经提交过，不能重复作答");
        }
        return ExamDetailDto.from(exam, false);
    }

    @PostMapping("/exams/{id}/submissions")
    public SubmissionDto submit(@PathVariable Long id, @RequestBody SubmitExamRequest request, HttpSession session) {
        UserAccount student = authService.requireRole(session, Role.STUDENT);
        Map<String, String> answers = new HashMap<>();
        Map<Long, String> requestAnswers = request.answers() == null ? Map.of() : request.answers();
        requestAnswers.forEach((questionId, answer) -> answers.put("question_" + questionId, answer));
        ExamSubmission submission = examService.submitExam(id, student, answers);
        return SubmissionDto.from(submission, true, true);
    }

    @GetMapping("/submissions/{id}")
    public SubmissionDto result(@PathVariable Long id, HttpSession session) {
        UserAccount student = authService.requireRole(session, Role.STUDENT);
        ExamSubmission submission = submissionRepository.findById(id).orElseThrow();
        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new IllegalStateException("只能查看自己的考试结果");
        }
        return SubmissionDto.from(submission, true, true);
    }
}
