package com.book.store.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.book.store.entity.Book;
import com.book.store.entity.User;
import com.book.store.entity.Wishlist;
import com.book.store.repository.WishlistRepository;

@Service
@Transactional
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private BookService bookService;

    @Autowired
    private MyBookListService myBookListService;

    public boolean addToWishlist(Book book, User user) {
        if (wishlistRepository.existsByUserAndBook(user, book)) {
            return false; // already in wishlist
        }
        Wishlist wishlist = new Wishlist(user, book);
        wishlistRepository.save(wishlist);
        return true;
    }

    public void removeFromWishlist(int bookId, User user) {
        Book book = bookService.getBookById(bookId);
        if (book != null) {
            wishlistRepository.deleteByUserAndBook(user, book);
        }
    }

    public List<Wishlist> getUserWishlist(User user) {
        return wishlistRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public boolean isBookInWishlist(Book book, User user) {
        if (book == null || user == null) {
            return false;
        }
        return wishlistRepository.existsByUserAndBook(user, book);
    }

    public void moveToCart(int bookId, User user) {
        Book book = bookService.getBookById(bookId);
        if (book != null) {
            myBookListService.addToCart(book, user);
            wishlistRepository.deleteByUserAndBook(user, book);
        }
    }

    public long getWishlistCount(User user) {
        return wishlistRepository.countByUser(user);
    }
}
