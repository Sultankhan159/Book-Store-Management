package com.book.store.controller;

import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.book.store.entity.User;
import com.book.store.entity.Order;
import com.book.store.service.MyBookListService;
import com.book.store.service.UserService;

@Controller
public class MyBookListController {
	
	@Autowired
	private MyBookListService service;

	@Autowired
	private UserService userService;

	@RequestMapping("/deleteMyList/{id}")
	public String deleteMyList(@PathVariable("id") int id, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		User user = userService.findByUsername(principal.getName());
		service.deleteFromCart(id, user);
		return "redirect:/my_books";
	}

	@PostMapping("/cart/update")
	public String updateCartQuantity(
			@RequestParam("id") int cartItemId,
			@RequestParam("quantity") int quantity,
			Principal principal,
			org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
		if (principal == null) {
			return "redirect:/login";
		}
		User user = userService.findByUsername(principal.getName());
		try {
			service.updateQuantity(cartItemId, quantity, user);
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/my_books";
	}

	@PostMapping("/cart/checkout")
	public String checkout(Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
		if (principal == null) {
			return "redirect:/login";
		}
		User user = userService.findByUsername(principal.getName());
		try {
			Order order = service.checkout(user);
			if (order == null) {
				redirectAttributes.addFlashAttribute("errorMessage", "Cannot checkout an empty cart!");
				return "redirect:/my_books";
			}
			return "redirect:/orders?success";
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/my_books";
		}
	}

	@GetMapping("/orders")
	public String getOrderHistory(Model model, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("orders", service.getUserOrders(user));
		return "orders";
	}

	@GetMapping("/orders/invoice/{id}")
	public String getOrderInvoice(@PathVariable("id") Long id, Model model, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		User user = userService.findByUsername(principal.getName());
		Order order = service.getOrderById(id);
		if (order == null) {
			return "redirect:/orders";
		}
		boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
		if (!order.getUser().getId().equals(user.getId()) && !isAdmin) {
			return "redirect:/access-denied";
		}
		model.addAttribute("order", order);
		return "orderInvoice";
	}
}
