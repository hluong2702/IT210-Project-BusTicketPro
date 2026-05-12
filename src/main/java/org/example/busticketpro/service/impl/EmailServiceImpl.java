package org.example.busticketpro.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.busticketpro.entity.Ticket;
import org.example.busticketpro.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Async
    @Override
    public void sendBookingConfirmation(Ticket ticket) {
        try {
            if (!isEmailConfigured()) {
                log.warn("Email service not configured. Booking email for ticket {} would have been sent to {}",
                    ticket.getTicketCode(), ticket.getPassengerEmail());
                return;
            }
            String subject = "[BusTicketPro] Xác nhận đặt vé #" + ticket.getTicketCode();
            String content = buildBookingEmail(ticket);
            sendHtmlEmail(ticket.getPassengerEmail(), subject, content);
            log.info("Booking confirmation email sent to {}", ticket.getPassengerEmail());
        } catch (MessagingException e) {
            log.error("Failed to send booking email for ticket {}: Messaging error - {}", ticket.getTicketCode(), e.getMessage());
        } catch (MailException e) {
            log.error("Failed to send booking email for ticket {}: Mail server error - {}", ticket.getTicketCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send booking email for ticket {}: {}", ticket.getTicketCode(), e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void sendPaymentConfirmation(Ticket ticket) {
        try {
            if (!isEmailConfigured()) {
                log.warn("Email service not configured. Payment confirmation for ticket {} would have been sent to {}",
                    ticket.getTicketCode(), ticket.getPassengerEmail());
                return;
            }
            String subject = "[BusTicketPro] Thanh toán thành công - Vé #" + ticket.getTicketCode();
            String content = buildPaymentEmail(ticket);
            sendHtmlEmail(ticket.getPassengerEmail(), subject, content);
            log.info("Payment confirmation email sent to {}", ticket.getPassengerEmail());
        } catch (MessagingException e) {
            log.error("Failed to send payment email for ticket {}: Messaging error - {}", ticket.getTicketCode(), e.getMessage());
        } catch (MailException e) {
            log.error("Failed to send payment email for ticket {}: Mail server error - {}", ticket.getTicketCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send payment email for ticket {}: {}", ticket.getTicketCode(), e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void sendCancellationNotice(Ticket ticket) {
        try {
            if (ticket.getPassengerEmail() == null) return;
            if (!isEmailConfigured()) {
                log.warn("Email service not configured. Cancellation notice for ticket {} would have been sent to {}",
                    ticket.getTicketCode(), ticket.getPassengerEmail());
                return;
            }
            String subject = "[BusTicketPro] Vé đã bị hủy - #" + ticket.getTicketCode();
            String content = buildCancellationEmail(ticket);
            sendHtmlEmail(ticket.getPassengerEmail(), subject, content);
            log.info("Cancellation notice email sent to {}", ticket.getPassengerEmail());
        } catch (MessagingException e) {
            log.error("Failed to send cancellation email for ticket {}: Messaging error - {}", ticket.getTicketCode(), e.getMessage());
        } catch (MailException e) {
            log.error("Failed to send cancellation email for ticket {}: Mail server error - {}", ticket.getTicketCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send cancellation email for ticket {}: {}", ticket.getTicketCode(), e.getMessage(), e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException, UnsupportedEncodingException {
        if (!isEmailConfigured()) {
            throw new MailException("Email service is not configured. Please set MAIL_USERNAME and MAIL_PASSWORD environment variables.") {};
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail, "BusTicketPro");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private boolean isEmailConfigured() {
        return fromEmail != null && !fromEmail.isEmpty() && !fromEmail.equals("busticketpro@gmail.com") &&
               mailPassword != null && !mailPassword.isEmpty() && !mailPassword.equals("your-app-password");
    }

    private String buildBookingEmail(Ticket t) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
            <div style="max-width:600px;margin:auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1)">
              <div style="background:linear-gradient(135deg,#667eea,#764ba2);padding:30px;text-align:center">
                <h1 style="color:white;margin:0">🚌 BusTicketPro</h1>
                <p style="color:rgba(255,255,255,0.9);margin:8px 0 0">Xác nhận đặt vé thành công</p>
              </div>
              <div style="padding:30px">
                <p>Kính gửi <strong>%s</strong>,</p>
                <p>Vé của bạn đã được đặt thành công. Vui lòng thanh toán trong vòng 30 phút.</p>
                <div style="background:#f8f9ff;border-radius:8px;padding:20px;margin:20px 0;border-left:4px solid #667eea">
                  <h3 style="margin:0 0 15px;color:#333">Thông tin vé</h3>
                  <table style="width:100%%">
                    <tr><td style="color:#666;padding:4px 0">Mã vé:</td><td><strong style="color:#667eea;font-size:18px">%s</strong></td></tr>
                    <tr><td style="color:#666;padding:4px 0">Tuyến:</td><td><strong>%s → %s</strong></td></tr>
                    <tr><td style="color:#666;padding:4px 0">Giờ khởi hành:</td><td><strong>%s</strong></td></tr>
                    <tr><td style="color:#666;padding:4px 0">Ghế:</td><td><strong>%s</strong></td></tr>
                    <tr><td style="color:#666;padding:4px 0">Xe:</td><td><strong>%s - %s</strong></td></tr>
                    <tr><td style="color:#666;padding:4px 0">Tổng tiền:</td><td><strong style="color:#e53e3e">%,.0f đ</strong></td></tr>
                  </table>
                </div>
                <p style="color:#888;font-size:13px">Nếu cần hỗ trợ, vui lòng liên hệ hotline: <strong>1900 1234</strong></p>
              </div>
              <div style="background:#f8f9ff;padding:15px;text-align:center;color:#888;font-size:12px">
                © 2024 BusTicketPro. All rights reserved.
              </div>
            </div></body></html>
            """.formatted(
                t.getPassengerName(), t.getTicketCode(),
                t.getTrip().getRoute().getDepartureLocation().getName(),
                t.getTrip().getRoute().getArrivalLocation().getName(),
                t.getTrip().getDepartureTime().format(DTF),
                t.getSeat().getSeatNumber(),
                t.getTrip().getBus().getLicensePlate(),
                t.getTrip().getBus().getBusType().getDisplayName(),
                t.getTotalAmount().doubleValue()
        );
    }

    private String buildPaymentEmail(Ticket t) {
        return """
            <!DOCTYPE html><html><body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
            <div style="max-width:600px;margin:auto;background:white;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#48bb78,#38a169);padding:30px;text-align:center">
                <h1 style="color:white;margin:0">✅ Thanh toán thành công!</h1>
              </div>
              <div style="padding:30px">
                <p>Kính gửi <strong>%s</strong>, vé <strong>%s</strong> đã được xác nhận thanh toán.</p>
                <p>Chúc bạn có chuyến đi vui vẻ! 🎉</p>
              </div>
            </div></body></html>
            """.formatted(t.getPassengerName(), t.getTicketCode());
    }

    private String buildCancellationEmail(Ticket t) {
        return """
            <!DOCTYPE html><html><body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
            <div style="max-width:600px;margin:auto;background:white;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#fc8181,#e53e3e);padding:30px;text-align:center">
                <h1 style="color:white;margin:0">❌ Vé đã bị hủy</h1>
              </div>
              <div style="padding:30px">
                <p>Kính gửi <strong>%s</strong>, vé <strong>%s</strong> đã bị hủy.</p>
                <p>Lý do: %s</p>
              </div>
            </div></body></html>
            """.formatted(t.getPassengerName(), t.getTicketCode(),
                t.getCancellationReason() != null ? t.getCancellationReason() : "Không có lý do");
    }
}
