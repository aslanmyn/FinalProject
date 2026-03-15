package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.ChatService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatV1Controller {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public List<ChatService.RoomDto> getRooms(@AuthenticationPrincipal User user) {
        return chatService.getUserRooms(user);
    }

    @PostMapping("/rooms/section/{sectionId}")
    public ChatService.RoomDto getOrCreateSectionRoom(@PathVariable Long sectionId,
                                                       @AuthenticationPrincipal User user) {
        return chatService.getOrCreateSectionRoom(sectionId, user);
    }

    @PostMapping("/rooms/direct")
    public ChatService.RoomDto getOrCreateDirectRoom(@RequestBody Map<String, Long> body,
                                                      @AuthenticationPrincipal User user) {
        Long otherUserId = body.get("userId");
        if (otherUserId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return chatService.getOrCreateDirectRoom(otherUserId, user);
    }

    @PostMapping("/rooms/group")
    public ChatService.RoomDto createGroupRoom(@RequestBody ChatService.GroupRoomRequest body,
                                                @AuthenticationPrincipal User user) {
        return chatService.createGroupRoom(body.name(), body.userIds(), user);
    }

    @GetMapping("/users")
    public List<ChatService.UserDto> searchUsers(@RequestParam(required = false) String q) {
        return chatService.searchUsers(q);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ChatService.PageResult<ChatService.MessageDto> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User user) {
        return chatService.getMessages(roomId, user, page, size);
    }

    @GetMapping("/rooms/{roomId}/members")
    public List<ChatService.MemberDto> getMembers(@PathVariable Long roomId,
                                                    @AuthenticationPrincipal User user) {
        if (!chatService.isMember(roomId, user.getId())) {
            throw new IllegalArgumentException("Not a member of this room");
        }
        return chatService.getRoomMembers(roomId);
    }
}
