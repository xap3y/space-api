package me.xap3y.space.model.request;

public record FileRegisterItemRequest(
        String uniqueId,
        String fileName,
        String fileType,
        Long size,
        String description,
        String password,
        Long expiryDate
) {
}
