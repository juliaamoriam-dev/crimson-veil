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

export function executarAcaoNarrativa(id, operacao) {
    return requisicao(`/campanhas/${encodeURIComponent(id)}/narrativa/acao`, {
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

export function reiniciarCampanha(id, operacao) {
    return requisicao(`/campanhas/${encodeURIComponent(id)}/reiniciar`, {
        method: 'POST',
        headers: { 'Idempotency-Key': operacao.chaveOperacao },
        body: JSON.stringify(operacao)
    });
}

export function urlImagemPersonagem(personagemId) {
    return `${API_BASE_URL}/personagens/${encodeURIComponent(personagemId)}/imagem`;
}

export function carregarPerfilPersonagem(personagemId) {
    return requisicao(`/personagens/${encodeURIComponent(personagemId)}/perfil`);
}

export async function salvarImagemPersonagem(personagemId, arquivo) {
    let resposta;
    try {
        const corpo = new FormData();
        corpo.append('arquivo', arquivo);
        resposta = await fetch(urlImagemPersonagem(personagemId), {
            method: 'PUT',
            body: corpo
        });
    } catch (erro) {
        throw new ApiError(`Não foi possível enviar a imagem: ${erro.message}`);
    }
    return lerRespostaImagem(resposta);
}

export async function removerImagemPersonagem(personagemId) {
    let resposta;
    try {
        resposta = await fetch(urlImagemPersonagem(personagemId), { method: 'DELETE' });
    } catch (erro) {
        throw new ApiError(`Não foi possível remover a imagem: ${erro.message}`);
    }
    return lerRespostaImagem(resposta);
}

async function lerRespostaImagem(resposta) {
    let corpo;
    try {
        corpo = await resposta.json();
    } catch {
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

export function listarContatosCelular(campanhaId) {
    return requisicao(`/campanhas/${encodeURIComponent(campanhaId)}/celular/contatos`);
}

export function criarContatoCelular(campanhaId, contato) {
    return requisicao(`/campanhas/${encodeURIComponent(campanhaId)}/celular/contatos`, {
        method: 'POST',
        headers: { 'Idempotency-Key': contato.chaveOperacao },
        body: JSON.stringify(contato)
    });
}

export function listarMensagensCelular(campanhaId, contatoId) {
    return requisicao(
        `/campanhas/${encodeURIComponent(campanhaId)}/celular/contatos/${encodeURIComponent(contatoId)}/mensagens`
    );
}

export function enviarMensagemCelular(campanhaId, contatoId, mensagem) {
    return requisicao(
        `/campanhas/${encodeURIComponent(campanhaId)}/celular/contatos/${encodeURIComponent(contatoId)}/mensagens`,
        {
            method: 'POST',
            headers: { 'Idempotency-Key': mensagem.chaveOperacao },
            body: JSON.stringify(mensagem)
        }
    );
}

export function marcarMensagensCelularComoLidas(campanhaId, conversaId, leitura) {
    return requisicao(
        `/campanhas/${encodeURIComponent(campanhaId)}/celular/conversas/${encodeURIComponent(conversaId)}/leitura`,
        {
            method: 'PATCH',
            body: JSON.stringify(leitura)
        }
    );
}
