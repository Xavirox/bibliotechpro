package com.biblioteca.dto;

import java.util.List;

/**
 * DTO genérico para respuestas paginadas.
 * Proporciona metadatos de paginación junto con el contenido.
 *
 * @param <T> Tipo del contenido
 */
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    // Constructors
    public PageResponse() {
    }

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.first = page == 0;
        this.last = page >= totalPages - 1;
    }

    // Static factory method from Spring Page
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> springPage) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(springPage.getContent());
        response.setPage(springPage.getNumber());
        response.setSize(springPage.getSize());
        response.setTotalElements(springPage.getTotalElements());
        response.setTotalPages(springPage.getTotalPages());
        response.setFirst(springPage.isFirst());
        response.setLast(springPage.isLast());
        return response;
    }

    // Getters and Setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
