package com.example.exam.service;

import com.example.exam.model.Role;
import com.example.exam.model.UserAccount;
import com.example.exam.repository.UserAccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {
    public static final String SESSION_USER_ID = "currentUserId";
    private static final Pattern STUDENT_NUMBER_PATTERN = Pattern.compile("\\d{10}");
    private static final Pattern ALNUM_PASSWORD_PATTERN = Pattern.compile("[A-Za-z0-9]{6,}");

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Optional<UserAccount> login(String username, String password, Role role) {
        return userAccountRepository.findByUsername(username)
                .filter(UserAccount::isEnabled)
                .filter(user -> passwordMatchesAndUpgrade(user, clean(password)))
                .filter(user -> user.getRole() == role);
    }

    public void signIn(HttpSession session, UserAccount user) {
        session.setAttribute(SESSION_USER_ID, user.getId());
    }

    public Optional<UserAccount> currentUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (id instanceof Long userId) {
            return userAccountRepository.findById(userId).filter(UserAccount::isEnabled);
        }
        return Optional.empty();
    }

    public UserAccount requireRole(HttpSession session, Role role) {
        UserAccount user = currentUser(session)
                .orElseThrow(() -> new IllegalStateException("请先登录"));
        if (user.getRole() != role) {
            throw new IllegalStateException("当前账号无权访问该接口");
        }
        return user;
    }

    public String homeFor(Role role) {
        return switch (role) {
            case STUDENT -> "/student";
            case TEACHER -> "/teacher";
            case ADMIN -> "/admin";
        };
    }

    @Transactional
    public UserAccount createUser(String username, String password, String displayName, Role role) {
        String cleanUsername = required(username, "用户名不能为空");
        String cleanDisplayName = required(displayName, "显示名称不能为空");
        UserAccount user = new UserAccount(cleanUsername, encodePassword(password), cleanDisplayName, role);
        user.setEnabled(true);
        return userAccountRepository.save(user);
    }

    @Transactional
    public UserAccount registerStudent(String username, String displayName, String password) {
        String cleanUsername = required(username, "学号不能为空");
        String cleanDisplayName = required(displayName, "姓名不能为空");
        String cleanPassword = clean(password);
        if (!STUDENT_NUMBER_PATTERN.matcher(cleanUsername).matches()) {
            throw new IllegalStateException("用户名必须是 10 位数字学号");
        }
        if (!ALNUM_PASSWORD_PATTERN.matcher(cleanPassword).matches()) {
            throw new IllegalStateException("密码至少 6 位，且只能包含数字或字母");
        }
        if (userAccountRepository.findByUsername(cleanUsername).isPresent()) {
            throw new IllegalStateException("该学号已注册");
        }
        UserAccount user = new UserAccount(cleanUsername, encodePassword(cleanPassword), cleanDisplayName, Role.STUDENT);
        user.setEnabled(true);
        return userAccountRepository.save(user);
    }

    @Transactional
    public void changePassword(UserAccount user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalStateException("当前密码不正确");
        }
        user.setPassword(encodePassword(newPassword));
        userAccountRepository.save(user);
    }

    public String encodePassword(String password) {
        String value = clean(password);
        if (value.length() < 6) {
            throw new IllegalStateException("密码长度不能少于 6 位");
        }
        return passwordEncoder.encode(value);
    }

    private boolean passwordMatchesAndUpgrade(UserAccount user, String rawPassword) {
        String storedPassword = user.getPassword();
        if (storedPassword == null) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (storedPassword.equals(rawPassword)) {
            user.setPassword(encodePassword(rawPassword));
            userAccountRepository.save(user);
            return true;
        }
        return false;
    }

    private boolean isBcryptHash(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String required(String value, String message) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            throw new IllegalStateException(message);
        }
        return cleaned;
    }
}
