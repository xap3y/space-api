package me.xap3y.space.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.xap3y.space.dto.InboundEmailDto;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "inbound_emails",
        indexes = {
                @Index(name = "uk_inbound_email_message_id", columnList = "message_id", unique = true),
                @Index(name = "idx_inbound_email_temp_mail", columnList = "temp_mail_id"),
                @Index(name = "idx_inbound_email_received_at", columnList = "received_at")
        })
@Data
@NoArgsConstructor
public class InboundEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", length = 255, unique = true)
    private String messageId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "temp_mail_id")
    private TempMail tempMail;

    @Column(name = "from_address", length = 512, nullable = false)
    private String fromAddress;

    @Column(name = "to_addresses", length = 1000, nullable = false)
    private String toAddresses;

    @Column(name = "cc_addresses", length = 1000)
    private String ccAddresses;

    @Column(length = 200)
    private String subject;

    @Lob
    @Column(name = "text_body", columnDefinition = "MEDIUMTEXT")
    private String textBody;

    @Lob
    @Column(name = "html_body", columnDefinition = "MEDIUMTEXT")
    private String htmlBody;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "received_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime receivedAt;

    // For workflow logic (e.g., processed by downstream consumer)
    @Column(name = "processed", nullable = false)
    @ColumnDefault("false")
    private boolean processed = false;

    public InboundEmail(
            TempMail tempMail,
            InboundEmailDto dto
    ) {
        this.messageId = dto.getMessageId();
        this.tempMail = tempMail;
        this.fromAddress = dto.getFrom();
        this.toAddresses = dto.getTo();
        this.ccAddresses = getCcAddresses();
        this.subject = (dto.subject.length() <= 200 ? dto.subject : dto.subject.substring(0, 200));
        this.textBody = dto.text;
        this.htmlBody = dto.getHtml();
        this.sentDate = ZonedDateTime.parse(dto.date).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();;
        this.receivedAt = LocalDateTime.now();
    }
}
