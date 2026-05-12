package org.example.busticketpro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 100)
    private String province;

    @Column(length = 200)
    private String address;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "departureLocation", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Route> departureRoutes = new ArrayList<>();

    @OneToMany(mappedBy = "arrivalLocation", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Route> arrivalRoutes = new ArrayList<>();
}
