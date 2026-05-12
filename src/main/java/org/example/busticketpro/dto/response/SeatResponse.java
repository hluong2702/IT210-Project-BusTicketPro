package org.example.busticketpro.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.busticketpro.enums.SeatStatus;

@Data
@Builder
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private Integer floor;
    private SeatStatus status;
    private boolean available;
}
