package com.disasteralert.devices;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="user_devices")
@Getter @Setter @NoArgsConstructor
public class UserDevice {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false) Long userId;
    @Column(nullable=false, unique=true, length=500) String fcmToken;
    String deviceName;
    String platform;
    Instant lastSeen = Instant.now();
}
