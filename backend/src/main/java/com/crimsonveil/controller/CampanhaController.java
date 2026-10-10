package com.crimsonveil.controller;

import com.crimsonveil.dto.CampanhaCriacaoRequest;
import com.crimsonveil.dto.CampanhaResposta;
import com.crimsonveil.dto.CampanhaResumoResposta;
import com.crimsonveil.dto.HistoricoCampanhaResposta;
import com.crimsonveil.dto.MutacaoCampanhaRequest;
import com.crimsonveil.service.CampanhaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campanhas")
public class CampanhaController {
    private final CampanhaService service;

    public CampanhaController(CampanhaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CampanhaResumoResposta> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public CampanhaResposta carregar(@PathVariable String id) {
        return service.carregar(id);
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoCampanhaResposta> listarHistorico(@PathVariable String id) {
        return service.listarHistorico(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampanhaResposta criar(@Valid @RequestBody CampanhaCriacaoRequest request) {
        return service.criar(request);
    }

    @PostMapping("/migracao-canonica")
    public CampanhaResposta migrarCanonica(@Valid @RequestBody CampanhaCriacaoRequest request) {
        return service.migrarCanonica(request);
    }

    @PutMapping("/{id}/estado")
    public CampanhaResposta salvarEstado(@PathVariable String id,
                                         @Valid @RequestBody MutacaoCampanhaRequest request) {
        return service.salvarEstado(id, request);
    }

    @PostMapping("/{id}/acoes")
    public CampanhaResposta registrarAcao(@PathVariable String id,
                                          @Valid @RequestBody MutacaoCampanhaRequest request) {
        return service.registrarAcao(id, request);
    }
}
