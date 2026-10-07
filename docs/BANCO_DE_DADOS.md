# Modelagem de Banco de Dados — Crimson Veil

> **Aviso:** O banco de dados **não** está implementado nesta fase. Este documento serve como base estrutural para a futura modelagem relacional ou não-relacional, a ser validada nas fases de Modelagem de Domínio e Arquitetura.

---

## 1. Tecnologia de Banco de Dados

* Sistema Gerenciador de Banco de Dados (SGBD): `A DEFINIR`
* Estratégia de Migrações (Migrations): `A DEFINIR`

---

## 2. Entidades Candidatas Iniciais (Sujeitas a Validação)

As entidades a seguir representam conceitos essenciais do domínio identificados preliminarmente para avaliação futura:

1. **Usuário:** Representa o operador ou investigador que acessa o sistema.
2. **Caso:** O dossiê ou inquérito central em andamento.
3. **Pessoa:** Indivíduos de interesse (suspeitos, vítimas, testemunhas, contatos).
4. **Local:** Endereços, cenas de crime, pontos de interesse geográfico.
5. **Evidência:** Elementos materiais, digitais ou periciais coletados.
6. **Pista:** Linhas de raciocínio, indícios ou informações parciais a serem confirmadas.
7. **Evento:** Ocorrências datadas e registradas na cronologia do caso.
8. **Documento:** Arquivos textuais, laudos, ofícios ou relatórios anexados.
9. **Organização:** Instituições, empresas ou grupos de interesse investigados.

---

## 3. Relacionamentos entre Entidades

Os relacionamentos definitivos entre as entidades dependem da validação das regras de negócio e da modelagem de domínio:

* Relacionamentos: `A DEFINIR`
* Cardinalidades (1:1, 1:N, N:N): `A DEFINIR`

---

## 4. Dicionário de Dados e Esquema Físico

* Atributos por entidade: `A DEFINIR`
* Tipos de dados e restrições (chaves primárias, estrangeiras, índices): `A DEFINIR`
* Scripts de criação (DDL): `A DEFINIR`
