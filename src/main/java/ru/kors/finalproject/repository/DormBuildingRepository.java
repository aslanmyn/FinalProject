package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.DormBuilding;

public interface DormBuildingRepository extends JpaRepository<DormBuilding, Long> {
}
