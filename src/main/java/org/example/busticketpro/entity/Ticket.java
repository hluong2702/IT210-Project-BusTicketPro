package org.example.busticketpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.busticketpro.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, length = 100)
    private String passengerName;

    @Column(nullable = false, length = 15)
    private String passengerPhone;

    @Column(length = 100)
    private String passengerEmail;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime bookedAt;

    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;

    @Column(length = 255)
    private String cancellationReason;

    @Column(length = 100)
    private String paymentTransactionId;

    @Column(length = 100)
    private String paymentReference;

    // Staff who approved
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @PrePersist
    protected void onCreate() {
        bookedAt = LocalDateTime.now();
        if (ticketCode == null) {
            ticketCode = generateCode();
        }
    }

    private String generateCode() {
        return "BTP" + System.currentTimeMillis() % 10000000L;
    }
}
