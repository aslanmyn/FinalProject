package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<RoomDto> getUserRooms(User user) {
        List<ChatRoom> rooms = chatRoomRepository.findRoomsByUserId(user.getId());
        List<RoomDto> result = new ArrayList<>();
        for (ChatRoom room : rooms) {
            RoomDto dto = toRoomDto(room, user);
            result.add(dto);
        }
        return result;
    }

    @Transactional
    public RoomDto getOrCreateSectionRoom(Long sectionId, User user) {
        SubjectOffering section = subjectOfferingRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        boolean isTeacher = false;
        boolean isStudent = false;

        if (user.getRole() == User.UserRole.PROFESSOR) {
            Teacher teacher = teacherRepository.findByEmail(user.getEmail()).orElse(null);
            if (teacher != null && section.getTeacher() != null
                    && section.getTeacher().getId().equals(teacher.getId())) {
                isTeacher = true;
            }
        }
        if (user.getRole() == User.UserRole.STUDENT) {
            Student student = studentRepository.findByEmail(user.getEmail()).orElse(null);
            if (student != null && registrationRepository.existsByStudentIdAndSubjectOfferingId(
                    student.getId(), sectionId)) {
                isStudent = true;
            }
        }

        if (!isTeacher && !isStudent) {
            throw new IllegalArgumentException("You are not a member of this section");
        }

        ChatRoom room = chatRoomRepository.findBySectionIdAndType(sectionId, ChatRoom.ChatRoomType.SECTION)
                .orElseGet(() -> {
                    String subjectName = section.getSubject() != null ? section.getSubject().getName() : "Section";
                    String semesterName = section.getSemester() != null ? section.getSemester().getName() : "";
                    ChatRoom newRoom = chatRoomRepository.save(ChatRoom.builder()
                            .name(subjectName + " — " + semesterName)
                            .type(ChatRoom.ChatRoomType.SECTION)
                            .section(section)
                            .createdAt(Instant.now())
                            .build());
                    seedSectionMembers(newRoom, section);
                    return newRoom;
                });

        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(room.getId(), user.getId())) {
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .chatRoom(room).user(user).joinedAt(Instant.now()).build());
        }

        return toRoomDto(room, user);
    }

    @Transactional
    public RoomDto getOrCreateDirectRoom(Long otherUserId, User currentUser) {
        if (otherUserId.equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot create DM with yourself");
        }
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<ChatRoom> existing = chatRoomRepository.findDirectRoomBetween(
                currentUser.getId(), otherUserId);
        if (existing.isPresent()) {
            return toRoomDto(existing.get(), currentUser);
        }

        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .name(null)
                .type(ChatRoom.ChatRoomType.DIRECT)
                .createdAt(Instant.now())
                .build());

        chatRoomMemberRepository.saveAll(List.of(
                ChatRoomMember.builder().chatRoom(room).user(currentUser).joinedAt(Instant.now()).build(),
                ChatRoomMember.builder().chatRoom(room).user(otherUser).joinedAt(Instant.now()).build()
        ));

        return toRoomDto(room, currentUser);
    }

    @Transactional
    public MessageDto sendMessage(Long roomId, User sender, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(roomId, sender.getId())) {
            throw new IllegalArgumentException("Not a member of this room");
        }
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        ChatMessage msg = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content.trim())
                .createdAt(Instant.now())
                .build());

        return toMessageDto(msg);
    }

    @Transactional(readOnly = true)
    public PageResult<MessageDto> getMessages(Long roomId, User user, int page, int size) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(roomId, user.getId())) {
            throw new IllegalArgumentException("Not a member of this room");
        }
        Page<ChatMessage> msgPage = chatMessageRepository.findByRoomIdWithSender(
                roomId, PageRequest.of(page, size));
        List<MessageDto> items = msgPage.getContent().stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
        return new PageResult<>(items, msgPage.getNumber(), msgPage.getSize(),
                msgPage.getTotalElements(), msgPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long roomId, Long userId) {
        return chatRoomMemberRepository.existsByChatRoomIdAndUserId(roomId, userId);
    }

    @Transactional(readOnly = true)
    public List<MemberDto> getRoomMembers(Long roomId) {
        return chatRoomMemberRepository.findByChatRoomId(roomId).stream()
                .map(m -> new MemberDto(m.getUser().getId(), m.getUser().getFullName(), m.getUser().getRole().name()))
                .collect(Collectors.toList());
    }

    private void seedSectionMembers(ChatRoom room, SubjectOffering section) {
        Set<Long> addedUserIds = new HashSet<>();

        if (section.getTeacher() != null) {
            userRepository.findByEmail(section.getTeacher().getEmail()).ifPresent(u -> {
                chatRoomMemberRepository.save(ChatRoomMember.builder()
                        .chatRoom(room).user(u).joinedAt(Instant.now()).build());
                addedUserIds.add(u.getId());
            });
        }

        List<Registration> regs = registrationRepository.findBySubjectOfferingIdAndStatusIn(
                section.getId(),
                List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED));
        for (Registration reg : regs) {
            userRepository.findByEmail(reg.getStudent().getEmail()).ifPresent(u -> {
                if (addedUserIds.add(u.getId())) {
                    chatRoomMemberRepository.save(ChatRoomMember.builder()
                            .chatRoom(room).user(u).joinedAt(Instant.now()).build());
                }
            });
        }
    }

    private RoomDto toRoomDto(ChatRoom room, User viewer) {
        String displayName = room.getName();
        if (room.getType() == ChatRoom.ChatRoomType.DIRECT && displayName == null) {
            List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(room.getId());
            displayName = members.stream()
                    .map(m -> m.getUser().getFullName())
                    .filter(n -> !n.equals(viewer.getFullName()))
                    .findFirst()
                    .orElse("Direct Message");
        }
        if (displayName == null) {
            displayName = "Chat";
        }
        Long sectionId = room.getSection() != null ? room.getSection().getId() : null;
        return new RoomDto(room.getId(), displayName, room.getType().name(), sectionId, room.getCreatedAt().toString());
    }

    private MessageDto toMessageDto(ChatMessage msg) {
        return new MessageDto(
                msg.getId(),
                msg.getChatRoom().getId(),
                msg.getSender().getId(),
                msg.getSender().getFullName(),
                msg.getSender().getRole().name(),
                msg.getContent(),
                msg.getCreatedAt().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String q) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return userRepository.searchChatUsers(query).stream()
                .map(u -> new UserDto(u.getId(), u.getFullName(), u.getRole().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomDto createGroupRoom(String name, List<Long> userIds, User creator) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("At least one other member is required");
        }

        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .name(name.trim())
                .type(ChatRoom.ChatRoomType.GROUP)
                .createdAt(Instant.now())
                .build());

        Set<Long> added = new HashSet<>();
        chatRoomMemberRepository.save(
                ChatRoomMember.builder().chatRoom(room).user(creator).joinedAt(Instant.now()).build());
        added.add(creator.getId());

        for (Long uid : userIds) {
            if (!added.add(uid)) continue;
            userRepository.findById(uid).ifPresent(u ->
                    chatRoomMemberRepository.save(
                            ChatRoomMember.builder().chatRoom(room).user(u).joinedAt(Instant.now()).build()));
        }

        return toRoomDto(room, creator);
    }

    public record RoomDto(Long id, String name, String type, Long sectionId, String createdAt) {}
    public record MessageDto(Long id, Long roomId, Long senderId, String senderName, String senderRole,
                             String content, String createdAt) {}
    public record MemberDto(Long id, String name, String role) {}
    public record UserDto(Long id, String name, String role) {}
    public record GroupRoomRequest(String name, List<Long> userIds) {}
    public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {}
}
