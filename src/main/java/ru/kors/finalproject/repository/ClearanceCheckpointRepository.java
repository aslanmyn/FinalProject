package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.ClearanceCheckpoint;

import java.util.List;

public interface ClearanceCheckpointRepository extends JpaRepository<ClearanceCheckpoint, Long> {

    List<ClearanceCheckpoint> findByClearanceSheetId(Long clearanceSheetId);
}
