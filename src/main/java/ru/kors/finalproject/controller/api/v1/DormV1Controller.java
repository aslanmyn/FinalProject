package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.service.DormService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/student/dorm")
@RequiredArgsConstructor
public class DormV1Controller {

    private final DormService dormService;
    private final CurrentUserHelper currentUserHelper;

    // ===== Buildings & Rooms =====

    @GetMapping("/buildings")
    public ResponseEntity<?> getBuildings() {
        return ResponseEntity.ok(dormService.getAllBuildings().stream()
                .map(b -> new BuildingDto(b.getId(), b.getName(), b.getAddress(), b.getTotalFloors()))
                .toList());
    }

    @GetMapping("/rooms")
    public ResponseEntity<?> getAvailableRooms(@RequestParam(required = false) String roomType) {
        DormRoom.RoomType type = null;
        if (roomType != null) {
            type = DormRoom.RoomType.valueOf(roomType);
        }
        return ResponseEntity.ok(dormService.getAvailableRooms(type).stream()
                .map(this::toRoomDto)
                .toList());
    }

    @GetMapping("/buildings/{buildingId}/rooms")
    public ResponseEntity<?> getRoomsByBuilding(@PathVariable Long buildingId) {
        return ResponseEntity.ok(dormService.getRoomsByBuilding(buildingId).stream()
                .map(this::toRoomDto)
                .toList());
    }

    // ===== Applications =====

    @GetMapping("/applications")
    public ResponseEntity<?> getApplications(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(dormService.getStudentApplications(student.getId()).stream()
                .map(this::toApplicationDto)
                .toList());
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<?> getApplication(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toApplicationDto(dormService.getApplication(id, student.getId())));
    }

    @PostMapping("/applications")
    public ResponseEntity<?> createApplication(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toApplicationDto(dormService.createApplication(student)));
    }

    @PutMapping("/applications/{id}/step1")
    public ResponseEntity<?> updateStep1(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Step1Body body) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toApplicationDto(
                dormService.updateStep1(id, student.getId(),
                        body.emergencyContactName(), body.emergencyContactPhone(), body.specialNeeds())));
    }

    @PutMapping("/applications/{id}/step2")
    public ResponseEntity<?> updateStep2(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Step2Body body) {
        Student student = currentUserHelper.requireStudent(user);
        DormRoom.RoomType roomType = body.roomTypePreference() != null
                ? DormRoom.RoomType.valueOf(body.roomTypePreference()) : null;
        return ResponseEntity.ok(toApplicationDto(
                dormService.updateStep2(id, student.getId(), roomType, body.dormRoomId())));
    }

    @PutMapping("/applications/{id}/step3")
    public ResponseEntity<?> updateStep3(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Step3Body body) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toApplicationDto(
                dormService.updateStep3(id, student.getId(),
                        body.sleepSchedule(), body.studyEnvironment(), body.preferredRoommateUid())));
    }

    @PostMapping("/applications/{id}/submit")
    public ResponseEntity<?> submitApplication(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody SubmitBody body) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toApplicationDto(
                dormService.submitApplication(id, student.getId(), body.termsAccepted())));
    }

    @PostMapping("/applications/{id}/cancel")
    public ResponseEntity<?> cancelApplication(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toApplicationDto(dormService.cancelApplication(id, student.getId())));
    }

    // ===== DTOs =====

    private RoomDto toRoomDto(DormRoom r) {
        return new RoomDto(r.getId(),
                r.getDormBuilding() != null ? r.getDormBuilding().getId() : null,
                r.getRoomNumber(), r.getFloor(), r.getRoomType().name(),
                r.getPricePerSemester(), r.getCapacity(), r.getOccupied(),
                r.getDescription(), r.hasSpace());
    }

    private ApplicationDto toApplicationDto(DormApplication a) {
        return new ApplicationDto(
                a.getId(), a.getStatus().name(), a.getCurrentStep(),
                a.getRoomTypePreference() != null ? a.getRoomTypePreference().name() : null,
                a.getDormRoom() != null ? a.getDormRoom().getId() : null,
                a.getSleepSchedule(), a.getStudyEnvironment(), a.getPreferredRoommateUid(),
                a.isTermsAccepted(),
                a.getEmergencyContactName(), a.getEmergencyContactPhone(), a.getSpecialNeeds(),
                a.getCreatedAt(), a.getUpdatedAt());
    }

    public record BuildingDto(Long id, String name, String address, int totalFloors) {}
    public record RoomDto(Long id, Long dormBuildingId, String roomNumber, int floor,
                          String roomType, BigDecimal pricePerSemester, int capacity, int occupied,
                          String description, boolean hasSpace) {}
    public record ApplicationDto(Long id, String status, int currentStep, String roomTypePreference,
                                  Long dormRoomId, String sleepSchedule, String studyEnvironment,
                                  String preferredRoommateUid, boolean termsAccepted,
                                  String emergencyContactName, String emergencyContactPhone,
                                  String specialNeeds, Instant createdAt, Instant updatedAt) {}

    public record Step1Body(String emergencyContactName, String emergencyContactPhone, String specialNeeds) {}
    public record Step2Body(String roomTypePreference, Long dormRoomId) {}
    public record Step3Body(String sleepSchedule, String studyEnvironment, String preferredRoommateUid) {}
    public record SubmitBody(boolean termsAccepted) {}
}
