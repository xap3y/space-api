package me.xap3y.space.repository;

import me.xap3y.space.entity.FileUploadPack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileUploadPackRepository extends JpaRepository<FileUploadPack, Long> {
    Optional<FileUploadPack> findByPackId(String packId);

    Page<FileUploadPack> findByUploaderId(Long uploaderId, Pageable pageable);
}
