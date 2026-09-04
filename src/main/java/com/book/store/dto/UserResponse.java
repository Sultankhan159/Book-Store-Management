package com.book.store.dto;

public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phoneNumber;
    private String roles;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
}
