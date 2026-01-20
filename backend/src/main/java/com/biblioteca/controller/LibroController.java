package com.biblioteca.controller;

import com.biblioteca.model.Libro;
import com.biblioteca.repository.LibroRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.SocioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/libros")
@Tag(name = "Libros", description = "API para consultar el catálogo de libros")
public class LibroController {

    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;
    private final SocioRepository socioRepository;

    public LibroController(LibroRepository libroRepository,
            PrestamoRepository prestamoRepository,
            SocioRepository socioRepository) {
        this.libroRepository = libroRepository;
        this.prestamoRepository = prestamoRepository;
        this.socioRepository = socioRepository;
    }

    @GetMapping
    @Operation(summary = "Listar libros", description = "Obtiene el catálogo de libros con filtros opcionales")
    @Cacheable(value = "libros", key = "#categoria ?: 'all'", condition = "#excludeRead == null && #onlyRead == null")
    public List<Libro> getAllLibros(
            @Parameter(description = "Filtrar por categoría") @RequestParam(required = false) String categoria,
            @Parameter(description = "Excluir libros ya leídos") @RequestParam(required = false) Boolean excludeRead,
            @Parameter(description = "Mostrar solo libros leídos") @RequestParam(required = false) Boolean onlyRead,
            @Parameter(description = "Username para filtros de lectura") @RequestParam(required = false) String username) {

        List<Libro> libros;

        if (categoria != null && !categoria.isEmpty() && !categoria.equals("Todas")) {
            libros = libroRepository.findByCategoria(categoria);
        } else {
            libros = libroRepository.findAll();
        }

        // Apply user-specific filters (not cached)
        if (Boolean.TRUE.equals(excludeRead) && username != null) {
            Long idSocio = socioRepository.findByUsuario(username).map(s -> s.getIdSocio()).orElse(null);
            if (idSocio != null) {
                List<Long> readBookIds = prestamoRepository.findBySocioIdSocio(idSocio).stream()
                        .map(p -> p.getEjemplar().getLibro().getIdLibro())
                        .collect(Collectors.toList());

                libros = libros.stream()
                        .filter(l -> !readBookIds.contains(l.getIdLibro()))
                        .collect(Collectors.toList());
            }
        }

        if (Boolean.TRUE.equals(onlyRead) && username != null) {
            Long idSocio = socioRepository.findByUsuario(username).map(s -> s.getIdSocio()).orElse(null);
            if (idSocio != null) {
                List<Long> readBookIds = prestamoRepository.findBySocioIdSocio(idSocio).stream()
                        .map(p -> p.getEjemplar().getLibro().getIdLibro())
                        .collect(Collectors.toList());

                libros = libros.stream()
                        .filter(l -> readBookIds.contains(l.getIdLibro()))
                        .collect(Collectors.toList());
            } else {
                libros.clear();
            }
        }

        return libros;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener libro por ID", description = "Retorna un libro específico")
    public ResponseEntity<?> getLibroById(
            @Parameter(description = "ID del libro") @PathVariable(required = true) Long id) {
        if (id == null)
            return ResponseEntity.badRequest().body("ID requerido");
        return libroRepository.findById(id)
                .map(libro -> ResponseEntity.ok(libro))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/paginated")
    @Operation(summary = "Listar libros paginados", description = "Obtiene el catálogo de libros con paginación")
    public com.biblioteca.dto.PageResponse<Libro> getLibrosPaginated(
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filtrar por categoría") @RequestParam(required = false) String categoria,
            @Parameter(description = "Buscar por título o autor") @RequestParam(required = false) String search,
            @Parameter(description = "Excluir libros ya leídos") @RequestParam(required = false) Boolean excludeRead,
            @Parameter(description = "Username para filtros de lectura") @RequestParam(required = false) String username) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("titulo").ascending());

        // Logic to determine excluded IDs
        List<Long> excludedIds = new java.util.ArrayList<>();
        if (Boolean.TRUE.equals(excludeRead) && username != null) {
            Long idSocio = socioRepository.findByUsuario(username).map(s -> s.getIdSocio()).orElse(null);
            if (idSocio != null) {
                excludedIds = prestamoRepository.findBySocioIdSocio(idSocio).stream()
                        .map(p -> p.getEjemplar().getLibro().getIdLibro())
                        .collect(Collectors.toList());
            }
        }
        boolean hasExclusions = !excludedIds.isEmpty();

        org.springframework.data.domain.Page<Libro> pageResult;

        if (search != null && !search.isEmpty()) {
            if (hasExclusions) {
                pageResult = libroRepository.findBySearchAndIdLibroNotIn(search, excludedIds, pageable);
            } else {
                pageResult = libroRepository.findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(
                        search, search, pageable);
            }
        } else if (categoria != null && !categoria.isEmpty() && !categoria.equals("Todas")) {
            if (hasExclusions) {
                pageResult = libroRepository.findByCategoriaAndIdLibroNotIn(categoria, excludedIds, pageable);
            } else {
                pageResult = libroRepository.findByCategoria(categoria, pageable);
            }
        } else {
            if (hasExclusions) {
                pageResult = libroRepository.findByIdLibroNotIn(excludedIds, pageable);
            } else {
                pageResult = libroRepository.findAll(pageable);
            }
        }

        return com.biblioteca.dto.PageResponse.from(pageResult);
    }
}
