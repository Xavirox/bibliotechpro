package com.biblioteca.repository;

import com.biblioteca.model.Socio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocioRepository extends JpaRepository<Socio, Long> {
    Optional<Socio> findByUsuario(String usuario);
}
