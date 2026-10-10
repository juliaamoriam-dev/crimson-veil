package com.crimsonveil.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

@RestControllerAdvice
public class ManipuladorGlobalExcecoes {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManipuladorGlobalExcecoes.class);

    @ExceptionHandler(CampanhaNaoEncontradaException.class)
    public ResponseEntity<ErroApiResposta> campanhaNaoEncontrada(CampanhaNaoEncontradaException exception) {
        return resposta(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroApiResposta> recursoNaoEncontrado(NoResourceFoundException exception) {
        return resposta(HttpStatus.NOT_FOUND, "Rota ou recurso não encontrado.");
    }

    @ExceptionHandler({ConflitoVersaoException.class, ConflitoOperacaoCelularException.class,
            DuplicateKeyException.class})
    public ResponseEntity<ErroApiResposta> conflito(RuntimeException exception) {
        return resposta(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({RegraCampanhaException.class, ConstraintViolationException.class})
    public ResponseEntity<ErroApiResposta> requisicaoInvalida(RuntimeException exception) {
        return resposta(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ImagemInvalidaException.class)
    public ResponseEntity<ErroApiResposta> imagemInvalida(ImagemInvalidaException exception) {
        return resposta(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({ImagemTamanhoException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ErroApiResposta> imagemGrande(RuntimeException exception) {
        String mensagem = exception instanceof ImagemTamanhoException
                ? exception.getMessage()
                : "O arquivo excede o limite permitido para envio.";
        return resposta(HttpStatus.PAYLOAD_TOO_LARGE, mensagem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApiResposta> validacao(MethodArgumentNotValidException exception) {
        String mensagem = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Requisição inválida.");
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroApiResposta> corpoInvalido(HttpMessageNotReadableException exception) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.");
    }

    @ExceptionHandler(ConfiguracaoIaPendenteException.class)
    public ResponseEntity<ErroApiResposta> configuracaoIaPendente(ConfiguracaoIaPendenteException exception) {
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(FalhaIntegracaoIaException.class)
    public ResponseEntity<ErroApiResposta> falhaIntegracaoIa(FalhaIntegracaoIaException exception) {
        return resposta(HttpStatus.BAD_GATEWAY, exception.getMessage());
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
