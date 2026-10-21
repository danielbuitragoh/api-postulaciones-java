package dev.danielbuitrago.postulaciones;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ApiPostulacionesJavaApplicationTests {

	@Test
	void contextLoads() {
	}

}
