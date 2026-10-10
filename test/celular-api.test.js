import { afterEach, test } from 'node:test';
import assert from 'node:assert/strict';
import {
    criarContatoCelular,
    enviarMensagemCelular,
    listarContatosCelular,
    listarMensagensCelular,
    marcarMensagensCelularComoLidas
} from '../frontend/js/core/api.js';

const fetchOriginal = globalThis.fetch;

afterEach(() => {
    globalThis.fetch = fetchOriginal;
});

test('API do celular lista contatos e mensagens com identificadores escapados', async () => {
    const requests = [];
    globalThis.fetch = async (url, options = {}) => {
        requests.push({ url, options });
        return Response.json({ campanhaId: 'camp/002', contatos: [], mensagens: [] });
    };

    await listarContatosCelular('camp/002');
    await listarMensagensCelular('camp/002', 'contact/1');

    assert.equal(requests[0].url, 'http://localhost:8080/api/v1/campanhas/camp%2F002/celular/contatos');
    assert.equal(requests[1].url,
        'http://localhost:8080/api/v1/campanhas/camp%2F002/celular/contatos/contact%2F1/mensagens');
});

test('API do celular envia mensagens e contatos com chaves idempotentes', async () => {
    const requests = [];
    globalThis.fetch = async (url, options) => {
        requests.push({ url, options });
        return Response.json({ versaoCampanha: 2, repetida: false });
    };

    const contact = { chaveOperacao: 'contact-op-1', versaoEsperada: 1, nome: 'Adrian', categoria: 'PROFISSIONAL' };
    await criarContatoCelular('camp-001', contact);
    const message = { chaveOperacao: 'message-op-1', versaoEsperada: 2, conteudo: 'Chegou?' };
    await enviarMensagemCelular('camp-001', 'contact-1', message);
    await marcarMensagensCelularComoLidas('camp-001', 'conversation-1', {
        versaoEsperada: 3,
        ateMensagemId: 4
    });

    assert.equal(requests[0].options.headers['Idempotency-Key'], 'contact-op-1');
    assert.equal(requests[1].options.headers['Idempotency-Key'], 'message-op-1');
    assert.equal(requests[2].options.method, 'PATCH');
    assert.deepEqual(JSON.parse(requests[2].options.body), { versaoEsperada: 3, ateMensagemId: 4 });
});

test('falhas na API do celular são propagadas sem sucesso simulado', async () => {
    globalThis.fetch = async () => Response.json(
        { mensagem: 'Versão desatualizada.' },
        { status: 409 }
    );

    await assert.rejects(
        enviarMensagemCelular('camp-001', 'contact-1', {
            chaveOperacao: 'op-1',
            versaoEsperada: 1,
            conteudo: 'Teste'
        }),
        /Versão desatualizada/
    );
});
