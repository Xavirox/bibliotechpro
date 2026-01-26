
package com.biblioteca.service;

import com.biblioteca.dto.LibroDTO;
import com.biblioteca.model.Libro;
import com.biblioteca.repository.EjemplarRepository;
import com.biblioteca.repository.LibroRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.SocioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibroService {

    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;
    private final SocioRepository socioRepository;
    private final EjemplarRepository ejemplarRepository;

    public LibroService(LibroRepository libroRepository,
            PrestamoRepository prestamoRepository,
            SocioRepository socioRepository,
            EjemplarRepository ejemplarRepository) {
        this.libroRepository = libroRepository;
        this.prestamoRepository = prestamoRepository;
        this.socioRepository = socioRepository;
        this.ejemplarRepository = ejemplarRepository;
    }

    @Transactional(readOnly = true)
    public List<LibroDTO> getAllLibros(String categoria, Boolean excludeRead, Boolean onlyRead, String username) {
        List<Libro> libros;

        if (categoria != null && !categoria.isEmpty() && !categoria.equals("Todas")) {
            libros = libroRepository.findByCategoria(categoria);
        } else {
            libros = libroRepository.findAll();
        }

        if (username != null && (Boolean.TRUE.equals(excludeRead) || Boolean.TRUE.equals(onlyRead))) {
            List<Long> readBookIds = getReadBookIds(username);

            if (Boolean.TRUE.equals(excludeRead)) {
                libros = libros.stream()
                        .filter(l -> !readBookIds.contains(l.getIdLibro()))
                        .collect(Collectors.toList());
            } else if (Boolean.TRUE.equals(onlyRead)) {
                libros = libros.stream()
                        .filter(l -> readBookIds.contains(l.getIdLibro()))
                        .collect(Collectors.toList());
            }
        }

        return libros.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LibroDTO> getLibrosPaginated(int page, int size, String categoria, String search, Boolean excludeRead,
            String username) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("titulo").ascending());

        List<Long> excludedIds = (Boolean.TRUE.equals(excludeRead) && username != null)
                ? getReadBookIds(username)
                : Collections.emptyList();

        boolean hasExclusions = !excludedIds.isEmpty();

        Page<Libro> libroPage;
        if (search != null && !search.isEmpty()) {
            libroPage = hasExclusions
                    ? libroRepository.findBySearchAndIdLibroNotIn(search, excludedIds, pageable)
                    : libroRepository.findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(search, search,
                            pageable);
        } else if (categoria != null && !categoria.isEmpty() && !categoria.equals("Todas")) {
            libroPage = hasExclusions
                    ? libroRepository.findByCategoriaAndIdLibroNotIn(categoria, excludedIds, pageable)
                    : libroRepository.findByCategoria(categoria, pageable);
        } else {
            libroPage = hasExclusions
                    ? libroRepository.findByIdLibroNotIn(excludedIds, pageable)
                    : libroRepository.findAll(pageable);
        }

        return libroPage.map(this::toDTO);
    }

    private LibroDTO toDTO(Libro libro) {
        long disponibles = ejemplarRepository.countByLibroIdLibroAndEstado(libro.getIdLibro(), "DISPONIBLE");
        return LibroDTO.fromEntity(libro, disponibles);
    }

    private List<Long> getReadBookIds(String username) {
        return socioRepository.findByUsuario(username)
                .map(s -> prestamoRepository.findIdsLibrosLeidosBySocio(s.getIdSocio()))
                .orElse(Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public java.util.Optional<LibroDTO> getLibroById(@org.springframework.lang.NonNull Long id) {
        java.util.Objects.requireNonNull(id, "El ID del libro no puede ser nulo");
        return libroRepository.findById(id).map(this::toDTO);
    }
}
