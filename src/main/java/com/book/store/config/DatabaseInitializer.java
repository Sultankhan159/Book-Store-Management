package com.book.store.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.book.store.entity.User;
import com.book.store.repository.UserRepository;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        User admin = userRepository.findByUsername("admin");
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles("ROLE_ADMIN");
            userRepository.save(admin);
            System.out.println("====== Admin User Bootstrapped successfully! ======");
        } else if (!"ROLE_ADMIN".equals(admin.getRoles())) {
            admin.setRoles("ROLE_ADMIN");
            userRepository.save(admin);
            System.out.println("====== Admin User roles updated to ROLE_ADMIN successfully! ======");
        }
        
        // Also check if a default user exists for convenience
        User user = userRepository.findByUsername("user");
        if (user == null) {
            user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRoles("ROLE_USER");
            userRepository.save(user);
            System.out.println("====== Regular User Bootstrapped successfully! ======");
        }
    }
}
