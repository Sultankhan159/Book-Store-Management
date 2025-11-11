package com.book.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.book.store.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User , Long> {

	User findByUsername(String username);
}
