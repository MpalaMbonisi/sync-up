package com.github.mpalambonisi.syncup.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime timestamp;
    private List<String> message;

    public ErrorResponse(List<String> message){
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }
}
