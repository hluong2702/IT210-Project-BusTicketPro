package org.example.busticketpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.busticketpro.dto.response.DashboardStats;
import org.example.busticketpro.enums.Role;
import org.example.busticketpro.enums.TicketStatus;
import org.example.busticketpro.repository.*;
import org.example.busticketpro.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final TicketRepository ticketRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardStats getStats() {
        // All aggregation done in DB via GROUP BY/SUM — no Java loops
        BigDecimal totalRevenue = ticketRepository.getTotalRevenue().orElse(BigDecimal.ZERO);
        long pendingTickets = ticketRepository.countByStatus(TicketStatus.PENDING);
        long paidThisMonth = ticketRepository.countPaidSince(LocalDateTime.now().withDayOfMonth(1).withHour(0));
        long totalPassengers = userRepository.countByRole(Role.PASSENGER);
        long totalTrips = tripRepository.count();
        long totalTickets = ticketRepository.count();

        // Revenue by route per month (native query with GROUP BY)
        List<Object[]> rawRevenue = ticketRepository.getRevenueByRouteAndMonth(
            LocalDateTime.now().minusMonths(6));
        List<Map<String, Object>> revenueByRoute = rawRevenue.stream()
            .map(row -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("departure", row[0]);
                m.put("arrival", row[1]);
                m.put("month", row[2]);
                m.put("year", row[3]);
                m.put("revenue", row[4]);
                m.put("ticketCount", row[5]);
                return m;
            }).toList();

        // Top 5 trips by booking (native query)
        List<Object[]> rawTop5 = tripRepository.findTop5TripsByBookings();
        List<Map<String, Object>> top5Trips = rawTop5.stream()
            .map(row -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("tripId", row[0]);
                m.put("departure", row[2]);
                m.put("arrival", row[3]);
                m.put("bookingCount", row[4]);
                m.put("revenue", row[5]);
                return m;
            }).toList();

        // Monthly revenue for chart (last 6 months)
        List<Map<String, Object>> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = LocalDateTime.now().minusMonths(i).withDayOfMonth(1).withHour(0);
            LocalDateTime end = start.plusMonths(1);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", start.getMonth().getDisplayName(
                java.time.format.TextStyle.SHORT, new java.util.Locale("vi")));
            m.put("count", ticketRepository.countPaidSince(start));
            monthly.add(m);
        }

        return DashboardStats.builder()
            .totalPassengers(totalPassengers)
            .totalTrips(totalTrips)
            .totalTickets(totalTickets)
            .pendingTickets(pendingTickets)
            .totalRevenue(totalRevenue)
            .paidThisMonth(paidThisMonth)
            .revenueByRoute(revenueByRoute)
            .top5Trips(top5Trips)
            .monthlyRevenue(monthly)
            .build();
    }
}
