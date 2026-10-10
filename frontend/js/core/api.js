const API_BASE_URL = globalThis.CRIMSON_VEIL_API_URL || 'http://localhost:8080/api/v1';

export class ApiError extends Error {
    constructor(message, status = null) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
    }
}

async function requisicao(caminho, opcoes = {}) {
    let resposta;
    try {
        resposta = await fetch(`${API_BASE_URL}${caminho}`, {
            ...opcoes,
            headers: {
                'Content-Type': 'application/json',
                ...opcoes.headers
            }
        });
    } catch (erro) {
        throw new ApiError(`Não foi possível comunicar com o backend: ${erro.message}`);
    }

    let corpo;
    try {
        corpo = await resposta.json();
    } catch (erro) {
        throw new ApiError(`O backend retornou uma resposta inválida (HTTP ${resposta.status}).`, resposta.status);
    }

    if (!resposta.ok) {
        throw new ApiError(
            corpo.mensagem || `A operação falhou (HTTP ${resposta.status}).`,
            resposta.status
        );
    }
    return corpo;
}

export function listarCampanhas() {
    return requisicao('/campanhas');
}

export function carregarCampanha(id) {
    return requisicao(`/campanhas/${encodeURIComponent(id)}`);
}

export function migrarCampanhaCanonica(campanha) {
    return requisicao('/campanhas/migracao-canonica', {
        method: 'POST',
        body: JSON.stringify({
            id: campanha.id,
            titulo: campanha.titulo,
            campanha
        })
    });
}

export function criarCampanha(campanha) {
    return requisicao('/campanhas', {
        method: 'POST',
        body: JSON.stringify({
            id: campanha.id,
            titulo: campanha.titulo,
            campanha
        })
    });
}

export function salvarAcao(id, operacao) {
    return requisicao(`/campanhas/${encodeURIComponent(id)}/acoes`, {
        method: 'POST',
        headers: { 'Idempotency-Key': operacao.chaveOperacao },
        body: JSON.stringify(operacao)
    });
}

export function salvarEstadoCampanha(id, operacao) {
    return requisicao(`/campanhas/${encodeURIComponent(id)}/estado`, {
        method: 'PUT',
        headers: { 'Idempotency-Key': operacao.chaveOperacao },
        body: JSON.stringify(operacao)
    });
}
