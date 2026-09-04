package com.book.store.controller.api;

import com.book.store.dto.CartItemResponse;
import com.book.store.dto.OrderItemResponse;
import com.book.store.dto.OrderResponse;
import com.book.store.entity.Book;
import com.book.store.entity.MyBookList;
import com.book.store.entity.Order;
import com.book.store.entity.User;
import com.book.store.exception.BadRequestException;
import com.book.store.exception.ConflictException;
import com.book.store.exception.ResourceNotFoundException;
import com.book.store.service.BookService;
import com.book.store.service.IdempotencyService;
import com.book.store.service.MyBookListService;
import com.book.store.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Cart & Orders API", description = "Endpoints for managing the shopping cart, checkouts, and order history")
public class CartRestController {

    @Autowired
    private MyBookListService myBookListService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/cart")
    @Operation(summary = "Get current user's shopping cart")
    public List<CartItemResponse> getCart(Principal principal) {
        User user = getAuthenticatedUser(principal);
        return myBookListService.getMyCart(user).stream()
                .map(item -> modelMapper.map(item, CartItemResponse.class))
                .collect(Collectors.toList());
    }

    @PostMapping("/cart/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a book to the shopping cart")
    public void addToCart(@RequestParam("bookId") int bookId, Principal principal) {
        User user = getAuthenticatedUser(principal);
        Book book = bookService.getBookById(bookId);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found with id: " + bookId);
        }
        myBookListService.addToCart(book, user);
    }

    @PutMapping("/cart/items/{id}")
    @Operation(summary = "Update the quantity of a cart item")
    public void updateCartItem(@PathVariable("id") int cartItemId, @RequestParam("quantity") int quantity, Principal principal) {
        User user = getAuthenticatedUser(principal);
        myBookListService.updateQuantity(cartItemId, quantity, user);
    }

    @DeleteMapping("/cart/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an item from the shopping cart")
    public void deleteCartItem(@PathVariable("id") int cartItemId, Principal principal) {
        User user = getAuthenticatedUser(principal);
        myBookListService.deleteFromCart(cartItemId, user);
    }

    @PostMapping("/cart/checkout")
    @Operation(summary = "Checkout the shopping cart and place an order (Supports Idempotency-Key header)")
    @CircuitBreaker(name = "checkoutService", fallbackMethod = "fallbackCheckout")
    @RateLimiter(name = "checkoutRateLimiter")
    public OrderResponse checkout(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Principal principal) {
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            IdempotencyService.IdempotencyStatus status = idempotencyService.checkAndAcquire(idempotencyKey);
            if (status == IdempotencyService.IdempotencyStatus.PROCESSING) {
                throw new ConflictException("Checkout with Idempotency-Key '" + idempotencyKey + "' is already in progress. Duplicate prevented.");
            } else if (status == IdempotencyService.IdempotencyStatus.COMPLETED) {
                throw new ConflictException("An order has already been processed with Idempotency-Key '" + idempotencyKey + "'. Duplicate order prevented.");
            }
        }

        try {
            User user = getAuthenticatedUser(principal);
            Order order = myBookListService.checkout(user);
            if (order == null) {
                throw new BadRequestException("Cannot checkout an empty cart!");
            }
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                idempotencyService.markCompleted(idempotencyKey);
            }
            return mapToOrderResponse(order);
        } catch (Exception e) {
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                idempotencyService.release(idempotencyKey);
            }
            throw e;
        }
    }

    public OrderResponse fallbackCheckout(String idempotencyKey, Principal principal, Throwable t) {
        if (t instanceof ConflictException) {
            throw (ConflictException) t;
        }
        if (t instanceof BadRequestException) {
            throw (BadRequestException) t;
        }
        throw new BadRequestException("Checkout service is temporarily unavailable or busy. Please try again later. Reason: " + t.getMessage());
    }

    @GetMapping("/orders")
    @Operation(summary = "Get the order history of the current user")
    public List<OrderResponse> getOrderHistory(Principal principal) {
        User user = getAuthenticatedUser(principal);
        return myBookListService.getUserOrders(user).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
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

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = modelMapper.map(order, OrderResponse.class);
        response.setUserId(order.getUser().getId());
        response.setUsername(order.getUser().getUsername());
        
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> {
                    OrderItemResponse itemDto = modelMapper.map(item, OrderItemResponse.class);
                    itemDto.setBookId(item.getBook().getId());
                    itemDto.setBookName(item.getBook().getName());
                    return itemDto;
                })
                .collect(Collectors.toList());
        response.setItems(items);
        return response;
    }
}
