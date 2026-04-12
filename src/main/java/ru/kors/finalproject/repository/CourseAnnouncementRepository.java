package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.kors.finalproject.entity.CourseAnnouncement;

import java.time.Instant;
import java.util.List;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, Long> {
    List<CourseAnnouncement> findByTeacherIdOrderByPinnedDescPublishedAtDesc(Long teacherId);

    @Query("SELECT a FROM CourseAnnouncement a " +
           "LEFT JOIN FETCH a.teacher " +
           "LEFT JOIN FETCH a.subjectOffering so " +
           "LEFT JOIN FETCH so.subject " +
           "WHERE a.teacher.id = :teacherId ORDER BY a.pinned DESC, a.publishedAt DESC")
    List<CourseAnnouncement> findByTeacherIdWithDetailsOrderByPinnedDescPublishedAtDesc(Long teacherId);

    List<CourseAnnouncement> findBySubjectOfferingIdOrderByPinnedDescPublishedAtDesc(Long subjectOfferingId);

    @Query("SELECT a FROM CourseAnnouncement a " +
           "LEFT JOIN FETCH a.teacher " +
           "LEFT JOIN FETCH a.subjectOffering so " +
           "LEFT JOIN FETCH so.subject " +
           "WHERE a.subjectOffering.id = :offeringId AND a.published = true " +
           "ORDER BY a.pinned DESC, a.publishedAt DESC")
    List<CourseAnnouncement> findPublishedBySubjectOfferingIdWithDetailsOrderByPinnedDescPublishedAtDesc(Long offeringId);

    @Query("SELECT a FROM CourseAnnouncement a " +
           "LEFT JOIN FETCH a.teacher " +
           "LEFT JOIN FETCH a.subjectOffering so " +
           "LEFT JOIN FETCH so.subject " +
           "WHERE a.teacher.id = :teacherId AND a.publicVisible = true AND a.published = true " +
           "ORDER BY a.pinned DESC, a.publishedAt DESC")
    List<CourseAnnouncement> findByTeacherIdAndPublicVisibleTrueAndPublishedTrueWithDetails(Long teacherId);

    @Query("SELECT a FROM CourseAnnouncement a " +
           "LEFT JOIN FETCH a.subjectOffering so " +
           "LEFT JOIN FETCH so.subject " +
           "WHERE a.subjectOffering.id IN :offeringIds AND a.published = true " +
           "ORDER BY a.pinned DESC, a.publishedAt DESC")
    List<CourseAnnouncement> findPublishedForOfferingIdsWithDetailsOrderByPinnedDescPublishedAtDesc(List<Long> offeringIds);

    @Query(value = "SELECT a FROM CourseAnnouncement a " +
                   "LEFT JOIN FETCH a.subjectOffering so " +
                   "LEFT JOIN FETCH so.subject " +
                   "WHERE a.subjectOffering.id IN :offeringIds AND a.published = true",
           countQuery = "SELECT count(a) FROM CourseAnnouncement a " +
                        "WHERE a.subjectOffering.id IN :offeringIds AND a.published = true")
    Page<CourseAnnouncement> findPublishedForOfferingIdsWithDetails(List<Long> offeringIds, Pageable pageable);

    List<CourseAnnouncement> findByPublishedFalseAndScheduledAtBefore(Instant now);
}
