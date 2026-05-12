package org.example.busticketpro.service;

import org.example.busticketpro.dto.request.BookingRequest;
import org.example.busticketpro.dto.response.TicketResponse;
import org.example.busticketpro.entity.Ticket;

import java.util.List;

public interface TicketService {
    Ticket book(BookingRequest request, Long passengerId);
    TicketResponse lookup(String ticketCode, String phone);
    List<TicketResponse> findByPassenger(Long passengerId);
    List<TicketResponse> findAllPending();
    List<TicketResponse> findAll();
    void confirm(Long ticketId, Long staffId);
    void cancelByStaff(Long ticketId, Long staffId, String reason);
    void cancelByPassenger(Long ticketId, Long passengerId);
    void autoCancelExpired();
    TicketResponse toResponse(Ticket ticket);
}
