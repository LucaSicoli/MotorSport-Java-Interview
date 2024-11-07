package com.motorsport;
import com.motorsport.service.MotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MotorsportApplication implements CommandLineRunner {

	@Autowired
	private MotoService motoService;

	public static void main(String[] args) {
		SpringApplication.run(MotorsportApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Fetching motorcycle data...");
		motoService.obtenerPromedioPorMarca(); // Automatically perform a GET request when the application starts
	}
}