package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.UserRole;
import ru.kors.finalproject.service.UserRoleDetector;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UserRoleDetector roleDetector;
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
            @RequestParam(required = false) String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        UserRole role = roleDetector.detectRole(email);
        if (role == UserRole.UNKNOWN) {
            redirectAttributes.addFlashAttribute("error", "Invalid email format. Use student format (a_mustafayev@kbtu.kz) or teacher format (z.teacher@kbtu.kz)");
            return "redirect:/login";
        }
        if (role == UserRole.STUDENT && studentRepository.findByEmail(email).isEmpty()) {
            var f = facultyRepository.findAll().stream().findFirst().orElse(null);
            var p = programRepository.findAll().stream().findFirst().orElse(null);
            var s = semesterRepository.findByCurrentTrue().orElse(semesterRepository.findAll().stream().findFirst().orElse(null));
            if (f != null && p != null && s != null) {
                studentRepository.save(Student.builder()
                        .email(email).name(email.split("@")[0])
                        .course(1).groupName("TBD").status(Student.StudentStatus.ACTIVE)
                        .faculty(f).program(p).currentSemester(s).creditsEarned(0).build());
            }
        }
        if (role == UserRole.TEACHER && teacherRepository.findByEmail(email).isEmpty()) {
            var f = facultyRepository.findAll().stream().findFirst().orElse(null);
            if (f != null) {
                teacherRepository.save(Teacher.builder().email(email).name(email.split("@")[0]).faculty(f).build());
            }
        }
        session.setAttribute("userEmail", email);
        session.setAttribute("userRole", role.name());
        return "redirect:/news";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
