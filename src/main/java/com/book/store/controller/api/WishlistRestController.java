package com.book.store.controller.api;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.book.store.dto.WishlistItemResponse;
import com.book.store.entity.Book;
import com.book.store.entity.User;
import com.book.store.exception.BadRequestException;
import com.book.store.exception.ConflictException;
import com.book.store.exception.ResourceNotFoundException;
import com.book.store.service.BookService;
import com.book.store.service.UserService;
import com.book.store.service.WishlistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/wishlist")
@Tag(name = "Wishlist API", description = "Endpoints for managing user wishlist and favorites")
public class WishlistRestController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Get current user's wishlist")
    public List<WishlistItemResponse> getWishlist(Principal principal) {
        User user = getAuthenticatedUser(principal);
        return wishlistService.getUserWishlist(user).stream()
                .map(item -> modelMapper.map(item, WishlistItemResponse.class))
                .collect(Collectors.toList());
    }

    @PostMapping("/items/{bookId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a book to the user's wishlist")
    public void addToWishlist(@PathVariable("bookId") int bookId, Principal principal) {
        User user = getAuthenticatedUser(principal);
        Book book = bookService.getBookById(bookId);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found with id: " + bookId);
        }
        boolean added = wishlistService.addToWishlist(book, user);
        if (!added) {
            throw new ConflictException("Book is already in your wishlist");
        }
    }

    @DeleteMapping("/items/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a book from the user's wishlist")
    public void removeFromWishlist(@PathVariable("bookId") int bookId, Principal principal) {
        User user = getAuthenticatedUser(principal);
        wishlistService.removeFromWishlist(bookId, user);
    }

    @PostMapping("/items/{bookId}/move-to-cart")
    @Operation(summary = "Move an item from the wishlist directly to the shopping cart")
    public void moveToCart(@PathVariable("bookId") int bookId, Principal principal) {
        User user = getAuthenticatedUser(principal);
        wishlistService.moveToCart(bookId, user);
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new BadRequestException("Authentication required");
        }
        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return user;
    }
}
