package me.xap3y.space.repository;

import me.xap3y.space.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByUniqueId(String uniqueId);

    boolean existsByUniqueId(String uniqueId);
}
