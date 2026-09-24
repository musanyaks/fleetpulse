package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.domain.BusinessUnitEntity;
import io.fleetpulse.telemetry.domain.BusinessUnitJpaRepository;
import io.fleetpulse.telemetry.domain.FleetGroupEntity;
import io.fleetpulse.telemetry.domain.FleetGroupJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/org")
public class OrgController {

    private final FleetGroupJpaRepository groups;
    private final BusinessUnitJpaRepository units;
    private final JdbcTemplate jdbc;

    public OrgController(FleetGroupJpaRepository groups, BusinessUnitJpaRepository units, JdbcTemplate jdbc) {
        this.groups = groups;
        this.units = units;
        this.jdbc = jdbc;
    }

    /** Fleet groups with live vehicle counts (by vehicle class keyword). */
    @GetMapping("/groups")
    public List<Map<String, Object>> groups() {
        List<Map<String, Object>> out = jdbc.queryForList("""
            SELECT g.name, g.description,
                   count(v.vehicle_id) AS vehicles
            FROM fleet_groups g
            LEFT JOIN vehicles v ON
              (CASE
                 WHEN v.make || ' ' || coalesce(v.model,'') ~* 'Hiace|Coaster|Rosa|Matatu|Bus' THEN 'Buses'
                 WHEN v.make || ' ' || coalesce(v.model,'') ~* 'Hilux|Ranger|H1' THEN 'Pickups'
                 ELSE 'Trucks' END) = g.name
            GROUP BY g.name, g.description
            ORDER BY g.name
            """);
        return out;
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public FleetGroupEntity addGroup(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "").trim();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name required");
        if (groups.existsByNameIgnoreCase(name))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group already exists");
        return groups.save(FleetGroupEntity.builder()
                .name(name).description(body.get("description"))
                .createdAt(Instant.now()).build());
    }

    @DeleteMapping("/groups/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable String name) {
        groups.delete(groups.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown group")));
    }

    @GetMapping("/units")
    public List<Map<String, Object>> units() {
        return jdbc.queryForList(
                "SELECT name, description, active FROM business_units ORDER BY name");
    }

    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessUnitEntity addUnit(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "").trim();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name required");
        if (units.existsByNameIgnoreCase(name))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit already exists");
        return units.save(BusinessUnitEntity.builder()
                .name(name).description(body.get("description")).active(true)
                .createdAt(Instant.now()).build());
    }

    @DeleteMapping("/units/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUnit(@PathVariable String name) {
        units.delete(units.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown unit")));
    }
}
