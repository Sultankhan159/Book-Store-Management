package com.book.store.controller.api;

import com.book.store.dto.UserRegisterRequest;
import com.book.store.dto.UserResponse;
import com.book.store.entity.User;
import com.book.store.exception.BadRequestException;
import com.book.store.exception.ResourceNotFoundException;
import com.book.store.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "User API", description = "Endpoints for user authentication and user profile management")
public class UserRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user account")
    public UserResponse registerUser(@Valid @RequestBody UserRegisterRequest request) {
        User existing = userService.findByUsername(request.getUsername());
        if (existing != null) {
            throw new BadRequestException("Username already exists!");
        }
        User user = modelMapper.map(request, User.class);
        userService.registerUser(user);
        return modelMapper.map(user, UserResponse.class);
    }

    @GetMapping("/users/me")
    @Operation(summary = "Get the profile of the currently logged-in user")
    public UserResponse getProfile(Principal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            throw new ResourceNotFoundException("User profile not found");
        }
        return modelMapper.map(user, UserResponse.class);
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered users (Admin only)")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(user -> modelMapper.map(user, UserResponse.class))
                .collect(Collectors.toList());
    }
}
