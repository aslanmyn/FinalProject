package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.DormRoom;

import java.util.List;

public interface DormRoomRepository extends JpaRepository<DormRoom, Long> {

    List<DormRoom> findByDormBuildingId(Long dormBuildingId);

    @Query("SELECT r FROM DormRoom r WHERE r.roomType = :roomType AND r.occupied < r.capacity")
    List<DormRoom> findAvailableByType(DormRoom.RoomType roomType);

    @Query("SELECT r FROM DormRoom r WHERE r.occupied < r.capacity")
    List<DormRoom> findAllAvailable();
}
