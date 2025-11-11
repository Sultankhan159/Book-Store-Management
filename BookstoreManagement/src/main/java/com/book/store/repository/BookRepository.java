/*A Repository is an interface in Spring used to handle data access operations—like create, read, update, and delete 
 * (CRUD)—without writing SQL or boilerplate code.
   Spring Data JPA provides implementations at runtime so you can focus on your business logic instead of JDBC code.
 * 
 * JpaRepository provides built-in CRUD methods 
 */



package com.book.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.book.store.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book,Integer>{ 
	
}



 /*JpaRepository<Book, Integer>
  * Book:

Specifies the entity type that this repository manages.
It maps to the Book entity class, which represents a database table.

Integer:

Specifies the type of the primary key (ID) of the Book entity.
The id field in the Book class is of type int, which corresponds to Integer in Java.
  */
  