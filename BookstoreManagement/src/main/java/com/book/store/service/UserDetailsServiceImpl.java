//package com.book.store.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import com.book.store.entity.User;
//import com.book.store.repository.UserRepository;
//
//@Service
//public class UserDetailsServiceImpl implements UserDetailsService {
//
//	@Autowired
//    private UserRepository userRepository;
//	
//	 @Override
//	    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//	        User user = userRepository.findByUsername(username);
//	        if (user == null) {
//	            throw new UsernameNotFoundException("User not found :" + username);
//	        }
//	        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
//	        return new User(user.getUsername(), user.getPassword(), authorities);
//	    }
//}
