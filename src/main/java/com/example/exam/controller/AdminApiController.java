package com.example.exam.controller;

import com.example.exam.dto.ApiDtos.AdminOverviewDto;
import com.example.exam.dto.ApiDtos.CreateUserRequest;
import com.example.exam.dto.ApiDtos.ExamSummaryDto;
import com.example.exam.dto.ApiDtos.ResetPasswordRequest;
import com.example.exam.dto.ApiDtos.UpdateEnabledRequest;
import com.example.exam.dto.ApiDtos.UserDto;
import com.example.exam.model.ExamStatus;
import com.example.exam.model.Role;
import com.example.exam.model.SubmissionStatus;
import com.example.exam.model.UserAccount;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamSubmissionRepository;
import com.example.exam.repository.UserAccountRepository;
import com.example.exam.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final AuthService authService;
    private final UserAccountRepository userRepository;
    private final ExamRepository examRepository;
    private final ExamSubmissionRepository submissionRepository;

    public AdminApiController(AuthService authService, UserAccountRepository userRepository,
                              ExamRepository examRepository, ExamSubmissionRepository submissionRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/overview")
    public AdminOverviewDto overview(HttpSession session) {
        authService.requireRole(session, Role.ADMIN);
        return new AdminOverviewDto(
                userRepository.findAll().stream().map(UserDto::from).toList(),
                examRepository.findAll().stream().map(ExamSummaryDto::from).toList(),
                userRepository.count(),
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByRole(Role.TEACHER),
                examRepository.countByStatus(ExamStatus.PUBLISHED),
                submissionRepository.countByStatus(SubmissionStatus.SUBMITTED));
    }

    @PostMapping("/users")
    public UserDto createUser(@RequestBody CreateUserRequest request, HttpSession session) {
        authService.requireRole(session, Role.ADMIN);
        if (userRepository.findByUsername(clean(request.username())).isPresent()) {
            throw new IllegalStateException("用户名已存在");
        }
        UserAccount user = authService.createUser(request.username(), request.password(),
                request.displayName(), request.role());
        return UserDto.from(user);
    }

    @PatchMapping("/users/{id}/enabled")
    public UserDto updateEnabled(@PathVariable Long id, @RequestBody UpdateEnabledRequest request,
                                 HttpSession session) {
        UserAccount admin = authService.requireRole(session, Role.ADMIN);
        UserAccount user = userRepository.findById(id).orElseThrow();
        if (user.getId().equals(admin.getId())) {
            throw new IllegalStateException("不能停用当前登录的管理员账号");
        }
        user.setEnabled(request.enabled());
        return UserDto.from(userRepository.save(user));
    }

    @PatchMapping("/users/{id}/password")
    public UserDto resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request,
                                 HttpSession session) {
        authService.requireRole(session, Role.ADMIN);
        UserAccount user = userRepository.findById(id).orElseThrow();
        user.setPassword(authService.encodePassword(request.newPassword()));
        return UserDto.from(userRepository.save(user));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
