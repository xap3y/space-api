package me.xap3y.space.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class DiscordTranscript {

    private String generatedAt;
    private String channelId;
    private String channelName;
    private String channelTopic;
    private String createdBy;
    private String target;
    private String closedBy;
    private String closeComment;
    private List<MessageEntry> messages;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageEntry {
        private String id;
        private String timestamp;
        private AuthorEntry author;
        private String replyToMessageId;
        private String content;
        private List<AttachmentEntry> attachments;
        private List<StickerEntry> stickers;
        private List<EmbedEntry> embeds;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StickerEntry {
        String name;
        String formatType;
        String ext;
        String url;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorEntry {
        private String username;
        private String avatarUrl;
        private String color;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttachmentEntry {
        private String filename;
        private String url;
        private String safeUrl;
        private long size;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbedEntry {
        private String title;
        private String description;
        private String url;
        private String timestamp;
        private String color;
        private String author;
        private String footer;
        private List<EmbedFieldEntry> fields;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbedFieldEntry {
        private String name;
        private String value;
        private boolean inline;
    }
}