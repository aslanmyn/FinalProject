package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.TeacherStudentNote;

import java.util.List;
import java.util.Optional;

public interface TeacherStudentNoteRepository extends JpaRepository<TeacherStudentNote, Long> {
    List<TeacherStudentNote> findByTeacherIdAndSubjectOfferingIdOrderByCreatedAtDesc(Long teacherId, Long subjectOfferingId);

    Optional<TeacherStudentNote> findByTeacherIdAndStudentIdAndSubjectOfferingId(Long teacherId, Long studentId, Long subjectOfferingId);
}
