package com.disasteralert.users;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public record CreateUserRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            String phone,
            String password,
            Double latitude,
            Double longitude,
            String district,
            String state
    ) {}

    public record LocationRequest(
            Double latitude,
            Double longitude,
            String district,
            String state
    ) {}

    @GetMapping
    public List<User> all() {
        return users.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public User create(@Valid @RequestBody CreateUserRequest request) {
        if (users.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(blankToNull(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(
                request.password() == null || request.password().isBlank()
                        ? "ChangeMe@123"
                        : request.password()
        ));
        user.setRole("USER");
        user.setLatitude(request.latitude());
        user.setLongitude(request.longitude());
        user.setDistrict(blankToNull(request.district()));
        user.setState(blankToNull(request.state()));
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        return users.save(user);
    }

    @PatchMapping("/{id}/location")
    public User updateLocation(@PathVariable Long id, @RequestBody LocationRequest request) {
        User user = users.findById(id).orElseThrow();
        user.setLatitude(request.latitude());
        user.setLongitude(request.longitude());
        user.setDistrict(blankToNull(request.district()));
        user.setState(blankToNull(request.state()));
        return users.save(user);
    }

    @PatchMapping("/{id}/enabled")
    public User setEnabled(@PathVariable Long id, @RequestParam boolean value) {
        User user = users.findById(id).orElseThrow();
        user.setEnabled(value);
        return users.save(user);
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return users.findById(id).orElseThrow();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
