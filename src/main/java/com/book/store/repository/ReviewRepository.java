package com.book.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.book.store.entity.Review;
import com.book.store.entity.Book;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookOrderByCreatedAtDesc(Book book);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book = :book")
    Double getAverageRatingForBook(@Param("book") Book book);
}
