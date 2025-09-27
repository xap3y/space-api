package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.dto.ImageAlbumDto;
import me.xap3y.space.entity.ImagePlaylist;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ImageAlbumMapper;
import me.xap3y.space.model.request.AddPlaylistImageRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ImagePlaylistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/v1/image/playlist")
public class ImagePlaylistController {

    private final ImagePlaylistService imagePlaylistService;
    private final ImageAlbumMapper imageAlbumMapper;


    @GetMapping("/get/{identifier}")
    public ResponseEntity<?> getImagePlaylist(
            HttpServletRequest request,
            @PathVariable(value = "identifier") String identifier
    ) {
        ImagePlaylist playlist = imagePlaylistService.getPlaylistByUniqueId(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found"));

        ImageAlbumDto imageAlbumDto = imageAlbumMapper.apply(playlist);

        return new ResponseEntity<>(new DefaultResponse(false, imageAlbumDto), HttpStatus.OK);
    }

    @PutMapping("/get/{identifier}/images")
    @RequiresApiKey
    public ResponseEntity<?> addImageToPlaylist(
            HttpServletRequest request,
            @PathVariable(value = "identifier") String identifier,
            @RequestBody AddPlaylistImageRequest body
            ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) throw new InvalidApiKeyException();
        else if (body.getImagesUids() == null || body.getImagesUids().isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "Image UID is required"), HttpStatus.BAD_REQUEST);
        }

        boolean playListExists = imagePlaylistService.exists(identifier);
        if (!playListExists) {
            return new ResponseEntity<>(new DefaultResponse(true, "Playlist not found"), HttpStatus.NOT_FOUND);
        }

        for (String imageUid : body.getImagesUids()) {
            boolean exists = imagePlaylistService.existsImageInPlaylistByUids(identifier, imageUid);

            if (exists) {
                return new ResponseEntity<>(new DefaultResponse(true, "Image with UID " + imageUid + " is already exists in playlist"), HttpStatus.BAD_REQUEST);
            }

            imagePlaylistService.addImageToPlaylist(identifier, imageUid);
        }

        return new ResponseEntity<>(new DefaultResponse(false, body.getImagesUids().size() + " Images added to playlist"), HttpStatus.OK);
    }

    @DeleteMapping("/get/{identifier}/images/{imageUid}")
    @RequiresApiKey
    public ResponseEntity<?> removeImageFromPlaylist(
            HttpServletRequest request,
            @PathVariable(value = "identifier") String identifier,
            @PathVariable(value = "imageUid") String imageUid
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) throw new InvalidApiKeyException();

        boolean playListExists = imagePlaylistService.exists(identifier);

        if (!playListExists) {
            return new ResponseEntity<>(new DefaultResponse(true, "Playlist not found"), HttpStatus.NOT_FOUND);
        }

        boolean exists = imagePlaylistService.existsImageInPlaylistByUids(identifier, imageUid);

        if (!exists) {
            return new ResponseEntity<>(new DefaultResponse(true, "Image doesn't exists in this playlist"), HttpStatus.BAD_REQUEST);
        }

        imagePlaylistService.removeImageFromPlaylist(identifier, imageUid);

        return new ResponseEntity<>(new DefaultResponse(false, "Image removed from playlist"), HttpStatus.OK);
    }

}
