package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;

import dev.danielbuitrago.postulaciones.domain.Empresa;

// "postulaciones" es cuántas postulaciones tiene el usuario con esa empresa. Se construye
// directamente desde la consulta JPQL del repositorio.
public record EmpresaRespuesta(Long id, String nombre, String sitioWeb, String sector, Instant creadoEn,
		Long postulaciones) {

	public static EmpresaRespuesta nueva(Empresa empresa) {
		return new EmpresaRespuesta(empresa.getId(), empresa.getNombre(), empresa.getSitioWeb(), empresa.getSector(),
				empresa.getCreadoEn(), 0L);
	}

}
