package dto;

public record PagedResponse<T>(int page, int size, int totalElements, int totalPages, T data) {
}