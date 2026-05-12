package org.example.busticketpro.service;

import org.example.busticketpro.entity.Ticket;

public interface EmailService {
    void sendBookingConfirmation(Ticket ticket);
    void sendPaymentConfirmation(Ticket ticket);
    void sendCancellationNotice(Ticket ticket);
}
