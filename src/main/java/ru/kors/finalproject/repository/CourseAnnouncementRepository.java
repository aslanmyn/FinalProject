package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.kors.finalproject.entity.CourseAnnouncement;

import java.time.Instant;
import java.util.List;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, Long> {
    List<CourseAnnouncement> findByTeacherIdOrderByPinnedDescPublishedAtDesc(Long teacherId);

    List<CourseAnnouncement> findBySubjectOfferingIdOrderByPinnedDescPublishedAtDesc(Long subjectOfferingId);

    List<CourseAnnouncement> findByTeacherIdAndPublicVisibleTrueAndPublishedTrueOrderByPinnedDescPublishedAtDesc(Long teacherId);

    List<CourseAnnouncement> findBySubjectOfferingIdInAndPublishedTrueOrderByPinnedDescPublishedAtDesc(List<Long> offeringIds);

    Page<CourseAnnouncement> findBySubjectOfferingIdInAndPublishedTrueOrderByPinnedDescPublishedAtDesc(List<Long> offeringIds, Pageable pageable);

    List<CourseAnnouncement> findByPublishedFalseAndScheduledAtBefore(Instant now);
}
