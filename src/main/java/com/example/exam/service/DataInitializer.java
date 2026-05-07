package com.example.exam.service;

import com.example.exam.model.*;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.QuestionRepository;
import com.example.exam.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserAccountRepository userRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final AuthService authService;

    public DataInitializer(UserAccountRepository userRepository, QuestionRepository questionRepository,
                           ExamRepository examRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        authService.createUser("admin", "admin123", "系统管理员", Role.ADMIN);
        UserAccount teacher = authService.createUser("teacher", "teacher123", "课程教师", Role.TEACHER);
        authService.createUser("student", "student123", "演示学生", Role.STUDENT);
        authService.createUser("student2", "student123", "实践学生", Role.STUDENT);

        Question q1 = questionRepository.save(new Question(
                QuestionType.SINGLE_CHOICE,
                "Java 中用于定义类继承关系的关键字是哪个？",
                "implements", "extends", "import", "package",
                "B", 10, teacher));
        Question q2 = questionRepository.save(new Question(
                QuestionType.TRUE_FALSE,
                "HTTP 是无状态协议，服务端通常需要借助 Session 或 Token 识别用户。",
                null, null, null, null,
                "正确", 10, teacher));
        Question q3 = questionRepository.save(new Question(
                QuestionType.FILL_BLANK,
                "Spring Boot 默认使用的内嵌 Servlet 容器是 ____。",
                null, null, null, null,
                "Tomcat", 10, teacher));
        Question q4 = questionRepository.save(new Question(
                QuestionType.ESSAY,
                "请说明在线考试系统中防止重复提交和保证阅卷公平性的两个设计思路。",
                null, null, null, null,
                "人工评分", 20, teacher));

        Exam exam = new Exam(
                "Java Web 综合能力测试",
                "覆盖 Java 基础、Web 会话机制、Spring Boot 与系统设计。",
                60,
                teacher,
                List.of(q1, q2, q3, q4));
        exam.setStatus(ExamStatus.PUBLISHED);
        examRepository.save(exam);
    }
}
