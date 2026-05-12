package org.example.busticketpro.service;

import org.example.busticketpro.dto.response.SeatResponse;
import org.example.busticketpro.entity.Seat;
import org.example.busticketpro.entity.User;

import java.util.List;

public interface SeatService {
    List<SeatResponse> getSeatMapForTrip(Long tripId);
    Seat holdSeat(Long seatId, User user);
    void releaseHold(Long seatId, Long userId);
    Seat findByIdWithLock(Long seatId);
}
