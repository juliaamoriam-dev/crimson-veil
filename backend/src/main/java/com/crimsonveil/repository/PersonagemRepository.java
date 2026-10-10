package com.crimsonveil.repository;

import com.crimsonveil.entity.ImagemPersonagemRegistro;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class PersonagemRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public PersonagemRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void assegurarPerfil(Map<String, Object> perfil) {
        String id = texto(perfil.get("id"));
        if (id.isBlank()) {
            throw new IllegalArgumentException("O perfil da personagem precisa de um identificador.");
        }
        jdbc.update("""
                INSERT OR IGNORE INTO character_profiles (character_id, profile_json, updated_at)
                VALUES (?, ?, ?)
                """, id, serializar(perfil), Instant.now().toString());
    }

    public Map<String, Object> aplicarPerfil(Map<String, Object> campanha) {
        Object valor = campanha.get("protagonista");
        if (!(valor instanceof Map<?, ?> personagem)) {
            return campanha;
        }
        String id = texto(personagem.get("id"));
        Optional<Map<String, Object>> perfil = buscarPerfil(id);
        if (perfil.isEmpty()) {
            return campanha;
        }
        Map<String, Object> copiaCampanha = new LinkedHashMap<>(campanha);
        Map<String, Object> copiaPersonagem = new LinkedHashMap<>();
        personagem.forEach((chave, conteudo) -> {
            if (chave instanceof String string) {
                copiaPersonagem.put(string, conteudo);
            }
        });
        copiaPersonagem.putAll(perfil.get());
        copiaCampanha.put("protagonista", copiaPersonagem);
        return copiaCampanha;
    }

    public Map<String, Object> buscarPerfilObrigatorio(String personagemId) {
        return buscarPerfil(personagemId).orElseThrow(() ->
                new IllegalArgumentException("O perfil da personagem não foi encontrado."));
    }

    public void salvarImagem(String personagemId, String mimeType, byte[] dados) {
        jdbc.update("""
                UPDATE character_profiles
                SET image_mime_type = ?, image_data = ?, updated_at = ?
                WHERE character_id = ?
                """, mimeType, dados, Instant.now().toString(), personagemId);
    }

    public void removerImagem(String personagemId) {
        jdbc.update("""
                UPDATE character_profiles
                SET image_mime_type = NULL, image_data = NULL, updated_at = ?
                WHERE character_id = ?
                """, Instant.now().toString(), personagemId);
    }

    public Optional<ImagemPersonagemRegistro> buscarImagem(String personagemId) {
        return jdbc.query("""
                SELECT image_mime_type, image_data FROM character_profiles
                WHERE character_id = ? AND image_data IS NOT NULL
                """, rs -> rs.next()
                ? Optional.of(new ImagemPersonagemRegistro(rs.getString("image_mime_type"),
                        rs.getBytes("image_data")))
                : Optional.empty(), personagemId);
    }

    public boolean possuiImagem(String personagemId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM character_profiles
                    WHERE character_id = ? AND image_data IS NOT NULL)
                """, Boolean.class, personagemId);
        return Boolean.TRUE.equals(existe);
    }

    private Optional<Map<String, Object>> buscarPerfil(String personagemId) {
        return jdbc.query("""
                SELECT profile_json FROM character_profiles WHERE character_id = ?
                """, rs -> rs.next() ? Optional.of(lerJson(rs.getString(1))) : Optional.empty(), personagemId);
    }

    private Map<String, Object> lerJson(String valor) {
        try {
            return mapper.readValue(valor, mapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON de perfil persistido inválido.", exception);
        }
    }

    private String serializar(Object valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar o perfil da personagem.", exception);
        }
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }
}
