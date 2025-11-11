
/*In Spring Boot, a Controller is a class responsible for handling web requests 
 * (like GET, POST, PUT, DELETE), interacting with the Service layer, and returning a view (for web apps) or data (for REST APIs).
 * @Controller=Used for traditional MVC apps, returns views (HTML/Thymeleaf).
 * @RestController=	Used for REST APIs, returns data (usually JSON).
 * @RequestMapping, @GetMapping, @PostMapping, etc.	Map HTTP methods to methods in your class.
 */





package com.book.store.controller;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.book.store.entity.MyBookList;
import com.book.store.service.BookService;
import com.book.store.service.MyBookListService;

@Controller    
public class BookController {
	
	
	/*@Autowired=Annotation provided by Spring , Tells Spring to inject the required dependency automatically.
	 * Instead of writing: BookService service = new BookService();
	 */
	@Autowired
	private BookService service; 
	@Autowired
	private MyBookListService myBookService;
	
	
	
    /*This method handles HTTP GET requests to the root URL / and returns a view name called "home" — 
     * usually an HTML page like home.html.
     * home()=Handles the request
     * return "home"=	Tells Spring to render the home.html view
     */
	@GetMapping("/")     
	public String home() {  
		return"home";
	}
	
	
	
	/*@GetMapping("/book_rsgister")	Maps GET requests to /book_rsgister URL
      bookRegister()=	Method that runs when URL is hit
      return "bookRegister"=	Loads the bookRegister.html view page
	 */
	@GetMapping("/book_rsgister")   
	public String bookRegister() {   
		return"bookRegister";
	}
	
	
	/*@GetMapping("/available_books")=	Handles requests to this URL
      ModelAndView=	Combines view name + data
      "bookList"=	View to be rendered
      "book"=	Name of the list in the view
       list=	List of books from service
	 */
	@GetMapping("/available_books")   
		public ModelAndView getAllBook() {  
		List<Book>list=service.getAllBook();
		return new ModelAndView("bookList","book",list);
	}
	
	
	
	/*@PostMapping("/save")	Handles form submission
      @ModelAttribute Book b=	Binds form data to a Book object
      service.save(b)=	Saves book via service layer
      redirect:/available_books=	Redirects to view all books after saving
	 */
	@PostMapping("/save")   
	public String addBook(@ModelAttribute Book b) {  
		service.save(b);
		return "redirect:/available_books";
	
	}
	
	
	
	
	@GetMapping("/my_books") 
	public String getMyBooks(Model model)
	{
		List<MyBookList>List=myBookService.getAllMyBooks();
		model.addAttribute("book",List);  
		return "myBooks";                 

	}
	
	@RequestMapping("/myList/{id}")      
	public String getMyList(@PathVariable("id")int id)
	{
		Book b=service.getBookById(id);
		MyBookList mb=new MyBookList(b.getId(),b.getName(),b.getAuthor(),b.getPrice());
		myBookService.saveMyBooks(mb); 
		return "redirect:/my_books";    
	}
	
	@RequestMapping("/editBook/{id}")  
	public String editBook(@PathVariable("id")int id,Model model) { 
		Book b=service.getBookById(id);
		model.addAttribute("book",b);
		return "bookEdit";  
	}
	
	@RequestMapping("/deleteBook/{id}") 
	public String deleteBook(@PathVariable("id")int id)
	{
		service.deleteById(id);
		return "redirect:/available_books";
	}
	
//	@GetMapping("/viewPdf/{id}")
//	public ResponseEntity<byte[]> viewPdf(@PathVariable("id") int id) {
//	    Book book = service.getBookById(id);
//	    if (book.getPdfContent() == null) {
//	        return ResponseEntity.notFound().build();
//	    }
//	    
//	    return ResponseEntity.ok()
//	        .contentType(MediaType.parseMediaType(book.getPdfContentType()))
//	        .header("Content-Disposition", "inline; filename=\"" + book.getName() + ".pdf\"")
//	        .body(book.getPdfContent());
//	}
	

	
	@GetMapping("/login")
	public String login() {
		return "login";
	}
	
	// Add access denied handler
	@GetMapping("/access-denied")
	public String accessDenied() {
		return "accessDenied";
	}
	
}
