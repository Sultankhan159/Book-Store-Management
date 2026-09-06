package com.book.store.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.book.store.entity.Book;
import com.book.store.entity.User;
import com.book.store.entity.Wishlist;
import com.book.store.exception.ConflictException;
import com.book.store.service.BookService;
import com.book.store.service.UserService;
import com.book.store.service.WishlistService;

@Controller
@RequestMapping("/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String viewWishlist(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(principal.getName());
        List<Wishlist> wishlist = wishlistService.getUserWishlist(user);
        model.addAttribute("wishlist", wishlist);
        return "wishlist";
    }

    @GetMapping("/add/{id}")
    public String addToWishlist(@PathVariable("id") int bookId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        Book book = bookService.getBookById(bookId);
        if (book == null) {
            return "redirect:/available_books";
        }
        User user = userService.findByUsername(principal.getName());
        boolean added = wishlistService.addToWishlist(book, user);
        if (added) {
            redirectAttributes.addFlashAttribute("successMessage", "'" + book.getName() + "' has been added to your wishlist!");
        } else {
            redirectAttributes.addFlashAttribute("infoMessage", "'" + book.getName() + "' is already in your wishlist.");
        }
        return "redirect:/wishlist";
    }

    @GetMapping("/remove/{id}")
    public String removeFromWishlist(@PathVariable("id") int bookId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(principal.getName());
        wishlistService.removeFromWishlist(bookId, user);
        redirectAttributes.addFlashAttribute("successMessage", "Item removed from your wishlist.");
        return "redirect:/wishlist";
    }

    @GetMapping("/moveToCart/{id}")
    public String moveToCart(@PathVariable("id") int bookId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(principal.getName());
        try {
            wishlistService.moveToCart(bookId, user);
            redirectAttributes.addFlashAttribute("successMessage", "Item moved to your cart successfully!");
            return "redirect:/my_books";
        } catch (ConflictException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/wishlist";
        }
    }
}
