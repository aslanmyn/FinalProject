package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.TeacherStudentNote;

import java.util.List;
import java.util.Optional;

public interface TeacherStudentNoteRepository extends JpaRepository<TeacherStudentNote, Long> {
    List<TeacherStudentNote> findByTeacherIdAndSubjectOfferingIdOrderByCreatedAtDesc(Long teacherId, Long subjectOfferingId);

    @Query("SELECT n FROM TeacherStudentNote n LEFT JOIN FETCH n.teacher LEFT JOIN FETCH n.student LEFT JOIN FETCH n.subjectOffering WHERE n.teacher.id = :teacherId AND n.subjectOffering.id = :offeringId ORDER BY n.createdAt DESC")
    List<TeacherStudentNote> findByTeacherIdAndSubjectOfferingIdWithDetailsOrderByCreatedAtDesc(Long teacherId, Long offeringId);

    Optional<TeacherStudentNote> findByTeacherIdAndStudentIdAndSubjectOfferingId(Long teacherId, Long studentId, Long subjectOfferingId);
}
