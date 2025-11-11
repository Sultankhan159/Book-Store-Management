package com.book.store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.book.store.service.MyBookListService;

@Controller
public class MyBookListController { //handles operations related to a user's personalized book list, specifically deleting a book from the list.
	
	@Autowired
	private MyBookListService service; //Injects the MyBookListService dependency to access business logic for managing the user's book list.

	@RequestMapping("/deleteMyList/{id}") //{id} is a path variable, capturing a dynamic value (the ID of the book to delete).
	public String deleteMyList(@PathVariable("id")int id) //Binds the value from the URL (e.g., /deleteMyList/123 → id = 123) to the method parameter.
	{
		service.deleteById(id); //Calls the deleteById method in the MyBookListService to remove the book with the given ID from the personalized book list.
		return"redirect:/my_books"; //After deleting the book, the method redirects the user to the /my_books URL.
		                              //This ensures the user sees the updated list of books in their personal collection.

	}
}
