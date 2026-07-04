//Main Class (Spring Boot Starter)
package com.book.store;

//This is the package name (folder structure).
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//enables Spring Boot features and This is the main annotation of Spring Boot.
//It means:@Configuration (configuration class),,@EnableAutoConfiguration (auto setup),,@ComponentScan (scan controller/service/repository)
@SpringBootApplication

// main project class.
public class BookstoreManagementApplication {

	//Main method (Java execution starts here)
	public static void main(String[] args) {
		//SpringApplication → runs the application
		SpringApplication.run(BookstoreManagementApplication.class, args);
	}

}
