package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.MeetingTime;

import java.util.List;

public interface MeetingTimeRepository extends JpaRepository<MeetingTime, Long> {
    List<MeetingTime> findBySubjectOfferingId(Long subjectOfferingId);
}
