package me.xap3y.space.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserStats {

    private int totalUploads;
    private long storageUsed;
    private int totalDownloads;
    private int pastesCreated;
    private int urlsShortened;

    public UserStats() {
        this.totalUploads = 0;
        this.storageUsed = 0;
        this.totalDownloads = 0;
        this.pastesCreated = 0;
        this.urlsShortened = 0;
    }

}
