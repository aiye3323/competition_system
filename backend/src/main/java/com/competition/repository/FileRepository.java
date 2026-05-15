package com.competition.repository;

import com.competition.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long>,
        JpaSpecificationExecutor<FileEntity> {

    List<FileEntity> findByRelatedTypeAndRelatedId(String relatedType, Long relatedId);

    void deleteByRelatedTypeAndRelatedId(String relatedType, Long relatedId);

    List<FileEntity> findByUploaderId(Long uploaderId);

    @Query("SELECT f FROM FileEntity f WHERE f.uploaderId = :uploaderId AND f.isDeleted = 0")
    List<FileEntity> findActiveByUploaderId(@Param("uploaderId") Long uploaderId);

    @Query("SELECT f FROM FileEntity f WHERE f.uploaderId = :uploaderId AND f.isDeleted = 0 AND f.fileType = :fileType")
    List<FileEntity> findActiveByUploaderIdAndFileType(@Param("uploaderId") Long uploaderId, @Param("fileType") String fileType);

    @Query("SELECT f FROM FileEntity f WHERE f.uploaderId = :uploaderId AND f.isDeleted = 0 AND f.relatedType = :relatedType")
    List<FileEntity> findActiveByUploaderIdAndRelatedType(@Param("uploaderId") Long uploaderId, @Param("relatedType") String relatedType);
}
