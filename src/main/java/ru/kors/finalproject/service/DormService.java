package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DormService {

    private final DormApplicationRepository dormApplicationRepository;
    private final DormRoomRepository dormRoomRepository;
    private final DormBuildingRepository dormBuildingRepository;

    public List<DormBuilding> getAllBuildings() {
        return dormBuildingRepository.findAll();
    }

    public List<DormRoom> getAvailableRooms(DormRoom.RoomType roomType) {
        if (roomType != null) {
            return dormRoomRepository.findAvailableByType(roomType);
        }
        return dormRoomRepository.findAllAvailable();
    }

    public List<DormRoom> getRoomsByBuilding(Long buildingId) {
        return dormRoomRepository.findByDormBuildingId(buildingId);
    }

    public List<DormApplication> getStudentApplications(Long studentId) {
        return dormApplicationRepository.findByStudentId(studentId);
    }

    public DormApplication getApplication(Long id, Long studentId) {
        return dormApplicationRepository.findByIdAndStudentId(id, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
    }

    @Transactional
    public DormApplication createApplication(Student student) {
        List<DormApplication.ApplicationStatus> activeStatuses = List.of(
                DormApplication.ApplicationStatus.DRAFT,
                DormApplication.ApplicationStatus.SUBMITTED,
                DormApplication.ApplicationStatus.APPROVED
        );
        if (dormApplicationRepository.existsByStudentIdAndStatusIn(student.getId(), activeStatuses)) {
            throw new IllegalStateException("You already have an active dorm application");
        }

        DormApplication application = DormApplication.builder()
                .student(student)
                .status(DormApplication.ApplicationStatus.DRAFT)
                .currentStep(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return dormApplicationRepository.save(application);
    }

    @Transactional
    public DormApplication updateStep1(Long applicationId, Long studentId,
                                        String emergencyContactName, String emergencyContactPhone, String specialNeeds) {
        DormApplication app = getApplication(applicationId, studentId);
        validateDraft(app);
        app.setEmergencyContactName(emergencyContactName);
        app.setEmergencyContactPhone(emergencyContactPhone);
        app.setSpecialNeeds(specialNeeds);
        app.setCurrentStep(Math.max(app.getCurrentStep(), 1));
        app.setUpdatedAt(Instant.now());
        return dormApplicationRepository.save(app);
    }

    @Transactional
    public DormApplication updateStep2(Long applicationId, Long studentId,
                                        DormRoom.RoomType roomTypePreference, Long dormRoomId) {
        DormApplication app = getApplication(applicationId, studentId);
        validateDraft(app);
        app.setRoomTypePreference(roomTypePreference);
        if (dormRoomId != null) {
            DormRoom room = dormRoomRepository.findById(dormRoomId)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found"));
            if (!room.hasSpace()) {
                throw new IllegalStateException("Selected room is full");
            }
            app.setDormRoom(room);
        }
        app.setCurrentStep(Math.max(app.getCurrentStep(), 2));
        app.setUpdatedAt(Instant.now());
        return dormApplicationRepository.save(app);
    }

    @Transactional
    public DormApplication updateStep3(Long applicationId, Long studentId,
                                        String sleepSchedule, String studyEnvironment, String preferredRoommateUid) {
        DormApplication app = getApplication(applicationId, studentId);
        validateDraft(app);
        app.setSleepSchedule(sleepSchedule);
        app.setStudyEnvironment(studyEnvironment);
        app.setPreferredRoommateUid(preferredRoommateUid);
        app.setCurrentStep(Math.max(app.getCurrentStep(), 3));
        app.setUpdatedAt(Instant.now());
        return dormApplicationRepository.save(app);
    }

    @Transactional
    public DormApplication submitApplication(Long applicationId, Long studentId, boolean termsAccepted) {
        DormApplication app = getApplication(applicationId, studentId);
        validateDraft(app);
        if (!termsAccepted) {
            throw new IllegalArgumentException("You must accept the terms and conditions");
        }
        if (app.getRoomTypePreference() == null) {
            throw new IllegalStateException("Room type preference is required");
        }
        app.setTermsAccepted(true);
        app.setCurrentStep(4);
        app.setStatus(DormApplication.ApplicationStatus.SUBMITTED);
        app.setUpdatedAt(Instant.now());
        return dormApplicationRepository.save(app);
    }

    @Transactional
    public DormApplication cancelApplication(Long applicationId, Long studentId) {
        DormApplication app = getApplication(applicationId, studentId);
        if (app.getStatus() == DormApplication.ApplicationStatus.CANCELLED) {
            throw new IllegalStateException("Application is already cancelled");
        }
        if (app.getStatus() == DormApplication.ApplicationStatus.APPROVED && app.getDormRoom() != null) {
            DormRoom room = app.getDormRoom();
            room.setOccupied(Math.max(0, room.getOccupied() - 1));
            dormRoomRepository.save(room);
        }
        app.setStatus(DormApplication.ApplicationStatus.CANCELLED);
        app.setUpdatedAt(Instant.now());
        return dormApplicationRepository.save(app);
    }

    private void validateDraft(DormApplication app) {
        if (app.getStatus() != DormApplication.ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Application can only be modified in DRAFT status");
        }
    }
}
