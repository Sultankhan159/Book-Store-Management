package com.book.store.controller.api;

import com.book.store.dto.BookRequest;
import com.book.store.dto.BookResponse;
import com.book.store.dto.ReviewRequest;
import com.book.store.dto.ReviewResponse;
import com.book.store.entity.Book;
import com.book.store.entity.Review;
import com.book.store.entity.User;
import com.book.store.exception.ResourceNotFoundException;
import com.book.store.service.BookService;
import com.book.store.service.ReviewService;
import com.book.store.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books API", description = "Endpoints for managing the book catalog and reviews")
public class BookRestController {

    @Autowired
    private BookService bookService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Get list of available books with optional filtering")
    public List<BookResponse> getAllBooks(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "category", required = false) String category) {
        return bookService.searchBooks(query, category).stream()
                .map(book -> modelMapper.map(book, BookResponse.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details of a single book by ID")
    public BookResponse getBookById(@PathVariable("id") int id) {
        Book book = bookService.getBookById(id);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        return modelMapper.map(book, BookResponse.class);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a new book (Admin only)")
    public BookResponse createBook(@Valid @RequestBody BookRequest request) {
        Book book = modelMapper.map(request, Book.class);
        bookService.save(book);
        return modelMapper.map(book, BookResponse.class);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing book by ID (Admin only)")
    public BookResponse updateBook(@PathVariable("id") int id, @Valid @RequestBody BookRequest request) {
        Book existing = bookService.getBookById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        modelMapper.map(request, existing);
        bookService.save(existing);
        return modelMapper.map(existing, BookResponse.class);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a book by ID (Admin only)")
    public void deleteBook(@PathVariable("id") int id) {
        Book existing = bookService.getBookById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        bookService.deleteById(id);
    }

    @PostMapping("/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a review for a book")
    public ReviewResponse addReview(
            @PathVariable("id") int id,
            @Valid @RequestBody ReviewRequest request,
            Principal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("User must be authenticated to add a review");
        }
        Book book = bookService.getBookById(id);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        User user = userService.findByUsername(principal.getName());
        Review review = reviewService.addReview(book, user, request.getRating(), request.getComment());
        
        ReviewResponse response = modelMapper.map(review, ReviewResponse.class);
        response.setUsername(user.getUsername());
        response.setBookName(book.getName());
        return response;
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get all reviews for a book")
    public List<ReviewResponse> getReviews(@PathVariable("id") int id) {
        Book book = bookService.getBookById(id);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        return reviewService.getReviewsForBook(book).stream()
                .map(review -> {
                    ReviewResponse response = modelMapper.map(review, ReviewResponse.class);
                    response.setUsername(review.getUser().getUsername());
                    response.setBookName(book.getName());
                    return response;
                })
                .collect(Collectors.toList());
    }
}
