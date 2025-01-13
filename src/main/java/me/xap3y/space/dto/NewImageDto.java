package me.xap3y.space.dto;

import me.xap3y.space.entity.User;

import java.nio.file.Path;

public record NewImageDto (
        Path path,
        User uploader,
        String type,
        long size,
        String base64
){ }
