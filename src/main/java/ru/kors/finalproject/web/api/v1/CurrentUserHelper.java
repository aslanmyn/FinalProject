package ru.kors.finalproject.web.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.StudentRepository;
import ru.kors.finalproject.repository.TeacherRepository;

/**
 * Resolves the domain entity (Student, Teacher) from the authenticated User principal.
 * Used by controllers that receive {@code @AuthenticationPrincipal User user}.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserHelper {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public Student requireStudent(User user) {
        return studentRepository.findByEmailWithDetails(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));
    }

    public Teacher requireTeacher(User user) {
        return teacherRepository.findByEmailWithDetails(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Teacher profile not found"));
    }

    public Student saveStudent(Student student) {
        return studentRepository.save(student);
    }

    public Teacher saveTeacher(Teacher teacher) {
        return teacherRepository.save(teacher);
    }
}
