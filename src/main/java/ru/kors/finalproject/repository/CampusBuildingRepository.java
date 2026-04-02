package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.CampusBuilding;

import java.util.List;

public interface CampusBuildingRepository extends JpaRepository<CampusBuilding, Long> {

    List<CampusBuilding> findByBuildingType(CampusBuilding.BuildingType buildingType);

    List<CampusBuilding> findByNameContainingIgnoreCase(String name);
}
