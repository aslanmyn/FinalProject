package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.CampusNavigationEdge;

import java.util.List;

public interface CampusNavigationEdgeRepository extends JpaRepository<CampusNavigationEdge, Long> {

    @Query("SELECT e FROM CampusNavigationEdge e WHERE e.fromRoom.id = :roomId OR e.toRoom.id = :roomId")
    List<CampusNavigationEdge> findEdgesByRoomId(Long roomId);

    @Query("SELECT e FROM CampusNavigationEdge e WHERE e.accessible = true")
    List<CampusNavigationEdge> findAllAccessible();
}
