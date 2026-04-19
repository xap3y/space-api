package me.xap3y.space.model.request;


import me.xap3y.space.api.enums.ResourceSourceType;
import java.util.List;

public record FileRegisterRequest(
        List<FileRegisterItemRequest> items,
        String password,
        String description,
        ResourceSourceType source
) {}