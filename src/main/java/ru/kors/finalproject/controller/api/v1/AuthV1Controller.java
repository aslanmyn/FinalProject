package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.Faculty;
import ru.kors.finalproject.entity.Program;
import ru.kors.finalproject.entity.Semester;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.FacultyRepository;
import ru.kors.finalproject.repository.ProgramRepository;
import ru.kors.finalproject.repository.SemesterRepository;
import ru.kors.finalproject.repository.StudentRepository;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.repository.UserRepository;
import ru.kors.finalproject.service.JwtService;
import ru.kors.finalproject.service.RefreshTokenService;
import ru.kors.finalproject.service.UserRole;
import ru.kors.finalproject.service.UserRoleDetector;
import ru.kors.finalproject.web.api.v1.ApiUnauthorizedException;

import java.util.EnumSet;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, refresh, logout, and self-service registration.")
public class AuthV1Controller {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleDetector roleDetector;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates a user and returns access and refresh tokens.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiUnauthorizedException("Invalid credentials"));
        if (!user.isEnabled() || !user.validatePassword(request.password())) {
            throw new ApiUnauthorizedException("Invalid credentials");
        }
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return ResponseEntity.ok(tokenResponse(user, accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Rotates the refresh token and issues a new access token.")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        var rotated = refreshTokenService.rotate(request.refreshToken());
        String accessToken = jwtService.generateAccessToken(rotated.user());
        return ResponseEntity.ok(tokenResponse(rotated.user(), accessToken, rotated.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the provided refresh token.")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/register")
    @Transactional
    @Operation(summary = "Register", description = "Self-registers a new user. Role is detected from email format.")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String fullName = request.fullName().trim();

        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already registered");
        }

        UserRole detectedRole = roleDetector.detectRole(email);
        if (detectedRole == UserRole.UNKNOWN) {
            throw new IllegalArgumentException("Invalid email format. Use admin@..., a_surname@... (student), or a.surname@... (professor)");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .fullName(fullName)
                .role(User.UserRole.valueOf(detectedRole.name()))
                .adminPermissions(detectedRole == UserRole.ADMIN
                        ? EnumSet.allOf(User.AdminPermission.class)
                        : EnumSet.noneOf(User.AdminPermission.class))
                .enabled(true)
                .build();
        userRepository.save(user);

        createRoleProfile(detectedRole, user);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole(),
                "status", "registered"
        ));
    }

    private Map<String, Object> tokenResponse(User user, String accessToken, String refreshToken) {
        return Map.of(
                "tokenType", "Bearer",
                "accessToken", accessToken,
                "accessTokenExpiresInSeconds", jwtService.getAccessExpirationSeconds(),
                "refreshToken", refreshToken,
                "refreshTokenExpiresInDays", refreshTokenService.getRefreshExpirationDays(),
                "role", user.getRole(),
                "permissions", user.getAdminPermissions()
        );
    }

    public record LoginRequest(
            @Schema(example = "a_mustafayev@kbtu.kz")
            @jakarta.validation.constraints.NotBlank(message = "Email is required")
            @jakarta.validation.constraints.Email
            String email,
            @Schema(example = "student123")
            @jakarta.validation.constraints.NotBlank(message = "Password is required")
            @jakarta.validation.constraints.Size(min = 6)
            String password) {
    }

    public record RefreshRequest(
            @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
            @jakarta.validation.constraints.NotBlank(message = "Refresh token is required")
            String refreshToken) {
    }

    public record RegisterRequest(
            @Schema(example = "a_testov@kbtu.kz")
            @jakarta.validation.constraints.NotBlank(message = "Email is required")
            @jakarta.validation.constraints.Email
            String email,
            @Schema(example = "student123")
            @jakarta.validation.constraints.NotBlank(message = "Password is required")
            @jakarta.validation.constraints.Size(min = 6, message = "Password must be at least 6 characters")
            String password,
            @Schema(example = "student123")
            @jakarta.validation.constraints.NotBlank(message = "Confirm password is required")
            String confirmPassword,
            @Schema(example = "Aslan Testov")
            @jakarta.validation.constraints.NotBlank(message = "Full name is required")
            String fullName) {
    }

    private void createRoleProfile(UserRole role, User user) {
        if (role == UserRole.STUDENT) {
            Faculty faculty = facultyRepository.findAll().stream().findFirst().orElse(null);
            Program program = programRepository.findAll().stream().findFirst().orElse(null);
            Semester semester = semesterRepository.findByCurrentTrue().orElse(
                    semesterRepository.findAll().stream().findFirst().orElse(null)
            );
            if (faculty != null && program != null && semester != null) {
                studentRepository.save(Student.builder()
                        .email(user.getEmail())
                        .name(user.getFullName())
                        .course(1)
                        .groupName("TBD")
                        .status(Student.StudentStatus.ACTIVE)
                        .faculty(faculty)
                        .program(program)
                        .currentSemester(semester)
                        .creditsEarned(0)
                        .build());
            }
            return;
        }

        if (role == UserRole.PROFESSOR) {
            Faculty faculty = facultyRepository.findAll().stream().findFirst().orElse(null);
            if (faculty != null) {
                teacherRepository.save(Teacher.builder()
                        .email(user.getEmail())
                        .name(user.getFullName())
                        .faculty(faculty)
                        .role(Teacher.TeacherRole.TEACHER)
                        .build());
            }
        }
    }
}
