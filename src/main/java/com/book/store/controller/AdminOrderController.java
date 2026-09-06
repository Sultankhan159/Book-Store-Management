package com.book.store.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.book.store.entity.Order;
import com.book.store.service.MyBookListService;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private MyBookListService myBookListService;

    @GetMapping
    public String listOrders(
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            Model model) {
        List<Order> orders = myBookListService.getAllOrders(status);
        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", status.toUpperCase());
        model.addAttribute("totalOrdersCount", myBookListService.getTotalOrdersCount());
        return "adminOrders";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes) {
        Order updated = myBookListService.updateOrderStatus(id, status);
        if (updated != null) {
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " updated to " + status + " successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Order #" + id + " could not be found.");
        }
        return "redirect:/admin/orders";
    }
}
