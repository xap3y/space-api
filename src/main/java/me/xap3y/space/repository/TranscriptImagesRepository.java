package me.xap3y.space.repository;

import me.xap3y.space.entity.TranscriptImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranscriptImagesRepository extends JpaRepository<TranscriptImage, Long> {

    Optional<TranscriptImage> findByUniqueId(String uniqueId);

    List<TranscriptImage> getAllByUploader_Id(Long uploaderId);

    long count();
}
