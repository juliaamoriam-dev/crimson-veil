package com.crimsonveil.service;

import com.crimsonveil.dto.CelularContatoCriacaoRequest;
import com.crimsonveil.dto.CelularContatoResposta;
import com.crimsonveil.dto.CelularContatoResultado;
import com.crimsonveil.dto.CelularContatosResposta;
import com.crimsonveil.dto.CelularHistoricoResposta;
import com.crimsonveil.dto.CelularLeituraResposta;
import com.crimsonveil.dto.CelularLeituraRequest;
import com.crimsonveil.dto.CelularMensagemCriacaoRequest;
import com.crimsonveil.dto.CelularMensagemResposta;
import com.crimsonveil.dto.CelularMensagemResultado;
import com.crimsonveil.entity.ContatoCelularRegistro;
import com.crimsonveil.entity.ContextoCampanhaCelular;
import com.crimsonveil.entity.MensagemCelularRegistro;
import com.crimsonveil.exception.CampanhaNaoEncontradaException;
import com.crimsonveil.exception.ConflitoOperacaoCelularException;
import com.crimsonveil.exception.ConflitoVersaoException;
import com.crimsonveil.repository.CelularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CelularService {
    private final CelularRepository repository;

    public CelularService(CelularRepository repository) {
        this.repository = repository;
    }

    public CelularContatosResposta listarContatos(String campanhaId) {
        ContextoCampanhaCelular contexto = buscarContexto(campanhaId);
        return new CelularContatosResposta(
                campanhaId,
                contexto.protagonistaId(),
                contexto.versaoCampanha(),
                repository.listarContatos(campanhaId, contexto.protagonistaId())
        );
    }

    @Transactional
    public CelularContatoResultado criarContato(String campanhaId, CelularContatoCriacaoRequest request) {
        ContextoCampanhaCelular contexto = buscarContexto(campanhaId);
        String nome = request.nome().strip();
        String categoria = request.categoria().strip();
        String personagemCanonicoId = request.personagemCanonicoId() == null
                ? null
                : request.personagemCanonicoId().strip();
        if (personagemCanonicoId != null && personagemCanonicoId.isBlank()) {
            personagemCanonicoId = null;
        }

        var repetido = repository.buscarContatoPorChave(campanhaId, contexto.protagonistaId(),
                request.chaveOperacao());
        if (repetido.isPresent()) {
            ContatoCelularRegistro contato = repetido.get();
            if (!contato.nome().equals(nome) || !contato.categoria().equals(categoria)
                    || !equalsNullable(contato.personagemCanonicoId(), personagemCanonicoId)) {
                throw new ConflitoOperacaoCelularException("A chave de operação já foi usada para outro contato.");
            }
            return new CelularContatoResultado(mapearContato(contato), contexto.versaoCampanha(), true);
        }

        if (personagemCanonicoId != null
                && repository.existeContatoCanonico(campanhaId, contexto.protagonistaId(), personagemCanonicoId)) {
            throw new ConflitoOperacaoCelularException(
                    "Este personagem canônico já está associado aos contatos da protagonista.");
        }
        validarVersao(contexto, request.versaoEsperada(), campanhaId);
        String agora = Instant.now().toString();
        exigirAtualizacao(contexto, campanhaId, agora);
        ContatoCelularRegistro contato = new ContatoCelularRegistro(
                UUID.randomUUID().toString(),
                campanhaId,
                contexto.protagonistaId(),
                personagemCanonicoId,
                nome,
                categoria,
                request.chaveOperacao(),
                Instant.parse(agora)
        );
        repository.criarContato(contato.id(), campanhaId, contexto.protagonistaId(), personagemCanonicoId,
                nome, categoria, request.chaveOperacao(), agora);
        return new CelularContatoResultado(mapearContato(contato), contexto.versaoCampanha() + 1, false);
    }

    public CelularHistoricoResposta listarMensagens(String campanhaId, String contatoId) {
        ContextoCampanhaCelular contexto = buscarContexto(campanhaId);
        buscarContatoObrigatorio(campanhaId, contexto.protagonistaId(), contatoId);
        String conversaId = repository.buscarConversaId(campanhaId, contexto.protagonistaId(), contatoId)
                .orElse(null);
        List<CelularMensagemResposta> mensagens = conversaId == null
                ? List.of()
                : repository.listarMensagens(campanhaId, contexto.protagonistaId(), conversaId)
                        .stream().map(this::mapearMensagem).toList();
        return new CelularHistoricoResposta(campanhaId, contexto.protagonistaId(), contatoId, conversaId,
                contexto.versaoCampanha(), mensagens);
    }

    @Transactional
    public CelularMensagemResultado enviarMensagem(String campanhaId, String contatoId,
                                                    CelularMensagemCriacaoRequest request) {
        return gravarMensagem(campanhaId, contatoId, request, "SAIDA");
    }

    @Transactional
    public CelularMensagemResultado receberMensagemDeTeste(String campanhaId, String contatoId,
                                                            CelularMensagemCriacaoRequest request) {
        return gravarMensagem(campanhaId, contatoId, request, "ENTRADA");
    }

    @Transactional
    public CelularLeituraResposta marcarComoLidas(String campanhaId, String conversaId,
                                                  CelularLeituraRequest request) {
        ContextoCampanhaCelular contexto = buscarContexto(campanhaId);
        MensagemCelularRegistro limite = repository.buscarMensagem(campanhaId, contexto.protagonistaId(),
                conversaId, request.ateMensagemId()).orElseThrow(() ->
                new CampanhaNaoEncontradaException("Conversa ou mensagem não encontrada."));
        long pendentes = repository.contarRecebidasNaoLidas(campanhaId, contexto.protagonistaId(),
                conversaId, limite.id());
        if (pendentes == 0) {
            return new CelularLeituraResposta(conversaId, limite.id(), 0,
                    contexto.versaoCampanha(), true);
        }
        validarVersao(contexto, request.versaoEsperada(), campanhaId);
        exigirAtualizacao(contexto, campanhaId, Instant.now().toString());
        long atualizadas = repository.marcarRecebidasComoLidas(campanhaId, contexto.protagonistaId(),
                conversaId, limite.id(), Instant.now().toString());
        return new CelularLeituraResposta(conversaId, limite.id(), atualizadas,
                contexto.versaoCampanha() + 1, false);
    }

    private CelularMensagemResultado gravarMensagem(String campanhaId, String contatoId,
                                                     CelularMensagemCriacaoRequest request, String direcao) {
        ContextoCampanhaCelular contexto = buscarContexto(campanhaId);
        String conteudo = request.conteudo().strip();
        var repetida = repository.buscarMensagemPorChave(campanhaId, contexto.protagonistaId(),
                request.chaveOperacao());
        if (repetida.isPresent()) {
            MensagemCelularRegistro mensagem = repetida.get();
            if (!mensagem.contatoId().equals(contatoId) || !mensagem.direcao().equals(direcao)
                    || !mensagem.conteudo().equals(conteudo)) {
                throw new ConflitoOperacaoCelularException("A chave de operação já foi usada para outra mensagem.");
            }
            return new CelularMensagemResultado(mapearMensagem(mensagem), contexto.versaoCampanha(), true);
        }

        buscarContatoObrigatorio(campanhaId, contexto.protagonistaId(), contatoId);
        validarVersao(contexto, request.versaoEsperada(), campanhaId);
        String agora = Instant.now().toString();
        exigirAtualizacao(contexto, campanhaId, agora);
        String conversaId = repository.obterOuCriarConversa(UUID.randomUUID().toString(), campanhaId,
                contexto.protagonistaId(), contatoId, agora);
        repository.criarMensagem(campanhaId, contexto.protagonistaId(), conversaId, contatoId, direcao,
                conteudo, contexto.dataFiccional(), contexto.horarioFiccional(), request.chaveOperacao(), agora);
        MensagemCelularRegistro mensagem = repository.buscarMensagemPorChave(campanhaId,
                contexto.protagonistaId(), request.chaveOperacao()).orElseThrow(() ->
                new IllegalStateException("A mensagem não foi encontrada após a gravação."));
        return new CelularMensagemResultado(mapearMensagem(mensagem), contexto.versaoCampanha() + 1, false);
    }

    private void exigirAtualizacao(ContextoCampanhaCelular contexto, String campanhaId, String agora) {
        if (repository.incrementarVersao(campanhaId, contexto.versaoCampanha(), agora) != 1) {
            throw new ConflitoVersaoException(campanhaId);
        }
    }

    private void validarVersao(ContextoCampanhaCelular contexto, long esperada, String campanhaId) {
        if (contexto.versaoCampanha() != esperada) {
            throw new ConflitoVersaoException(campanhaId);
        }
    }

    private ContextoCampanhaCelular buscarContexto(String campanhaId) {
        return repository.buscarContexto(campanhaId)
                .orElseThrow(() -> new CampanhaNaoEncontradaException(campanhaId));
    }

    private ContatoCelularRegistro buscarContatoObrigatorio(String campanhaId, String protagonistaId,
                                                            String contatoId) {
        return repository.buscarContato(campanhaId, protagonistaId, contatoId)
                .orElseThrow(() -> new CampanhaNaoEncontradaException("Contato não encontrado."));
    }

    private CelularContatoResposta mapearContato(ContatoCelularRegistro contato) {
        return new CelularContatoResposta(contato.id(), contato.nome(), contato.categoria(),
                contato.personagemCanonicoId(), null, null, null, null, null, 0);
    }

    private CelularMensagemResposta mapearMensagem(MensagemCelularRegistro mensagem) {
        String estado = switch (mensagem.direcao()) {
            case "SAIDA" -> "PERSISTIDA";
            case "ENTRADA" -> mensagem.lidaEm() == null ? "NAO_LIDA" : "LIDA";
            default -> throw new IllegalStateException("Direção de mensagem persistida inválida.");
        };
        return new CelularMensagemResposta(mensagem.id(), mensagem.conversaId(), mensagem.contatoId(),
                mensagem.direcao(), mensagem.conteudo(), mensagem.dataFiccional(), mensagem.horarioFiccional(),
                mensagem.lidaEm(), mensagem.registradaEm(), estado);
    }

    private boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
