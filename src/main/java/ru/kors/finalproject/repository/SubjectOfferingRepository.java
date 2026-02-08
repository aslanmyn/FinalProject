package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.SubjectOffering;

import java.util.List;

public interface SubjectOfferingRepository extends JpaRepository<SubjectOffering, Long> {
    List<SubjectOffering> findBySemesterId(Long semesterId);

    @Query("SELECT so FROM SubjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH so.teacher WHERE so.semester.id = :semesterId")
    List<SubjectOffering> findBySemesterIdWithDetails(Long semesterId);
}
