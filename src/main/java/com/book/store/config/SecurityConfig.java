//config package 
package com.book.store.config;

//Used to create Spring Beans.
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Used to configure security rules.
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

//Enables Spring Security in your project.
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

//Used for creating user details for login.
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

//Used to encrypt passwords.
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

//Stores users inside memory (not database).
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

//This is required for Spring Security latest version.
import org.springframework.security.web.SecurityFilterChain;

//class annotation 
//Tells Spring: this is a configuration class.
@Configuration

//Enables security in web application.
@EnableWebSecurity
public class SecurityConfig {
	

    @Bean // means Spring will manage this object.
    //This method defines security rules.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/home", "/login", "/register", "/css/**", "/images/**", "/js/**", "/book/cover/**", "/available_books", "/book/details/**").permitAll()
                .requestMatchers("/my_books", "/myList/**", "/cart/**", "/orders", "/addReview/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/book_rsgister", "/book_register", "/save", "/editBook/**", "/deleteBook/**", "/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/available_books", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .exceptionHandling(exception -> 
                exception.accessDeniedPage("/access-denied")
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    
    
}



