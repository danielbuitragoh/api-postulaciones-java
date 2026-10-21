package dev.danielbuitrago.postulaciones.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.danielbuitrago.postulaciones.domain.Empresa;
import dev.danielbuitrago.postulaciones.dto.EmpresaPeticion;
import dev.danielbuitrago.postulaciones.dto.EmpresaRespuesta;
import dev.danielbuitrago.postulaciones.error.ConflictoException;
import dev.danielbuitrago.postulaciones.error.RecursoNoEncontradoException;
import dev.danielbuitrago.postulaciones.repository.EmpresaRepository;

@Service
public class EmpresaService {

	private final EmpresaRepository empresas;

	public EmpresaService(EmpresaRepository empresas) {
		this.empresas = empresas;
	}

	@Transactional(readOnly = true)
	public List<EmpresaRespuesta> listar(Long usuarioId) {
		return empresas.listar(usuarioId);
	}

	@Transactional
	public EmpresaRespuesta crear(Long usuarioId, EmpresaPeticion peticion) {
		String nombre = peticion.nombre().trim();
		if (empresas.findByUsuarioIdAndNombreIgnoreCase(usuarioId, nombre).isPresent()) {
			throw new ConflictoException("Ya tienes una empresa con ese nombre");
		}
		try {
			return EmpresaRespuesta.nueva(
					empresas.saveAndFlush(new Empresa(usuarioId, nombre, peticion.sitioWeb(), peticion.sector())));
		}
		catch (DataIntegrityViolationException e) {
			// Dos altas simultáneas con el mismo nombre: gana el índice único de la base.
			throw new ConflictoException("Ya tienes una empresa con ese nombre");
		}
	}

	@Transactional
	public void borrar(Long usuarioId, Long id) {
		int borradas;
		try {
			borradas = empresas.borrar(id, usuarioId);
		}
		catch (DataIntegrityViolationException e) {
			// La clave foránea de postulaciones.empresa_id impide borrar una empresa en uso.
			throw new ConflictoException("Esa empresa tiene postulaciones; bórralas primero");
		}
		if (borradas == 0) {
			throw new RecursoNoEncontradoException();
		}
	}

}
