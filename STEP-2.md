# STEP 2 — Monorepo + BFF com OpenFeign + Docker (premissa da próxima aula)

> ⚠️ **Fora do escopo da aula atual.** Este documento descreve, parcialmente, o que faremos na próxima aula: consolidar os 5 repositórios em um monorepo, transformar o composite em um BFF com OpenFeign e dockerizar o cenário completo com possibilidade de escala.

## 1. Monorepo (Gradle multi-project)

Os 5 repositórios do STEP-1 serão unidos:

```
workshop-microservices/
├── settings.gradle            # include 'api', 'util', microservices/*, spring-cloud/*
├── api/                       # lib: contratos (interfaces REST, DTOs, exceções)
├── util/                      # lib: utilitários compartilhados
├── microservices/
│   ├── product-service/
│   ├── recommendation-service/
│   ├── review-service/
│   └── product-composite-service/   # evolui para BFF
├── spring-cloud/
│   ├── eureka-server/
│   └── gateway/
└── docker-compose.yml
```

## 2. Libs compartilhadas

**`api`** — os contratos do STEP-1 (seções 1.1, 1.2 e 1.4) saem da duplicação entre repositórios e viram dependência única:
- Interfaces: `ProductService`, `RecommendationService`, `ReviewService`, `ProductCompositeService`
- DTOs: `Product`, `Recommendation`, `Review`, `ProductAggregate`, `RecommendationSummary`, `ReviewSummary`, `ServiceAddresses`
- Exceções: `NotFoundException`, `InvalidInputException`

**`util`** — código compartilhado (base: `dev.sdras.utils.http` do MicroservicesPlayground):
- `ServiceUtil`: resolve host/porta via `WebServerInitializedEvent` (standalone) ou `Registration` (com Eureka) — substitui o `@Value("${server.port}")` do livro com API moderna do Spring
- `HttpErrorInfo` + `GlobalControllerExceptionHandler` (`@ControllerAdvice` mapeando `NotFoundException`→404, `InvalidInputException`→422)

## 3. BFF + OpenFeign

O `product-composite-service` **sai do mock** do STEP-1 e evolui para um **BFF (Backend for Frontend)**: única porta de entrada de negócio para o frontend, orquestrando os núcleos.

A orquestração real é implementada com **Spring Cloud OpenFeign** (substituindo a classe mockada e o `RestTemplate` manual), aproveitando as interfaces da lib `api` como contrato do client:

```java
@FeignClient(name = "product")
public interface ProductClient extends ProductService { }
```

- `@EnableFeignClients` na aplicação do BFF
- Resolução via Eureka (`feign.client` + `spring-cloud-openfeign` no classpath)
- Regras do BFF permanecem as do STEP-1: cascata em create/delete, propagação de 404, propagação de 404/422 com base no `HttpErrorInfo` do núcleo

## 4. Docker + escalonamento

- Um `Dockerfile` por serviço (base JRE 17).
- `docker-compose.yml` com o cenário completo: eureka, gateway, BFF, product, recommendation, review, MongoDB e MySQL.
- Profile `docker` em cada `application.yml` ajustando hostnames (ex.: `app.eureka-server: eureka`, hosts dos bancos).
- **Escalonamento**: subir múltiplas instâncias de um núcleo e observar o balanceamento pelo Eureka + Gateway:

```bash
docker compose up -d --build
docker compose up -d --scale product=2
curl localhost:8080/product-composite/1 | jq '.serviceAddresses.pro'   # alterna entre instâncias
```

> Dica: com múltiplas instâncias locais use `server.port: 0`; o `ServiceUtil` (via `WebServerInitializedEvent`) já resolve a porta real. (Referência: seção 1.5 do STEP-1.)

## 5. Arquitetura alvo do STEP-2

```mermaid
flowchart TB
    C["Cliente"] --> GW["Gateway :8080<br/><i>lb://</i>"]

    subgraph MONO["Monorepo (Gradle multi-project)"]
        subgraph LIBS["libs compartilhadas"]
            API["api<br/>interfaces + DTOs + exceções"]
            UTIL["util<br/>ServiceUtil + error handler"]
        end
        subgraph SPRINGCLOUD["spring-cloud"]
            GW2["gateway"]
            EUR["eureka-server :8761"]
        end
        subgraph MICRO["microservices"]
            BFF["product-composite-service (BFF)<br/>@FeignClient"]
            P["product-service"]
            R["recommendation-service"]
            V["review-service"]
        end
        MICRO -.->|depende de| LIBS
    end

    GW --> BFF
    BFF -->|Feign lb://| P
    BFF -->|Feign lb://| R
    BFF -->|Feign lb://| V
    P --> M[("MongoDB")]
    R --> M
    V --> S[("MySQL")]
    GW -.-> EUR
```

```mermaid
flowchart LR
    subgraph COMPOSE["docker compose"]
        GW["gateway"]
        BFF["bff x1"]
        P1["product #1"]
        P2["product #2<br/>(--scale)"]
        R["recommendation"]
        V["review"]
        EUR["eureka"]
        M[("mongo")]
        S[("mysql")]
        GW --> BFF
        BFF --> P1
        BFF --> P2
        BFF --> R
        BFF --> V
    end
    GW -.-> EUR
    BFF -.-> EUR
    P1 -.-> EUR
    P2 -.-> EUR
    R -.-> EUR
    V -.-> EUR
```

## 6. O que vem depois (alinhamento com a disciplina)

A disciplina é focada em **Microservices com Message Broker** — os próximos passos naturais (capítulos do livro):

1. **Spring Cloud Stream + Kafka/RabbitMQ**: `createProduct`/`deleteProduct` passam a publicar **eventos**; os núcleos consomem de tópicos de forma assíncrona (comunicação event-driven, complementar ao GET síncrono).
2. **Resilience4j**: circuit breaker, retry e fallback no BFF.
3. **Observabilidade**: distributed tracing (Micrometer Tracing) e métricas.

## 7. Pré-requisitos para a próxima aula

- STEP-1 versionado nos 5 repositórios (tags por grupo facilitam o merge).
- Docker rodando na máquina (Docker Engine + Compose plugin).
- Contratos do STEP-1 estáveis — qualquer divergência de DTO/endpoint deve ser corrigida **antes** do merge no monorepo.
- Aceite do STEP-1 concluído por grupo (núcleos entregues com endpoints reais; o mock do composite é substituído na própria aula 2).
- Rota de debug removida/desativada após a validação (núcleos ficam internos, acessíveis só pelo BFF via Eureka).
