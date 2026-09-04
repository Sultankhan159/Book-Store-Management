package com.book.store.dto;

public class CartItemResponse {
    private int id;
    private BookResponse book;
    private int quantity;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public BookResponse getBook() { return book; }
    public void setBook(BookResponse book) { this.book = book; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
