package me.xap3y.space.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.ImagePlaylist;
import me.xap3y.space.repository.ImagePlaylistRepository;
import me.xap3y.space.repository.ImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ImagePlaylistService {

    private final ImagePlaylistRepository imagePlaylistRepository;
    private final ImageRepository imageRepository;

    public ImagePlaylistService(ImagePlaylistRepository imagePlaylistRepository, ImageRepository imageRepository) {
        this.imagePlaylistRepository = imagePlaylistRepository;
        this.imageRepository = imageRepository;
    }

    @Transactional
    public ImagePlaylist createPlaylist(ImagePlaylist playlist) {
        return imagePlaylistRepository.save(playlist);
    }

    public Optional<ImagePlaylist> getPlaylistByUniqueId(String uniqueId) {
        return imagePlaylistRepository.findByUniqueId(uniqueId);
    }

    public List<Image> getImagesByPlaylistId(Long playlistId) {
        return imagePlaylistRepository.findById(playlistId)
                .map(ImagePlaylist::getImages)
                .orElseThrow(() -> new EntityNotFoundException("Playlist not found"));
    }

    @Transactional
    public void addImageToPlaylist(String playlistUid, String imageUid) {
        ImagePlaylist playlist = imagePlaylistRepository.findByUniqueId(playlistUid)
                .orElseThrow(() -> new EntityNotFoundException("Playlist not found"));
        Image image = imageRepository.findByUniqueId(imageUid)
                .orElseThrow(() -> new EntityNotFoundException("Image not found"));
        if (!playlist.getImages().contains(image)) {
            playlist.getImages().add(image);
            imagePlaylistRepository.save(playlist);
        }
    }

    @Transactional
    public void removeImageFromPlaylist(String playlistUid, String imageUid) {
        ImagePlaylist playlist = imagePlaylistRepository.findByUniqueId(playlistUid)
                .orElseThrow(() -> new EntityNotFoundException("Playlist not found"));
        Image image = imageRepository.findByUniqueId(imageUid)
                .orElseThrow(() -> new EntityNotFoundException("Image not found"));
        if (playlist.getImages().contains(image)) {
            playlist.getImages().remove(image);
            imagePlaylistRepository.save(playlist);
        }
    }

    public boolean existsImageInPlaylistByUids(String playlistUid, String imageUid) {
        return imagePlaylistRepository.existsImageInPlaylistByUids(playlistUid, imageUid);
    }

    public boolean exists(String uniqueId) {
        return imagePlaylistRepository.existsByUniqueId(uniqueId);
    }
}
