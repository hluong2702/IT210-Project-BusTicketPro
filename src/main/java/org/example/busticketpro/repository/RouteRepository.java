package org.example.busticketpro.repository;

import org.example.busticketpro.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByActiveTrue();

    @Query("SELECT r FROM Route r JOIN FETCH r.departureLocation JOIN FETCH r.arrivalLocation WHERE r.active = true")
    List<Route> findAllActiveWithLocations();

    @Query("SELECT r FROM Route r JOIN FETCH r.departureLocation dl JOIN FETCH r.arrivalLocation al " +
           "WHERE dl.id = :departureId AND al.id = :arrivalId AND r.active = true")
    Optional<Route> findByDepartureAndArrival(@Param("departureId") Long departureId,
                                               @Param("arrivalId") Long arrivalId);
}
