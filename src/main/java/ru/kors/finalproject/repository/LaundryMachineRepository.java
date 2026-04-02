package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.LaundryMachine;

import java.util.List;

public interface LaundryMachineRepository extends JpaRepository<LaundryMachine, Long> {

    List<LaundryMachine> findByLaundryRoomId(Long laundryRoomId);

    List<LaundryMachine> findByLaundryRoomIdAndStatus(Long laundryRoomId, LaundryMachine.MachineStatus status);

    int countByLaundryRoomIdAndStatus(Long laundryRoomId, LaundryMachine.MachineStatus status);
}
