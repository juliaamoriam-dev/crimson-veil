# Visão Arquitetural — Micro SaaS

> **Documentos de Referência:** [docs/ARQUITETURA.md](file:///d:/CrimsonVeil/docs/ARQUITETURA.md) e [docs/MODELO_DE_DOMINIO.md](file:///d:/CrimsonVeil/docs/MODELO_DE_DOMINIO.md).

---

## 1. Arquitetura em Camadas Desacopladas

```text
Frontend (HTML5 Semântico + Tailwind CSS + JavaScript Modular)
    ↓ (HTTP / JSON via /api/v1)
Controladores REST (Spring Boot Controllers)
    ↓ (DTOs validados)
Camada de Serviço (Regras de Domínio, Turnos, Motor Narrativo e Validador de Agência)
    ↓ (Entidades JPA)
Camada de Persistência (Spring Data JPA Repositories)
    ↓ (SQL / JDBC)
Banco de Dados Relacional (PostgreSQL)
```

---

## 2. Decisões Consolidadas e Propostas

* **Backend:** Proposta Java 21 LTS + Spring Boot 3 + Spring Data JPA + Maven.
* **Banco de Dados:** Proposta PostgreSQL com Flyway para versionamento de migrações.
* **Frontend:** Proposta HTML/CSS/Tailwind + JS nativo modular (sem frameworks pesados no MVP).
* **Motor Narrativo:** Pipeline de 10 etapas para execução de turnos com preservação do `WORLD_STATE` e validação estrita da agência da protagonista.
* **Provedor de IA e Autenticação:** `A DEFINIR`.
