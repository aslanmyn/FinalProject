package ru.kors.finalproject.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.StudentRepository;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public Long getUserId(HttpSession session) {
        Object id = session.getAttribute("userId");
        return id instanceof Long ? (Long) id : null;
    }

    public String getEmail(HttpSession session) {
        return (String) session.getAttribute("userEmail");
    }

    public String getRole(HttpSession session) {
        return (String) session.getAttribute("userRole");
    }

    public String getFullName(HttpSession session) {
        return (String) session.getAttribute("fullName");
    }

    public Optional<Student> getCurrentStudent(HttpSession session) {
        String email = getEmail(session);
        if (email == null || !"STUDENT".equals(getRole(session))) return Optional.empty();
        return studentRepository.findByEmailWithDetails(email);
    }

    public Optional<Teacher> getCurrentTeacher(HttpSession session) {
        String email = getEmail(session);
        if (email == null || !"PROFESSOR".equals(getRole(session))) return Optional.empty();
        return teacherRepository.findByEmailWithDetails(email);
    }

    public Optional<User> getCurrentUser(HttpSession session) {
        String email = getEmail(session);
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email);
    }

    public boolean isStudent(HttpSession session) {
        return "STUDENT".equals(getRole(session));
    }

    public boolean isTeacher(HttpSession session) {
        return "PROFESSOR".equals(getRole(session));
    }

    public boolean isTa(HttpSession session) {
        var teacher = getCurrentTeacher(session);
        return teacher.isPresent() && teacher.get().getRole() == Teacher.TeacherRole.TA;
    }

    public boolean isLeadTeacher(HttpSession session) {
        var teacher = getCurrentTeacher(session);
        return teacher.isPresent() && teacher.get().getRole() == Teacher.TeacherRole.TEACHER;
    }

    public boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(getRole(session));
    }

    public boolean hasAdminPermission(HttpSession session, User.AdminPermission permission) {
        var user = getCurrentUser(session);
        return user.filter(value -> value.hasPermission(permission)).isPresent();
    }

    public boolean isLoggedIn(HttpSession session) {
        return getEmail(session) != null;
    }
}

