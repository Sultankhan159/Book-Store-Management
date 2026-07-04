/*A Repository is an interface in Spring used to handle data access operations—like create, read, update, and delete 
 * (CRUD)—without writing SQL or boilerplate code.
   Spring Data JPA provides implementations at runtime so you can focus on your business logic instead of JDBC code.
 * 
 * JpaRepository provides built-in CRUD methods 
 */



package com.book.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.book.store.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book,Integer>{ 
	
    @Query("SELECT b FROM Book b WHERE " +
           "(LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:category = '' OR :category IS NULL OR b.category = :category)")
    List<Book> searchBooks(@Param("query") String query, @Param("category") String category);
    
    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.category IS NOT NULL AND b.category != ''")
    List<String> findDistinctCategories();
}



 /*JpaRepository<Book, Integer>
  * Book:

Specifies the entity type that this repository manages.
It maps to the Book entity class, which represents a database table.

Integer:

Specifies the type of the primary key (ID) of the Book entity.
The id field in the Book class is of type int, which corresponds to Integer in Java.
  */
  