package com.book.store.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.book.store.entity.Book;
import com.book.store.entity.User;
import com.book.store.entity.Review;
import com.book.store.service.BookService;
import com.book.store.service.MyBookListService;
import com.book.store.service.UserService;
import com.book.store.service.ReviewService;

@Controller    
public class BookController {
	
	@Autowired
	private BookService service; 
	
	@Autowired
	private MyBookListService myBookService;

	@Autowired
	private UserService userService;

	@Autowired
	private ReviewService reviewService;
	
	@GetMapping("/")     
	public String home() {  
		return "home";
	}
	
	@GetMapping("/book_rsgister")   
	public String bookRegister() {   
		return "bookRegister";
	}
	
	@GetMapping("/available_books")   
	public ModelAndView getAllBook(
			@RequestParam(value = "query", required = false) String query,
			@RequestParam(value = "category", required = false) String category) {  
		List<Book> list = service.searchBooks(query, category);
		List<String> categories = service.getAllCategories();
		
		ModelAndView mv = new ModelAndView("bookList");
		mv.addObject("book", list);
		mv.addObject("categories", categories);
		mv.addObject("selectedQuery", query);
		mv.addObject("selectedCategory", category);
		return mv;
	}
	
	@PostMapping("/save")   
	public String addBook(
			@ModelAttribute Book b,
			@RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
			@RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile) throws IOException {  
		
		if (coverFile != null && !coverFile.isEmpty()) {
			b.setCoverImage(coverFile.getBytes());
			b.setCoverImageContentType(coverFile.getContentType());
		} else if (b.getId() != 0) {
			Book existing = service.getBookById(b.getId());
			b.setCoverImage(existing.getCoverImage());
			b.setCoverImageContentType(existing.getCoverImageContentType());
		}
		
		if (pdfFile != null && !pdfFile.isEmpty()) {
			b.setPdfContent(pdfFile.getBytes());
			b.setPdfContentType(pdfFile.getContentType());
		} else if (b.getId() != 0) {
			Book existing = service.getBookById(b.getId());
			b.setPdfContent(existing.getPdfContent());
			b.setPdfContentType(existing.getPdfContentType());
		}
		
		service.save(b);
		return "redirect:/available_books";
	}
	
	@GetMapping("/book/cover/{id}")
	public ResponseEntity<byte[]> getBookCover(@PathVariable("id") int id) {
		Book b = service.getBookById(id);
		if (b == null || b.getCoverImage() == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(b.getCoverImageContentType()))
				.body(b.getCoverImage());
	}
	
	@GetMapping("/book/pdf/{id}")
	public ResponseEntity<byte[]> getBookPdf(@PathVariable("id") int id) {
		Book b = service.getBookById(id);
		if (b == null || b.getPdfContent() == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(b.getPdfContentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + b.getName() + ".pdf\"")
				.body(b.getPdfContent());
	}
	
	@GetMapping("/book/details/{id}")
	public String getBookDetails(@PathVariable("id") int id, Model model, Principal principal) {
		Book b = service.getBookById(id);
		List<Review> reviews = reviewService.getReviewsForBook(b);
		Double avgRating = reviewService.getAverageRating(b);
		
		model.addAttribute("book", b);
		model.addAttribute("reviews", reviews);
		model.addAttribute("avgRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
		
		if (principal != null) {
			model.addAttribute("currentUser", principal.getName());
		}
		
		return "bookDetails";
	}
	
	@PostMapping("/addReview/{id}")
	public String addReview(
			@PathVariable("id") int id,
			@RequestParam("rating") int rating,
			@RequestParam("comment") String comment,
			Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		Book b = service.getBookById(id);
		User user = userService.findByUsername(principal.getName());
		reviewService.addReview(b, user, rating, comment);
		return "redirect:/book/details/" + id;
	}
	
	@GetMapping("/admin/dashboard")
	public String adminDashboard(Model model) {
		List<Book> books = service.getAllBook();
		long totalUsers = userService.countUsers();
		double totalSales = myBookService.getTotalSales();
		long totalOrders = myBookService.getTotalOrdersCount();
		
		// Group books by category for charts
		Map<String, Integer> categoryData = new HashMap<>();
		for (Book b : books) {
			String category = b.getCategory();
			if (category == null || category.trim().isEmpty()) {
				category = "Uncategorized";
			}
			categoryData.put(category, categoryData.getOrDefault(category, 0) + 1);
		}
		
		model.addAttribute("totalBooks", books.size());
		model.addAttribute("totalUsers", totalUsers);
		model.addAttribute("totalSales", Math.round(totalSales * 100.0) / 100.0);
		model.addAttribute("totalOrders", totalOrders);
		model.addAttribute("categoryData", categoryData);
		model.addAttribute("usersList", userService.getAllUsers());
		
		return "dashboard";
	}
	
	@GetMapping("/my_books") 
	public String getMyBooks(Model model, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("book", myBookService.getMyCart(user));  
		return "myBooks";                 
	}
	
	@GetMapping("/myList/{id}")      
	public String getMyList(@PathVariable("id") int id, Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		Book b = service.getBookById(id);
		User user = userService.findByUsername(principal.getName());
		myBookService.addToCart(b, user);
		return "redirect:/my_books";    
	}
	
	@RequestMapping("/editBook/{id}")  
	public String editBook(@PathVariable("id") int id, Model model) { 
		Book b = service.getBookById(id);
		model.addAttribute("book", b);
		return "bookEdit";  
	}
	
	@RequestMapping("/deleteBook/{id}") 
	public String deleteBook(@PathVariable("id") int id) {
		service.deleteById(id);
		return "redirect:/available_books";
	}
	
	@GetMapping("/login")
	public String login() {
		return "login";
	}
	
	@GetMapping("/access-denied")
	public String accessDenied() {
		return "accessDenied";
	}
}