package com.crimsonveil.repository;

import com.crimsonveil.dto.CampanhaResumoResposta;
import com.crimsonveil.dto.HistoricoCampanhaResposta;
import com.crimsonveil.entity.CampanhaRegistro;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CampanhaRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public CampanhaRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public List<CampanhaResumoResposta> listar() {
        return jdbc.query("""
                SELECT id, title, code, status, version, created_at, updated_at
                FROM campaigns ORDER BY created_at, id
                """, (rs, row) -> new CampanhaResumoResposta(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("code"),
                rs.getString("status"),
                rs.getLong("version"),
                Instant.parse(rs.getString("created_at")),
                Instant.parse(rs.getString("updated_at"))
        ));
    }

    public Optional<CampanhaRegistro> buscar(String id) {
        Optional<CampanhaRegistro> registro = jdbc.query("""
                SELECT id, title, code, status, campaign_json, version, created_at, updated_at
                FROM campaigns WHERE id = ?
                """, rs -> rs.next()
                ? Optional.of(new CampanhaRegistro(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("code"),
                        rs.getString("status"),
                        lerJson(rs.getString("campaign_json")),
                        rs.getLong("version"),
                        Instant.parse(rs.getString("created_at")),
                        Instant.parse(rs.getString("updated_at"))
                ))
                : Optional.empty(), id);
        return registro.map(this::incluirEstadoMundoPersistido);
    }

    public void criar(String id, String title, String code, String status, String campaignJson, String now) {
        jdbc.update("""
                INSERT INTO campaigns (id, title, code, status, campaign_json, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 1, ?, ?)
                """, id, title, code, status, campaignJson, now, now);
    }

    public int atualizar(String id, long expectedVersion, String title, String code, String status,
                         String campaignJson, String now) {
        return jdbc.update("""
                UPDATE campaigns SET title = ?, code = ?, status = ?, campaign_json = ?,
                    version = version + 1, updated_at = ?
                WHERE id = ? AND version = ?
                """, title, code, status, campaignJson, now, id, expectedVersion);
    }

    public void salvarEstadoMundo(String campaignId, Map<String, Object> world, String campaignJson) {
        jdbc.update("""
                INSERT INTO world_state (campaign_id, world_date, world_time, world_location,
                    action_count, state_json)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(campaign_id) DO UPDATE SET world_date = excluded.world_date,
                    world_time = excluded.world_time, world_location = excluded.world_location,
                    action_count = excluded.action_count, state_json = excluded.state_json
                """,
                campaignId,
                texto(world.get("dataAtual")),
                texto(world.get("horarioAtual")),
                texto(world.get("localAtual")),
                inteiro(world.get("contadorAcoes"), campaignJson),
                serializar(world));
    }

    public void salvarEventos(String campaignId, Map<String, Object> campaign) {
        Object mundoVivo = campaign.get("mundoVivo");
        if (!(mundoVivo instanceof Map<?, ?> liveWorld)) {
            return;
        }
        salvarEventosLista(campaignId, liveWorld.get("eventosAgendados"));
        salvarEventosLista(campaignId, liveWorld.get("eventosOcorridos"));
    }

    public Optional<String> buscarOperacao(String campaignId, String key) {
        return jdbc.query("""
                SELECT response_json FROM campaign_operations
                WHERE campaign_id = ? AND operation_key = ?
                """, rs -> rs.next() ? Optional.of(rs.getString(1)) : Optional.empty(), campaignId, key);
    }

    public void salvarOperacao(String campaignId, String key, String kind, String action, String responseJson,
                               String now) {
        jdbc.update("""
                INSERT INTO campaign_operations
                    (campaign_id, operation_key, operation_type, action_text, response_json, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, campaignId, key, kind, action, responseJson, now);
    }

    public void salvarHistorico(String campaignId, String key, String kind, String description,
                                String date, String time, long version, String now) {
        jdbc.update("""
                INSERT INTO campaign_history
                    (campaign_id, operation_key, entry_type, description, world_date, world_time,
                     campaign_version, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, campaignId, key, kind, description, date, time, version, now);
    }

    public long contarHistorico(String campaignId) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_history WHERE campaign_id = ?", Long.class, campaignId);
        return total == null ? 0 : total;
    }

    public long contarEventos(String campaignId) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_events WHERE campaign_id = ?", Long.class, campaignId);
        return total == null ? 0 : total;
    }

    public List<HistoricoCampanhaResposta> listarHistorico(String campaignId) {
        return jdbc.query("""
                SELECT operation_key, entry_type, description, world_date, world_time,
                    campaign_version, created_at
                FROM campaign_history WHERE campaign_id = ?
                ORDER BY created_at, operation_key
                """, (rs, row) -> new HistoricoCampanhaResposta(
                rs.getString("operation_key"),
                rs.getString("entry_type"),
                rs.getString("description"),
                rs.getString("world_date"),
                rs.getString("world_time"),
                rs.getLong("campaign_version"),
                Instant.parse(rs.getString("created_at"))
        ), campaignId);
    }

    private void salvarEventosLista(String campaignId, Object entries) {
        if (!(entries instanceof List<?> events)) {
            return;
        }
        for (Object entry : events) {
            if (!(entry instanceof Map<?, ?> event)) {
                continue;
            }
            String id = texto(event.get("id"));
            if (id.isBlank()) {
                continue;
            }
            jdbc.update("""
                    INSERT INTO campaign_events (campaign_id, event_id, event_status, event_json)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(campaign_id, event_id) DO UPDATE SET
                        event_status = excluded.event_status, event_json = excluded.event_json
                    """, campaignId, id, texto(event.get("status")), serializar(event));
        }
    }

    private Map<String, Object> lerJson(String value) {
        try {
            return mapper.readValue(value, mapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON persistido inválido.", exception);
        }
    }

    private CampanhaRegistro incluirEstadoMundoPersistido(CampanhaRegistro record) {
        List<Map<String, Object>> worldRows = jdbc.query("""
                SELECT world_date, world_time, world_location, action_count, state_json
                FROM world_state WHERE campaign_id = ?
                """, (rs, row) -> {
            Map<String, Object> world = lerJson(rs.getString("state_json"));
            world.put("dataAtual", rs.getString("world_date"));
            world.put("horarioAtual", rs.getString("world_time"));
            world.put("localAtual", rs.getString("world_location"));
            return Map.of("estadoMundo", world, "contadorAcoes", rs.getInt("action_count"));
        }, record.id());
        if (worldRows.isEmpty()) {
            throw new IllegalStateException("Estado do mundo ausente para a campanha " + record.id());
        }
        Map<String, Object> campaign = new LinkedHashMap<>(record.campanha());
        campaign.put("estadoMundo", worldRows.get(0).get("estadoMundo"));
        campaign.put("contadorAcoes", worldRows.get(0).get("contadorAcoes"));
        return new CampanhaRegistro(record.id(), record.titulo(), record.codigo(), record.status(),
                campaign, record.versao(), record.criadaEm(), record.atualizadaEm());
    }

    private String serializar(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar os dados da campanha.", exception);
        }
    }

    private long inteiro(Object value, String campaignJson) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            Map<String, Object> parsed = lerJson(campaignJson);
            Object count = parsed.get("contadorAcoes");
            return count instanceof Number number ? number.longValue() : 0;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Contador de ações inválido.", exception);
        }
    }

    private String texto(Object value) {
        return value == null ? "" : value.toString();
    }
}
