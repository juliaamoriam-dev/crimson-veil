import { afterEach, test } from 'node:test';
import assert from 'node:assert/strict';
import {
    ApiError,
    carregarCampanha,
    carregarPerfilPersonagem,
    criarCampanha,
    listarCampanhas,
    migrarCampanhaCanonica,
    salvarAcao,
    reiniciarCampanha,
    removerImagemPersonagem,
    salvarImagemPersonagem,
    urlImagemPersonagem
} from '../frontend/js/core/api.js';

const fetchOriginal = globalThis.fetch;

afterEach(() => {
    globalThis.fetch = fetchOriginal;
});

test('API carrega campanha persistida usando identificador escapado', async () => {
    let url;
    globalThis.fetch = async (requestUrl) => {
        url = requestUrl;
        return Response.json({ campanha: { id: 'camp-001' }, versao: 1, repetida: false });
    };

    const result = await carregarCampanha('camp-001');
    assert.equal(result.campanha.id, 'camp-001');
    assert.equal(url, 'http://localhost:8080/api/v1/campanhas/camp-001');
});

test('API lista campanhas pelo endpoint real configurado', async () => {
    let url;
    globalThis.fetch = async (requestUrl) => {
        url = requestUrl;
        return Response.json([]);
    };

    assert.deepEqual(await listarCampanhas(), []);
    assert.equal(url, 'http://localhost:8080/api/v1/campanhas');
});

test('migração envia a campanha canônica sem transformar resposta duplicada em novo estado', async () => {
    let request;
    globalThis.fetch = async (url, options) => {
        request = { url, options };
        return Response.json({ campanha: { id: 'camp-001' }, versao: 7, repetida: true });
    };
    const canonical = { id: 'camp-001', titulo: 'Campanha Principal' };

    const result = await migrarCampanhaCanonica(canonical);
    assert.equal(request.url, 'http://localhost:8080/api/v1/campanhas/migracao-canonica');
    assert.deepEqual(JSON.parse(request.options.body).campanha, canonical);
    assert.equal(result.versao, 7);
    assert.equal(result.repetida, true);
});

test('cria campanhas via POST e registra chave idempotente nas ações', async () => {
    const requests = [];
    globalThis.fetch = async (url, options) => {
        requests.push({ url, options });
        return Response.json({ campanha: { id: 'camp-002' }, versao: 1, repetida: false });
    };
    const campaign = { id: 'camp-002', titulo: 'Nova história' };

    await criarCampanha(campaign);
    await salvarAcao('camp-002', {
        chaveOperacao: 'action-1',
        versaoEsperada: 1,
        acao: 'Examinar o local',
        campanha: campaign
    });

    assert.equal(requests[0].options.method, 'POST');
    assert.equal(requests[0].url, 'http://localhost:8080/api/v1/campanhas');
    assert.equal(requests[1].options.headers['Idempotency-Key'], 'action-1');
    assert.equal(requests[1].url, 'http://localhost:8080/api/v1/campanhas/camp-002/acoes');
});

test('reinicia campanha com versão e chave idempotente', async () => {
    let request;
    globalThis.fetch = async (url, options) => {
        request = { url, options };
        return Response.json({ campanha: { id: 'camp-001' }, versao: 4, repetida: false });
    };
    const operation = {
        chaveOperacao: 'reset-001',
        versaoEsperada: 3,
        campanhaInicial: { id: 'camp-001' }
    };

    const result = await reiniciarCampanha('camp-001', operation);
    assert.equal(request.url, 'http://localhost:8080/api/v1/campanhas/camp-001/reiniciar');
    assert.equal(request.options.headers['Idempotency-Key'], 'reset-001');
    assert.deepEqual(JSON.parse(request.options.body), operation);
    assert.equal(result.versao, 4);
});

test('API de imagem do perfil carrega, envia multipart e remove a imagem', async () => {
    const requests = [];
    globalThis.fetch = async (url, options = {}) => {
        requests.push({ url, options });
        return Response.json({ personagemId: 'char / milena', possuiImagem: true });
    };
    const imageUrl = urlImagemPersonagem('char / milena');
    await carregarPerfilPersonagem('char / milena');
    await salvarImagemPersonagem('char / milena', new Blob(['png'], { type: 'image/png' }));
    await removerImagemPersonagem('char / milena');

    assert.equal(imageUrl, 'http://localhost:8080/api/v1/personagens/char%20%2F%20milena/imagem');
    assert.equal(requests[0].url, 'http://localhost:8080/api/v1/personagens/char%20%2F%20milena/perfil');
    assert.equal(requests[1].options.method, 'PUT');
    assert.ok(requests[1].options.body instanceof FormData);
    assert.equal(requests[1].options.headers, undefined);
    assert.equal(requests[2].options.method, 'DELETE');
});

test('erro HTTP e erro de rede são reportados explicitamente', async () => {
    globalThis.fetch = async () => Response.json(
        { mensagem: 'Versão desatualizada.' },
        { status: 409 }
    );
    await assert.rejects(carregarCampanha('camp-001'), (error) => {
        assert.ok(error instanceof ApiError);
        assert.equal(error.status, 409);
        assert.equal(error.message, 'Versão desatualizada.');
        return true;
    });

    globalThis.fetch = async () => { throw new Error('servidor indisponível'); };
    await assert.rejects(carregarCampanha('camp-001'), /Não foi possível comunicar com o backend/);
});
