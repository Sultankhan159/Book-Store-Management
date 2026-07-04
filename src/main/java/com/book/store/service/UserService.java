package com.book.store.service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.book.store.entity.User;
import com.book.store.repository.UserRepository;

@Service
public class UserService {

	 private final UserRepository userRepository;
	 private final PasswordEncoder passwordEncoder;

	 
	    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
	        this.userRepository = userRepository;
	        this.passwordEncoder = passwordEncoder;
	    }
	 
    public void registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRoles() == null || user.getRoles().trim().isEmpty()) {
            user.setRoles("ROLE_USER");
        }
        userRepository.save(user);
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRoles() == null || user.getRoles().trim().isEmpty()) {
            user.setRoles("ROLE_USER");
        }
        return userRepository.save(user);
    }

    public long countUsers() {
        return userRepository.count();
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
