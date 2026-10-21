package dev.danielbuitrago.postulaciones;

import org.springframework.boot.SpringApplication;

public class TestApiPostulacionesJavaApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiPostulacionesJavaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
