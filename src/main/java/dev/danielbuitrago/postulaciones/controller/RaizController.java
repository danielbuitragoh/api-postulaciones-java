package dev.danielbuitrago.postulaciones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Hidden;

// La raíz no tiene nada que mostrar: quien abre la dirección de la API en un navegador llega a la
// documentación en lugar de ver un 401.
@Controller
@Hidden
public class RaizController {

	@GetMapping("/")
	public String raiz() {
		return "redirect:/docs";
	}

}
