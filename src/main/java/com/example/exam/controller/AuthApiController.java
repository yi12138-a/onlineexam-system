package com.example.exam.controller;

import com.example.exam.dto.ApiDtos.LoginRequest;
import com.example.exam.dto.ApiDtos.MetaDto;
import com.example.exam.dto.ApiDtos.RegisterStudentRequest;
import com.example.exam.dto.ApiDtos.UserDto;
import com.example.exam.model.UserAccount;
import com.example.exam.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthApiController {
    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/meta")
    public MetaDto meta() {
        return MetaDto.current();
    }

    @GetMapping("/auth/me")
    public UserDto me(HttpSession session) {
        UserAccount user = authService.currentUser(session)
                .orElseThrow(() -> new IllegalStateException("请先登录"));
        return UserDto.from(user);
    }

    @PostMapping("/auth/login")
    public UserDto login(@RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpSession session) {
        UserAccount user = authService.login(request.username(), request.password(), request.role())
                .orElseThrow(() -> new IllegalStateException("账号、密码或身份选择不正确"));
        httpRequest.changeSessionId();
        authService.signIn(session, user);
        return UserDto.from(user);
    }

    @PostMapping("/auth/register")
    public UserDto register(@RequestBody RegisterStudentRequest request, HttpServletRequest httpRequest,
                            HttpSession session) {
        UserAccount user = authService.registerStudent(request.username(), request.displayName(), request.password());
        httpRequest.changeSessionId();
        authService.signIn(session, user);
        return UserDto.from(user);
    }

    @PostMapping("/auth/logout")
    public void logout(HttpSession session) {
        session.invalidate();
    }
}
