package com.crimsonveil.repository;

import com.crimsonveil.dto.CelularContatoResposta;
import com.crimsonveil.entity.ContatoCelularRegistro;
import com.crimsonveil.entity.ContextoCampanhaCelular;
import com.crimsonveil.entity.MensagemCelularRegistro;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CelularRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public CelularRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public Optional<ContextoCampanhaCelular> buscarContexto(String campanhaId) {
        return jdbc.query("""
                SELECT c.campaign_json, c.version, w.world_date, w.world_time
                FROM campaigns c
                JOIN world_state w ON w.campaign_id = c.id
                WHERE c.id = ?
                """, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            Map<String, Object> campaign = lerJson(rs.getString("campaign_json"));
            Object protagonistValue = campaign.get("protagonista");
            if (!(protagonistValue instanceof Map<?, ?> protagonist)
                    || protagonist.get("id") == null
                    || protagonist.get("id").toString().isBlank()) {
                throw new IllegalStateException("A campanha não possui um identificador de protagonista persistido.");
            }
            return Optional.of(new ContextoCampanhaCelular(
                    campanhaId,
                    protagonist.get("id").toString(),
                    rs.getLong("version"),
                    rs.getString("world_date"),
                    rs.getString("world_time")
            ));
        }, campanhaId);
    }

    public int incrementarVersao(String campanhaId, long versaoEsperada, String agora) {
        return jdbc.update("""
                UPDATE campaigns SET version = version + 1, updated_at = ?
                WHERE id = ? AND version = ?
                """, agora, campanhaId, versaoEsperada);
    }

    public Optional<ContatoCelularRegistro> buscarContato(String campanhaId, String protagonistaId,
                                                           String contatoId) {
        return jdbc.query("""
                SELECT contact_id, campaign_id, protagonist_id, canonical_character_id, display_name,
                    category, creation_key, created_at
                FROM campaign_contacts
                WHERE campaign_id = ? AND protagonist_id = ? AND contact_id = ?
                """, rs -> rs.next()
                ? Optional.of(mapearContato(rs))
                : Optional.empty(), campanhaId, protagonistaId, contatoId);
    }

    public Optional<ContatoCelularRegistro> buscarContatoPorChave(String campanhaId, String protagonistaId,
                                                                  String chave) {
        return jdbc.query("""
                SELECT contact_id, campaign_id, protagonist_id, canonical_character_id, display_name,
                    category, creation_key, created_at
                FROM campaign_contacts
                WHERE campaign_id = ? AND protagonist_id = ? AND creation_key = ?
                """, rs -> rs.next()
                ? Optional.of(mapearContato(rs))
                : Optional.empty(), campanhaId, protagonistaId, chave);
    }

    public boolean existeContatoCanonico(String campanhaId, String protagonistaId, String personagemCanonicoId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM campaign_contacts
                WHERE campaign_id = ? AND protagonist_id = ? AND canonical_character_id = ?
                """, Integer.class, campanhaId, protagonistaId, personagemCanonicoId);
        return count != null && count > 0;
    }

    public List<CelularContatoResposta> listarContatos(String campanhaId, String protagonistaId) {
        return jdbc.query("""
                SELECT c.contact_id, c.display_name, c.category, c.canonical_character_id,
                    v.conversation_id,
                    (SELECT m.body FROM campaign_messages m
                     WHERE m.conversation_id = v.conversation_id
                     ORDER BY m.message_id DESC LIMIT 1) AS last_message,
                    (SELECT m.direction FROM campaign_messages m
                     WHERE m.conversation_id = v.conversation_id
                     ORDER BY m.message_id DESC LIMIT 1) AS last_direction,
                    (SELECT m.world_date FROM campaign_messages m
                     WHERE m.conversation_id = v.conversation_id
                     ORDER BY m.message_id DESC LIMIT 1) AS last_date,
                    (SELECT m.world_time FROM campaign_messages m
                     WHERE m.conversation_id = v.conversation_id
                     ORDER BY m.message_id DESC LIMIT 1) AS last_time,
                    (SELECT COUNT(*) FROM campaign_messages m
                     WHERE m.conversation_id = v.conversation_id
                       AND m.direction = 'ENTRADA' AND m.read_at IS NULL) AS unread_count
                FROM campaign_contacts c
                LEFT JOIN campaign_conversations v
                    ON v.campaign_id = c.campaign_id
                    AND v.protagonist_id = c.protagonist_id
                    AND v.contact_id = c.contact_id
                WHERE c.campaign_id = ? AND c.protagonist_id = ?
                ORDER BY c.display_name COLLATE NOCASE, c.contact_id
                """, (rs, row) -> new CelularContatoResposta(
                rs.getString("contact_id"),
                rs.getString("display_name"),
                rs.getString("category"),
                rs.getString("canonical_character_id"),
                rs.getString("conversation_id"),
                rs.getString("last_message"),
                rs.getString("last_direction"),
                rs.getString("last_date"),
                rs.getString("last_time"),
                rs.getLong("unread_count")
        ), campanhaId, protagonistaId);
    }

    public void criarContato(String contatoId, String campanhaId, String protagonistaId, String personagemCanonicoId,
                             String nome, String categoria, String chave, String agora) {
        jdbc.update("""
                INSERT INTO campaign_contacts (
                    contact_id, campaign_id, protagonist_id, canonical_character_id,
                    display_name, category, creation_key, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, contatoId, campanhaId, protagonistaId, personagemCanonicoId, nome, categoria, chave, agora);
    }

    public Optional<String> buscarConversaId(String campanhaId, String protagonistaId, String contatoId) {
        return jdbc.query("""
                SELECT conversation_id FROM campaign_conversations
                WHERE campaign_id = ? AND protagonist_id = ? AND contact_id = ?
                """, rs -> rs.next() ? Optional.of(rs.getString(1)) : Optional.empty(),
                campanhaId, protagonistaId, contatoId);
    }

    public String obterOuCriarConversa(String conversaNovaId, String campanhaId, String protagonistaId,
                                       String contatoId, String agora) {
        jdbc.update("""
                INSERT INTO campaign_conversations (
                    conversation_id, campaign_id, protagonist_id, contact_id, created_at
                ) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(campaign_id, protagonist_id, contact_id) DO NOTHING
                """, conversaNovaId, campanhaId, protagonistaId, contatoId, agora);
        return buscarConversaId(campanhaId, protagonistaId, contatoId)
                .orElseThrow(() -> new IllegalStateException("Não foi possível criar a conversa do contato."));
    }

    public Optional<MensagemCelularRegistro> buscarMensagemPorChave(String campanhaId, String protagonistaId,
                                                                    String chave) {
        return jdbc.query("""
                SELECT message_id, campaign_id, protagonist_id, conversation_id, contact_id,
                    direction, body, world_date, world_time, idempotency_key, read_at, created_at
                FROM campaign_messages
                WHERE campaign_id = ? AND protagonist_id = ? AND idempotency_key = ?
                """, rs -> rs.next()
                ? Optional.of(mapearMensagem(rs))
                : Optional.empty(), campanhaId, protagonistaId, chave);
    }

    public Optional<MensagemCelularRegistro> buscarMensagem(String campanhaId, String protagonistaId,
                                                             String conversaId, long mensagemId) {
        return jdbc.query("""
                SELECT message_id, campaign_id, protagonist_id, conversation_id, contact_id,
                    direction, body, world_date, world_time, idempotency_key, read_at, created_at
                FROM campaign_messages
                WHERE campaign_id = ? AND protagonist_id = ? AND conversation_id = ? AND message_id = ?
                """, rs -> rs.next()
                ? Optional.of(mapearMensagem(rs))
                : Optional.empty(), campanhaId, protagonistaId, conversaId, mensagemId);
    }

    public List<MensagemCelularRegistro> listarMensagens(String campanhaId, String protagonistaId,
                                                          String conversaId) {
        return jdbc.query("""
                SELECT message_id, campaign_id, protagonist_id, conversation_id, contact_id,
                    direction, body, world_date, world_time, idempotency_key, read_at, created_at
                FROM campaign_messages
                WHERE campaign_id = ? AND protagonist_id = ? AND conversation_id = ?
                ORDER BY message_id
                """, (rs, row) -> mapearMensagem(rs), campanhaId, protagonistaId, conversaId);
    }

    public void criarMensagem(String campanhaId, String protagonistaId, String conversaId, String contatoId,
                              String direcao, String conteudo, String data, String horario, String chave,
                              String agora) {
        jdbc.update("""
                INSERT INTO campaign_messages (
                    campaign_id, protagonist_id, conversation_id, contact_id, direction,
                    body, world_date, world_time, idempotency_key, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, campanhaId, protagonistaId, conversaId, contatoId, direcao,
                conteudo, data, horario, chave, agora);
    }

    public long marcarRecebidasComoLidas(String campanhaId, String protagonistaId, String conversaId,
                                         long ateMensagemId, String agora) {
        return jdbc.update("""
                UPDATE campaign_messages SET read_at = ?
                WHERE campaign_id = ? AND protagonist_id = ? AND conversation_id = ?
                    AND message_id <= ? AND direction = 'ENTRADA' AND read_at IS NULL
                """, agora, campanhaId, protagonistaId, conversaId, ateMensagemId);
    }

    public long contarRecebidasNaoLidas(String campanhaId, String protagonistaId, String conversaId,
                                        long ateMensagemId) {
        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM campaign_messages
                WHERE campaign_id = ? AND protagonist_id = ? AND conversation_id = ?
                    AND message_id <= ? AND direction = 'ENTRADA' AND read_at IS NULL
                """, Long.class, campanhaId, protagonistaId, conversaId, ateMensagemId);
        return total == null ? 0 : total;
    }

    private ContatoCelularRegistro mapearContato(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ContatoCelularRegistro(
                rs.getString("contact_id"),
                rs.getString("campaign_id"),
                rs.getString("protagonist_id"),
                rs.getString("canonical_character_id"),
                rs.getString("display_name"),
                rs.getString("category"),
                rs.getString("creation_key"),
                Instant.parse(rs.getString("created_at"))
        );
    }

    private MensagemCelularRegistro mapearMensagem(java.sql.ResultSet rs) throws java.sql.SQLException {
        String readAt = rs.getString("read_at");
        return new MensagemCelularRegistro(
                rs.getLong("message_id"),
                rs.getString("campaign_id"),
                rs.getString("protagonist_id"),
                rs.getString("conversation_id"),
                rs.getString("contact_id"),
                rs.getString("direction"),
                rs.getString("body"),
                rs.getString("world_date"),
                rs.getString("world_time"),
                rs.getString("idempotency_key"),
                readAt == null ? null : Instant.parse(readAt),
                Instant.parse(rs.getString("created_at"))
        );
    }

    private Map<String, Object> lerJson(String value) {
        try {
            return mapper.readValue(value, mapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Snapshot persistido da campanha inválido.", exception);
        }
    }
}
