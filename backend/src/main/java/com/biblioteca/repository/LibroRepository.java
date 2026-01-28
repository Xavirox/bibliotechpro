package com.biblioteca.repository;

import com.biblioteca.model.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long> {
        List<Libro> findByTituloContainingIgnoreCase(String titulo);

        List<Libro> findByAutorContainingIgnoreCase(String autor);

        List<Libro> findByCategoria(String categoria);

        // Paginated methods
        Page<Libro> findByCategoria(String categoria, Pageable pageable);

        Page<Libro> findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(
                        String titulo, String autor, Pageable pageable);

        // Filtered by ID exclusion (for 'excludeRead' feature)
        Page<Libro> findByIdLibroNotIn(List<Long> ids, Pageable pageable);

        List<Libro> findByIdLibroNotIn(List<Long> ids);

        Page<Libro> findByCategoriaAndIdLibroNotIn(String categoria, List<Long> ids, Pageable pageable);

        List<Libro> findByCategoriaAndIdLibroNotIn(String categoria, List<Long> ids);

        @org.springframework.data.jpa.repository.Query("SELECT l FROM Libro l WHERE (LOWER(l.titulo) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :search, '%'))) AND l.idLibro NOT IN :ids")
        Page<Libro> findBySearchAndIdLibroNotIn(
                        @org.springframework.data.repository.query.Param("search") String search,
                        @org.springframework.data.repository.query.Param("ids") List<Long> ids, Pageable pageable);

        // SEGURIDAD: Prevención de DoS por carga masiva en memoria.
        // Usar muestreo nativo de Oracle en lugar de cargar todo y mezclar en Java.
        @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM (SELECT * FROM LIBRO ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= :limit", nativeQuery = true)
        List<Libro> findRandomBooks(@org.springframework.data.repository.query.Param("limit") int limit);
}
