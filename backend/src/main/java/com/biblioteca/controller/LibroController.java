package com.biblioteca.controller;

import com.biblioteca.service.LibroService;
import com.biblioteca.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
@Tag(name = "Libros", description = "API para consultar el catálogo de libros")
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    @Operation(summary = "Listar libros", description = "Obtiene el catálogo de libros con filtros opcionales")
    @Cacheable(value = "libros", key = "#categoria ?: 'all'", condition = "#excludeRead == null && #onlyRead == null")
    public List<com.biblioteca.dto.LibroDTO> getAllLibros(
            @Parameter(description = "Filtrar por categoría") @RequestParam(required = false) String categoria,
            @Parameter(description = "Excluir libros ya leídos") @RequestParam(required = false) Boolean excludeRead,
            @Parameter(description = "Mostrar solo libros leídos") @RequestParam(required = false) Boolean onlyRead,
            @Parameter(description = "Username para filtros de lectura") @RequestParam(required = false) String username) {

        return libroService.getAllLibros(categoria, excludeRead, onlyRead, username);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener libro por ID", description = "Retorna un libro específico")
    public ResponseEntity<?> getLibroById(
            @Parameter(description = "ID del libro") @PathVariable(required = true) Long id) {
        if (id == null)
            return ResponseEntity.badRequest().body("ID requerido");

        return libroService.getLibroById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/paginated")
    @Operation(summary = "Listar libros paginados", description = "Obtiene el catálogo de libros con paginación")
    public PageResponse<com.biblioteca.dto.LibroDTO> getLibrosPaginated(
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filtrar por categoría") @RequestParam(required = false) String categoria,
            @Parameter(description = "Buscar por título o autor") @RequestParam(required = false) String search,
            @Parameter(description = "Excluir libros ya leídos") @RequestParam(required = false) Boolean excludeRead,
            @Parameter(description = "Username para filtros de lectura") @RequestParam(required = false) String username) {

        return PageResponse.from(libroService.getLibrosPaginated(page, size, categoria, search, excludeRead, username));
    }
}
