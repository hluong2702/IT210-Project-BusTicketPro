package org.example.busticketpro.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.busticketpro.dto.request.BookingRequest;
import org.example.busticketpro.dto.response.TicketResponse;
import org.example.busticketpro.entity.*;
import org.example.busticketpro.enums.SeatStatus;
import org.example.busticketpro.enums.TicketStatus;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.exception.ResourceNotFoundException;
import org.example.busticketpro.exception.SeatConflictException;
import org.example.busticketpro.repository.*;
import org.example.busticketpro.service.EmailService;
import org.example.busticketpro.service.TicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.ticket.auto-cancel-minutes:30}")
    private int autoCancelMinutes;

    @Value("${app.ticket.cancel-before-hours:12}")
    private int cancelBeforeHours;

    @Override
    public Ticket book(BookingRequest request, Long passengerId) {
        // CORE-06: Transactional booking with optimistic locking
        User passenger = userRepository.findById(passengerId)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng", passengerId));

        Trip trip = tripRepository.findByIdWithDetails(request.getTripId())
            .orElseThrow(() -> new ResourceNotFoundException("Chuyến xe", request.getTripId()));

        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("TRIP_DEPARTED", "Chuyến xe này đã khởi hành");
        }

        // Pessimistic lock on seat to prevent race condition
        Seat seat = seatRepository.findByIdWithLock(request.getSeatId())
            .orElseThrow(() -> new ResourceNotFoundException("Ghế", request.getSeatId()));

        // Validate seat belongs to the trip
        if (!seat.getTrip().getId().equals(trip.getId())) {
            throw new BusinessException("INVALID_SEAT", "Ghế không thuộc chuyến xe này");
        }

        // Check seat availability (handle expired holds)
        LocalDateTime now = LocalDateTime.now();
        boolean expiredHold = seat.getStatus() == SeatStatus.PENDING &&
            seat.getHeldUntil() != null && seat.getHeldUntil().isBefore(now);

        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new SeatConflictException("Ghế " + seat.getSeatNumber() + " đã được đặt. Vui lòng chọn ghế khác.");
        }
        if (seat.getStatus() == SeatStatus.PENDING && !expiredHold) {
            // Allow if the hold belongs to this same passenger
            if (seat.getHeldByUser() == null || !seat.getHeldByUser().getId().equals(passengerId)) {
                throw new SeatConflictException("Ghế " + seat.getSeatNumber() + " đang được giữ. Vui lòng chọn ghế khác.");
            }
        }

        try {
            // Update seat status to PENDING (within same transaction)
            seat.setStatus(SeatStatus.PENDING);
            seat.setHeldUntil(now.plusMinutes(autoCancelMinutes));
            seat.setHeldByUser(passenger);
            seatRepository.save(seat);

            // Generate unique ticket code
            String code = "BTP" + System.currentTimeMillis() % 10_000_000L +
                UUID.randomUUID().toString().substring(0, 4).toUpperCase();

            // Create ticket
            Ticket ticket = Ticket.builder()
                .ticketCode(code)
                .passenger(passenger)
                .trip(trip)
                .seat(seat)
                .passengerName(request.getPassengerName())
                .passengerPhone(request.getPassengerPhone())
                .passengerEmail(request.getPassengerEmail())
                .totalAmount(trip.getPrice())
                .status(TicketStatus.PENDING)
                .build();

            ticket = ticketRepository.save(ticket);
            log.info("Ticket created: {} for seat {}", ticket.getTicketCode(), seat.getSeatNumber());

            // Send confirmation email asynchronously (Extension 3)
            final Ticket finalTicket = ticket;
            if (request.getPassengerEmail() != null && !request.getPassengerEmail().isBlank()) {
                emailService.sendBookingConfirmation(finalTicket);
            }

            return ticket;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SeatConflictException("Ghế vừa được đặt bởi người khác. Vui lòng chọn ghế khác.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse lookup(String ticketCode, String phone) {
        Ticket ticket = ticketRepository.findByCodeAndPhone(ticketCode.trim(), phone.trim())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Không tìm thấy vé với mã " + ticketCode + " và SĐT " + phone));
        return toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByPassenger(Long passengerId) {
        return ticketRepository.findByPassengerIdWithDetails(passengerId)
            .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findAllPending() {
        return ticketRepository.findByStatusWithDetails(TicketStatus.PENDING)
            .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findAll() {
        return ticketRepository.findAllWithDetails()
            .stream().map(this::toResponse).toList();
    }

    @Override
    public void confirm(Long ticketId, Long staffId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Vé", ticketId));

        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS",
                "Chỉ có thể xác nhận vé ở trạng thái Chờ thanh toán");
        }

        User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", staffId));

        // Update ticket
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPaidAt(LocalDateTime.now());
        ticket.setApprovedBy(staff);

        // Update seat to BOOKED
        Seat seat = ticket.getSeat();
        seat.setStatus(SeatStatus.BOOKED);
        seat.setHeldUntil(null);
        seatRepository.save(seat);

        ticketRepository.save(ticket);
        log.info("Ticket {} confirmed by staff {}", ticket.getTicketCode(), staffId);

        // Send payment confirmation email
        if (ticket.getPassengerEmail() != null) {
            emailService.sendPaymentConfirmation(ticket);
        }
    }

    @Override
    public void cancelByStaff(Long ticketId, Long staffId, String reason) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Vé", ticketId));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessException("ALREADY_CANCELLED", "Vé đã bị hủy trước đó");
        }
        if (ticket.getStatus() == TicketStatus.PAID) {
            throw new BusinessException("ALREADY_PAID", "Không thể hủy vé đã thanh toán");
        }

        cancelTicket(ticket, reason);
        log.info("Ticket {} cancelled by staff {}", ticket.getTicketCode(), staffId);
    }

    @Override
    public void cancelByPassenger(Long ticketId, Long passengerId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Vé", ticketId));

        if (!ticket.getPassenger().getId().equals(passengerId)) {
            throw new BusinessException("UNAUTHORIZED", "Bạn không có quyền hủy vé này");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessException("ALREADY_CANCELLED", "Vé đã bị hủy trước đó");
        }
        if (ticket.getStatus() == TicketStatus.PAID) {
            throw new BusinessException("ALREADY_PAID",
                "Vé đã thanh toán. Vui lòng liên hệ nhân viên để được hỗ trợ hoàn vé.");
        }

        // CORE-09: Can only cancel 12 hours before departure
        LocalDateTime cutoff = ticket.getTrip().getDepartureTime().minusHours(cancelBeforeHours);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new BusinessException("CANCEL_TOO_LATE",
                "Chỉ có thể hủy vé trước " + cancelBeforeHours + " tiếng so với giờ khởi hành");
        }

        cancelTicket(ticket, "Hành khách chủ động hủy");
        log.info("Ticket {} cancelled by passenger {}", ticket.getTicketCode(), passengerId);
    }

    private void cancelTicket(Ticket ticket, String reason) {
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(LocalDateTime.now());
        ticket.setCancellationReason(reason);
        ticketRepository.save(ticket);

        // Release seat back to AVAILABLE
        Seat seat = ticket.getSeat();
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setHeldUntil(null);
        seat.setHeldByUser(null);
        seatRepository.save(seat);
    }

    @Override
    public void autoCancelExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(autoCancelMinutes);
        List<Ticket> expired = ticketRepository.findPendingOlderThan(cutoff);
        int count = 0;
        for (Ticket ticket : expired) {
            cancelTicket(ticket, "Tự động hủy do quá thời gian thanh toán");
            count++;
        }
        if (count > 0) {
            log.info("Auto-cancelled {} expired PENDING tickets", count);
        }
    }

    @Override
    public TicketResponse toResponse(Ticket ticket) {
        Trip trip = ticket.getTrip();
        Route route = trip.getRoute();
        Seat seat = ticket.getSeat();

        boolean canCancel = ticket.getStatus() == TicketStatus.PENDING &&
            LocalDateTime.now().isBefore(trip.getDepartureTime().minusHours(cancelBeforeHours));

        return TicketResponse.builder()
            .id(ticket.getId())
            .ticketCode(ticket.getTicketCode())
            .passengerName(ticket.getPassengerName())
            .passengerPhone(ticket.getPassengerPhone())
            .passengerEmail(ticket.getPassengerEmail())
            .departureName(route.getDepartureLocation().getName())
            .arrivalName(route.getArrivalLocation().getName())
            .departureTime(trip.getDepartureTime())
            .arrivalTime(trip.getArrivalTime())
            .licensePlate(trip.getBus().getLicensePlate())
            .busType(trip.getBus().getBusType().getDisplayName())
            .driverName(trip.getBus().getDriverName())
            .seatNumber(seat.getSeatNumber())
            .seatFloor(seat.getFloor())
            .totalAmount(ticket.getTotalAmount())
            .status(ticket.getStatus())
            .bookedAt(ticket.getBookedAt())
            .paidAt(ticket.getPaidAt())
            .cancelledAt(ticket.getCancelledAt())
            .canCancel(canCancel)
            .build();
    }
}
