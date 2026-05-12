package org.example.busticketpro.service;

import org.example.busticketpro.dto.request.TripRequest;
import org.example.busticketpro.entity.Trip;

import java.time.LocalDate;
import java.util.List;

public interface TripService {
    List<Trip> search(Long departureId, Long arrivalId, LocalDate date);
    Trip findById(Long id);
    Trip create(TripRequest request);
    Trip update(Long id, TripRequest request);
    void delete(Long id);
    List<Trip> findAll();
}
