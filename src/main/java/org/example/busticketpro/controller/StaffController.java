package org.example.busticketpro.controller;

import lombok.RequiredArgsConstructor;
import org.example.busticketpro.dto.response.ApiResponse;
import org.example.busticketpro.dto.response.TicketResponse;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.security.CustomUserDetails;
import org.example.busticketpro.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final TicketService ticketService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<TicketResponse> pending = ticketService.findAllPending();
        model.addAttribute("pendingTickets", pending);
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("staffName", userDetails.getFullName());
        return "staff/dashboard";
    }

    @GetMapping("/tickets")
    public String allTickets(@RequestParam(required = false) String status, Model model) {
        List<TicketResponse> tickets;
        if ("PENDING".equalsIgnoreCase(status)) {
            tickets = ticketService.findAllPending();
        } else {
            tickets = ticketService.findAll();
        }
        if (status != null && !status.isBlank() && !"PENDING".equalsIgnoreCase(status)) {
            tickets = tickets.stream()
                .filter(t -> t.getStatus().name().equalsIgnoreCase(status))
                .toList();
        }
        model.addAttribute("tickets", tickets);
        model.addAttribute("filterStatus", status);
        return "staff/tickets";
    }

    @GetMapping("/ticket/{id}")
    public String ticketDetail(@PathVariable Long id, Model model) {
        List<TicketResponse> all = ticketService.findAll();
        TicketResponse ticket = all.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
        if (ticket == null) return "redirect:/staff/tickets";
        model.addAttribute("ticket", ticket);
        return "staff/ticket-detail";
    }

    @PostMapping("/ticket/{id}/confirm")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> confirmTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            ticketService.confirm(id, userDetails.getId());
            return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán thành công"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/ticket/{id}/cancel")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> cancelTicket(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String reason = (body != null && body.get("reason") != null)
                ? body.get("reason") : "Nhân viên hủy";
            ticketService.cancelByStaff(id, userDetails.getId(), reason);
            return ResponseEntity.ok(ApiResponse.success("Hủy vé thành công"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
