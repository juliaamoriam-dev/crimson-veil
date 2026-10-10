package com.crimsonveil.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utilitário responsável por localizar e carregar configurações a partir de arquivos .env locais.
 * <p>
 * Prioridade das configurações:
 * 1. Variáveis de ambiente externas do SO (System.getenv)
 * 2. Propriedades de linha de comando ou JVM (System.getProperty)
 * 3. Variáveis locais do arquivo .env (backend/.env ou .env)
 * 4. Padrões definidos em application.properties
 */
public final class DotenvLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DotenvLoader.class);

    private static final List<Path> CAMINHOS_CANDIDATOS = List.of(
            Path.of("backend", ".env"),
            Path.of(".env"),
            Path.of("..", "backend", ".env"),
            Path.of("..", ".env")
    );

    private DotenvLoader() {
    }

    /**
     * Carrega variáveis do arquivo .env como System Properties caso não existam no ambiente do SO.
     * Prioridade: System.getenv() > System.getProperty() > .env.
     */
    public static Map<String, String> carregarSeExistir() {
        Optional<Path> arquivoOpt = localizarArquivoDotenv();
        if (arquivoOpt.isEmpty()) {
            LOGGER.debug("Nenhum arquivo .env local encontrado para carregar.");
            return Collections.emptyMap();
        }

        Path arquivo = arquivoOpt.get();
        Map<String, String> variaveis = lerArquivo(arquivo);

        int carregadas = 0;
        for (Map.Entry<String, String> entrada : variaveis.entrySet()) {
            String chave = entrada.getKey();
            String valor = entrada.getValue();

            // Respeita variáveis já existentes no sistema operacional ou JVM
            if (System.getenv(chave) == null && System.getProperty(chave) == null) {
                System.setProperty(chave, valor);
                carregadas++;
            }
        }

        LOGGER.info("Arquivo .env carregado de '{}' ({} variáveis aplicadas ao ambiente local)",
                arquivo.toAbsolutePath().normalize(), carregadas);
        return variaveis;
    }

    /**
     * Registra as propriedades do .env no ConfigurableEnvironment do Spring Boot.
     * Posicionado imediatamente após as variáveis do sistema operacional para respeitar a precedência externa.
     */
    public static void carregarNoEnvironment(ConfigurableEnvironment environment) {
        Optional<Path> arquivoOpt = localizarArquivoDotenv();
        if (arquivoOpt.isEmpty()) {
            return;
        }

        Path arquivo = arquivoOpt.get();
        Map<String, String> variaveis = lerArquivo(arquivo);
        if (variaveis.isEmpty()) {
            return;
        }

        Map<String, Object> propriedades = new HashMap<>(variaveis);
        MapPropertySource propertySource = new MapPropertySource("dotenvProperties", propriedades);

        MutablePropertySources sources = environment.getPropertySources();
        if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
        } else {
            sources.addLast(propertySource);
        }

        LOGGER.info("Configurações do .env ('{}') registradas no Spring Environment", arquivo.toAbsolutePath().normalize());
    }

    /**
     * Localiza o arquivo .env procurando nos locais padrão e no diretório de execução.
     */
    public static Optional<Path> localizarArquivoDotenv() {
        String caminhoCustomizado = System.getProperty("dotenv.path");
        if (caminhoCustomizado != null && !caminhoCustomizado.isBlank()) {
            Path custom = Path.of(caminhoCustomizado);
            if (Files.isRegularFile(custom)) {
                return Optional.of(custom);
            }
        }

        for (Path candidato : CAMINHOS_CANDIDATOS) {
            if (Files.isRegularFile(candidato)) {
                return Optional.of(candidato);
            }
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            Path backendNoUserDir = Path.of(userDir, "backend", ".env");
            if (Files.isRegularFile(backendNoUserDir)) {
                return Optional.of(backendNoUserDir);
            }
            Path raizNoUserDir = Path.of(userDir, ".env");
            if (Files.isRegularFile(raizNoUserDir)) {
                return Optional.of(raizNoUserDir);
            }
        }

        return Optional.empty();
    }

    public static Map<String, String> lerArquivo(Path arquivo) {
        try {
            List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
            return processarLinhas(linhas);
        } catch (IOException exception) {
            LOGGER.warn("Não foi possível ler o arquivo .env em '{}': {}", arquivo, exception.getMessage());
            return Collections.emptyMap();
        }
    }

    public static Map<String, String> processarLinhas(List<String> linhas) {
        Map<String, String> mapa = new LinkedHashMap<>();
        if (linhas == null) {
            return mapa;
        }

        for (String linhaOriginal : linhas) {
            String linha = linhaOriginal.trim();
            if (linha.isEmpty() || linha.startsWith("#")) {
                continue;
            }

            int separador = linha.indexOf('=');
            if (separador <= 0) {
                continue;
            }

            String chave = linha.substring(0, separador).trim();
            String valor = linha.substring(separador + 1).trim();

            if (valor.startsWith("\"")) {
                int fechamento = valor.indexOf('"', 1);
                if (fechamento != -1) {
                    valor = valor.substring(1, fechamento);
                } else if (valor.endsWith("\"") && valor.length() > 1) {
                    valor = valor.substring(1, valor.length() - 1);
                }
            } else if (valor.startsWith("'")) {
                int fechamento = valor.indexOf('\'', 1);
                if (fechamento != -1) {
                    valor = valor.substring(1, fechamento);
                } else if (valor.endsWith("'") && valor.length() > 1) {
                    valor = valor.substring(1, valor.length() - 1);
                }
            } else {
                int posComentario = valor.indexOf('#');
                if (posComentario != -1) {
                    valor = valor.substring(0, posComentario).trim();
                }
            }

            if (!chave.isBlank()) {
                mapa.put(chave, valor);
            }
        }
        return mapa;
    }
}
