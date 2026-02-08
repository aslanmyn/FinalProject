package ru.kors.finalproject.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.repository.StudentRepository;
import ru.kors.finalproject.repository.TeacherRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public String getEmail(HttpSession session) {
        return (String) session.getAttribute("userEmail");
    }

    public String getRole(HttpSession session) {
        return (String) session.getAttribute("userRole");
    }

    public Optional<Student> getCurrentStudent(HttpSession session) {
        String email = getEmail(session);
        if (email == null || !"STUDENT".equals(getRole(session))) return Optional.empty();
        return studentRepository.findByEmailWithDetails(email);
    }

    public Optional<Teacher> getCurrentTeacher(HttpSession session) {
        String email = getEmail(session);
        if (email == null || !"TEACHER".equals(getRole(session))) return Optional.empty();
        return teacherRepository.findByEmail(email);
    }

    public boolean isStudent(HttpSession session) {
        return "STUDENT".equals(getRole(session));
    }

    public boolean isTeacher(HttpSession session) {
        return "TEACHER".equals(getRole(session));
    }
}
