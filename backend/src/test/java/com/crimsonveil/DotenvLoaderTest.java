package com.crimsonveil;

import com.crimsonveil.configuration.DotenvLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotenvLoaderTest {

    @Test
    void processaLinhasComFormatosVariadosCorretamente() {
        List<String> linhas = List.of(
                "# Linha de comentário que deve ser ignorada",
                "",
                "   ",
                "GEMINI_API_KEY=AIzaSy_chave_teste_123",
                "GEMINI_MODEL=\"gemini-2.5-flash\"",
                "PORT='8080'",
                "CHAVE_COM_ESPACOS = valor_sem_aspas   ",
                "CHAVE_COM_COMENTARIO=valor_puro # comentario inline",
                "CHAVE_COM_ASPAS_E_COMENTARIO=\"valor entre aspas\" # comentario depois",
                "LINHA_INVALIDA_SEM_SEPARADOR"
        );

        Map<String, String> resultado = DotenvLoader.processarLinhas(linhas);

        assertEquals("AIzaSy_chave_teste_123", resultado.get("GEMINI_API_KEY"));
        assertEquals("gemini-2.5-flash", resultado.get("GEMINI_MODEL"));
        assertEquals("8080", resultado.get("PORT"));
        assertEquals("valor_sem_aspas", resultado.get("CHAVE_COM_ESPACOS"));
        assertEquals("valor_puro", resultado.get("CHAVE_COM_COMENTARIO"));
        assertEquals("valor entre aspas", resultado.get("CHAVE_COM_ASPAS_E_COMENTARIO"));
        assertFalse(resultado.containsKey("LINHA_INVALIDA_SEM_SEPARADOR"));
    }

    @Test
    void retornaMapaVazioQuandoLinhasForemNulasOuVazias() {
        assertTrue(DotenvLoader.processarLinhas(null).isEmpty());
        assertTrue(DotenvLoader.processarLinhas(List.of()).isEmpty());
        assertTrue(DotenvLoader.processarLinhas(List.of("# so comentarios", "   ")).isEmpty());
    }

    @Test
    void leArquivoCorretamente(@TempDir Path tempDir) throws IOException {
        Path arquivoEnv = tempDir.resolve(".env");
        Files.writeString(arquivoEnv, "GEMINI_API_KEY=teste-leitura-arquivo\nGEMINI_MODEL=gemini-teste");

        Map<String, String> variaveis = DotenvLoader.lerArquivo(arquivoEnv);

        assertEquals(2, variaveis.size());
        assertEquals("teste-leitura-arquivo", variaveis.get("GEMINI_API_KEY"));
        assertEquals("gemini-teste", variaveis.get("GEMINI_MODEL"));
    }

    @Test
    void lidaComArquivoInexistenteSemLancarExcecao() {
        Path inexistente = Path.of("caminho_inexistente_12345", ".env");
        Map<String, String> variaveis = DotenvLoader.lerArquivo(inexistente);
        assertNotNull(variaveis);
        assertTrue(variaveis.isEmpty());
    }

    @Test
    void registraPropriedadesNoSpringEnvironmentComPrecedenciaCorreta(@TempDir Path tempDir) throws IOException {
        Path arquivoEnv = tempDir.resolve(".env");
        Files.writeString(arquivoEnv, "VAR_TESTE_ENV=valor_do_env\n");

        System.setProperty("dotenv.path", arquivoEnv.toAbsolutePath().toString());
        try {
            StandardEnvironment environment = new StandardEnvironment();
            DotenvLoader.carregarNoEnvironment(environment);

            assertTrue(environment.getPropertySources().contains("dotenvProperties"));
            assertEquals("valor_do_env", environment.getProperty("VAR_TESTE_ENV"));
        } finally {
            System.clearProperty("dotenv.path");
        }
    }

    @Test
    void naoSobrescreveVariaveisExistentesNaJvm(@TempDir Path tempDir) throws IOException {
        String chavePreexistente = "CHAVE_TESTE_PREEXISTENTE_" + System.currentTimeMillis();
        System.setProperty(chavePreexistente, "valor_original_jvm");

        Path arquivoEnv = tempDir.resolve(".env");
        Files.writeString(arquivoEnv, chavePreexistente + "=valor_tentativa_env");

        System.setProperty("dotenv.path", arquivoEnv.toAbsolutePath().toString());
        try {
            DotenvLoader.carregarSeExistir();
            assertEquals("valor_original_jvm", System.getProperty(chavePreexistente));
        } finally {
            System.clearProperty("dotenv.path");
            System.clearProperty(chavePreexistente);
        }
    }
}
