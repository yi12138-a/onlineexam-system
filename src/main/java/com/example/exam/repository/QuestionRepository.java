package com.example.exam.repository;

import com.example.exam.model.Question;
import com.example.exam.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByCreatorOrderByIdDesc(UserAccount creator);
}
