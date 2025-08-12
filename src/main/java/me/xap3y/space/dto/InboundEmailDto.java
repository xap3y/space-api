package me.xap3y.space.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboundEmailDto {
    public static class Attachment {
        public String filename;
        public String contentType;
        public long size;
        public String contentBase64;
    }

    public static class Envelope {
        public String from;
        public String to;
        public String helo;
        public String mailFrom;
        public List<String> rcptTo;
    }

    public Envelope envelope;
    public Map<String, String> headers;
    public String subject;
    public String from;
    public String to;
    public String cc;
    public String date;
    public String messageId;
    public String text;
    public String html;
    public List<Attachment> attachments;
}