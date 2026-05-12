package org.example.busticketpro.repository;

import org.example.busticketpro.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("SELECT t FROM Trip t JOIN FETCH t.route r JOIN FETCH r.departureLocation dl " +
           "JOIN FETCH r.arrivalLocation al JOIN FETCH t.bus b " +
           "WHERE dl.id = :departureId AND al.id = :arrivalId " +
           "AND t.departureTime >= :startOfDay AND t.departureTime < :endOfDay " +
           "AND t.active = true ORDER BY t.departureTime ASC")
    List<Trip> findAvailableTrips(@Param("departureId") Long departureId,
                                   @Param("arrivalId") Long arrivalId,
                                   @Param("startOfDay") LocalDateTime startOfDay,
                                   @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT t FROM Trip t JOIN FETCH t.route r JOIN FETCH r.departureLocation JOIN FETCH r.arrivalLocation JOIN FETCH t.bus WHERE t.id = :id")
    Optional<Trip> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM Trip t JOIN FETCH t.route r JOIN FETCH r.departureLocation JOIN FETCH r.arrivalLocation JOIN FETCH t.bus WHERE t.active = true ORDER BY t.departureTime DESC")
    List<Trip> findAllActiveWithDetails();

    // For statistics: top trips by booking count
    @Query(value = "SELECT t.id, r.id as route_id, dl.name as dep, al.name as arr, " +
                   "COUNT(tk.id) as booking_count, SUM(tk.total_amount) as revenue " +
                   "FROM trips t JOIN routes r ON t.route_id = r.id " +
                   "JOIN locations dl ON r.departure_location_id = dl.id " +
                   "JOIN locations al ON r.arrival_location_id = al.id " +
                   "LEFT JOIN tickets tk ON tk.trip_id = t.id AND tk.status = 'PAID' " +
                   "GROUP BY t.id, r.id, dl.name, al.name " +
                   "ORDER BY booking_count DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5TripsByBookings();
}
