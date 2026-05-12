package org.example.busticketpro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardStats {
    private long totalPassengers;
    private long totalTrips;
    private long totalTickets;
    private long pendingTickets;
    private BigDecimal totalRevenue;
    private long paidThisMonth;
    private List<Map<String, Object>> revenueByRoute;
    private List<Map<String, Object>> top5Trips;
    private List<Map<String, Object>> monthlyRevenue;
}
