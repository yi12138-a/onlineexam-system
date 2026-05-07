package com.example.exam.repository;

import com.example.exam.model.Role;
import com.example.exam.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    long countByRole(Role role);

    List<UserAccount> findByRoleOrderByIdDesc(Role role);
}
