package com.biblioteca.repository;

import com.biblioteca.model.Ejemplar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EjemplarRepository extends JpaRepository<Ejemplar, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Ejemplar e JOIN FETCH e.libro WHERE e.libro.idLibro = :idLibro")
    List<Ejemplar> findByLibroIdLibroWithLibro(
            @org.springframework.data.repository.query.Param("idLibro") Long idLibro);

    List<Ejemplar> findByEstado(String estado);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Ejemplar e JOIN FETCH e.libro WHERE e.estado = :estado")
    List<Ejemplar> findByEstadoWithLibro(@org.springframework.data.repository.query.Param("estado") String estado);

    long countByLibroIdLibroAndEstado(Long idLibro, String estado);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE EJEMPLAR SET ESTADO = 'DISPONIBLE' WHERE ESTADO = 'PRESTADO' AND ID_EJEMPLAR NOT IN (SELECT ID_EJEMPLAR FROM PRESTAMO WHERE ESTADO = 'ACTIVO')", nativeQuery = true)
    void fixOrphanedPrestamos();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE EJEMPLAR SET ESTADO = 'DISPONIBLE' WHERE ESTADO = 'BLOQUEADO' AND ID_EJEMPLAR NOT IN (SELECT ID_EJEMPLAR FROM BLOQUEO WHERE ESTADO = 'ACTIVO')", nativeQuery = true)
    void fixOrphanedBloqueos();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Ejemplar e WHERE e.idEjemplar = :id")
    java.util.Optional<Ejemplar> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);
}
