package com.biblioteca.repository;

import com.biblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findBySocioIdSocio(Long idSocio);

    // Optimized query with JOIN FETCH to avoid N+1 problem
    @Query("SELECT p FROM Prestamo p JOIN FETCH p.ejemplar e JOIN FETCH e.libro JOIN FETCH p.socio WHERE p.socio.idSocio = :idSocio")
    List<Prestamo> findBySocioIdSocioWithDetails(@Param("idSocio") Long idSocio);

    @Query("SELECT p FROM Prestamo p JOIN FETCH p.ejemplar e JOIN FETCH e.libro JOIN FETCH p.socio WHERE p.estado = :estado")
    List<Prestamo> findByEstadoWithDetails(@Param("estado") String estado);

    List<Prestamo> findByEstado(String estado);

    // Query optimizada para cargar todos los préstamos con relaciones
    @Query("SELECT p FROM Prestamo p JOIN FETCH p.ejemplar e JOIN FETCH e.libro JOIN FETCH p.socio")
    List<Prestamo> findAllWithDetails();
}
