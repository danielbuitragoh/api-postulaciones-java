package dev.danielbuitrago.postulaciones.service;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.danielbuitrago.postulaciones.domain.Empresa;
import dev.danielbuitrago.postulaciones.domain.Evento;
import dev.danielbuitrago.postulaciones.domain.Postulacion;
import dev.danielbuitrago.postulaciones.domain.TipoEvento;
import dev.danielbuitrago.postulaciones.dto.EmpresaPeticion;
import dev.danielbuitrago.postulaciones.dto.EventoPeticion;
import dev.danielbuitrago.postulaciones.dto.EventoRespuesta;
import dev.danielbuitrago.postulaciones.dto.PaginaRespuesta;
import dev.danielbuitrago.postulaciones.dto.PostulacionCambios;
import dev.danielbuitrago.postulaciones.dto.PostulacionDetalle;
import dev.danielbuitrago.postulaciones.dto.PostulacionPeticion;
import dev.danielbuitrago.postulaciones.dto.PostulacionRespuesta;
import dev.danielbuitrago.postulaciones.error.ConflictoException;
import dev.danielbuitrago.postulaciones.error.PeticionInvalidaException;
import dev.danielbuitrago.postulaciones.error.RecursoNoEncontradoException;
import dev.danielbuitrago.postulaciones.repository.EmpresaRepository;
import dev.danielbuitrago.postulaciones.repository.EventoRepository;
import dev.danielbuitrago.postulaciones.repository.PostulacionRepository;

@Service
public class PostulacionService {

	private static final int TAMANO_PAGINA = 20;

	private final PostulacionRepository postulaciones;
	private final EmpresaRepository empresas;
	private final EventoRepository eventos;

	public PostulacionService(PostulacionRepository postulaciones, EmpresaRepository empresas,
			EventoRepository eventos) {
		this.postulaciones = postulaciones;
		this.empresas = empresas;
		this.eventos = eventos;
	}

	@Transactional(readOnly = true)
	public PaginaRespuesta<PostulacionRespuesta> listar(Long usuarioId, TipoEvento estado, String empresa,
			int pagina, String orden) {
		if (pagina < 0) {
			throw new PeticionInvalidaException("La página no puede ser negativa");
		}
		String filtroEmpresa = (empresa == null || empresa.isBlank()) ? null : empresa.trim();
		String filtroEstado = estado == null ? null : estado.name();

		Page<PostulacionRespuesta> resultado = postulaciones.listar(usuarioId, filtroEstado, filtroEmpresa,
				PageRequest.of(pagina, TAMANO_PAGINA, ordenar(orden)));
		return PaginaRespuesta.de(resultado);
	}

	@Transactional(readOnly = true)
	public PostulacionDetalle obtener(Long usuarioId, Long id) {
		PostulacionRespuesta resumen = postulaciones.buscarResumen(id, usuarioId)
				.orElseThrow(RecursoNoEncontradoException::new);
		List<EventoRespuesta> historial = eventos.listarDe(id, usuarioId).stream()
				.map(EventoRespuesta::de).toList();
		return PostulacionDetalle.de(resumen, historial);
	}

	@Transactional
	public PostulacionDetalle crear(Long usuarioId, PostulacionPeticion peticion) {
		validarSalarios(peticion.salarioMin(), peticion.salarioMax());

		Long empresaId = resolverEmpresa(usuarioId, peticion);
		Instant postuladoEn = peticion.postuladoEn() != null ? peticion.postuladoEn() : Instant.now();

		Postulacion nueva = postulaciones.save(new Postulacion(usuarioId, empresaId, peticion.puesto().trim(),
				peticion.fuente(), peticion.urlOferta(), peticion.salarioMin(), peticion.salarioMax(),
				peticion.moneda(), postuladoEn));
		// El estado sale del último evento, así que toda postulación nace con su evento "postulada".
		eventos.save(new Evento(nueva.getId(), TipoEvento.postulada, null, postuladoEn));

		return obtener(usuarioId, nueva.getId());
	}

	@Transactional
	public PostulacionDetalle actualizar(Long usuarioId, Long id, PostulacionCambios cambios) {
		Postulacion postulacion = postulaciones.findByIdAndUsuarioId(id, usuarioId)
				.orElseThrow(RecursoNoEncontradoException::new);

		Integer salarioMin = cambios.salarioMin() != null ? cambios.salarioMin() : postulacion.getSalarioMin();
		Integer salarioMax = cambios.salarioMax() != null ? cambios.salarioMax() : postulacion.getSalarioMax();
		validarSalarios(salarioMin, salarioMax);

		postulacion.actualizar(
				cambios.puesto() != null ? cambios.puesto().trim() : postulacion.getPuesto(),
				cambios.fuente() != null ? cambios.fuente() : postulacion.getFuente(),
				cambios.urlOferta() != null ? cambios.urlOferta() : postulacion.getUrlOferta(),
				salarioMin,
				salarioMax,
				cambios.moneda() != null ? cambios.moneda() : postulacion.getMoneda());

		return obtener(usuarioId, id);
	}

	@Transactional
	public void borrar(Long usuarioId, Long id) {
		if (postulaciones.borrar(id, usuarioId) == 0) {
			throw new RecursoNoEncontradoException();
		}
	}

	@Transactional
	public EventoRespuesta registrarEvento(Long usuarioId, Long postulacionId, EventoPeticion peticion) {
		Postulacion postulacion = postulaciones.findByIdAndUsuarioId(postulacionId, usuarioId)
				.orElseThrow(RecursoNoEncontradoException::new);

		Instant ocurridoEn = peticion.ocurridoEn() != null ? peticion.ocurridoEn() : Instant.now();
		// Que no sea futura lo valida el DTO (@PastOrPresent). Aquí se comprueba lo que solo se sabe
		// con la postulación a mano: un evento no puede ocurrir antes de que exista la postulación.
		if (ocurridoEn.isBefore(postulacion.getPostuladoEn())) {
			throw new PeticionInvalidaException("ocurridoEn no puede ser anterior a la fecha de la postulación");
		}
		Evento evento = eventos.save(new Evento(postulacionId, peticion.tipo(), peticion.nota(), ocurridoEn));
		return EventoRespuesta.de(evento);
	}

	// La empresa siempre es del usuario: con empresaId se busca con el usuario en el WHERE (una
	// empresa ajena responde 404, no se puede colgar una postulación de la empresa de otro), y con
	// el objeto "empresa" se busca o se crea entre las empresas de ese mismo usuario.
	private Long resolverEmpresa(Long usuarioId, PostulacionPeticion peticion) {
		if ((peticion.empresaId() != null) == (peticion.empresa() != null)) {
			throw new PeticionInvalidaException("Indica empresaId o empresa, pero no ambos");
		}
		if (peticion.empresaId() != null) {
			return empresas.findByIdAndUsuarioId(peticion.empresaId(), usuarioId)
					.orElseThrow(RecursoNoEncontradoException::new).getId();
		}

		EmpresaPeticion datos = peticion.empresa();
		String nombre = datos.nombre().trim();
		return empresas.findByUsuarioIdAndNombreIgnoreCase(usuarioId, nombre)
				.orElseGet(() -> crearEmpresa(usuarioId, nombre, datos))
				.getId();
	}

	private Empresa crearEmpresa(Long usuarioId, String nombre, EmpresaPeticion datos) {
		try {
			return empresas.saveAndFlush(new Empresa(usuarioId, nombre, datos.sitioWeb(), datos.sector()));
		}
		catch (DataIntegrityViolationException e) {
			// Dos altas simultáneas de la misma empresa: gana el índice único; el cliente reintenta.
			throw new ConflictoException("Ya tienes una empresa con ese nombre; vuelve a intentarlo");
		}
	}

	// Lista blanca de órdenes: lo que escribe el cliente nunca llega tal cual al ORDER BY.
	private static Sort ordenar(String orden) {
		String valor = orden == null ? "reciente" : orden;
		return switch (valor) {
			case "reciente" -> Sort.by(Sort.Order.desc("p.postuladoEn"), Sort.Order.desc("p.id"));
			case "antigua" -> Sort.by(Sort.Order.asc("p.postuladoEn"), Sort.Order.asc("p.id"));
			case "empresa" -> Sort.by(Sort.Order.asc("e.nombre"), Sort.Order.desc("p.postuladoEn"),
					Sort.Order.desc("p.id"));
			default -> throw new PeticionInvalidaException("Orden no válido. Usa: reciente, antigua o empresa");
		};
	}

	private static void validarSalarios(Integer minimo, Integer maximo) {
		if (minimo != null && maximo != null && minimo > maximo) {
			throw new PeticionInvalidaException("salarioMin no puede ser mayor que salarioMax");
		}
	}

}
