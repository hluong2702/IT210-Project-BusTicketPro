package org.example.busticketpro.repository;

import org.example.busticketpro.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByActiveTrue();
    boolean existsByName(String name);
}
