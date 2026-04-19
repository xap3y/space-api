package me.xap3y.space.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3ObjectInfo {
    private String key;
    private Long size;
    private String lastModified;
}