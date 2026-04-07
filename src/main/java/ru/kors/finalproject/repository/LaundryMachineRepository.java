package ru.kors.finalproject.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.LaundryMachine;

import java.util.List;
import java.util.Optional;

public interface LaundryMachineRepository extends JpaRepository<LaundryMachine, Long> {

    List<LaundryMachine> findByLaundryRoomId(Long laundryRoomId);

    List<LaundryMachine> findByLaundryRoomIdAndStatus(Long laundryRoomId, LaundryMachine.MachineStatus status);

    int countByLaundryRoomIdAndStatus(Long laundryRoomId, LaundryMachine.MachineStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM LaundryMachine m WHERE m.id = :id")
    Optional<LaundryMachine> findByIdForUpdate(Long id);
}
