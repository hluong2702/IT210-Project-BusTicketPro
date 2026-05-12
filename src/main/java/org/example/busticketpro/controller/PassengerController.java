package org.example.busticketpro.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.busticketpro.dto.request.UpdateProfileRequest;
import org.example.busticketpro.dto.response.ApiResponse;
import org.example.busticketpro.dto.response.TicketResponse;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.security.CustomUserDetails;
import org.example.busticketpro.service.TicketService;
import org.example.busticketpro.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/passenger")
@RequiredArgsConstructor
public class PassengerController {

    private final TicketService ticketService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<TicketResponse> tickets = ticketService.findByPassenger(userDetails.getId());
        long pending = tickets.stream().filter(t -> t.getStatus().name().equals("PENDING")).count();
        long paid = tickets.stream().filter(t -> t.getStatus().name().equals("PAID")).count();
        long cancelled = tickets.stream().filter(t -> t.getStatus().name().equals("CANCELLED")).count();

        model.addAttribute("tickets", tickets);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("paidCount", paid);
        model.addAttribute("cancelledCount", cancelled);
        model.addAttribute("user", userDetails.getUser());
        return "passenger/dashboard";
    }

    @GetMapping("/tickets")
    public String myTickets(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam(required = false) String status,
                            Model model) {
        List<TicketResponse> tickets = ticketService.findByPassenger(userDetails.getId());
        if (status != null && !status.isBlank()) {
            tickets = tickets.stream()
                .filter(t -> t.getStatus().name().equalsIgnoreCase(status))
                .toList();
        }
        model.addAttribute("tickets", tickets);
        model.addAttribute("filterStatus", status);
        return "passenger/my-tickets";
    }

    @GetMapping("/ticket/{id}")
    public String ticketDetail(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        List<TicketResponse> tickets = ticketService.findByPassenger(userDetails.getId());
        TicketResponse ticket = tickets.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(null);
        if (ticket == null) return "redirect:/passenger/tickets";
        model.addAttribute("ticket", ticket);
        return "passenger/ticket-detail";
    }

    @PostMapping("/ticket/{id}/cancel")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> cancelTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            ticketService.cancelByPassenger(id, userDetails.getId());
            return ResponseEntity.ok(ApiResponse.success("Hủy vé thành công"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails.getUser());
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName(userDetails.getUser().getFullName());
        req.setPhone(userDetails.getUser().getPhone());
        req.setEmail(userDetails.getUser().getEmail());
        req.setAddress(userDetails.getUser().getAddress());
        model.addAttribute("profileRequest", req);
        return "passenger/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute UpdateProfileRequest request,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userDetails.getUser());
            return "passenger/profile";
        }
        try {
            userService.updateProfile(userDetails.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/passenger/profile";
    }

    @PostMapping("/change-password")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            userService.changePassword(userDetails.getId(),
                body.get("oldPassword"), body.get("newPassword"));
            return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
