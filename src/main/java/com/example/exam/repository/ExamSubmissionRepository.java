package com.example.exam.repository;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamSubmission;
import com.example.exam.model.SubmissionStatus;
import com.example.exam.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamSubmissionRepository extends JpaRepository<ExamSubmission, Long> {
    List<ExamSubmission> findByStudentOrderBySubmittedAtDesc(UserAccount student);

    List<ExamSubmission> findByExamOrderBySubmittedAtDesc(Exam exam);

    Optional<ExamSubmission> findFirstByExamAndStudentOrderBySubmittedAtDesc(Exam exam, UserAccount student);

    boolean existsByExamAndStudent(Exam exam, UserAccount student);

    long countByStatus(SubmissionStatus status);
}
