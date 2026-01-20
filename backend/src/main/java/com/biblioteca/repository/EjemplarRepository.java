package com.biblioteca.repository;

import com.biblioteca.model.Ejemplar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EjemplarRepository extends JpaRepository<Ejemplar, Long> {
    List<Ejemplar> findByLibroIdLibro(Long idLibro);

    List<Ejemplar> findByEstado(String estado);

    // Query optimizada para cargar ejemplares con sus libros (evita N+1 y errores
    // de serialización)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Ejemplar e JOIN FETCH e.libro WHERE e.estado = :estado")
    List<Ejemplar> findByEstadoWithLibro(@org.springframework.data.repository.query.Param("estado") String estado);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE EJEMPLAR SET ESTADO = 'DISPONIBLE' WHERE ESTADO = 'PRESTADO' AND ID_EJEMPLAR NOT IN (SELECT ID_EJEMPLAR FROM PRESTAMO WHERE ESTADO = 'ACTIVO')", nativeQuery = true)
    void fixOrphanedPrestamos();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE EJEMPLAR SET ESTADO = 'DISPONIBLE' WHERE ESTADO = 'BLOQUEADO' AND ID_EJEMPLAR NOT IN (SELECT ID_EJEMPLAR FROM BLOQUEO WHERE ESTADO = 'ACTIVO')", nativeQuery = true)
    void fixOrphanedBloqueos();
}
