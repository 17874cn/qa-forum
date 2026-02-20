package com.forum.controller;

import com.forum.activity.ActivityLogService;
import com.forum.activity.ActivityType;
import com.forum.model.User;
import com.forum.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public UserController(UserRepository userRepository, ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> create(@RequestBody User user, ServerWebExchange exchange) {
        return userRepository.save(user)
                .doOnSuccess(saved -> {
                    String ip = getClientIp(exchange);
                    activityLogService.log(saved.getId(),
                            ActivityType.USER_REGISTERED,
                            "User registered: " + saved.getUsername(),
                            saved.getId(),
                            Map.of("username", saved.getUsername(),
                                    "email", saved.getEmail(),
                                    "ip", ip));
                });
    }

    @PostMapping("/login")
    public Mono<User> login(@RequestBody Map<String, String> credentials, ServerWebExchange exchange) {
        String username = credentials.get("username");
        String ip = getClientIp(exchange);

        return userRepository.findByUsername(username)
                .doOnSuccess(user -> {
                    if (user != null) {
                        activityLogService.log(user.getId(),
                                ActivityType.USER_LOGIN_SUCCESS,
                                "Login successful",
                                null,
                                Map.of("username", username, "ip", ip));
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    activityLogService.log(null,
                            ActivityType.USER_LOGIN_FAILED,
                            "Login failed: user not found",
                            null,
                            Map.of("username", username, "ip", ip));
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Invalid credentials"));
                }));
    }

    @GetMapping
    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    private String getClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null) {
            return forwarded.split(",")[0].trim();
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
