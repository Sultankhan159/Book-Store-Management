package com.book.store.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.book.store.entity.Book;
import com.book.store.entity.User;
import com.book.store.entity.MyBookList;
import com.book.store.entity.Order;
import com.book.store.entity.OrderItem;
import com.book.store.event.OrderPlacedEvent;
import com.book.store.exception.ConflictException;
import com.book.store.repository.MyBookRepository;
import com.book.store.repository.OrderRepository;

@Service
@Transactional
public class MyBookListService {
	
	@Autowired
	private MyBookRepository mybook;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired(required = false)
	private RedissonClient redissonClient;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	private final java.util.concurrent.ConcurrentHashMap<Long, Object> localUserLocks = new java.util.concurrent.ConcurrentHashMap<>();

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
		String lockKey = "lock:checkout:user:" + user.getId();
		RLock lock = null;
		boolean acquired = false;
		if (redissonClient != null) {
			try {
				lock = redissonClient.getLock(lockKey);
				acquired = lock.tryLock(5, 15, TimeUnit.SECONDS);
				if (!acquired) {
					throw new ConflictException("Checkout is currently being processed by another concurrent request for this user. Duplicate order prevented.");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ConflictException("Checkout interrupted while waiting for lock.");
			} catch (Exception e) {
				// Redis connectivity fallback
				lock = null;
			}
		}

		if (lock == null) {
			synchronized (localUserLocks.computeIfAbsent(user.getId(), k -> new Object())) {
				return executeCheckoutTransaction(user);
			}
		}

		try {
			return executeCheckoutTransaction(user);
		} finally {
			if (acquired && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	private Order executeCheckoutTransaction(User user) {
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

		if (eventPublisher != null) {
			eventPublisher.publishEvent(new OrderPlacedEvent(this, savedOrder));
		}
		
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
