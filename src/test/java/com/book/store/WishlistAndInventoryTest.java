package com.book.store;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.book.store.entity.Book;
import com.book.store.entity.Order;
import com.book.store.entity.User;
import com.book.store.entity.Wishlist;
import com.book.store.exception.ConflictException;
import com.book.store.repository.BookRepository;
import com.book.store.repository.UserRepository;
import com.book.store.service.BookService;
import com.book.store.service.MyBookListService;
import com.book.store.service.WishlistService;

@SpringBootTest
public class WishlistAndInventoryTest {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MyBookListService myBookListService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        testUser = new User();
        testUser.setUsername("user_" + unique);
        testUser.setPassword(passwordEncoder.encode("secret123"));
        testUser.setRoles("ROLE_USER");
        testUser = userRepository.save(testUser);

        testBook = new Book();
        testBook.setName("Domain-Driven Design: " + unique);
        testBook.setAuthor("Eric Evans");
        testBook.setPrice("55.00");
        testBook.setCategory("Software Architecture");
        testBook.setStock(10);
        testBook = bookRepository.save(testBook);
    }

    @Test
    @DisplayName("Wishlist add, duplicate prevention, query, and move-to-cart lifecycle")
    void testWishlistLifecycle() {
        // 1. Add to wishlist
        boolean added = wishlistService.addToWishlist(testBook, testUser);
        Assertions.assertTrue(added, "Initial addition to wishlist should return true");

        // 2. Duplicate addition must be rejected gracefully
        boolean duplicate = wishlistService.addToWishlist(testBook, testUser);
        Assertions.assertFalse(duplicate, "Duplicate addition to wishlist must return false");

        // 3. Query wishlist
        List<Wishlist> wishlist = wishlistService.getUserWishlist(testUser);
        Assertions.assertEquals(1, wishlist.size());
        Assertions.assertEquals(testBook.getId(), wishlist.get(0).getBook().getId());
        Assertions.assertTrue(wishlistService.isBookInWishlist(testBook, testUser));

        // 4. Move to cart
        wishlistService.moveToCart(testBook.getId(), testUser);

        // Verify it was removed from wishlist
        Assertions.assertFalse(wishlistService.isBookInWishlist(testBook, testUser));
        Assertions.assertEquals(0, wishlistService.getUserWishlist(testUser).size());

        // Verify it was added to cart
        var cart = myBookListService.getMyCart(testUser);
        Assertions.assertEquals(1, cart.size());
        Assertions.assertEquals(testBook.getId(), cart.get(0).getBook().getId());
    }

    @Test
    @DisplayName("Checkout atomically decrements book inventory and sets status PLACED")
    void testStockDecrementOnCheckout() {
        testBook.setStock(5);
        bookRepository.save(testBook);

        // Add 2 copies to cart
        myBookListService.addToCart(testBook, testUser);
        myBookListService.addToCart(testBook, testUser);

        // Checkout
        Order order = myBookListService.checkout(testUser);
        Assertions.assertNotNull(order);
        Assertions.assertEquals("PLACED", order.getStatus());
        Assertions.assertEquals(110.0, order.getTotalAmount(), 0.01);

        // Verify stock decremented from 5 to 3
        Book updated = bookService.getBookById(testBook.getId());
        Assertions.assertEquals(3, updated.getStock(), "Stock must decrease by ordered quantity");
    }

    @Test
    @DisplayName("Checkout fails if item quantity exceeds available stock")
    void testInsufficientStockPreventsCheckout() {
        testBook.setStock(1);
        testBook = bookRepository.saveAndFlush(testBook);

        myBookListService.addToCart(testBook, testUser);

        // Simulate stock running out before checkout completes
        Book bookToUpdate = bookRepository.findById(testBook.getId()).orElseThrow();
        bookToUpdate.setStock(0);
        bookRepository.saveAndFlush(bookToUpdate);

        Assertions.assertThrows(ConflictException.class, () -> {
            myBookListService.checkout(testUser);
        }, "Checkout must throw ConflictException when item is out of stock");
    }

    @Test
    @DisplayName("Admin can view all orders and update order status")
    void testAdminOrderStatusManagement() {
        myBookListService.addToCart(testBook, testUser);
        Order order = myBookListService.checkout(testUser);
        Assertions.assertNotNull(order);

        // Update status to SHIPPED
        Order shipped = myBookListService.updateOrderStatus(order.getId(), "SHIPPED");
        Assertions.assertEquals("SHIPPED", shipped.getStatus());

        // Update status to DELIVERED
        Order delivered = myBookListService.updateOrderStatus(order.getId(), "DELIVERED");
        Assertions.assertEquals("DELIVERED", delivered.getStatus());

        // Filter orders by status
        List<Order> deliveredOrders = myBookListService.getAllOrders("DELIVERED");
        Assertions.assertTrue(deliveredOrders.stream().anyMatch(o -> o.getId().equals(order.getId())));
    }
}
