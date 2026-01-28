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

    private final LibroService servicioLibro;

    public LibroController(LibroService servicioLibro) {
        this.servicioLibro = servicioLibro;
    }

    @GetMapping
    @Operation(summary = "Listar libros", description = "Obtiene el catálogo de libros con filtros opcionales")
    @Cacheable(value = "libros", key = "#categoria ?: 'all'", condition = "#excluirLeidos == null && #soloLeidos == null")
    public List<com.biblioteca.dto.LibroDTO> listarLibros(
            @Parameter(description = "Filtrar por categoría") @RequestParam(required = false) String categoria,
            @Parameter(description = "Excluir libros ya leídos") @RequestParam(required = false) Boolean excluirLeidos,
            @Parameter(description = "Mostrar solo libros leídos") @RequestParam(required = false) Boolean soloLeidos,
            @Parameter(description = "Username para filtros de lectura") @RequestParam(required = false) String usuario) {

        return servicioLibro.obtenerTodosLosLibros(categoria, excluirLeidos, soloLeidos, usuario);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener libro por ID", description = "Retorna un libro específico")
    public ResponseEntity<?> obtenerLibroPorId(
            @Parameter(description = "ID del libro") @PathVariable(required = true) Long id) {
        if (id == null)
            return ResponseEntity.badRequest().body("ID requerido");

        return servicioLibro.obtenerLibroPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/paginated")
    @Operation(summary = "Listar libros paginados", description = "Obtiene el catálogo de libros con paginación")
    public PageResponse<com.biblioteca.dto.LibroDTO> listarLibrosPaginados(
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int tamanio,
            @Parameter(description = "Filtrar por categoría") @RequestParam(required = false) String categoria,
            @Parameter(description = "Buscar por título o autor") @RequestParam(required = false) String busqueda,
            @Parameter(description = "Excluir libros ya leídos") @RequestParam(required = false) Boolean excluirLeidos,
            @Parameter(description = "Username para filtros de lectura") @RequestParam(required = false) String usuario) {

        return PageResponse.from(
                servicioLibro.obtenerLibrosPaginados(pagina, tamanio, categoria, busqueda, excluirLeidos, usuario));
    }
}
