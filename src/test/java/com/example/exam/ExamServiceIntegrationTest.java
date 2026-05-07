package com.example.exam;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamStatus;
import com.example.exam.model.Role;
import com.example.exam.model.UserAccount;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamSubmissionRepository;
import com.example.exam.repository.UserAccountRepository;
import com.example.exam.service.AuthService;
import com.example.exam.service.ExamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:examtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.root=WARN"
})
class ExamServiceIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamSubmissionRepository submissionRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private ExamService examService;

    @Test
    @Transactional
    void studentCanSubmitPublishedExamOnlyOnce() {
        UserAccount student = authService.login("student", "student123", Role.STUDENT).orElseThrow();
        Exam exam = examRepository.findByStatusOrderByCreatedAtDesc(ExamStatus.PUBLISHED).get(0);

        Map<String, String> answers = new HashMap<>();
        exam.getQuestions().forEach(question ->
                answers.put("question_" + question.getId(), question.isObjective() ? question.getCorrectAnswer() : "主观题作答"));

        var submission = examService.submitExam(exam.getId(), student, answers);

        assertThat(submissionRepository.existsByExamAndStudent(exam, student)).isTrue();
        assertThat(submission.getObjectiveScore()).isEqualTo(30);
        assertThatThrownBy(() -> examService.submitExam(exam.getId(), student, answers))
                .hasMessageContaining("不能重复交卷");
    }

    @Test
    @Transactional
    void studentCanRegisterWithValidStudentNumberOnly() {
        UserAccount registered = authService.registerStudent("2026000001", "张三", "abc123");

        assertThat(registered.getRole()).isEqualTo(Role.STUDENT);
        assertThat(registered.getDisplayName()).isEqualTo("张三");
        assertThat(userRepository.findByUsername("2026000001")).isPresent();
        assertThat(authService.login("2026000001", "abc123", Role.STUDENT)).isPresent();
        assertThatThrownBy(() -> authService.registerStudent("student001", "李四", "abc123"))
                .hasMessageContaining("10 位数字学号");
        assertThatThrownBy(() -> authService.registerStudent("2026000002", "李四", "abc12!"))
                .hasMessageContaining("只能包含数字或字母");
        assertThatThrownBy(() -> authService.registerStudent("2026000001", "王五", "abc123"))
                .hasMessageContaining("已注册");
    }
}
