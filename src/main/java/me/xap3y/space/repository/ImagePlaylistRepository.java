package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.DiscordConnection;
import me.xap3y.space.entity.ImagePlaylist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImagePlaylistRepository extends JpaRepository<ImagePlaylist, Long> {

    Optional<ImagePlaylist> findByUniqueId(String uniqueId);

    @Transactional
    void deleteByUniqueId(String uniqueId);

    boolean existsByUniqueId(String uniqueId);

    Optional<ImagePlaylist> findById(Long id);

    @Query("""
    SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
    FROM ImagePlaylist p
    JOIN p.images i
    WHERE p.uniqueId = :playlistUid AND i.uniqueId = :imageUid
""")
    boolean existsImageInPlaylistByUids(@Param("playlistUid") String playlistUid, @Param("imageUid") String imageUid);
}
