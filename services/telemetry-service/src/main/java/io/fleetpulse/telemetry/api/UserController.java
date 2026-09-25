package io.fleetpulse.telemetry.api;

import io.fleetpulse.telemetry.domain.AppUserEntity;
import io.fleetpulse.telemetry.domain.AppUserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AppUserJpaRepository users;

    public UserController(AppUserJpaRepository users) {
        this.users = users;
    }

    @GetMapping
    public List<AppUserEntity> list() {
        return users.findAll(org.springframework.data.domain.Sort.by("username"));
    }

    /** Register a user in the roster. Login credentials remain in-memory until DB-backed auth. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppUserEntity add(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim().toLowerCase();
        String name = body.getOrDefault("name", "").trim();
        String role = body.getOrDefault("role", "").trim().toUpperCase();
        if (!username.matches("[a-z]{3,20}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username must be 3-20 lowercase letters");
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        if (!List.of("ADMIN","FLEET_MANAGER","DISPATCHER","MAINTENANCE_MANAGER","ANALYST","DRIVER").contains(role))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid role");
        if (users.existsByUsernameIgnoreCase(username))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        return users.save(AppUserEntity.builder()
                .username(username).name(name).role(role)
                .email(body.get("email")).department(body.get("department"))
                .active(true).createdAt(Instant.now()).build());
    }

    /** Deactivate / reactivate a user in the roster. */
    @PutMapping("/{username}/toggle")
    public AppUserEntity toggle(@PathVariable String username) {
        AppUserEntity u = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown user"));
        u.setActive(!u.isActive());
        return users.save(u);
    }
}
