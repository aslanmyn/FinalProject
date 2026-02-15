package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.FileAsset;

import java.util.List;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {
    List<FileAsset> findByOwnerStudentIdOrderByUploadedAtDesc(Long ownerStudentId);

    List<FileAsset> findByLinkedEntityTypeAndLinkedEntityIdOrderByUploadedAtDesc(String linkedEntityType, Long linkedEntityId);
}
