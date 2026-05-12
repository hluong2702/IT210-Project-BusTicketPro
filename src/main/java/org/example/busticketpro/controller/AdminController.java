package org.example.busticketpro.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.busticketpro.dto.request.BusRequest;
import org.example.busticketpro.dto.request.TripRequest;
import org.example.busticketpro.dto.response.ApiResponse;
import org.example.busticketpro.entity.Bus;
import org.example.busticketpro.entity.Location;
import org.example.busticketpro.entity.Route;
import org.example.busticketpro.entity.Trip;
import org.example.busticketpro.enums.BusType;
import org.example.busticketpro.enums.Role;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.repository.LocationRepository;
import org.example.busticketpro.repository.RouteRepository;
import org.example.busticketpro.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final BusService busService;
    private final TripService tripService;
    private final UserService userService;
    private final LocationRepository locationRepository;
    private final RouteRepository routeRepository;
    private final TicketService ticketService;

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        return "admin/dashboard";
    }

    // ─── Bus CRUD ─────────────────────────────────────────────────────────────

    @GetMapping("/buses")
    public String buses(Model model) {
        model.addAttribute("buses", busService.findAll());
        model.addAttribute("busTypes", BusType.values());
        model.addAttribute("busRequest", new BusRequest());
        return "admin/buses";
    }

    @PostMapping("/buses")
    public String createBus(@Valid @ModelAttribute BusRequest request,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("busRequest", request);
            model.addAttribute("buses", busService.findAll());
            model.addAttribute("busTypes", BusType.values());
            model.addAttribute("formError", true);
            return "admin/buses";
        }
        try {
            busService.create(request);
            redirectAttributes.addFlashAttribute("success", "Thêm xe thành công");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/buses";
    }

    @GetMapping("/buses/{id}/edit")
    public String editBusPage(@PathVariable Long id, Model model) {
        Bus bus = busService.findById(id);
        BusRequest req = new BusRequest();
        req.setLicensePlate(bus.getLicensePlate());
        req.setBusType(bus.getBusType());
        req.setCompany(bus.getCompany());
        req.setDriverName(bus.getDriverName());
        req.setDriverPhone(bus.getDriverPhone());
        req.setColor(bus.getColor());
        req.setAmenities(bus.getAmenities());
        model.addAttribute("bus", bus);
        model.addAttribute("busRequest", req);
        model.addAttribute("busTypes", BusType.values());
        return "admin/bus-edit";
    }

    @PostMapping("/buses/{id}/edit")
    public String updateBus(@PathVariable Long id,
                            @Valid @ModelAttribute BusRequest request,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("bus", busService.findById(id));
            model.addAttribute("busTypes", BusType.values());
            return "admin/bus-edit";
        }
        try {
            busService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật xe thành công");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/buses";
    }

    @PostMapping("/buses/{id}/delete")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteBus(@PathVariable Long id) {
        try {
            busService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa xe thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Trip CRUD ────────────────────────────────────────────────────────────

    @GetMapping("/trips")
    public String trips(Model model) {
        model.addAttribute("trips", tripService.findAll());
        model.addAttribute("routes", routeRepository.findAllActiveWithLocations());
        model.addAttribute("buses", busService.findAll());
        model.addAttribute("tripRequest", new TripRequest());
        return "admin/trips";
    }

    @PostMapping("/trips")
    public String createTrip(@Valid @ModelAttribute TripRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("trips", tripService.findAll());
            model.addAttribute("routes", routeRepository.findAllActiveWithLocations());
            model.addAttribute("buses", busService.findAll());
            return "admin/trips";
        }
        try {
            tripService.create(request);
            redirectAttributes.addFlashAttribute("success", "Thêm chuyến xe thành công");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/trips";
    }

    @GetMapping("/trips/{id}/edit")
    public String editTripPage(@PathVariable Long id, Model model) {
        Trip trip = tripService.findById(id);
        TripRequest req = new TripRequest();
        req.setRouteId(trip.getRoute().getId());
        req.setBusId(trip.getBus().getId());
        req.setDepartureTime(trip.getDepartureTime());
        req.setArrivalTime(trip.getArrivalTime());
        req.setPrice(trip.getPrice());
        model.addAttribute("trip", trip);
        model.addAttribute("tripRequest", req);
        model.addAttribute("routes", routeRepository.findAllActiveWithLocations());
        model.addAttribute("buses", busService.findAll());
        return "admin/trip-edit";
    }

    @PostMapping("/trips/{id}/edit")
    public String updateTrip(@PathVariable Long id,
                             @Valid @ModelAttribute TripRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("trip", tripService.findById(id));
            model.addAttribute("routes", routeRepository.findAllActiveWithLocations());
            model.addAttribute("buses", busService.findAll());
            return "admin/trip-edit";
        }
        try {
            tripService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật chuyến xe thành công");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/trips";
    }

    @PostMapping("/trips/{id}/delete")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable Long id) {
        try {
            tripService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa chuyến xe thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Ticket Management ────────────────────────────────────────────────────

    @GetMapping("/tickets")
    public String tickets(@RequestParam(required = false) String status, Model model) {
        var tickets = ticketService.findAll();
        if (status != null && !status.isBlank()) {
            tickets = tickets.stream()
                    .filter(t -> t.getStatus().name().equalsIgnoreCase(status))
                    .toList();
        }
        model.addAttribute("tickets", tickets);
        model.addAttribute("filterStatus", status);
        return "admin/tickets";
    }

    // ─── User Management ─────────────────────────────────────────────────────

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("passengers", userService.findByRole(Role.PASSENGER));
        model.addAttribute("staffList", userService.findByRole(Role.STAFF));
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> toggleUser(@PathVariable Long id) {
        try {
            var user = userService.findById(id);
            user.setEnabled(!user.getEnabled());
            return ResponseEntity.ok(ApiResponse.success(
                    user.getEnabled() ? "Đã kích hoạt tài khoản" : "Đã vô hiệu hóa tài khoản"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Locations & Routes ───────────────────────────────────────────────────

    @GetMapping("/routes")
    public String routes(Model model) {
        model.addAttribute("routes", routeRepository.findAllActiveWithLocations());
        model.addAttribute("locations", locationRepository.findByActiveTrue());
        return "admin/routes";
    }
}