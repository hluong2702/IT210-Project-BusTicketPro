package org.example.busticketpro.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.busticketpro.dto.request.BookingRequest;
import org.example.busticketpro.dto.response.ApiResponse;
import org.example.busticketpro.dto.response.SeatResponse;
import org.example.busticketpro.entity.Ticket;
import org.example.busticketpro.entity.Trip;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.exception.SeatConflictException;
import org.example.busticketpro.repository.TicketRepository;
import org.example.busticketpro.security.CustomUserDetails;
import org.example.busticketpro.service.SeatService;
import org.example.busticketpro.service.TicketService;
import org.example.busticketpro.service.TripService;
import org.example.busticketpro.util.VNPayUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final TripService tripService;
    private final SeatService seatService;
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final VNPayUtil vnPayUtil;

    @GetMapping("/trip/{tripId}")
    public String selectSeat(@PathVariable Long tripId,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model) {
        Trip trip = tripService.findById(tripId);
        List<SeatResponse> seats = seatService.getSeatMapForTrip(tripId);

        List<SeatResponse> floor1Seats = seats.stream().filter(s -> s.getFloor() == 1).toList();
        List<SeatResponse> floor2Seats = seats.stream().filter(s -> s.getFloor() == 2).toList();
        long available = seats.stream().filter(SeatResponse::isAvailable).count();

        model.addAttribute("trip", trip);
        model.addAttribute("seats", seats);
        model.addAttribute("floor1Seats", floor1Seats);
        model.addAttribute("floor2Seats", floor2Seats);
        model.addAttribute("availableCount", available);
        model.addAttribute("bookingRequest", new BookingRequest());
        model.addAttribute("passengerName", userDetails.getFullName());
        return "booking/seat-selection";
    }

    @PostMapping("/confirm")
    public String confirmBooking(@Valid @ModelAttribute BookingRequest request,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes,
                                 Model model,
                                 HttpServletRequest httpRequest) {

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return "redirect:/booking/trip/" + request.getTripId();
        }

        try {
            Ticket ticket = ticketService.book(request, userDetails.getId());
            if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
                String paymentReference = DateTimeFormatter.ofPattern("HHmmssSSS").format(LocalDateTime.now());
                ticket.setPaymentReference(paymentReference);
                ticketRepository.save(ticket);
                String paymentUrl = vnPayUtil.createPaymentUrl(
                    paymentReference,
                    ticket.getTotalAmount().longValue(),
                    "Thanh toan ve xe " + ticket.getTicketCode(),
                    getClientIp(httpRequest));
                model.addAttribute("paymentUrl", paymentUrl);
                return "payment/vnpay-redirect";
            }
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("ticketCode", ticket.getTicketCode());
            return "redirect:/booking/success/" + ticket.getId();
        } catch (SeatConflictException | BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking/trip/" + request.getTripId();
        }
    }

    @GetMapping("/success/{ticketId}")
    public String bookingSuccess(@PathVariable Long ticketId,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        var tickets = ticketService.findByPassenger(userDetails.getId());
        var ticket = tickets.stream().filter(t -> t.getId().equals(ticketId)).findFirst();
        if (ticket.isEmpty()) return "redirect:/passenger/dashboard";
        model.addAttribute("ticket", ticket.get());
        return "booking/success";
    }

    @GetMapping("/api/seats/{tripId}")
    @ResponseBody
    public ApiResponse<List<SeatResponse>> getSeats(@PathVariable Long tripId) {
        return ApiResponse.success("OK", seatService.getSeatMapForTrip(tripId));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip == null || ip.isBlank()) ? request.getRemoteAddr() : ip.split(",")[0].trim();
    }
}
