package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.kors.finalproject.config.LegacyWebEnabled;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.UserRole;
import ru.kors.finalproject.service.UserRoleDetector;

@LegacyWebEnabled
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final PasswordEncoder passwordEncoder;
    private final UserRoleDetector roleDetector;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/do-login")
    public String doLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found. Please register first.");
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        if (!user.isEnabled()) {
            redirectAttributes.addFlashAttribute("error", "Account is disabled.");
            return "redirect:/login";
        }
        
        if (!user.validatePassword(password)) {
            redirectAttributes.addFlashAttribute("error", "Invalid password.");
            return "redirect:/login";
        }

        session.setAttribute("userId", user.getId());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userRole", user.getRole().name());
        session.setAttribute("fullName", user.getFullName());

        if (user.getRole() == User.UserRole.ADMIN) {
            return "redirect:/admin/dashboard";
        }
        if (user.getRole() == User.UserRole.PROFESSOR) {
            return "redirect:/professor/dashboard";
        }
        return "redirect:/news";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/do-register")
    @Transactional
    public String doRegister(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String fullName,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Validation
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Email and password are required.");
            return "redirect:/register";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/register";
        }

        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/register";
        }

        if (userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email already registered.");
            return "redirect:/register";
        }

        // Detect role by email format
        UserRole role = roleDetector.detectRole(email);
        if (role == UserRole.UNKNOWN) {
            redirectAttributes.addFlashAttribute("error", "Invalid email format. Use: admin@kbtu.kz, a_name@kbtu.kz (student), or z.name@kbtu.kz (professor)");
            return "redirect:/register";
        }

        // Create user with encoded password
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(User.UserRole.valueOf(role.name()))
                .adminPermissions(role == UserRole.ADMIN
                        ? java.util.EnumSet.allOf(User.AdminPermission.class)
                        : java.util.EnumSet.noneOf(User.AdminPermission.class))
                .enabled(true)
                .build();
        userRepository.save(user);

        // Create related entities for students and professors
        if (role == UserRole.STUDENT) {
            var faculty = facultyRepository.findAll().stream().findFirst();
            var program = programRepository.findAll().stream().findFirst();
            var semester = semesterRepository.findByCurrentTrue().or(() -> semesterRepository.findAll().stream().findFirst());
            
            if (faculty.isPresent() && program.isPresent() && semester.isPresent()) {
                Student student = Student.builder()
                        .email(email)
                        .name(fullName)
                        .course(1)
                        .groupName("TBD")
                        .status(Student.StudentStatus.ACTIVE)
                        .faculty(faculty.get())
                        .program(program.get())
                        .currentSemester(semester.get())
                        .creditsEarned(0)
                        .build();
                studentRepository.save(student);
            }
        } else if (role == UserRole.PROFESSOR) {
            var faculty = facultyRepository.findAll().stream().findFirst();
            if (faculty.isPresent()) {
                Teacher teacher = Teacher.builder()
                        .email(email)
                        .name(fullName)
                        .faculty(faculty.get())
                        .build();
                teacherRepository.save(teacher);
            }
        }

        redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
