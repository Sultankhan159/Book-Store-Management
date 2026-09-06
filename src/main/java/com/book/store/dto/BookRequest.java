package com.book.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BookRequest {
    @NotBlank(message = "Book name cannot be blank")
    @Size(max = 255, message = "Book name must be less than 255 characters")
    private String name;

    @NotBlank(message = "Author name cannot be blank")
    private String author;

    @NotBlank(message = "Price cannot be blank")
    private String price;

    private String category;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @jakarta.validation.constraints.Min(value = 0, message = "Stock must be at least 0")
    private int stock = 50;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
