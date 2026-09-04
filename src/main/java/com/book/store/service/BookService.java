
/*The Service layer is a middle layer that contains the business logic of the application.
  It sits between the Controller (handles HTTP requests) and the Repository (interacts with the database).
 * 
 */




package com.book.store.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.book.store.entity.Book;
import com.book.store.repository.BookRepository;

@Service
public class BookService { 

	@Autowired
	private BookRepository bRepo;

	@CacheEvict(value = {"books", "categories"}, allEntries = true)
	public void save(Book b) {
		bRepo.save(b);          //Saves or updates a book in the database.
	}
	
	public List<Book> getAllBook(){
		return bRepo.findAll();       //Returns a list of all books from the database.
	}
	
	public List<Book> searchBooks(String query, String category) {
		if (query == null) {
			query = "";
		}
		return bRepo.searchBooks(query, category);
	}
	
	@Cacheable(value = "categories")
	public List<String> getAllCategories() {
		return bRepo.findDistinctCategories();
	}
	
	@Cacheable(value = "books", key = "#id")
	public Book getBookById(int id){
		return bRepo.findById(id).orElse(null);   //Returns a single book based on its id safely
	}
	
	@CacheEvict(value = {"books", "categories"}, allEntries = true)
	public void deleteById(int id){
		bRepo.deleteById(id);            //Deletes a book from the database based on its id.
	}
}
