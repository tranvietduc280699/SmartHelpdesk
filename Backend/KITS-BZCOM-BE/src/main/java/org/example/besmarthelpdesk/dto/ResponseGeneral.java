package org.example.besmarthelpdesk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.constant.MessageConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseGeneral<T> {
    private int status;
    private String errorCode;
    private String message;
    private T data;

    public static <T> ResponseGeneral<T> success(T data) {
        return ResponseGeneral.<T>builder()
                .status(200)
                .message(MessageConstants.SUCCESS)
                .data(data)
                .build();
    }

    public static <T> ResponseGeneral<T> success(String message, T data) {
        return ResponseGeneral.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseGeneral<T> of(int status, String message, T data) {
        return ResponseGeneral.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseGeneral<T> error(int status, String errorCode, String message, T data) {
        return ResponseGeneral.<T>builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .data(data)
                .build();
    }
}
