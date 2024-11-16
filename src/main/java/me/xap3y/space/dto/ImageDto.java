package me.xap3y.space.dto;

import me.xap3y.space.entity.User;

public record ImageDto(
        byte[] bytes,
        User uploader,
        String type,
        long size,
        String base64
) { }
