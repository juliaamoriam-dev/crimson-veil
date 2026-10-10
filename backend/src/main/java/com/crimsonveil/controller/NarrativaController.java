package com.crimsonveil.controller;

import com.crimsonveil.dto.AcaoNarrativaRequest;
import com.crimsonveil.dto.AcaoNarrativaResposta;
import com.crimsonveil.service.NarrativaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campanhas/{campanhaId}/narrativa")
public class NarrativaController {
    private final NarrativaService service;

    public NarrativaController(NarrativaService service) {
        this.service = service;
    }

    @PostMapping("/acao")
    public ResponseEntity<AcaoNarrativaResposta> registrarAcao(
            @PathVariable String campanhaId,
            @Valid @RequestBody AcaoNarrativaRequest request
    ) {
        AcaoNarrativaResposta resposta = service.processarAcao(campanhaId, request);
        return ResponseEntity.status(resposta.repetida() ? HttpStatus.OK : HttpStatus.CREATED).body(resposta);
    }
}
