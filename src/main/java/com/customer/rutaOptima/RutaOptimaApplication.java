package com.customer.rutaOptima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RutaOptimaApplication {

	public static void main(String[] args) {
		SpringApplication.run(RutaOptimaApplication.class, args);
	}

}
