package dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ApiResponse<T>(boolean success, T data, LocalDateTime timestamp) {
}