package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.CampusRoom;

import java.util.List;
import java.util.Optional;

public interface CampusRoomRepository extends JpaRepository<CampusRoom, Long> {

    @Query("SELECT r FROM CampusRoom r JOIN FETCH r.building WHERE r.building.id = :buildingId")
    List<CampusRoom> findByBuildingId(Long buildingId);

    @Query("SELECT r FROM CampusRoom r JOIN FETCH r.building WHERE r.id = :id")
    Optional<CampusRoom> findByIdWithBuilding(Long id);

    @Query("SELECT r FROM CampusRoom r JOIN FETCH r.building WHERE LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<CampusRoom> searchByQuery(String query);

    List<CampusRoom> findByRoomType(CampusRoom.CampusRoomType roomType);
}
