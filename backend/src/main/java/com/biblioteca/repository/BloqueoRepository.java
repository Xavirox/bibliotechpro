package com.biblioteca.repository;

import com.biblioteca.model.EstadoBloqueo;
import com.biblioteca.model.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {
        List<Bloqueo> findBySocioIdSocio(Long idSocio);

        List<Bloqueo> findByEstado(EstadoBloqueo estado);

        // Optimized queries to avoid N+1 problem
        @Query("SELECT b FROM Bloqueo b JOIN FETCH b.ejemplar e JOIN FETCH e.libro JOIN FETCH b.socio WHERE b.estado = :estado")
        List<Bloqueo> findByEstadoWithDetails(@Param("estado") EstadoBloqueo estado);

        @Query("SELECT b FROM Bloqueo b JOIN FETCH b.ejemplar e JOIN FETCH e.libro JOIN FETCH b.socio WHERE b.estado = :estado AND b.fechaFin > :now")
        List<Bloqueo> findActiveBloqueosWithDetails(@Param("estado") EstadoBloqueo estado,
                        @Param("now") java.util.Date now);

        @Query("SELECT COUNT(b) FROM Bloqueo b WHERE b.socio.idSocio = :idSocio AND b.estado = :estado AND b.fechaFin > :now")
        long countActiveBloqueosBySocio(@Param("idSocio") Long idSocio, @Param("estado") EstadoBloqueo estado,
                        @Param("now") java.util.Date now);

        @Query("SELECT b FROM Bloqueo b JOIN FETCH b.ejemplar e JOIN FETCH e.libro JOIN FETCH b.socio WHERE b.socio.idSocio = :idSocio AND b.estado = :estado AND b.fechaFin > :now")
        List<Bloqueo> findActiveBloqueosBySocioWithDetails(@Param("idSocio") Long idSocio,
                        @Param("estado") EstadoBloqueo estado,
                        @Param("now") java.util.Date now);

        // SEGURIDAD: Recuperación ante fallos del Job de Oracle.
        // Permite invocar la lógica de limpieza desde la aplicación si el Scheduler
        // falla.
        @org.springframework.data.jpa.repository.Modifying
        @Query(value = "BEGIN PROC_LIMPIEZA_BLOQUEOS; END;", nativeQuery = true)
        void ejecutarLimpiezaBloqueos();
}
