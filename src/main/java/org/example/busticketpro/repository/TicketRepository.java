package org.example.busticketpro.repository;

import org.example.busticketpro.entity.Ticket;
import org.example.busticketpro.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t JOIN FETCH t.passenger JOIN FETCH t.trip tr " +
           "JOIN FETCH tr.route r JOIN FETCH r.departureLocation JOIN FETCH r.arrivalLocation " +
           "JOIN FETCH tr.bus JOIN FETCH t.seat " +
           "WHERE t.ticketCode = :code AND t.passengerPhone = :phone")
    Optional<Ticket> findByCodeAndPhone(@Param("code") String code, @Param("phone") String phone);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.passenger JOIN FETCH t.trip tr " +
           "JOIN FETCH tr.route r JOIN FETCH r.departureLocation JOIN FETCH r.arrivalLocation " +
           "JOIN FETCH tr.bus JOIN FETCH t.seat " +
           "WHERE t.passenger.id = :passengerId ORDER BY t.bookedAt DESC")
    List<Ticket> findByPassengerIdWithDetails(@Param("passengerId") Long passengerId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.passenger JOIN FETCH t.trip tr " +
           "JOIN FETCH tr.route r JOIN FETCH r.departureLocation JOIN FETCH r.arrivalLocation " +
           "JOIN FETCH tr.bus JOIN FETCH t.seat " +
           "WHERE t.status = :status ORDER BY t.bookedAt DESC")
    List<Ticket> findByStatusWithDetails(@Param("status") TicketStatus status);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.passenger JOIN FETCH t.trip tr " +
           "JOIN FETCH tr.route r JOIN FETCH r.departureLocation JOIN FETCH r.arrivalLocation " +
           "JOIN FETCH tr.bus JOIN FETCH t.seat ORDER BY t.bookedAt DESC")
    List<Ticket> findAllWithDetails();

    // Auto-cancel: PENDING tickets older than X minutes
    @Query("SELECT t FROM Ticket t JOIN FETCH t.seat WHERE t.status = 'PENDING' AND t.bookedAt < :cutoff")
    List<Ticket> findPendingOlderThan(@Param("cutoff") LocalDateTime cutoff);

    Optional<Ticket> findByTicketCode(String ticketCode);

    Optional<Ticket> findByPaymentReference(String paymentReference);

    // Statistics: revenue by route per month
    @Query(value = "SELECT dl.name as departure, al.name as arrival, " +
                   "MONTH(tk.paid_at) as month, YEAR(tk.paid_at) as year, " +
                   "SUM(tk.total_amount) as revenue, COUNT(tk.id) as ticket_count " +
                   "FROM tickets tk " +
                   "JOIN trips tr ON tk.trip_id = tr.id " +
                   "JOIN routes r ON tr.route_id = r.id " +
                   "JOIN locations dl ON r.departure_location_id = dl.id " +
                   "JOIN locations al ON r.arrival_location_id = al.id " +
                   "WHERE tk.status = 'PAID' AND tk.paid_at >= :startDate " +
                   "GROUP BY dl.name, al.name, MONTH(tk.paid_at), YEAR(tk.paid_at) " +
                   "ORDER BY year DESC, month DESC", nativeQuery = true)
    List<Object[]> getRevenueByRouteAndMonth(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(t.totalAmount) FROM Ticket t WHERE t.status = 'PAID'")
    Optional<BigDecimal> getTotalRevenue();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    long countByStatus(@Param("status") TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'PAID' AND t.paidAt >= :since")
    long countPaidSince(@Param("since") LocalDateTime since);
}
