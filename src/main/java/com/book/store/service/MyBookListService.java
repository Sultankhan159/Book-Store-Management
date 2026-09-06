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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.book.store.entity.Book;
import com.book.store.entity.User;
import com.book.store.entity.MyBookList;
import com.book.store.entity.Order;
import com.book.store.entity.OrderItem;
import com.book.store.event.OrderPlacedEvent;
import com.book.store.exception.ConflictException;
import com.book.store.repository.BookRepository;
import com.book.store.repository.MyBookRepository;
import com.book.store.repository.OrderRepository;

@Service
@Transactional
public class MyBookListService {
	
	@Autowired
	private MyBookRepository mybook;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired(required = false)
	private RedissonClient redissonClient;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private TransactionTemplate transactionTemplate;

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
		if (book.getStock() <= 0) {
			throw new ConflictException("Book '" + book.getName() + "' is out of stock!");
		}
		Optional<MyBookList> existing = mybook.findByUserAndBook(user, book);
		if (existing.isPresent()) {
			MyBookList cartItem = existing.get();
			if (cartItem.getQuantity() + 1 > book.getStock()) {
				throw new ConflictException("Cannot add more: Only " + book.getStock() + " copies available in stock!");
			}
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
					if (quantity > item.getBook().getStock()) {
						throw new ConflictException("Cannot set quantity to " + quantity + ": Only " + item.getBook().getStock() + " copies in stock!");
					}
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
	
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
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
				return transactionTemplate.execute(status -> executeCheckoutTransaction(user));
			}
		}

		try {
			return transactionTemplate.execute(status -> executeCheckoutTransaction(user));
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
		
		// Stock validation before checkout using fresh database records
		for (MyBookList item : cartItems) {
			Book book = bookRepository.findById(item.getBook().getId()).orElse(null);
			if (book == null || book.getStock() < item.getQuantity()) {
				int available = book != null ? book.getStock() : 0;
				throw new ConflictException("Insufficient stock for '" + item.getBook().getName() + "'. Available: " + available + ", In Cart: " + item.getQuantity());
			}
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
		
		Order order = new Order(user, LocalDateTime.now(), totalAmount, "PLACED");
		
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

			Book book = bookRepository.findById(item.getBook().getId()).orElse(item.getBook());
			book.setStock(book.getStock() - item.getQuantity());
			bookRepository.save(book);

			OrderItem orderItem = new OrderItem(order, book, item.getQuantity(), price);
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

	public List<Order> getAllOrders(String status) {
		if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
			return orderRepository.findByStatusOrderByOrderDateDesc(status.toUpperCase());
		}
		return orderRepository.findAllByOrderByOrderDateDesc();
	}

	public Order updateOrderStatus(Long orderId, String newStatus) {
		Order order = orderRepository.findById(orderId).orElse(null);
		if (order != null) {
			order.setStatus(newStatus.toUpperCase());
			return orderRepository.save(order);
		}
		return null;
	}

	public Order getOrderById(Long orderId) {
		return orderRepository.findById(orderId).orElse(null);
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
