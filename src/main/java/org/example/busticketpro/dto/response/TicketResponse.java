package org.example.busticketpro.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.busticketpro.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponse {
    private Long id;
    private String ticketCode;
    private String passengerName;
    private String passengerPhone;
    private String passengerEmail;
    private String departureName;
    private String arrivalName;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String licensePlate;
    private String busType;
    private String driverName;
    private String seatNumber;
    private Integer seatFloor;
    private BigDecimal totalAmount;
    private TicketStatus status;
    private LocalDateTime bookedAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private Boolean canCancel;
}
