package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.SocialActivity;

import java.util.List;

public interface SocialActivityRepository extends JpaRepository<SocialActivity, Long> {
    List<SocialActivity> findByStudentIdOrderByDateDesc(Long studentId);
}
