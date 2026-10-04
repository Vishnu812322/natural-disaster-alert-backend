package com.disasteralert.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="notification_logs")
@Getter @Setter @NoArgsConstructor
public class NotificationLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    Long alertId;
    Long userId;
    Long deviceId;
    String channel;
    String status;
    @Column(length=4000) String providerMessage;
    Instant createdAt = Instant.now();
}
