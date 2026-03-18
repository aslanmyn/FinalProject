package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.FileAssetRepository;
import ru.kors.finalproject.repository.RequestMessageRepository;
import ru.kors.finalproject.repository.StudentRequestRepository;
import ru.kors.finalproject.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private StudentRequestRepository studentRequestRepository;
    @Mock
    private RequestMessageRepository requestMessageRepository;
    @Mock
    private FileAssetRepository fileAssetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkflowEngineService workflowEngineService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private RequestService requestService;

    private Student student;
    private User studentUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .email("student@example.com")
                .name("John Doe")
                .course(2)
                .creditsEarned(60)
                .build();

        studentUser = User.builder()
                .id(10L)
                .email("student@example.com")
                .fullName("John Doe")
                .role(User.UserRole.STUDENT)
                .password("encoded")
                .enabled(true)
                .build();

        adminUser = User.builder()
                .id(20L)
                .email("admin@example.com")
                .fullName("Admin User")
                .role(User.UserRole.ADMIN)
                .password("encoded")
                .enabled(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // createRequest tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createRequest - creates request with status NEW")
    void createRequest_success() {
        when(studentRequestRepository.save(any(StudentRequest.class))).thenAnswer(inv -> {
            StudentRequest sr = inv.getArgument(0);
            sr.setId(1L);
            return sr;
        });

        StudentRequest result = requestService.createRequest(student, "TRANSCRIPT", "Need official transcript");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(StudentRequest.RequestStatus.NEW);
        assertThat(result.getCategory()).isEqualTo("TRANSCRIPT");
        assertThat(result.getDescription()).isEqualTo("Need official transcript");
        assertThat(result.getStudent()).isEqualTo(student);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(studentRequestRepository).save(any(StudentRequest.class));
        verify(auditService).logStudentAction(eq(student), eq("REQUEST_CREATED"), eq("StudentRequest"), eq(1L), any());
    }

    // -------------------------------------------------------------------------
    // addMessage tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("addMessage - message saved and request updated")
    void addMessage_success() {
        StudentRequest request = StudentRequest.builder()
                .id(1L)
                .student(student)
                .category("TRANSCRIPT")
                .description("Need transcript")
                .status(StudentRequest.RequestStatus.NEW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(studentRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestMessageRepository.save(any(RequestMessage.class))).thenAnswer(inv -> {
            RequestMessage msg = inv.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(studentRequestRepository.save(any(StudentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestMessage result = requestService.addMessage(1L, studentUser, "Please expedite");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getMessage()).isEqualTo("Please expedite");
        assertThat(result.getSender()).isEqualTo(studentUser);
        assertThat(result.getRequest()).isEqualTo(request);

        verify(requestMessageRepository).save(any(RequestMessage.class));
        verify(studentRequestRepository).save(any(StudentRequest.class)); // updatedAt refresh
        verify(auditService).logUserAction(eq(studentUser), eq("REQUEST_MESSAGE_ADDED"), eq("RequestMessage"), eq(100L), any());
    }

    @Test
    @DisplayName("addMessage - throws exception when student tries to message another student's request")
    void addMessage_wrongStudent() {
        Student otherStudent = Student.builder()
                .id(2L)
                .email("other@example.com")
                .name("Other Student")
                .build();

        StudentRequest request = StudentRequest.builder()
                .id(1L)
                .student(otherStudent)
                .category("TRANSCRIPT")
                .description("Other's transcript")
                .status(StudentRequest.RequestStatus.NEW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(studentRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> requestService.addMessage(1L, studentUser, "Trying to snoop"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Student can only write to own requests");

        verify(requestMessageRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updateStatus tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateStatus - sets closedAt when status is DONE")
    void updateStatus_toDone() {
        StudentRequest request = StudentRequest.builder()
                .id(1L)
                .student(student)
                .category("TRANSCRIPT")
                .description("Need transcript")
                .status(StudentRequest.RequestStatus.IN_REVIEW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(studentRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(studentRequestRepository.save(any(StudentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentRequest result = requestService.updateStatus(1L, StudentRequest.RequestStatus.DONE, adminUser);

        assertThat(result.getStatus()).isEqualTo(StudentRequest.RequestStatus.DONE);
        assertThat(result.getClosedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(studentRequestRepository).save(any(StudentRequest.class));
        verify(notificationService).notifyStudent(eq("student@example.com"), eq(Notification.NotificationType.REQUEST),
                any(), any(), any());
        verify(auditService).logUserAction(eq(adminUser), eq("REQUEST_STATUS_UPDATED"), eq("StudentRequest"), eq(1L), any());
    }

    // -------------------------------------------------------------------------
    // assign tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("assign - sets assignee and IN_REVIEW status")
    void assign_success() {
        StudentRequest request = StudentRequest.builder()
                .id(1L)
                .student(student)
                .category("TRANSCRIPT")
                .description("Need transcript")
                .status(StudentRequest.RequestStatus.NEW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(studentRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(20L)).thenReturn(Optional.of(adminUser));
        when(studentRequestRepository.save(any(StudentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentRequest result = requestService.assign(1L, 20L, adminUser);

        assertThat(result.getAssignedTo()).isEqualTo(adminUser);
        assertThat(result.getStatus()).isEqualTo(StudentRequest.RequestStatus.IN_REVIEW);
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(studentRequestRepository).save(any(StudentRequest.class));
        verify(auditService).logUserAction(eq(adminUser), eq("REQUEST_ASSIGNED"), eq("StudentRequest"), eq(1L), any());
    }

    // -------------------------------------------------------------------------
    // attachFile tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("attachFile - allowed when uploader is the owner student")
    void attachFile_ownerStudent() {
        StudentRequest request = StudentRequest.builder()
                .id(1L)
                .student(student)
                .category("TRANSCRIPT")
                .description("Need transcript")
                .status(StudentRequest.RequestStatus.NEW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(studentRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(fileAssetRepository.save(any(FileAsset.class))).thenAnswer(inv -> {
            FileAsset fa = inv.getArgument(0);
            fa.setId(500L);
            return fa;
        });

        FileAsset result = requestService.attachFile(
                1L, studentUser, "document.pdf", "/storage/doc.pdf",
                "application/pdf", 2048L, FileAsset.FileCategory.REQUEST_ATTACHMENT);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getOriginalName()).isEqualTo("document.pdf");
        assertThat(result.getLinkedEntityType()).isEqualTo("StudentRequest");
        assertThat(result.getLinkedEntityId()).isEqualTo(1L);
        assertThat(result.getOwnerStudent()).isEqualTo(student);
        assertThat(result.getUploadedBy()).isEqualTo(studentUser);

        verify(fileAssetRepository).save(any(FileAsset.class));
        verify(auditService).logUserAction(eq(studentUser), eq("REQUEST_ATTACHMENT_ADDED"), eq("FileAsset"), eq(500L), any());
    }

    @Test
    @DisplayName("attachFile - throws exception when uploader is unauthorized (not owner, not assigned, not admin)")
    void attachFile_unauthorizedUser() {
        Student otherStudent = Student.builder()
                .id(2L)
                .email("other@example.com")
                .name("Other Student")
                .build();

        StudentRequest request = StudentRequest.builder()
                .id(1L)
                .student(otherStudent)
                .category("TRANSCRIPT")
                .description("Other's transcript")
                .status(StudentRequest.RequestStatus.NEW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(studentRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        // studentUser is STUDENT role, email doesn't match otherStudent, not assigned, not admin
        assertThatThrownBy(() -> requestService.attachFile(
                1L, studentUser, "document.pdf", "/storage/doc.pdf",
                "application/pdf", 2048L, FileAsset.FileCategory.REQUEST_ATTACHMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You do not have permission to attach files to this request");

        verify(fileAssetRepository, never()).save(any());
    }
}
