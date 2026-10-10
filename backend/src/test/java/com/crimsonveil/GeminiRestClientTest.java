package com.crimsonveil;

import com.crimsonveil.exception.ConfiguracaoIaPendenteException;
import com.crimsonveil.exception.FalhaIntegracaoIaException;
import com.crimsonveil.ia.GeminiRestClient;
import com.crimsonveil.ia.ResultadoIa;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiRestClientTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void lancaExcecaoQuandoChaveEstiverAusente() {
        GeminiRestClient client = new GeminiRestClient(
                "",
                "gemini-2.5-flash",
                "https://generativelanguage.googleapis.com/v1beta",
                30,
                1000,
                0.7,
                mapper
        );

        ConfiguracaoIaPendenteException exception = assertThrows(
                ConfiguracaoIaPendenteException.class,
                () -> client.gerarNarracao("Instrucao", "Prompt")
        );

        assertTrue(exception.getMessage().contains("GEMINI_API_KEY"));
    }

    @Test
    void processaRespostaDoGeminiComSucesso() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        String respostaJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "Adrian verifica o relatório da perícia." }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP"
                    }
                  ],
                  "usageMetadata": {
                    "promptTokenCount": 85,
                    "candidatesTokenCount": 24
                  }
                }
                """;

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "chave-teste-123"))
                .andRespond(withSuccess(respostaJson, MediaType.APPLICATION_JSON));

        GeminiRestClient client = new GeminiRestClient(
                "chave-teste-123",
                "gemini-2.5-flash",
                "https://generativelanguage.googleapis.com/v1beta",
                1000,
                0.7,
                restClient,
                mapper
        );

        ResultadoIa resultado = client.gerarNarracao("Sistema", "Usuario");

        assertEquals("Adrian verifica o relatório da perícia.", resultado.texto());
        assertEquals("gemini-2.5-flash", resultado.modelo());
        assertEquals(85, resultado.tokensPrompt());
        assertEquals(24, resultado.tokensResposta());

        server.verify();
    }

    @Test
    void lancaFalhaIntegracaoQuandoGeminiRetornaErroHttp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        String erroJson = """
                {
                  "error": {
                    "code": 429,
                    "message": "Resource has been exhausted (e.g. check quota)."
                  }
                }
                """;

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body(erroJson).contentType(MediaType.APPLICATION_JSON));

        GeminiRestClient client = new GeminiRestClient(
                "chave-teste-123",
                "gemini-2.5-flash",
                "https://generativelanguage.googleapis.com/v1beta",
                1000,
                0.7,
                restClient,
                mapper
        );

        FalhaIntegracaoIaException exception = assertThrows(
                FalhaIntegracaoIaException.class,
                () -> client.gerarNarracao("Sistema", "Usuario")
        );

        assertTrue(exception.getMessage().contains("HTTP 429"));
        server.verify();
    }
}
