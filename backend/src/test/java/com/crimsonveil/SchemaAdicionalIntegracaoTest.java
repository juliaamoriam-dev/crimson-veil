package com.crimsonveil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaAdicionalIntegracaoTest {
    @TempDir
    Path diretorioTemporario;

    @Test
    void inicializarNovoEsquemaPreservaDadosDoEsquemaAnteriorEBancoDeBackup() throws Exception {
        Path arquivoBanco = diretorioTemporario.resolve("crimson-veil-legacy.sqlite");
        Path arquivoBackup = diretorioTemporario.resolve("crimson-veil-legacy-before-update.sqlite");
        DataSource bancoLegado = banco(arquivoBanco);
        executarScript(bancoLegado, "schema-v1-fixture.sql");
        JdbcTemplate jdbcLegado = new JdbcTemplate(bancoLegado);

        assertEquals(1, contar(jdbcLegado, "campaigns"));
        assertEquals(1, contar(jdbcLegado, "world_state"));
        assertEquals(1, contar(jdbcLegado, "campaign_events"));
        assertEquals(1, contar(jdbcLegado, "campaign_history"));
        assertEquals("03:35", jdbcLegado.queryForObject(
                "SELECT world_time FROM world_state WHERE campaign_id = 'camp-migration-fixture'", String.class));

        Files.copy(arquivoBanco, arquivoBackup);
        assertEquals(hashSha256(arquivoBanco), hashSha256(arquivoBackup));
        JdbcTemplate jdbcBackup = new JdbcTemplate(banco(arquivoBackup));
        assertEquals(1, contar(jdbcBackup, "campaigns"));
        assertEquals(1, contar(jdbcBackup, "world_state"));
        assertEquals(1, contar(jdbcBackup, "campaign_events"));
        assertEquals(1, contar(jdbcBackup, "campaign_history"));

        executarScript(bancoLegado, "schema.sql");

        JdbcTemplate atualizado = new JdbcTemplate(bancoLegado);
        assertEquals(1, contar(atualizado, "campaigns"));
        assertEquals(1, contar(atualizado, "world_state"));
        assertEquals(1, contar(atualizado, "campaign_events"));
        assertEquals(1, contar(atualizado, "campaign_operations"));
        assertEquals(1, contar(atualizado, "campaign_history"));
        assertEquals("Legacy campaign", atualizado.queryForObject(
                "SELECT title FROM campaigns WHERE id = 'camp-migration-fixture'", String.class));
        assertEquals(2, atualizado.queryForObject(
                "SELECT version FROM campaigns WHERE id = 'camp-migration-fixture'", Integer.class));
        assertEquals("03:35", atualizado.queryForObject(
                "SELECT world_time FROM world_state WHERE campaign_id = 'camp-migration-fixture'", String.class));
        assertEquals("Legacy Location", atualizado.queryForObject(
                "SELECT world_location FROM world_state WHERE campaign_id = 'camp-migration-fixture'", String.class));
        assertEquals("AGENDADO", atualizado.queryForObject(
                "SELECT event_status FROM campaign_events WHERE event_id = 'EV-LEGACY-001'", String.class));
        assertEquals("Legacy history entry", atualizado.queryForObject(
                "SELECT description FROM campaign_history WHERE operation_key = 'legacy-op-1'", String.class));
        assertTrue(tabelaExiste(atualizado, "campaign_contacts"));
        assertTrue(tabelaExiste(atualizado, "campaign_conversations"));
        assertTrue(tabelaExiste(atualizado, "campaign_messages"));
        assertEquals("Legacy campaign", jdbcBackup.queryForObject(
                "SELECT title FROM campaigns WHERE id = 'camp-migration-fixture'", String.class));
    }

    private DataSource banco(Path arquivo) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + arquivo);
        return dataSource;
    }

    private void executarScript(DataSource dataSource, String nome) {
        new ResourceDatabasePopulator(new ClassPathResource(nome)).execute(dataSource);
    }

    private int contar(JdbcTemplate jdbc, String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Integer.class);
    }

    private boolean tabelaExiste(JdbcTemplate jdbc, String tabela) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                Integer.class, tabela);
        return count != null && count == 1;
    }

    private String hashSha256(Path arquivo) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(arquivo)));
    }
}
