package com.book.store.service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.book.store.entity.Book;
import com.book.store.entity.User;
import com.book.store.entity.MyBookList;
import com.book.store.entity.Order;
import com.book.store.entity.OrderItem;
import com.book.store.repository.MyBookRepository;
import com.book.store.repository.OrderRepository;

@Service
@Transactional
public class MyBookListService {
	
	@Autowired
	private MyBookRepository mybook;

	@Autowired
	private OrderRepository orderRepository;
	
	public void saveMyBooks(MyBookList book) {
		mybook.save(book);
	}
	
	public List<MyBookList> getAllMyBooks() {
		return mybook.findAll();
	}
	
	public List<MyBookList> getMyCart(User user) {
		return mybook.findByUser(user);
	}
	
	public void addToCart(Book book, User user) {
		Optional<MyBookList> existing = mybook.findByUserAndBook(user, book);
		if (existing.isPresent()) {
			MyBookList cartItem = existing.get();
			cartItem.setQuantity(cartItem.getQuantity() + 1);
			mybook.save(cartItem);
		} else {
			MyBookList cartItem = new MyBookList(book, user, 1);
			mybook.save(cartItem);
		}
	}
	
	public void updateQuantity(int cartItemId, int quantity, User user) {
		Optional<MyBookList> itemOpt = mybook.findById(cartItemId);
		if (itemOpt.isPresent()) {
			MyBookList item = itemOpt.get();
			if (item.getUser().getId().equals(user.getId())) {
				if (quantity <= 0) {
					mybook.delete(item);
				} else {
					item.setQuantity(quantity);
					mybook.save(item);
				}
			}
		}
	}
	
	public void deleteById(int id) {
		mybook.deleteById(id);
	}
	
	public void deleteFromCart(int cartItemId, User user) {
		Optional<MyBookList> itemOpt = mybook.findById(cartItemId);
		if (itemOpt.isPresent()) {
			MyBookList item = itemOpt.get();
			if (item.getUser().getId().equals(user.getId())) {
				mybook.delete(item);
			}
		}
	}
	
	public Order checkout(User user) {
		List<MyBookList> cartItems = mybook.findByUser(user);
		if (cartItems.isEmpty()) {
			return null;
		}
		
		double totalAmount = 0.0;
		for (MyBookList item : cartItems) {
			double price = 0.0;
			try {
				price = Double.parseDouble(item.getBook().getPrice());
			} catch (Exception e) {
				// Handle fallback if price is not numeric (e.g. "$250")
				String cleanPrice = item.getBook().getPrice().replaceAll("[^0-9.]", "");
				if (!cleanPrice.isEmpty()) {
					price = Double.parseDouble(cleanPrice);
				}
			}
			totalAmount += price * item.getQuantity();
		}
		
		Order order = new Order(user, LocalDateTime.now(), totalAmount, "COMPLETED");
		
		for (MyBookList item : cartItems) {
			double price = 0.0;
			try {
				price = Double.parseDouble(item.getBook().getPrice());
			} catch (Exception e) {
				String cleanPrice = item.getBook().getPrice().replaceAll("[^0-9.]", "");
				if (!cleanPrice.isEmpty()) {
					price = Double.parseDouble(cleanPrice);
				}
			}
			OrderItem orderItem = new OrderItem(order, item.getBook(), item.getQuantity(), price);
			order.getItems().add(orderItem);
		}
		
		Order savedOrder = orderRepository.save(order);
		mybook.deleteByUser(user); // clear cart
		
		return savedOrder;
	}
	
	public List<Order> getUserOrders(User user) {
		return orderRepository.findByUserOrderByOrderDateDesc(user);
	}
	
	// Analytics for admin
	public double getTotalSales() {
		Double total = orderRepository.getTotalRevenue();
		return total == null ? 0.0 : total;
	}
	
	public long getTotalOrdersCount() {
		return orderRepository.count();
	}
}
