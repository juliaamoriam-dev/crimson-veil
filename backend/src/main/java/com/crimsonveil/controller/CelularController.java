package com.crimsonveil.controller;

import com.crimsonveil.dto.CelularContatoCriacaoRequest;
import com.crimsonveil.dto.CelularContatoResultado;
import com.crimsonveil.dto.CelularContatosResposta;
import com.crimsonveil.dto.CelularHistoricoResposta;
import com.crimsonveil.dto.CelularLeituraRequest;
import com.crimsonveil.dto.CelularLeituraResposta;
import com.crimsonveil.dto.CelularMensagemCriacaoRequest;
import com.crimsonveil.dto.CelularMensagemResultado;
import com.crimsonveil.service.CelularService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campanhas/{campanhaId}/celular")
public class CelularController {
    private final CelularService service;

    public CelularController(CelularService service) {
        this.service = service;
    }

    @GetMapping("/contatos")
    public CelularContatosResposta listarContatos(@PathVariable String campanhaId) {
        return service.listarContatos(campanhaId);
    }

    @PostMapping("/contatos")
    public ResponseEntity<CelularContatoResultado> criarContato(
            @PathVariable String campanhaId,
            @Valid @RequestBody CelularContatoCriacaoRequest request) {
        CelularContatoResultado resultado = service.criarContato(campanhaId, request);
        return ResponseEntity.status(resultado.repetida() ? HttpStatus.OK : HttpStatus.CREATED).body(resultado);
    }

    @GetMapping("/contatos/{contatoId}/mensagens")
    public CelularHistoricoResposta listarMensagens(@PathVariable String campanhaId,
                                                     @PathVariable String contatoId) {
        return service.listarMensagens(campanhaId, contatoId);
    }

    @PostMapping("/contatos/{contatoId}/mensagens")
    public ResponseEntity<CelularMensagemResultado> enviarMensagem(
            @PathVariable String campanhaId,
            @PathVariable String contatoId,
            @Valid @RequestBody CelularMensagemCriacaoRequest request) {
        CelularMensagemResultado resultado = service.enviarMensagem(campanhaId, contatoId, request);
        return ResponseEntity.status(resultado.repetida() ? HttpStatus.OK : HttpStatus.CREATED).body(resultado);
    }

    @PatchMapping("/conversas/{conversaId}/leitura")
    public CelularLeituraResposta marcarComoLidas(@PathVariable String campanhaId,
                                                   @PathVariable String conversaId,
                                                   @Valid @RequestBody CelularLeituraRequest request) {
        return service.marcarComoLidas(campanhaId, conversaId, request);
    }
}
