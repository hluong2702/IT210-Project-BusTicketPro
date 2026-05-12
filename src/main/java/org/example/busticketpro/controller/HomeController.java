package org.example.busticketpro.controller;

import lombok.RequiredArgsConstructor;
import org.example.busticketpro.entity.Location;
import org.example.busticketpro.entity.Trip;
import org.example.busticketpro.repository.LocationRepository;
import org.example.busticketpro.security.CustomUserDetails;
import org.example.busticketpro.service.TripService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final LocationRepository locationRepository;
    private final TripService tripService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("locations", locationRepository.findByActiveTrue());
        model.addAttribute("today", LocalDate.now());
        return "home";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) Long departureId,
                         @RequestParam(required = false) Long arrivalId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         Model model) {
        List<Location> locations = locationRepository.findByActiveTrue();
        model.addAttribute("locations", locations);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedDepartureId", departureId);
        model.addAttribute("selectedArrivalId", arrivalId);

        if (departureId != null && arrivalId != null && date != null) {
            if (departureId.equals(arrivalId)) {
                model.addAttribute("error", "Điểm đi và điểm đến không được trùng nhau");
            } else if (date.isBefore(LocalDate.now())) {
                model.addAttribute("error", "Vui lòng chọn ngày từ hôm nay trở đi");
            } else {
                List<Trip> trips = tripService.search(departureId, arrivalId, date);
                model.addAttribute("trips", trips);
                model.addAttribute("searched", true);
                if (trips.isEmpty()) {
                    model.addAttribute("noTrips", true);
                }
            }
        }
        return "search";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return "redirect:/auth/login";
        return switch (userDetails.getRole()) {
            case "ADMIN" -> "redirect:/admin/dashboard";
            case "STAFF" -> "redirect:/staff/dashboard";
            default -> "redirect:/passenger/dashboard";
        };
    }

//    @GetMapping("/ticket/lookup")
//    public String lookupPage() {
//        return "ticket-lookup";
//    }

    @GetMapping("/error/403")
    public String error403() { return "error/403"; }
}
