package dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorItem(
        String code,
        String detail,
        String pointer,
        String parameter,
        String header
) {
}