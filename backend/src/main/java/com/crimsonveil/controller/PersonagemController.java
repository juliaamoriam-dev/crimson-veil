package com.crimsonveil.controller;

import com.crimsonveil.dto.PerfilPersonagemResposta;
import com.crimsonveil.entity.ImagemPersonagemRegistro;
import com.crimsonveil.service.PersonagemService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/personagens/{personagemId}")
public class PersonagemController {
    private final PersonagemService service;

    public PersonagemController(PersonagemService service) {
        this.service = service;
    }

    @GetMapping("/perfil")
    public PerfilPersonagemResposta perfil(@PathVariable String personagemId) {
        return service.buscarPerfil(personagemId);
    }

    @GetMapping("/imagem")
    public ResponseEntity<byte[]> imagem(@PathVariable String personagemId) {
        ImagemPersonagemRegistro imagem = service.buscarImagem(personagemId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.mimeType()))
                .cacheControl(CacheControl.noStore())
                .body(imagem.dados());
    }

    @PutMapping(path = "/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PerfilPersonagemResposta salvarImagem(@PathVariable String personagemId,
                                                @RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        return service.salvarImagem(personagemId, arquivo.getContentType(), arquivo.getBytes());
    }

    @DeleteMapping("/imagem")
    public PerfilPersonagemResposta removerImagem(@PathVariable String personagemId) {
        return service.removerImagem(personagemId);
    }
}
