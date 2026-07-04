package com.book.store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.book.store.entity.MyBookList;
import com.book.store.entity.User;
import com.book.store.entity.Book;

@Repository
public interface MyBookRepository extends JpaRepository<MyBookList,Integer>{
    List<MyBookList> findByUser(User user);
    Optional<MyBookList> findByUserAndBook(User user, Book book);
    void deleteByUser(User user);
}
