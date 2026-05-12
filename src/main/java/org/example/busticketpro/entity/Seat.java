package org.example.busticketpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.busticketpro.enums.SeatStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "seats",
    uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "seat_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, length = 10)
    private String seatNumber;

    @Column(nullable = false)
    @Builder.Default
    private Integer floor = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    // Temporary hold tracking
    private LocalDateTime heldUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_user_id")
    private User heldByUser;

    @Version
    private Long version; // Optimistic locking

    public boolean isAvailable() {
        if (status == SeatStatus.AVAILABLE) return true;
        if (status == SeatStatus.PENDING && heldUntil != null && heldUntil.isBefore(LocalDateTime.now())) {
            return true; // Hold expired
        }
        return false;
    }
}
