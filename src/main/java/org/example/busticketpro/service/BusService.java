package org.example.busticketpro.service;

import org.example.busticketpro.dto.request.BusRequest;
import org.example.busticketpro.entity.Bus;

import java.util.List;

public interface BusService {
    List<Bus> findAll();
    Bus findById(Long id);
    Bus create(BusRequest request);
    Bus update(Long id, BusRequest request);
    void delete(Long id);
}
