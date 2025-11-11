
/*The Service layer is a middle layer that contains the business logic of the application.
  It sits between the Controller (handles HTTP requests) and the Repository (interacts with the database).
 * 
 */




package com.book.store.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.book.store.entity.Book;
import com.book.store.repository.BookRepository;

@Service
public class BookService { 
	
	
	/*@Autowired==This is a Spring annotation that tells Spring to inject a bean (a managed object) automatically at runtime.
                  Spring will search the application context for a matching bean of type BookRepository and inject it here.
                  You don’t need to write new BookRepository() manually — Spring does it for you!
	 */

	@Autowired
	private BookRepository bRepo; /*    This is a private field of type BookRepository.
	                                    BookRepository is an interface that extends JpaRepository.This means Spring will automatically provide 
	                                    an implementation with full CRUD operations for the Book entity.
                                        bRepo is just a variable name (you can name it anything).
                                        Once injected, you can call methods like:
                                        bRepo.findAll();
                                        bRepo.save(book);
	
                                   	*/
	
	
	
	
	public void save(Book b) {
		bRepo.save(b);          //Saves or updates a book in the database.
	}
	
	public List<Book>getAllBook(){
		return bRepo.findAll();       //Returns a list of all books from the database.
	}
	
	public Book getBookById(int id){
		return bRepo.findById(id).get();   //Returns a single book based on its id
	}
	
	public void deleteById(int id){
		bRepo.deleteById(id);            //Deletes a book from the database based on its id.
	}
}
