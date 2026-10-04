package com.disasteralert.disaster;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="disaster_types") @Getter @Setter @NoArgsConstructor
public class DisasterType {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false, unique=true) String code;
    @Column(nullable=false) String name;
}
