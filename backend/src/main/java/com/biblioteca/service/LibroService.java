
package com.biblioteca.service;

import com.biblioteca.dto.LibroDTO;
import com.biblioteca.model.Libro;
import com.biblioteca.repository.LibroRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.SocioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class LibroService {

    private final LibroRepository repositorioLibro;
    private final PrestamoRepository repositorioPrestamo;
    private final SocioRepository repositorioSocio;

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "libros", key = "#categoria ?: 'all'", condition = "#usuario == null && (#excluirLeidos == null || !#excluirLeidos) && (#soloLeidos == null || !#soloLeidos)")
    public List<LibroDTO> obtenerTodosLosLibros(String categoria, Boolean excluirLeidos, Boolean soloLeidos,
            String usuario) {
        List<Libro> libros;
        List<Long> idsLibrosUsuario = (usuario != null) ? obtenerIdsLibrosLeidos(usuario) : Collections.emptyList();
        boolean tieneFiltroCategoria = categoria != null && !categoria.isEmpty() && !categoria.equals("Todas");

        // Lógica "Solo Leídos"
        if (Boolean.TRUE.equals(soloLeidos) && !idsLibrosUsuario.isEmpty()) {
            if (tieneFiltroCategoria) {
                // Optimización: filtrar en memoria si la lista de leídos es pequeña
                libros = repositorioLibro.findAllById(idsLibrosUsuario).stream()
                        .filter(l -> l.getCategoria().equals(categoria))
                        .collect(Collectors.toList());
            } else {
                libros = repositorioLibro.findAllById(idsLibrosUsuario);
            }
        }
        // Lógica "Excluir Leídos"
        else if (Boolean.TRUE.equals(excluirLeidos) && !idsLibrosUsuario.isEmpty()) {
            if (tieneFiltroCategoria) {
                libros = repositorioLibro.findByCategoriaAndIdLibroNotIn(categoria, idsLibrosUsuario);
            } else {
                libros = repositorioLibro.findByIdLibroNotIn(idsLibrosUsuario);
            }
        }
        // Búsqueda estándar
        else {
            if (tieneFiltroCategoria) {
                libros = repositorioLibro.findByCategoria(categoria);
            } else {
                libros = repositorioLibro.findAll();
            }
        }

        return libros.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LibroDTO> obtenerLibrosPaginados(int pagina, int tamanio, String categoria, String busqueda,
            Boolean excluirLeidos, String usuario) {
        Pageable paginacion = org.springframework.data.domain.PageRequest.of(pagina, tamanio,
                org.springframework.data.domain.Sort.by("titulo").ascending());

        List<Long> idsExcluidos = (Boolean.TRUE.equals(excluirLeidos) && usuario != null)
                ? obtenerIdsLibrosLeidos(usuario)
                : Collections.emptyList();

        boolean hayExclusiones = !idsExcluidos.isEmpty();

        Page<Libro> paginaLibros;
        if (busqueda != null && !busqueda.isEmpty()) {
            paginaLibros = hayExclusiones
                    ? repositorioLibro.findBySearchAndIdLibroNotIn(busqueda, idsExcluidos, paginacion)
                    : repositorioLibro.findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(busqueda, busqueda,
                            paginacion);
        } else if (categoria != null && !categoria.isEmpty() && !categoria.equals("Todas")) {
            paginaLibros = hayExclusiones
                    ? repositorioLibro.findByCategoriaAndIdLibroNotIn(categoria, idsExcluidos, paginacion)
                    : repositorioLibro.findByCategoria(categoria, paginacion);
        } else {
            paginaLibros = hayExclusiones
                    ? repositorioLibro.findByIdLibroNotIn(idsExcluidos, paginacion)
                    : repositorioLibro.findAll(paginacion);
        }

        return paginaLibros.map(this::convertirADTO);
    }

    @Transactional(readOnly = true)
    public Optional<LibroDTO> obtenerLibroPorId(@NonNull Long id) {
        Objects.requireNonNull(id, "El ID del libro no puede ser nulo");
        // Aseguramos que el cache funcione evitando lógica compleja aquí
        return repositorioLibro.findById(id).map(this::convertirADTO);
    }

    // ------------------------------------------------------------------------------------------------
    // MÉTODOS PRIVADOS
    // ------------------------------------------------------------------------------------------------

    private LibroDTO convertirADTO(Libro libro) {
        // Optimizado: usa campos calculados con @Formula si aplica
        return LibroDTO.fromEntity(libro);
    }

    private List<Long> obtenerIdsLibrosLeidos(String usuario) {
        return repositorioSocio.findByUsuario(usuario)
                .map(s -> repositorioPrestamo.findIdsLibrosLeidosBySocio(s.getIdSocio()))
                .orElse(Collections.emptyList());
    }
}
