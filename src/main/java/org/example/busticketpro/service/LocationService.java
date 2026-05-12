package org.example.busticketpro.service;

import org.example.busticketpro.entity.Location;
import java.util.List;

public interface LocationService {
    List<Location> findAll();
    Location create(String name, String province, String address);
}
