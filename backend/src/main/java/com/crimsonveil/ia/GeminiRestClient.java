package com.crimsonveil.ia;

import com.crimsonveil.exception.ConfiguracaoIaPendenteException;
import com.crimsonveil.exception.FalhaIntegracaoIaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiRestClient implements GeminiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiRestClient.class);

    public static final String MODELO_PADRAO = "gemini-3.8-flash";

    private final String apiKey;
    private final String modelo;
    private final String baseUrl;
    private final int maxOutputTokens;
    private final double temperature;
    private final RestClient restClient;
    private final ObjectMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiRestClient(
            @Value("${crimson-veil.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${crimson-veil.gemini.model:${GEMINI_MODEL:gemini-3.8-flash}}") String modelo,
            @Value("${crimson-veil.gemini.base-url:${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta}}") String baseUrl,
            @Value("${crimson-veil.gemini.timeout-seconds:${GEMINI_TIMEOUT_SECONDS:30}}") int timeoutSeconds,
            @Value("${crimson-veil.gemini.max-output-tokens:${GEMINI_MAX_OUTPUT_TOKENS:1000}}") int maxOutputTokens,
            @Value("${crimson-veil.gemini.temperature:${GEMINI_TEMPERATURE:0.7}}") double temperature,
            ObjectMapper mapper
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = (modelo == null || modelo.isBlank()) ? MODELO_PADRAO : modelo.trim();
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://generativelanguage.googleapis.com/v1beta" : baseUrl.trim();
        this.maxOutputTokens = maxOutputTokens > 0 ? maxOutputTokens : 1000;
        this.temperature = temperature;
        this.mapper = mapper;

        Duration timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(this.baseUrl)
                .build();
    }

    public GeminiRestClient(
            String apiKey,
            String modelo,
            String baseUrl,
            int maxOutputTokens,
            double temperature,
            RestClient restClient,
            ObjectMapper mapper
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = (modelo == null || modelo.isBlank()) ? MODELO_PADRAO : modelo.trim();
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://generativelanguage.googleapis.com/v1beta" : baseUrl.trim();
        this.maxOutputTokens = maxOutputTokens > 0 ? maxOutputTokens : 1000;
        this.temperature = temperature;
        this.restClient = restClient;
        this.mapper = mapper;
    }

    @Override
    public ResultadoIa gerarNarracao(String systemInstruction, String promptUsuario) {
        if (apiKey.isBlank()) {
            throw new ConfiguracaoIaPendenteException(
                    "A integração com a IA requer a variável de ambiente GEMINI_API_KEY configurada no servidor."
            );
        }

        Map<String, Object> requisicao = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", promptUsuario))
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxOutputTokens
                )
        );

        String path = "/models/" + modelo + ":generateContent";

        try {
            LOGGER.info("Invocando modelo Gemini '{}' para resolução narrativa", modelo);
            String responseJson = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", apiKey)
                    .body(requisicao)
                    .retrieve()
                    .body(String.class);

            return processarResposta(responseJson);
        } catch (RestClientResponseException exception) {
            String sanitizedError = sanitizarErro(exception.getResponseBodyAsString(), exception.getStatusCode().value());
            LOGGER.error("Erro da API Gemini (HTTP {}): {}", exception.getStatusCode().value(), sanitizedError);
            throw new FalhaIntegracaoIaException(
                    "Falha na comunicação com o Google Gemini (HTTP " + exception.getStatusCode().value() + "): " + sanitizedError,
                    exception
            );
        } catch (ResourceAccessException exception) {
            LOGGER.error("Tempo limite ou falha de rede ao conectar à API Gemini");
            throw new FalhaIntegracaoIaException(
                    "Tempo limite esgotado ou indisponibilidade de rede ao consultar o Google Gemini.",
                    exception
            );
        } catch (ConfiguracaoIaPendenteException | FalhaIntegracaoIaException exception) {
            throw exception;
        } catch (Exception exception) {
            LOGGER.error("Erro inesperado ao invocar o Gemini", exception);
            throw new FalhaIntegracaoIaException("Erro inesperado ao consultar a IA narradora.", exception);
        }
    }

    private ResultadoIa processarResposta(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            throw new FalhaIntegracaoIaException("A API do Gemini retornou uma resposta vazia.");
        }

        try {
            JsonNode root = mapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                JsonNode feedback = root.path("promptFeedback");
                if (!feedback.isMissingNode()) {
                    throw new FalhaIntegracaoIaException(
                            "A requisição foi rejeitada pelos filtros de segurança do modelo Gemini: " + feedback.toString()
                    );
                }
                throw new FalhaIntegracaoIaException("O modelo Gemini não retornou nenhum candidato narrativo válido.");
            }

            JsonNode primeiro = candidates.get(0);
            JsonNode parts = primeiro.path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new FalhaIntegracaoIaException("O candidato retornado pelo Gemini não contém partes de texto.");
            }

            StringBuilder sb = new StringBuilder();
            for (JsonNode parte : parts) {
                if (parte.has("text")) {
                    sb.append(parte.get("text").asText());
                }
            }

            String texto = sb.toString().trim();
            if (texto.isBlank()) {
                throw new FalhaIntegracaoIaException("O texto gerado pelo modelo Gemini está em branco.");
            }

            int promptTokens = root.path("usageMetadata").path("promptTokenCount").asInt(0);
            int candidateTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(0);

            return new ResultadoIa(texto, modelo, promptTokens, candidateTokens);
        } catch (FalhaIntegracaoIaException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FalhaIntegracaoIaException("Não foi possível interpretar a resposta JSON do Gemini.", exception);
        }
    }

    private String sanitizarErro(String corpoErro, int statusHttp) {
        if (corpoErro == null || corpoErro.isBlank()) {
            return "Status " + statusHttp;
        }
        try {
            JsonNode root = mapper.readTree(corpoErro);
            if (root.has("error") && root.get("error").has("message")) {
                String mensagem = root.get("error").get("message").asText();
                return sanitizarChaves(mensagem);
            }
        } catch (Exception ignored) {
        }
        return sanitizarChaves(corpoErro.length() > 200 ? corpoErro.substring(0, 200) + "..." : corpoErro);
    }

    private String sanitizarChaves(String texto) {
        if (apiKey.isBlank()) return texto;
        return texto.replace(apiKey, "[REDACTED_API_KEY]");
    }
}
