package org.example.busticketpro.repository;

import jakarta.persistence.LockModeType;
import org.example.busticketpro.entity.Seat;
import org.example.busticketpro.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByTripIdOrderBySeatNumber(Long tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT s FROM Seat s WHERE s.trip.id = :tripId AND s.seatNumber = :seatNumber")
    Optional<Seat> findByTripIdAndSeatNumber(@Param("tripId") Long tripId,
                                              @Param("seatNumber") String seatNumber);

    long countByTripIdAndStatus(Long tripId, SeatStatus status);

    // Find expired holds to release
    @Query("SELECT s FROM Seat s WHERE s.status = 'PENDING' AND s.heldUntil IS NOT NULL AND s.heldUntil < :now")
    List<Seat> findExpiredHolds(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.heldUntil = null, s.heldByUser = null " +
           "WHERE s.status = 'PENDING' AND s.heldUntil IS NOT NULL AND s.heldUntil < :now")
    int releaseExpiredHolds(@Param("now") LocalDateTime now);
}
