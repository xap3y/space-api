package me.xap3y.space.repository;

import me.xap3y.space.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByUniqueId(String uniqueId);

    Long countByUploadPackId(Long uploadPackId);
}