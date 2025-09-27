package me.xap3y.space.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.xap3y.space.entity.InboundEmail;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    public InboundEmailDto(InboundEmail email) {
        this.subject = email.getSubject();
        this.from = email.getFromAddress();
        this.to = email.getToAddresses();
        this.cc = email.getCcAddresses();
        this.date = email.getSentDate().toString();
        this.messageId = email.getMessageId();
        this.text = email.getTextBody();
        this.html = email.getHtmlBody();
        this.envelope = new Envelope();
    }
}