import {
    criarContatoCelular,
    enviarMensagemCelular,
    listarContatosCelular,
    listarMensagensCelular,
    marcarMensagensCelularComoLidas
} from './api.js';

function escaparHtml(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, caractere => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[caractere]);
}

function identificadorOperacao() {
    if (!globalThis.crypto?.randomUUID) {
        throw new Error('Este navegador não oferece geração segura de identificadores para operações.');
    }
    return `celular-${globalThis.crypto.randomUUID()}`;
}

export function renderizarCelular(elemento, campanhaId, obterVersao, atualizarVersao) {
    const estado = {
        tela: 'inicio',
        contatos: [],
        contatoAtivo: null,
        conversaId: null,
        mensagens: [],
        protagonistaId: null,
        versao: obterVersao(),
        carregando: false,
        erro: null,
        enviando: false,
        operacaoContatoPendente: null,
        operacaoMensagemPendente: null
    };

    const atualizarVersaoPersistida = versaoRecebida => {
        const versaoAtual = Number(obterVersao());
        const versao = Math.max(Number(versaoRecebida), Number.isFinite(versaoAtual) ? versaoAtual : 0);
        estado.versao = versao;
        atualizarVersao(campanhaId, versao);
    };

    const renderizar = () => {
        const cabecalho = `
            <header class="celular-cabecalho">
                <div class="celular-marca">
                    <span class="celular-marca-icone" aria-hidden="true">
                        <svg viewBox="0 0 24 24" fill="none">
                            <rect x="6.5" y="2.5" width="11" height="19" rx="2.2"></rect>
                            <path d="M10 5h4M10.5 18.5h3"></path>
                        </svg>
                    </span>
                    <span class="celular-marca-texto">
                        <strong>CRIMSON VEIL</strong>
                        <small>DISPOSITIVO PESSOAL</small>
                    </span>
                </div>
                <div class="celular-sessao">
                    <span class="celular-sessao-label">CAMPANHA</span>
                    <strong>${escaparHtml(campanhaId)}</strong>
                    <span class="celular-versao" title="Versão persistida">V${escaparHtml(estado.versao)}</span>
                </div>
            </header>`;

        if (estado.carregando) {
            elemento.innerHTML = `
                <section class="celular-shell celular-shell-carregando">
                    ${cabecalho}
                    <div class="celular-carregando" role="status">
                        <span class="celular-indicador-carregamento" aria-hidden="true"></span>
                        <span><strong>Sincronizando dispositivo</strong><small>Carregando dados persistidos da campanha.</small></span>
                    </div>
                </section>`;
            return;
        }

        const erro = estado.erro
            ? `<div class="celular-erro" role="alert">
                <span class="celular-erro-marca" aria-hidden="true">!</span>
                <span>${escaparHtml(estado.erro)}</span>
            </div>`
            : '';
        let conteudo;
        if (estado.tela === 'inicio') {
            conteudo = `
                <div class="celular-inicio">
                    <div class="celular-home-intro">
                        <p class="celular-sobrelinha">LINHA PRIVADA <span></span> BROOKHAVEN</p>
                        <h1>Seu mundo,<br><em>fora do caso.</em></h1>
                        <p class="celular-home-descricao">Um espaço pessoal para manter contato durante a investigação. O que aparece aqui pertence a esta campanha e à sua protagonista.</p>
                        ${estado.protagonistaId ? `<div class="celular-vinculo"><span>PROTAGONISTA VINCULADA</span><strong>${escaparHtml(estado.protagonistaId)}</strong></div>` : ''}
                    </div>
                    <div class="celular-home-painel">
                        <div class="celular-home-painel-topo">
                            <span class="celular-painel-indice">01 <i>/ 01</i></span>
                            <span class="celular-app-disponivel"><span></span> DISPONÍVEL</span>
                        </div>
                        <button class="celular-app" type="button" data-acao="contatos" aria-label="Abrir Mensagens e contatos">
                            <span class="celular-app-icone" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none">
                                    <path d="M20 11.5a7.5 7.5 0 0 1-7.5 7.5H6l-2 2v-6.5a7.5 7.5 0 1 1 16-3Z"></path>
                                    <path d="M8 11h8M8 14h5"></path>
                                </svg>
                            </span>
                            <span class="celular-app-conteudo">
                                <small>COMUNICAÇÃO</small>
                                <strong>Mensagens</strong>
                                <span>Contatos e conversas</span>
                            </span>
                            <svg class="celular-app-seta" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 12h14m-6-6 6 6-6 6"></path></svg>
                        </button>
                        <div class="celular-home-nota">
                            <span class="celular-home-nota-linha"></span>
                            <p>Sem alertas ou atividade simulada. As conversas aparecem quando você as inicia ou quando há mensagens persistidas.</p>
                        </div>
                        <button class="celular-link-atualizar" type="button" data-acao="carregar-contatos">
                            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M20 7v5h-5M4 17v-5h5"></path><path d="M5.5 9a7 7 0 0 1 11.8-2L20 12M4 12l2.7 5a7 7 0 0 0 11.8-2"></path></svg>
                            Sincronizar contatos
                        </button>
                    </div>
                </div>`;
        } else if (estado.tela === 'contatos') {
            const lista = estado.contatos.length
                ? `<div class="celular-lista-contatos">${estado.contatos.map(contato => `
                    <button class="celular-contato" type="button" data-acao="abrir-conversa" data-contato-id="${escaparHtml(contato.id)}">
                        <span class="celular-avatar" aria-hidden="true">${escaparHtml(contato.nome.trim().slice(0, 1).toUpperCase())}</span>
                        <span class="celular-contato-info">
                            <strong>${escaparHtml(contato.nome)}</strong>
                            <small>${escaparHtml(contato.ultimaMensagem || (contato.categoria === 'PESSOAL' ? 'Contato pessoal' : 'Contato profissional'))}</small>
                        </span>
                        <span class="celular-contato-meta">
                            <small>${escaparHtml(contato.ultimoHorarioFiccional || '')}</small>
                            ${contato.naoLidas ? `<b class="celular-nao-lidas" aria-label="${contato.naoLidas} mensagens não lidas">${contato.naoLidas}</b>` : '<span class="celular-contato-seta" aria-hidden="true">›</span>'}
                        </span>
                    </button>`).join('')}</div>`
                : `<div class="celular-vazio">
                    <span class="celular-vazio-icone" aria-hidden="true">
                        <svg viewBox="0 0 24 24" fill="none"><path d="M16 20v-1.5a4.5 4.5 0 0 0-4.5-4.5h-3A4.5 4.5 0 0 0 4 18.5V20"></path><circle cx="10" cy="7.5" r="3.5"></circle><path d="M17 8h5m-2.5-2.5v5"></path></svg>
                    </span>
                    <p class="celular-sobrelinha">SUA REDE COMEÇA AQUI</p>
                    <h3>Nenhum contato ainda</h3>
                    <p>Adicione alguém para abrir uma conversa nesta campanha.</p>
                </div>`;
            conteudo = `
                <div class="celular-pagina">
                    <div class="celular-titulo-linha">
                        <button class="celular-voltar" type="button" data-acao="inicio" aria-label="Voltar à tela inicial do celular">
                            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m15 18-6-6 6-6M9 12h11"></path></svg>
                            <span>Início</span>
                        </button>
                        <div class="celular-titulo-texto"><p class="celular-sobrelinha">LINHA PRIVADA</p><h2>Contatos</h2></div>
                        <button class="celular-secundario" type="button" data-acao="carregar-contatos">
                            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M20 7v5h-5M4 17v-5h5"></path><path d="M5.5 9a7 7 0 0 1 11.8-2L20 12M4 12l2.7 5a7 7 0 0 0 11.8-2"></path></svg>
                            Atualizar
                        </button>
                    </div>
                    <div class="celular-contatos-layout">
                        <section class="celular-contatos-secao" aria-label="Lista de contatos">
                            <div class="celular-secao-cabecalho">
                                <div><span class="celular-secao-label">PESSOAS</span><h3>Conversas</h3></div>
                                <span class="celular-contagem">${String(estado.contatos.length).padStart(2, '0')}</span>
                            </div>
                            ${lista}
                        </section>
                        <form class="celular-form-contato" id="celular-form-contato">
                            <div class="celular-form-cabecalho">
                                <span class="celular-form-icone" aria-hidden="true">
                                    <svg viewBox="0 0 24 24" fill="none"><path d="M16 20v-1.5a4.5 4.5 0 0 0-4.5-4.5h-3A4.5 4.5 0 0 0 4 18.5V20"></path><circle cx="10" cy="7.5" r="3.5"></circle><path d="M19 8v6m-3-3h6"></path></svg>
                                </span>
                                <div><span class="celular-secao-label">NOVA PESSOA</span><h3>Adicionar contato</h3></div>
                            </div>
                            <label for="celular-nome-contato">Nome
                                <input id="celular-nome-contato" name="nome" maxlength="120" required autocomplete="off"
                                    placeholder="Nome do contato"
                                    value="${escaparHtml(estado.operacaoContatoPendente?.nome || '')}"
                                    ${estado.operacaoContatoPendente ? 'disabled' : ''}>
                            </label>
                            <label for="celular-categoria-contato">Categoria
                                <select id="celular-categoria-contato" name="categoria" ${estado.operacaoContatoPendente ? 'disabled' : ''}>
                                    <option value="PROFISSIONAL">Profissional</option>
                                    <option value="PESSOAL" ${estado.operacaoContatoPendente?.categoria === 'PESSOAL' ? 'selected' : ''}>Pessoal</option>
                                </select>
                            </label>
                            <button class="celular-primario" type="submit">
                                <span>${estado.operacaoContatoPendente ? 'Tentar novamente' : 'Salvar contato'}</span>
                                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 12h14m-6-6 6 6-6 6"></path></svg>
                            </button>
                            <p class="celular-form-ajuda">O contato será salvo nesta campanha.</p>
                        </form>
                    </div>
                </div>`;
        } else {
            const mensagens = estado.mensagens.length
                ? `<div class="celular-mensagens" aria-live="polite">${estado.mensagens.map(mensagem => `
                    <article class="celular-balao ${mensagem.direcao === 'SAIDA' ? 'celular-saida' : 'celular-entrada'}">
                        <span class="celular-remetente">${mensagem.direcao === 'SAIDA' ? 'Você' : escaparHtml(estado.contatoAtivo?.nome || 'Contato')}</span>
                        <p>${escaparHtml(mensagem.conteudo)}</p>
                        <small><time>${escaparHtml(mensagem.dataFiccional)} · ${escaparHtml(mensagem.horarioFiccional)}</time><span>${mensagem.direcao === 'SAIDA' ? 'Enviada' : mensagem.estado === 'LIDA' ? 'Lida' : 'Recebida'}</span></small>
                    </article>`).join('')}</div>`
                : `<div class="celular-vazio celular-vazio-conversa">
                    <span class="celular-vazio-icone" aria-hidden="true">
                        <svg viewBox="0 0 24 24" fill="none"><path d="M20 11.5a7.5 7.5 0 0 1-7.5 7.5H6l-2 2v-6.5a7.5 7.5 0 1 1 16-3Z"></path><path d="M8 11h8"></path></svg>
                    </span>
                    <h3>Esta conversa está começando</h3>
                    <p>Envie uma mensagem para iniciar o histórico com ${escaparHtml(estado.contatoAtivo?.nome || 'este contato')}.</p>
                </div>`;
            conteudo = `
                <div class="celular-pagina celular-conversa">
                    <div class="celular-titulo-linha">
                        <button class="celular-voltar" type="button" data-acao="voltar-contatos" aria-label="Voltar aos contatos">
                            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m15 18-6-6 6-6M9 12h11"></path></svg>
                            <span>Contatos</span>
                        </button>
                        <div class="celular-identidade-contato">
                            <span class="celular-avatar celular-avatar-cabecalho" aria-hidden="true">${escaparHtml((estado.contatoAtivo?.nome || '?').trim().slice(0, 1).toUpperCase())}</span>
                            <span><p class="celular-sobrelinha">CONVERSA INDIVIDUAL</p><h2>${escaparHtml(estado.contatoAtivo?.nome || '')}</h2><small>${estado.contatoAtivo?.categoria === 'PESSOAL' ? 'Contato pessoal' : 'Contato profissional'}</small></span>
                        </div>
                        <button class="celular-secundario celular-atualizar-conversa" type="button" data-acao="recarregar-conversa" aria-label="Atualizar conversa">
                            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M20 7v5h-5M4 17v-5h5"></path><path d="M5.5 9a7 7 0 0 1 11.8-2L20 12M4 12l2.7 5a7 7 0 0 0 11.8-2"></path></svg>
                            <span>Atualizar</span>
                        </button>
                    </div>
                    <div class="celular-conversa-corpo">${mensagens}</div>
                    <form class="celular-form-mensagem" id="celular-form-mensagem">
                        <label class="visualmente-oculto" for="celular-conteudo">Mensagem</label>
                        <div class="celular-compositor">
                            <span class="celular-compositor-label">${estado.operacaoMensagemPendente ? 'MENSAGEM NÃO CONFIRMADA · TENTE NOVAMENTE' : 'NOVA MENSAGEM'}</span>
                            <textarea id="celular-conteudo" name="conteudo" maxlength="4000" rows="2" required placeholder="Escreva uma mensagem..."
                            ${estado.operacaoMensagemPendente ? 'disabled' : ''}>${escaparHtml(estado.operacaoMensagemPendente?.conteudo || '')}</textarea>
                        </div>
                        <button class="celular-primario celular-enviar" type="submit" ${estado.enviando ? 'disabled' : ''}>
                            <span>${estado.enviando ? 'Enviando...' : estado.operacaoMensagemPendente ? 'Tentar novamente' : 'Enviar'}</span>
                            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m21 3-7.5 18-3.8-7.7L2 9.5 21 3Z"></path><path d="M9.7 13.3 21 3"></path></svg>
                        </button>
                    </form>
                </div>`;
        }

        elemento.innerHTML = `
            <section class="celular-shell">
                ${cabecalho}
                ${erro}
                ${conteudo}
            </section>`;

        const contatosForm = elemento.querySelector('#celular-form-contato');
        if (contatosForm) contatosForm.addEventListener('submit', submeterContato);
        const mensagemForm = elemento.querySelector('#celular-form-mensagem');
        if (mensagemForm) mensagemForm.addEventListener('submit', submeterMensagem);
        elemento.querySelectorAll('[data-acao]').forEach(botao => {
            botao.addEventListener('click', () => executarAcao(botao.dataset.acao, botao.dataset.contatoId));
        });
    };

    const carregarContatos = async () => {
        estado.carregando = true;
        estado.erro = null;
        renderizar();
        try {
            const resposta = await listarContatosCelular(campanhaId);
            estado.contatos = resposta.contatos;
            estado.protagonistaId = resposta.protagonistaId;
            atualizarVersaoPersistida(resposta.versaoCampanha);
        } catch (erro) {
            estado.erro = `Não foi possível carregar os contatos. ${erro.message}`;
        } finally {
            estado.carregando = false;
            renderizar();
        }
    };

    const atualizarVersaoDaOperacaoPendente = async operacao => {
        if (operacao?.status === 409) {
            const estadoAtual = await listarContatosCelular(campanhaId);
            atualizarVersaoPersistida(estadoAtual.versaoCampanha);
            estado.contatos = estadoAtual.contatos;
            if (estado.operacaoContatoPendente) {
                estado.operacaoContatoPendente.versaoEsperada = estado.versao;
            }
            if (estado.operacaoMensagemPendente) {
                estado.operacaoMensagemPendente.versaoEsperada = estado.versao;
            }
        }
    };

    const marcarHistoricoComoLido = async historico => {
        const recebidasNaoLidas = historico.mensagens.filter(mensagem =>
            mensagem.direcao === 'ENTRADA' && mensagem.estado === 'NAO_LIDA');
        if (!historico.conversaId || recebidasNaoLidas.length === 0) return historico;
        const resposta = await marcarMensagensCelularComoLidas(campanhaId, historico.conversaId, {
            versaoEsperada: estado.versao,
            ateMensagemId: historico.mensagens[historico.mensagens.length - 1].id
        });
        atualizarVersaoPersistida(resposta.versaoCampanha);
        return listarMensagensCelular(campanhaId, estado.contatoAtivo.id);
    };

    const carregarConversa = async () => {
        estado.erro = null;
        try {
            let historico = await listarMensagensCelular(campanhaId, estado.contatoAtivo.id);
            atualizarVersaoPersistida(historico.versaoCampanha);
            estado.conversaId = historico.conversaId;
            estado.mensagens = historico.mensagens;
            renderizar();
            historico = await marcarHistoricoComoLido(historico);
            atualizarVersaoPersistida(historico.versaoCampanha);
            estado.conversaId = historico.conversaId;
            estado.mensagens = historico.mensagens;
            const contatos = await listarContatosCelular(campanhaId);
            estado.contatos = contatos.contatos;
            atualizarVersaoPersistida(contatos.versaoCampanha);
        } catch (erro) {
            estado.erro = `Não foi possível atualizar a conversa. ${erro.message}`;
        }
        renderizar();
    };

    async function submeterContato(evento) {
        evento.preventDefault();
        const form = evento.currentTarget;
        const dados = new FormData(form);
        const nome = estado.operacaoContatoPendente?.nome
            ?? dados.get('nome')?.toString().trim()
            ?? '';
        const categoria = estado.operacaoContatoPendente?.categoria
            ?? dados.get('categoria')?.toString()
            ?? 'PROFISSIONAL';
        if (!nome) return;
        if (!estado.operacaoContatoPendente) {
            estado.operacaoContatoPendente = {
                tipo: 'contato',
                chaveOperacao: identificadorOperacao(),
                versaoEsperada: estado.versao,
                nome,
                categoria
            };
        }
        estado.carregando = true;
        estado.erro = null;
        renderizar();
        try {
            const resultado = await criarContatoCelular(campanhaId, estado.operacaoContatoPendente);
            atualizarVersaoPersistida(resultado.versaoCampanha);
            estado.operacaoContatoPendente = null;
        } catch (erro) {
            if (erro.status === 409) {
                try {
                    await atualizarVersaoDaOperacaoPendente(erro);
                } catch (falhaAtualizacao) {
                    estado.erro = `O contato não foi confirmado e a versão atual não pôde ser carregada. ${falhaAtualizacao.message}`;
                }
            }
            estado.erro ||= `O contato não foi confirmado. Tente novamente; a mesma chave de operação será reutilizada. ${erro.message}`;
            estado.carregando = false;
            renderizar();
            return;
        }

        try {
            const atualizados = await listarContatosCelular(campanhaId);
            estado.contatos = atualizados.contatos;
            atualizarVersaoPersistida(atualizados.versaoCampanha);
        } catch (erro) {
            estado.erro = `O contato foi salvo, mas a lista não pôde ser atualizada. ${erro.message}`;
        }
        estado.carregando = false;
        renderizar();
    }

    async function submeterMensagem(evento) {
        evento.preventDefault();
        const form = evento.currentTarget;
        const conteudo = estado.operacaoMensagemPendente?.conteudo
            ?? new FormData(form).get('conteudo')?.toString().trim()
            ?? '';
        if (!conteudo) return;
        if (!estado.operacaoMensagemPendente) {
            estado.operacaoMensagemPendente = {
                tipo: 'mensagem',
                chaveOperacao: identificadorOperacao(),
                versaoEsperada: estado.versao,
                conteudo
            };
        }
        estado.erro = null;
        estado.enviando = true;
        renderizar();
        try {
            const resultado = await enviarMensagemCelular(
                campanhaId, estado.contatoAtivo.id, estado.operacaoMensagemPendente);
            atualizarVersaoPersistida(resultado.versaoCampanha);
            estado.operacaoMensagemPendente = null;
            estado.enviando = false;
            await carregarConversa();
        } catch (erro) {
            estado.enviando = false;
            if (erro.status === 409) {
                try {
                    await atualizarVersaoDaOperacaoPendente(erro);
                } catch (falhaAtualizacao) {
                    estado.erro = `A mensagem não foi confirmada e a versão atual não pôde ser carregada. ${falhaAtualizacao.message}`;
                    renderizar();
                    return;
                }
            }
            estado.erro = `A mensagem não foi confirmada. Reenvie para repetir a mesma operação. ${erro.message}`;
            renderizar();
        }
    }

    async function executarAcao(acao, contatoId) {
        estado.erro = null;
        if (acao === 'inicio') {
            estado.tela = 'inicio';
            renderizar();
        } else if (acao === 'contatos' || acao === 'carregar-contatos' || acao === 'voltar-contatos') {
            estado.tela = 'contatos';
            await carregarContatos();
        } else if (acao === 'abrir-conversa') {
            estado.contatoAtivo = estado.contatos.find(contato => contato.id === contatoId);
            if (!estado.contatoAtivo) {
                estado.erro = 'O contato não pertence à lista carregada para esta campanha.';
                renderizar();
                return;
            }
            estado.tela = 'conversa';
            estado.carregando = true;
            renderizar();
            estado.carregando = false;
            await carregarConversa();
        } else if (acao === 'recarregar-conversa') {
            await carregarConversa();
        }
    };

    renderizar();
}
