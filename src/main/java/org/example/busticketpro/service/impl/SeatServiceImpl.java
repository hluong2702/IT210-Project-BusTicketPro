package org.example.busticketpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.busticketpro.dto.response.SeatResponse;
import org.example.busticketpro.entity.Seat;
import org.example.busticketpro.entity.User;
import org.example.busticketpro.enums.SeatStatus;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.exception.ResourceNotFoundException;
import org.example.busticketpro.exception.SeatConflictException;
import org.example.busticketpro.repository.SeatRepository;
import org.example.busticketpro.service.SeatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;

    @Value("${app.seat.hold-duration-minutes:15}")
    private int holdDurationMinutes;

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatMapForTrip(Long tripId) {
        LocalDateTime now = LocalDateTime.now();
        return seatRepository.findByTripIdOrderBySeatNumber(tripId).stream()
            .map(seat -> {
                boolean available = seat.getStatus() == SeatStatus.AVAILABLE ||
                    (seat.getStatus() == SeatStatus.PENDING &&
                        seat.getHeldUntil() != null && seat.getHeldUntil().isBefore(now));
                return SeatResponse.builder()
                    .id(seat.getId())
                    .seatNumber(seat.getSeatNumber())
                    .floor(seat.getFloor())
                    .status(seat.getStatus())
                    .available(available)
                    .build();
            })
            .collect(Collectors.toList());
    }

    @Override
    public Seat holdSeat(Long seatId, User user) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
            .orElseThrow(() -> new ResourceNotFoundException("Ghế", seatId));

        LocalDateTime now = LocalDateTime.now();
        boolean expired = seat.getStatus() == SeatStatus.PENDING &&
            seat.getHeldUntil() != null && seat.getHeldUntil().isBefore(now);

        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new SeatConflictException("Ghế " + seat.getSeatNumber() + " đã được đặt. Vui lòng chọn ghế khác.");
        }
        if (seat.getStatus() == SeatStatus.PENDING && !expired) {
            throw new SeatConflictException("Ghế " + seat.getSeatNumber() + " đang được giữ chỗ. Vui lòng chọn ghế khác.");
        }

        seat.setStatus(SeatStatus.PENDING);
        seat.setHeldUntil(now.plusMinutes(holdDurationMinutes));
        seat.setHeldByUser(user);
        return seatRepository.save(seat);
    }

    @Override
    public void releaseHold(Long seatId, Long userId) {
        seatRepository.findById(seatId).ifPresent(seat -> {
            if (seat.getStatus() == SeatStatus.PENDING &&
                seat.getHeldByUser() != null &&
                seat.getHeldByUser().getId().equals(userId)) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldUntil(null);
                seat.setHeldByUser(null);
                seatRepository.save(seat);
            }
        });
    }

    @Override
    public Seat findByIdWithLock(Long seatId) {
        return seatRepository.findByIdWithLock(seatId)
            .orElseThrow(() -> new ResourceNotFoundException("Ghế", seatId));
    }
}
