# workshop-microservices

Workshop de microserviços da FATEC Araras: Spring Boot, OpenFeign/Eureka, MongoDB, MySQL e RabbitMQ com Spring AMQP.

## Executar o ambiente completo

Requer JDK 17, Docker e Compose. O Wrapper fornece Gradle 8.14.3.

```bash
cp .env.example .env
# Configure CONFIG_GIT_URI/LABEL para um GitHub que já contenha config-repo/ (STEP-6).
./gradlew clean build
docker compose up --build -d --wait
python3 scripts/smoke-config.py
python3 scripts/smoke-security.py
```

Gateway: http://localhost:8080 · Eureka: http://localhost:8761 · RabbitMQ Management: http://localhost:15672 · Swagger: http://localhost:7100/swagger-ui.html

Credenciais locais do RabbitMQ: `workshop` / `workshop`; configure-as no `.env`.

## Imagens Docker

Cada aplicação tem seu próprio `Dockerfile` e `.dockerignore` no diretório do módulo. O Compose usa esse diretório como contexto, como em `build: microservices/product-service`, e nomeia a imagem como `workshop/product-service`.

Execute `./gradlew build` na raiz antes de construir as imagens. Os Dockerfiles copiam o JAR executável de `build/libs` de cada módulo; as bibliotecas `api` e `util` já estão incluídas no JAR pelo Spring Boot. Para reconstruir somente uma aplicação:

```bash
./gradlew :microservices:product-service:bootJar
docker compose build product-service
docker compose up -d --no-deps product-service
```

Config Server, Eureka e Gateway seguem a mesma organização em `spring-cloud/eureka-server` e `spring-cloud/gateway`.

## Módulos

- `api`: DTOs, contratos REST, envelope `Event<T>` e exceções.
- `util`: erros HTTP, identificação de instância, OpenAPI e mensageria RabbitMQ no pacote `br.com.fatecararas.util.messaging`.
- `microservices`: product-composite, product, review e recommendation.
- `spring-cloud`: Config Server com backend Git, Eureka Server e Gateway reativo.
- `config-repo`: configurações comuns, por aplicação e por profile, para publicação no GitHub.

A configuração `RabbitConfiguration` de `util` é importada explicitamente pelo composite e pelos núcleos. Eureka e Gateway não dependem de `util`. O contrato `Event<T>` permanece em `api`.

GET do composite consulta os núcleos por HTTP. POST e DELETE enviam comandos ao RabbitMQ e retornam 202 após confirmação do broker; os bancos são atualizados de forma assíncrona. Consulte o roteiro para entender consistência eventual, publicação parcial e DLQ.

## Etapas desta aula

Branches locais em sequência: `rabbitmq-base` → `spring-security-keycloak` → `central-configuration`.
A branch de segurança adiciona JWT no Gateway e no composite. A branch atual também centraliza as configurações no Config Server (porta 8888), com backend GitHub e profiles local/docker; publique os arquivos antes de iniciar, conforme STEP-6. Obtenha um token com `python3 scripts/keycloak-token.py writer`; os exemplos antigos das APIs agora exigem Bearer. Keycloak: http://localhost:8180.

## Roteiros

- [STEP-1 — Contratos e equipes](docs/STEP-1.md)
- [STEP-2 — Bibliotecas compartilhadas](docs/STEP-2-LIBS.md)
- [STEP-2 — Integração dos serviços](docs/STEP-2.md)
- [STEP-3 — Composite HTTP: etapa anterior](docs/STEP-3-COMPOSITE.md)
- [STEP-6 — Config Server, GitHub e profiles](docs/STEP-6-CENTRAL-CONFIGURATION.md)
- [STEP-5 — Spring Security e Keycloak](docs/STEP-5-SECURITY-KEYCLOAK.md)
- [STEP-4 — RabbitMQ, infraestrutura, testes e falhas](docs/STEP-4-RABBITMQ.md)
- [Aula prática — Feign + RabbitMQ e testes no Insomnia](docs/AULA-FEIGN-RABBITMQ.md)
- [Collection Insomnia — 35 requisições na ordem da aula](docs/insomnia/workshop-feign-rabbitmq.json)

Os STEPs anteriores documentam a evolução do workshop. O STEP-4 descreve as escritas assíncronas; o STEP-5 adiciona a segurança HTTP.

## Desenvolvimento e verificação

```bash
./gradlew projects
./gradlew clean build
```

Todos os módulos usam a toolchain Java 17. Spring Boot 3.0.4, Spring Cloud 2022.0.2 e springdoc 2.0.2 permanecem alinhados à base do workshop. As dependências AMQP são gerenciadas pelo BOM do Spring Boot.

Os testes com Testcontainers usam Docker; sem daemon disponível, são ignorados. Os demais testes continuam executáveis. O smoke usa polling e requer o ambiente completo iniciado.

Para execução fora do Docker, configure os bancos, RabbitMQ e Eureka pelas variáveis descritas no STEP-4 e use `./gradlew :microservices:<serviço>:bootRun`. A porta padrão local do composite é 7100 e aceita `SERVER_PORT`.

## Fluxo completo — Feign e RabbitMQ

Gateway e composite usam a descoberta de serviços do Eureka nas chamadas HTTP. O fluxo acontece em três etapas:

1. **Escrita (POST ou DELETE):** o cliente envia a requisição pelo Gateway ao composite, que valida os dados e publica comandos na exchange `products.commands`, com routing keys `product`, `recommendation` e `review`. Dados inválidos retornam 400/422 antes da publicação. Cada publicação aguarda confirmação do broker e verificação de roteamento; quando todas têm sucesso, a resposta é `202 Accepted`. Falhas retornam 503 e interrompem novas publicações, mas comandos anteriores podem já ter sido aceitos.
2. **Processamento assíncrono:** cada núcleo consome sua fila e confirma a mensagem (ACK) após concluir a operação no banco. Product e Recommendation usam MongoDB; Review usa MySQL. CREATE leva o DTO e gera um comando por item; DELETE leva `productId` com `data: null` e gera um comando por serviço, excluindo também todas as recomendações e reviews desse produto. Com o consumidor desligado, as mensagens aguardam na fila durável. Falhas transitórias têm até três tentativas totais; erros permanentes ou tentativas esgotadas encaminham a mensagem, via `products.dlx`, para a DLQ correspondente, sem replay automático.
3. **Consulta (GET):** o cliente consulta pelo Gateway; o composite busca produto, recomendações e reviews em paralelo via OpenFeign/HTTP e monta o `ProductAggregate` com os endereços dos serviços. O sucesso retorna 200; produto ausente retorna 404 quando as demais consultas estão disponíveis; falhas de comunicação retornam erro HTTP. GET não publica nem consome comandos RabbitMQ.

O `202 Accepted` confirma a aceitação dos comandos pelo broker; os consumidores podem processá-los antes ou depois dessa resposta. A persistência deve ser verificada por consultas posteriores.

As gravações e exclusões têm consistência eventual e não formam uma transação distribuída. Um produto pode ser excluído antes de suas recomendações e reviews; receber 404 na consulta não comprova a conclusão das três exclusões. DELETE de registros já ausentes é idempotente. No CREATE, repetir a mesma chave com os mesmos dados é aceito pelo consumidor; conteúdo diferente gera conflito e vai para a DLQ.
