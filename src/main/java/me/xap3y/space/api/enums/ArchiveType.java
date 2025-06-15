package me.xap3y.space.api.enums;

import lombok.Getter;

@Getter
public enum ArchiveType {
    ZIP,
    TAR_GZ,
    RAR;

    private final String extension;

    ArchiveType() {
        this.extension = name().replaceAll("_", ".").toLowerCase();
    }

    public static ArchiveType getExtensionType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        String lowerCaseFileName = fileName.toLowerCase();
        for (ArchiveType type : ArchiveType.values()) {
            if (lowerCaseFileName.endsWith(type.getExtension())) {
                return type;
            }
        }
        return null;
    }


}
