package ru.kors.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.AuditLog;
import ru.kors.finalproject.entity.Notification;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.AuditLogRepository;
import ru.kors.finalproject.repository.NotificationRepository;
import ru.kors.finalproject.repository.StudentRepository;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.repository.UserRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(2000)
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.identity-normalization.enabled", havingValue = "true")
public class IdentityNormalizationRunner implements CommandLineRunner {

    private static final String LOCAL_CREDENTIALS_FILE = "seed-users.local.txt";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String PROFESSOR_PASSWORD = "prof123";
    private static final String STUDENT_PASSWORD = "student123";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, String> emailMapping = new LinkedHashMap<>();

        normalizeTeachers(emailMapping);
        normalizeStudents(emailMapping);
        normalizeNotifications(emailMapping);
        normalizeAuditLogs(emailMapping);

        writeCredentialsFile();
    }

    private void normalizeTeachers(Map<String, String> emailMapping) {
        List<Teacher> teachers = teacherRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        for (Teacher teacher : teachers) {
            String oldEmail = teacher.getEmail();
            String newEmail = DemoIdentitySupport.teacherEmailFromFullName(teacher.getName());
            if (oldEmail == null || oldEmail.equalsIgnoreCase(newEmail)) {
                teacher.setPublicEmail(newEmail);
                continue;
            }

            User user = userRepository.findByEmail(oldEmail)
                    .orElseThrow(() -> new IllegalStateException("User not found for teacher email: " + oldEmail));

            user.setEmail(newEmail);
            user.setFullName(teacher.getName());

            teacher.setEmail(newEmail);
            teacher.setPublicEmail(newEmail);
            emailMapping.put(oldEmail, newEmail);
        }
    }

    private void normalizeStudents(Map<String, String> emailMapping) {
        List<Student> students = studentRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        for (int index = 0; index < students.size(); index++) {
            Student student = students.get(index);
            String fullName = DemoIdentitySupport.generateStudentName(index + 1);
            String newEmail = DemoIdentitySupport.studentEmailFromFullName(fullName);
            String oldEmail = student.getEmail();

            User user = userRepository.findByEmail(oldEmail)
                    .orElseThrow(() -> new IllegalStateException("User not found for student email: " + oldEmail));

            user.setEmail(newEmail);
            user.setFullName(fullName);

            student.setEmail(newEmail);
            student.setName(fullName);

            if (oldEmail != null && !oldEmail.equalsIgnoreCase(newEmail)) {
                emailMapping.put(oldEmail, newEmail);
            }
        }
    }

    private void normalizeNotifications(Map<String, String> emailMapping) {
        if (emailMapping.isEmpty()) {
            return;
        }
        List<Notification> notifications = notificationRepository.findAll();
        for (Notification notification : notifications) {
            String newEmail = emailMapping.get(notification.getRecipientEmail());
            if (newEmail != null) {
                notification.setRecipientEmail(newEmail);
            }
        }
    }

    private void normalizeAuditLogs(Map<String, String> emailMapping) {
        if (emailMapping.isEmpty()) {
            return;
        }
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        for (AuditLog auditLog : auditLogs) {
            String newEmail = emailMapping.get(auditLog.getActorEmail());
            if (newEmail != null) {
                auditLog.setActorEmail(newEmail);
            }
        }
    }

    private void writeCredentialsFile() {
        List<AccountCredential> credentials = new ArrayList<>();

        credentials.add(new AccountCredential("ADMIN", "admin@kbtu.kz", ADMIN_PASSWORD, "System Administrator", "[SUPER, REGISTRAR, FINANCE, MOBILITY, SUPPORT, CONTENT]"));
        credentials.add(new AccountCredential("ADMIN", "registrar@kbtu.kz", ADMIN_PASSWORD, "Registrar Office", "[REGISTRAR]"));
        credentials.add(new AccountCredential("ADMIN", "finance@kbtu.kz", ADMIN_PASSWORD, "Finance Office", "[FINANCE]"));
        credentials.add(new AccountCredential("ADMIN", "support@kbtu.kz", ADMIN_PASSWORD, "Student Support Office", "[SUPPORT, CONTENT]"));
        credentials.add(new AccountCredential("ADMIN", "mobility@kbtu.kz", ADMIN_PASSWORD, "Mobility Office", "[MOBILITY]"));

        List<Teacher> teachers = teacherRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        for (Teacher teacher : teachers) {
            credentials.add(new AccountCredential(
                    "PROFESSOR",
                    teacher.getEmail(),
                    PROFESSOR_PASSWORD,
                    teacher.getName(),
                    teacher.getFaculty() != null ? teacher.getFaculty().getName() : "Professor"
            ));
        }

        List<Student> students = studentRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        for (Student student : students) {
            credentials.add(new AccountCredential(
                    "STUDENT",
                    student.getEmail(),
                    STUDENT_PASSWORD,
                    student.getName(),
                    (student.getFaculty() != null ? student.getFaculty().getName() : "Student")
                            + ", year " + student.getCourse()
            ));
        }

        Map<String, List<AccountCredential>> grouped = new LinkedHashMap<>();
        for (AccountCredential credential : credentials) {
            grouped.computeIfAbsent(credential.role(), key -> new ArrayList<>()).add(credential);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("KBTU Portal demo accounts").append(System.lineSeparator())
                .append("Generated: ").append(Instant.now()).append(System.lineSeparator()).append(System.lineSeparator());

        for (Map.Entry<String, List<AccountCredential>> entry : grouped.entrySet()) {
            builder.append("[").append(entry.getKey()).append("]").append(System.lineSeparator());
            for (AccountCredential credential : entry.getValue()) {
                builder.append(credential.email())
                        .append(" | ")
                        .append(credential.password())
                        .append(" | ")
                        .append(credential.fullName())
                        .append(" | ")
                        .append(credential.details())
                        .append(System.lineSeparator());
            }
            builder.append(System.lineSeparator());
        }

        try {
            Files.writeString(
                    Path.of(LOCAL_CREDENTIALS_FILE),
                    builder.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write local seed credentials file", exception);
        }
    }

    private record AccountCredential(String role, String email, String password, String fullName, String details) {
    }
}
