package com.crimsonveil.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ManipuladorGlobalExcecoes {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManipuladorGlobalExcecoes.class);

    @ExceptionHandler(CampanhaNaoEncontradaException.class)
    public ResponseEntity<ErroApiResposta> campanhaNaoEncontrada(CampanhaNaoEncontradaException exception) {
        return resposta(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({ConflitoVersaoException.class, DuplicateKeyException.class})
    public ResponseEntity<ErroApiResposta> conflito(RuntimeException exception) {
        return resposta(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({RegraCampanhaException.class, ConstraintViolationException.class})
    public ResponseEntity<ErroApiResposta> requisicaoInvalida(RuntimeException exception) {
        return resposta(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApiResposta> validacao(MethodArgumentNotValidException exception) {
        String mensagem = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Requisição inválida.");
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApiResposta> erroInterno(Exception exception) {
        LOGGER.error("Falha não tratada na API", exception);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao processar a operação. Nenhuma confirmação foi emitida.");
    }

    private ResponseEntity<ErroApiResposta> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status)
                .body(new ErroApiResposta(Instant.now(), status.value(), status.getReasonPhrase(), mensagem));
    }
}
