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
        userRepository.save(user);
    }
    
//    public User saveUser(User user) {
//        // encode password before saving
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
//        // default role = USER
//        if (user.getRoles() == null) {
//            user.setRoles("USER");
//        }
//        return userRepository.save(user);
//    }
    
    
}
