package com.competition.repository;

import com.competition.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByRelatedTypeAndRelatedId(String relatedType, Long relatedId);

    void deleteByRelatedTypeAndRelatedId(String relatedType, Long relatedId);
}
