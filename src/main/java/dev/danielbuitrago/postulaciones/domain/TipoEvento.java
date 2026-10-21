package dev.danielbuitrago.postulaciones.domain;

// Los nombres van en minúsculas a propósito: son exactamente los valores que guarda la base
// (CHECK de la tabla eventos) y los que viajan en el JSON, así no hace falta convertir nada.
public enum TipoEvento {
	postulada, respuesta, entrevista, oferta, rechazo, seguimiento, cerrada
}
