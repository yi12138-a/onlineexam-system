package com.example.exam.repository;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamStatus;
import com.example.exam.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByTeacherOrderByCreatedAtDesc(UserAccount teacher);

    List<Exam> findByStatusOrderByCreatedAtDesc(ExamStatus status);

    long countByStatus(ExamStatus status);
}
