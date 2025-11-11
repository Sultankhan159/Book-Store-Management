//package com.book.store.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.ModelAttribute;
//import org.springframework.web.bind.annotation.PostMapping;
//
//import com.book.store.entity.User;
//import com.book.store.service.UserService;

//@Controller
//public class UserController {
//
//	 @Autowired
//	 private UserService userService;
//
//	    @GetMapping("/register")
//	    public String showRegistrationForm(@ModelAttribute("user") User user) {
//	        return "register";
//	    }
//
//	    @PostMapping("/register")
//	    public String registerUser(@ModelAttribute("user") User user) {
//	        userService.registerUser(user);
//	        return "redirect:/login?registered";
//	    }
//}
