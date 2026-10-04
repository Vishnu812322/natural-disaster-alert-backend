package com.disasteralert.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String email;
    @Column(nullable=false) private String passwordHash;
    @Column(nullable=false) private String name;
    @Column(unique=true) private String phone;
    private String role = "ADMIN";
    private Double latitude;
    private Double longitude;
    private String district;
    private String state;
    private boolean enabled = true;
    private Instant createdAt = Instant.now();
}
