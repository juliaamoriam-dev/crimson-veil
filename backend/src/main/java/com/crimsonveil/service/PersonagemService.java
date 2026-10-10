package com.crimsonveil.service;

import com.crimsonveil.dto.PerfilPersonagemResposta;
import com.crimsonveil.entity.ImagemPersonagemRegistro;
import com.crimsonveil.exception.CampanhaNaoEncontradaException;
import com.crimsonveil.exception.ImagemInvalidaException;
import com.crimsonveil.exception.ImagemTamanhoException;
import com.crimsonveil.repository.PersonagemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;

@Service
public class PersonagemService {
    private final PersonagemRepository repository;
    private final long limiteBytes;

    public PersonagemService(PersonagemRepository repository,
                             @Value("${crimson-veil.profile.max-image-bytes:2097152}") long limiteBytes) {
        this.repository = repository;
        this.limiteBytes = limiteBytes;
    }

    public PerfilPersonagemResposta buscarPerfil(String personagemId) {
        Map<String, Object> perfil = buscarPerfilObrigatorio(personagemId);
        return new PerfilPersonagemResposta(personagemId, perfil, repository.possuiImagem(personagemId), limiteBytes);
    }

    @Transactional
    public PerfilPersonagemResposta salvarImagem(String personagemId, String mimeType, byte[] dados) {
        buscarPerfilObrigatorio(personagemId);
        if (dados == null || dados.length == 0) {
            throw new ImagemInvalidaException("Selecione um arquivo de imagem válido.");
        }
        if (dados.length > limiteBytes) {
            String limite = limiteBytes < 1024 * 1024
                    ? Math.max(1, (limiteBytes + 1023) / 1024) + " KB"
                    : String.format(java.util.Locale.ROOT, "%.1f MB", limiteBytes / (1024.0 * 1024.0));
            throw new ImagemTamanhoException("A imagem excede o limite de " + limite + ".");
        }
        String mimeValidado = validarFormato(mimeType, dados);
        repository.salvarImagem(personagemId, mimeValidado, dados);
        return new PerfilPersonagemResposta(personagemId,
                repository.buscarPerfilObrigatorio(personagemId), true, limiteBytes);
    }

    @Transactional
    public PerfilPersonagemResposta removerImagem(String personagemId) {
        Map<String, Object> perfil = buscarPerfilObrigatorio(personagemId);
        repository.removerImagem(personagemId);
        return new PerfilPersonagemResposta(personagemId, perfil, false, limiteBytes);
    }

    public ImagemPersonagemRegistro buscarImagem(String personagemId) {
        buscarPerfilObrigatorio(personagemId);
        return repository.buscarImagem(personagemId).orElseThrow(() ->
                new CampanhaNaoEncontradaException("A personagem ainda não possui imagem personalizada."));
    }

    public Map<String, Object> assegurarEAplicarPerfil(Map<String, Object> campanha) {
        Object valor = campanha.get("protagonista");
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> perfil = new java.util.LinkedHashMap<>();
            mapa.forEach((chave, campo) -> {
                if (chave instanceof String texto) {
                    perfil.put(texto, campo);
                }
            });
            repository.assegurarPerfil(perfil);
        }
        return repository.aplicarPerfil(campanha);
    }

    private Map<String, Object> buscarPerfilObrigatorio(String personagemId) {
        try {
            return repository.buscarPerfilObrigatorio(personagemId);
        } catch (IllegalArgumentException exception) {
            throw new CampanhaNaoEncontradaException("O perfil da personagem não foi encontrado.");
        }
    }

    private String validarFormato(String mimeType, byte[] dados) {
        String declarado = mimeType == null ? "" : mimeType.toLowerCase(java.util.Locale.ROOT).strip();
        boolean jpeg = declarado.equals("image/jpeg") && dados.length >= 4
                && (dados[0] & 0xff) == 0xff && (dados[1] & 0xff) == 0xd8
                && (dados[2] & 0xff) == 0xff
                && (dados[dados.length - 2] & 0xff) == 0xff && (dados[dados.length - 1] & 0xff) == 0xd9;
        boolean png = declarado.equals("image/png") && dados.length >= 33
                && (dados[0] & 0xff) == 0x89 && corresponde(dados, 1, "PNG")
                && dados[4] == 0x0d && dados[5] == 0x0a && dados[6] == 0x1a && dados[7] == 0x0a
                && inteiroBigEndian(dados, 8) == 13 && corresponde(dados, 12, "IHDR")
                && inteiroBigEndian(dados, 16) > 0 && inteiroBigEndian(dados, 20) > 0
                && corresponde(dados, dados.length - 8, "IEND")
                && inteiroBigEndian(dados, dados.length - 12) == 0;
        boolean webp = declarado.equals("image/webp") && validarWebp(dados);
        if (!jpeg && !png && !webp) {
            throw new ImagemInvalidaException("Formato inválido. Use uma imagem JPEG, PNG ou WebP válida.");
        }
        return declarado;
    }

    private boolean validarWebp(byte[] dados) {
        if (dados.length < 20 || !corresponde(dados, 0, "RIFF") || !corresponde(dados, 8, "WEBP")
                || inteiroLittleEndian(dados, 4) != dados.length - 8) {
            return false;
        }
        String chunk = new String(dados, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        long tamanhoChunk = inteiroLittleEndian(dados, 16);
        return List.of("VP8 ", "VP8L", "VP8X").contains(chunk)
                && tamanhoChunk > 0 && 20 + tamanhoChunk + (tamanhoChunk & 1) <= dados.length;
    }

    private long inteiroBigEndian(byte[] dados, int inicio) {
        return ((long) (dados[inicio] & 0xff) << 24)
                | ((long) (dados[inicio + 1] & 0xff) << 16)
                | ((long) (dados[inicio + 2] & 0xff) << 8)
                | (dados[inicio + 3] & 0xffL);
    }

    private long inteiroLittleEndian(byte[] dados, int inicio) {
        return (dados[inicio] & 0xffL) | ((dados[inicio + 1] & 0xffL) << 8)
                | ((dados[inicio + 2] & 0xffL) << 16) | ((dados[inicio + 3] & 0xffL) << 24);
    }

    private boolean corresponde(byte[] dados, int inicio, String valor) {
        for (int i = 0; i < valor.length(); i++) {
            if (dados[inicio + i] != (byte) valor.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
