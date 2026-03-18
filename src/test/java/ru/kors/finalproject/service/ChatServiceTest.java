package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService business rules:
 *  - membership enforcement for sendMessage and getMessages
 *  - DM self-chat prevention
 *  - group room validation (name, members)
 *  - idempotent section room creation
 *  - section-room authorization (must be teacher or registered student)
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private SubjectOfferingRepository subjectOfferingRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;

    @InjectMocks
    private ChatService chatService;

    private User student;
    private User professor;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1L).email("a_student@kbtu.kz").fullName("Student One")
                .role(User.UserRole.STUDENT).enabled(true).build();

        professor = User.builder()
                .id(2L).email("z.prof@kbtu.kz").fullName("Prof Z")
                .role(User.UserRole.PROFESSOR).enabled(true).build();
    }

    // =========================================================================
    // sendMessage
    // =========================================================================

    @Test
    @DisplayName("sendMessage - persists message when sender is a room member")
    void sendMessage_success() {
        ChatRoom room = ChatRoom.builder().id(10L).name("CS101").type(ChatRoom.ChatRoomType.SECTION).createdAt(Instant.now()).build();

        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(10L, 1L)).thenReturn(true);
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            // simulate an id being assigned
            return ChatMessage.builder().id(99L).chatRoom(m.getChatRoom()).sender(m.getSender())
                    .content(m.getContent()).createdAt(m.getCreatedAt()).build();
        });

        ChatService.MessageDto dto = chatService.sendMessage(10L, student, "Hello world");

        assertThat(dto).isNotNull();
        assertThat(dto.content()).isEqualTo("Hello world");
        assertThat(dto.senderName()).isEqualTo("Student One");
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("sendMessage - throws when sender is NOT a member of the room")
    void sendMessage_notMember_throws() {
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> chatService.sendMessage(10L, student, "Hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a member");
    }

    @Test
    @DisplayName("sendMessage - throws for blank content (checked before membership)")
    void sendMessage_blankContent_throws() {
        // Content is validated before the membership check — no membership stub needed
        assertThatThrownBy(() -> chatService.sendMessage(10L, student, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("sendMessage - throws for null content (checked before membership)")
    void sendMessage_nullContent_throws() {
        assertThatThrownBy(() -> chatService.sendMessage(10L, student, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // getMessages
    // =========================================================================

    @Test
    @DisplayName("getMessages - throws when requester is not a room member")
    void getMessages_notMember_throws() {
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> chatService.getMessages(10L, student, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a member");
    }

    // =========================================================================
    // getOrCreateDirectRoom
    // =========================================================================

    @Test
    @DisplayName("getOrCreateDirectRoom - throws when target is same as current user (self-DM)")
    void getOrCreateDirectRoom_selfDm_throws() {
        assertThatThrownBy(() -> chatService.getOrCreateDirectRoom(1L, student))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yourself");
    }

    @Test
    @DisplayName("getOrCreateDirectRoom - returns existing room without creating a new one")
    void getOrCreateDirectRoom_returnsExistingRoom() {
        ChatRoom existing = ChatRoom.builder().id(5L).type(ChatRoom.ChatRoomType.DIRECT).createdAt(Instant.now()).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(chatRoomRepository.findDirectRoomBetween(1L, 2L)).thenReturn(Optional.of(existing));
        when(chatRoomMemberRepository.findByChatRoomId(5L)).thenReturn(List.of(
                ChatRoomMember.builder().chatRoom(existing).user(professor).joinedAt(Instant.now()).build()
        ));

        ChatService.RoomDto result = chatService.getOrCreateDirectRoom(2L, student);

        assertThat(result.id()).isEqualTo(5L);
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateDirectRoom - creates new room when none exists")
    void getOrCreateDirectRoom_createsNewRoom() {
        ChatRoom newRoom = ChatRoom.builder().id(20L).type(ChatRoom.ChatRoomType.DIRECT).createdAt(Instant.now()).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(chatRoomRepository.findDirectRoomBetween(1L, 2L)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(newRoom);
        when(chatRoomMemberRepository.saveAll(anyList())).thenReturn(List.of());
        when(chatRoomMemberRepository.findByChatRoomId(20L)).thenReturn(List.of(
                ChatRoomMember.builder().chatRoom(newRoom).user(professor).joinedAt(Instant.now()).build()
        ));

        ChatService.RoomDto result = chatService.getOrCreateDirectRoom(2L, student);

        assertThat(result.id()).isEqualTo(20L);
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("getOrCreateDirectRoom - throws when target user does not exist")
    void getOrCreateDirectRoom_unknownUser_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getOrCreateDirectRoom(99L, student))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    // =========================================================================
    // createGroupRoom
    // =========================================================================

    @Test
    @DisplayName("createGroupRoom - throws for blank room name")
    void createGroupRoom_blankName_throws() {
        assertThatThrownBy(() -> chatService.createGroupRoom("  ", List.of(2L), student))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("createGroupRoom - throws for null room name")
    void createGroupRoom_nullName_throws() {
        assertThatThrownBy(() -> chatService.createGroupRoom(null, List.of(2L), student))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createGroupRoom - throws for empty member list")
    void createGroupRoom_noMembers_throws() {
        assertThatThrownBy(() -> chatService.createGroupRoom("Study Group", List.of(), student))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("member");
    }

    @Test
    @DisplayName("createGroupRoom - throws for null member list")
    void createGroupRoom_nullMembers_throws() {
        assertThatThrownBy(() -> chatService.createGroupRoom("Study Group", null, student))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createGroupRoom - persists room and adds creator + all specified members")
    void createGroupRoom_success() {
        ChatRoom newRoom = ChatRoom.builder().id(30L).name("Study Group").type(ChatRoom.ChatRoomType.GROUP).createdAt(Instant.now()).build();
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(newRoom);
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(professor));

        ChatService.RoomDto result = chatService.createGroupRoom("Study Group", List.of(2L), student);

        assertThat(result.id()).isEqualTo(30L);
        assertThat(result.name()).isEqualTo("Study Group");
        // creator + 1 member = 2 saves
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }

    // =========================================================================
    // getOrCreateSectionRoom - authorization checks
    // =========================================================================

    @Test
    @DisplayName("getOrCreateSectionRoom - throws when user is not teacher or registered student")
    void getOrCreateSectionRoom_unauthorized_throws() {
        SubjectOffering section = SubjectOffering.builder().id(50L).build();
        when(subjectOfferingRepository.findById(50L)).thenReturn(Optional.of(section));
        // student not registered
        Student studentEntity = Student.builder().id(1L).email("a_student@kbtu.kz").build();
        when(studentRepository.findByEmail("a_student@kbtu.kz")).thenReturn(Optional.of(studentEntity));
        when(registrationRepository.existsByStudentIdAndSubjectOfferingId(1L, 50L)).thenReturn(false);

        assertThatThrownBy(() -> chatService.getOrCreateSectionRoom(50L, student))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    @DisplayName("getOrCreateSectionRoom - throws when section does not exist")
    void getOrCreateSectionRoom_sectionNotFound_throws() {
        when(subjectOfferingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getOrCreateSectionRoom(999L, student))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Section not found");
    }
}
