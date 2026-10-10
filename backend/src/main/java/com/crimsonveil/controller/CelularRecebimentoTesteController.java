package com.crimsonveil.controller;

import com.crimsonveil.dto.CelularMensagemCriacaoRequest;
import com.crimsonveil.dto.CelularMensagemResultado;
import com.crimsonveil.service.CelularService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("development")
@RestController
@RequestMapping("/api/v1/campanhas/{campanhaId}/celular/contatos/{contatoId}/mensagens-recebidas-teste")
public class CelularRecebimentoTesteController {
    private final CelularService service;

    public CelularRecebimentoTesteController(CelularService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CelularMensagemResultado> receberMensagem(
            @PathVariable String campanhaId,
            @PathVariable String contatoId,
            @Valid @RequestBody CelularMensagemCriacaoRequest request) {
        CelularMensagemResultado resultado =
                service.receberMensagemDeTeste(campanhaId, contatoId, request);
        return ResponseEntity.status(resultado.repetida() ? HttpStatus.OK : HttpStatus.CREATED).body(resultado);
    }
}
