package com.book.store.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.book.store.entity.Review;
import com.book.store.entity.Book;
import com.book.store.entity.User;
import com.book.store.repository.ReviewRepository;

@Service
@Transactional
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public List<Review> getReviewsForBook(Book book) {
        return reviewRepository.findByBookOrderByCreatedAtDesc(book);
    }

    public Double getAverageRating(Book book) {
        return reviewRepository.getAverageRatingForBook(book);
    }

    public Review addReview(Book book, User user, int rating, String comment) {
        Review review = new Review(book, user, rating, comment, LocalDateTime.now());
        return reviewRepository.save(review);
    }
}
