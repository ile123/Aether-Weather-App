package dto;

import java.time.LocalDateTime;

record ApiResponse<T>(boolean success, T data, LocalDateTime timestamp) {}