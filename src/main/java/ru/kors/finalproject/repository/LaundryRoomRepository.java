package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.LaundryRoom;

import java.util.List;

public interface LaundryRoomRepository extends JpaRepository<LaundryRoom, Long> {
    List<LaundryRoom> findByDormBuildingId(Long dormBuildingId);
}
