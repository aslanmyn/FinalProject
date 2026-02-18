package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.FileAsset;

import java.util.List;
import java.util.Optional;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {
    List<FileAsset> findByOwnerStudentIdOrderByUploadedAtDesc(Long ownerStudentId);

    List<FileAsset> findByLinkedEntityTypeAndLinkedEntityIdOrderByUploadedAtDesc(String linkedEntityType, Long linkedEntityId);

    @Query("SELECT f FROM FileAsset f " +
           "LEFT JOIN FETCH f.ownerStudent " +
           "LEFT JOIN FETCH f.uploadedBy " +
           "WHERE f.id = :id")
    Optional<FileAsset> findByIdWithDetails(Long id);
}
