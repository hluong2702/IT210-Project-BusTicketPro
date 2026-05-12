package org.example.busticketpro.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.busticketpro.dto.response.ApiResponse;
import org.example.busticketpro.dto.response.TicketResponse;
import org.example.busticketpro.entity.Ticket;
import org.example.busticketpro.entity.Seat;
import org.example.busticketpro.enums.SeatStatus;
import org.example.busticketpro.enums.TicketStatus;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.repository.SeatRepository;
import org.example.busticketpro.repository.TicketRepository;
import org.example.busticketpro.service.TicketService;
import org.example.busticketpro.util.VNPayUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class TicketLookupController {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final VNPayUtil vnPayUtil;

    @GetMapping("/ticket/lookup")
    public String lookupPage() {
        return "ticket-lookup";
    }

    @GetMapping("/ticket/search")
    @ResponseBody
    public ResponseEntity<ApiResponse<TicketResponse>> lookup(
            @RequestParam String code,
            @RequestParam String phone) {
        try {
            TicketResponse tr = ticketService.lookup(code.trim(), phone.trim());
            return ResponseEntity.ok(ApiResponse.success("Tìm thấy vé", tr));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── VNPay Payment ────────────────────────────────────────────────────────

    @PostMapping("/payment/vnpay/{ticketId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> initiateVNPay(
            @PathVariable Long ticketId,
            HttpServletRequest request) {
        try {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy vé"));
            if (ticket.getStatus() != TicketStatus.PENDING) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vé không ở trạng thái chờ thanh toán"));
            }
            String ip = getClientIp(request);
            String url = vnPayUtil.createPaymentUrl(
                ticket.getPaymentReference() != null ? ticket.getPaymentReference() : ticket.getId().toString(),
                ticket.getTotalAmount().longValue(),
                "Thanh toan ve xe " + ticket.getTicketCode(),
                ip);
            return ResponseEntity.ok(ApiResponse.success("Redirect", url));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/payment/vnpay-return")
    @Transactional
    public String vnpayReturn(HttpServletRequest request, Model model) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));

        boolean valid = vnPayUtil.validateCallback(params);
        boolean success = valid && vnPayUtil.isSuccess(params);

        if (success) {
            String txnRef = params.get("vnp_TxnRef");
            final String[] ticketCode = {txnRef};
            findTicketFromTxnRef(txnRef).ifPresent(ticket -> {
                ticketCode[0] = ticket.getTicketCode();
                // Staff confirm would normally do this — for VNPay auto-confirm:
                if (ticket.getStatus() == TicketStatus.PENDING) {
                    ticket.setStatus(TicketStatus.PAID);
                    ticket.setPaidAt(java.time.LocalDateTime.now());
                    ticket.setPaymentTransactionId(params.get("vnp_TransactionNo"));
                    Seat seat = ticket.getSeat();
                    seat.setStatus(SeatStatus.BOOKED);
                    seat.setHeldUntil(null);
                    seat.setHeldByUser(null);
                    seatRepository.save(seat);
                    ticketRepository.save(ticket);
                }
            });
            model.addAttribute("success", true);
            model.addAttribute("ticketCode", ticketCode[0]);
        } else {
            model.addAttribute("success", false);
            model.addAttribute("error", "Thanh toán thất bại. Vui lòng thử lại.");
        }
        return "payment/vnpay-result";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip == null || ip.isEmpty()) ? request.getRemoteAddr() : ip.split(",")[0];
    }

    private java.util.Optional<Ticket> findTicketFromTxnRef(String txnRef) {
        if (txnRef == null || txnRef.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            var byReference = ticketRepository.findByPaymentReference(txnRef);
            if (byReference.isPresent()) {
                return byReference;
            }
            return ticketRepository.findById(Long.parseLong(txnRef));
        } catch (NumberFormatException ignored) {
            return ticketRepository.findByTicketCode(vnPayUtil.extractTicketCode(java.util.Map.of("vnp_TxnRef", txnRef)));
        }
    }
}
