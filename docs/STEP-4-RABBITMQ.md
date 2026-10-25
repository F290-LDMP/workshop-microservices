# STEP-4 — Escritas assíncronas com RabbitMQ

> Para apresentar em sala: [roteiro de aula com Insomnia](AULA-FEIGN-RABBITMQ.md), incluindo fila acumulada, retomada do consumidor e DLQ. A [collection importável](insomnia/workshop-feign-rabbitmq.json) contém as requisições na ordem da demonstração.

## Objetivo e arquitetura

O composite continua consultando os núcleos por OpenFeign/Eureka. POST e DELETE publicam comandos JSON com Spring AMQP (`RabbitTemplate`); cada núcleo recebe comandos por `@RabbitListener` e grava em seu banco. RabbitMQ não participa da descoberta de serviços do Eureka.

O capítulo 7 da terceira edição do livro também apresenta RabbitMQ, mas através de Spring Cloud Stream. Neste workshop usamos Spring AMQP para trabalhar diretamente com exchanges, routing keys, filas, acknowledgments e dead letters.

| Aplicação | Porta | Persistência |
|---|---|---|
| Gateway | 8080 | — |
| Eureka | 8761 | — |
| Composite | 7100 | — |
| Product | 7001 | MongoDB, `product-db` |
| Recommendation | 7002 | MongoDB, `recommendation-db` |
| Review | 7003 | MySQL, `review-db` |

No Compose, os núcleos não publicam portas no host. Gateway, Eureka, composite e RabbitMQ Management publicam suas portas. O endereço HTTP retornado por um núcleo identifica sua instância na rede Docker.

## Executar

Requer JDK 17 e Docker Desktop/Engine com Compose.

```bash
cp .env.example .env
./gradlew clean build
docker compose up --build -d --wait
python3 scripts/smoke-messaging.py
```

Cada aplicação possui seu próprio `Dockerfile` e `.dockerignore`: os quatro módulos em `microservices` e Eureka/Gateway em `spring-cloud`. O Compose aponta diretamente para os diretórios, por exemplo `build: microservices/review-service`, e gera imagens `workshop/<nome-do-serviço>`. O build Gradle da raiz deve ser executado primeiro; cada imagem recebe somente o JAR executável do módulo, que já incorpora `api` e `util`.

O teste usa polling com timeout de 120 segundos para aguardar descoberta e consistência eventual. Configure `BASE_URL` e `SMOKE_TIMEOUT` quando necessário. Para inspecionar:

```bash
docker compose ps
docker compose logs -f product-composite-service product-service review-service recommendation-service
```

- Eureka: http://localhost:8761
- RabbitMQ Management: http://localhost:15672 (`workshop` / `workshop`, ou valores do `.env`).
- Swagger do composite: http://localhost:7100/swagger-ui.html

Variáveis principais: `SERVER_PORT`, `EUREKA_URL`, `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `MONGODB_URI`, `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`. O timeout de confirmação pode ser configurado com `APP_MESSAGING_CONFIRM_TIMEOUT_MS` (padrão: 5000).

O Compose mantém os dados em volumes nomeados. `docker compose down` encerra o ambiente preservando dados. As credenciais padrão são destinadas ao laboratório local.

## Organização do código compartilhado

A mensageria faz parte da biblioteca `util`, no pacote `br.com.fatecararas.util.messaging`; não existe um subprojeto Gradle `messaging` separado.

- `RabbitConfiguration`: declara exchanges, filas e bindings e configura conversão JSON, publicação e retries.
- `EventPublisher`: publica comandos e aguarda confirmação do broker.
- `EventHandler`: valida o envelope e processa comandos com proteção contra duplicação.

Composite e núcleos dependem de `util` e importam explicitamente `RabbitConfiguration`. Os listeners permanecem nos respectivos serviços, próximos das operações de negócio. A dependência AMQP é exposta por `util`; Eureka e Gateway não usam essa biblioteca. O envelope `Event<T>` permanece em `api`, sem dependência de RabbitMQ.

Os testes compartilhados de publicação, processamento e integração RabbitMQ ficam em `util/src/test/java/br/com/fatecararas/util/messaging`. Para executá-los isoladamente:

```bash
./gradlew :util:test
```

O script `scripts/smoke-messaging.py` continua no repositório como verificação do ambiente completo pelo Gateway. Ele não é necessário para executar as aplicações.

## Topologia e contrato

Exchange direct durável `products.commands`:

| Routing key | Fila | DLQ |
|---|---|---|
| product | product.commands | product.dlq |
| review | review.commands | review.dlq |
| recommendation | recommendation.commands | recommendation.dlq |

A exchange direct `products.dlx` recebe mensagens rejeitadas e encaminha para a DLQ correspondente. A configuração compartilhada declara a topologia também no composite: consumidores podem estar desligados no momento da publicação.

```json
{
  "eventId": "b337d0a3-5c58-47ec-a9b2-7ec490106fef",
  "correlationId": "b8716046-dce4-48a5-a6ca-3a9d884b5a01",
  "eventType": "CREATE",
  "productId": 100,
  "eventCreatedAt": "2026-10-18T12:00:00Z",
  "data": {"productId":100,"name":"Produto","weight":100}
}
```

CREATE contém o DTO do destino. DELETE contém `data: null`; review e recommendation excluem todos os itens do `productId`. Cada publicação tem `eventId` próprio; a mesma requisição compartilha `correlationId`. Logs incluem ambos.

## Exercício do fluxo completo

```bash
curl -i -X POST localhost:8080/product-composite \
  -H 'Content-Type: application/json' \
  --data '{"productId":100,"name":"Produto","weight":100,"recommendations":[{"recommendationId":1,"author":"Ana","rate":5,"content":"Ótimo"}],"reviews":[{"reviewId":1,"author":"Bia","subject":"Qualidade","content":"OK"}]}'
curl -i localhost:8080/product-composite/100
curl -i -X DELETE localhost:8080/product-composite/100
```

`202 Accepted` confirma publicação e roteamento no broker, não conclusão da persistência. Após POST, GET pode responder 404 ou agregado parcial até os três consumidores concluírem. Após DELETE, aguarde produto ausente e coleções vazias.

IDs inválidos, notas fora de 0–5 e IDs repetidos nas coleções são rejeitados antes da primeira publicação. Falhas de publicação retornam 503 com `HttpErrorInfo`; alguns comandos podem já ter sido aceitos. Não repita indiscriminadamente uma operação quando houver timeout: inspecione o estado e as filas.

## Experimentos de falha

1. **Consumidor desligado:** execute `docker compose stop review-service`; crie outro agregado e confira `review.commands` no Management. O composite retorna 202; a consulta pode falhar enquanto o serviço está indisponível. Execute `docker compose start review-service` e aguarde a fila esvaziar e o agregado ficar completo.
2. **Banco indisponível:** execute `docker compose stop mysql`; publique uma review. Observe três tentativas totais com intervalos de 1 e 2 segundos, além do tempo de conexão ao banco; ao esgotar, a mensagem vai para `review.dlq`. Reinicie MySQL e examine a mensagem antes de republicá-la.
3. **Duplicação:** repita o mesmo POST. Os consumidores não inserem outra linha/documento para a mesma chave com conteúdo igual. Altere os dados mantendo a chave: o conflito vai para DLQ. POST direto no núcleo continua retornando 422 em duplicidade.
4. **Mensagem inválida:** no Management, publique JSON inválido em `products.commands` com routing key `review`; confira a DLQ. Também experimente tipo desconhecido ou `productId` diferente do payload.
5. **Broker indisponível:** execute `docker compose stop rabbitmq`; envie POST ao composite e confira 503. Reinicie RabbitMQ. Mensagens persistentes em filas duráveis sobrevivem ao reinício com o volume preservado.
6. **Consumidores concorrentes:** execute `docker compose up -d --scale recommendation-service=2`. A fila utiliza single active consumer e prefetch 1: somente uma instância consome por vez nesta configuração didática; as leituras HTTP podem usar ambas.

Para examinar uma DLQ use a aba **Get messages** com requeue habilitado. Republicar uma mensagem é uma ação manual após corrigir a causa; preserve seu envelope e confira a chave de roteamento. Não há replay automático que possa gerar um ciclo infinito.

## Garantias e limites

As mensagens são persistentes, filas e exchanges são duráveis, o produtor usa confirms/returns com publicação obrigatória e o consumidor confirma somente depois da operação de banco. Falhas transitórias têm tentativas limitadas; erros permanentes não são repetidos.

As proteções por chave de negócio e conteúdo evitam duplicação em reentregas comuns. Não há deduplicação histórica depois de excluir e recriar uma chave. A ordem é local a cada fila e publicador; não existe ordem global nem transação entre serviços. Timeout pode deixar o resultado da publicação incerto e uma falha pode publicar apenas parte do agregado.

Outbox, saga e acompanhamento persistente de operações ficam para uma etapa posterior.

## Testes

```bash
./gradlew test
```

A suíte cobre os contratos HTTP existentes, validação prévia do agregado, publicação correlacionada, confirmações/nacks/returns/timeouts, desserialização, conflito e reentrega. Testcontainers testa RabbitMQ real com retry/DLQ e MongoDB/MySQL com índices únicos, persistência e exclusão. Testes de containers são marcados como ignorados quando Docker não está disponível; confira os relatórios em `*/build/reports/tests/test/`.

Para testar o fluxo completo com todos os processos e Gateway, execute o script de smoke com Compose iniciado. Falhas de banco, consumidor e broker podem ser reproduzidas nos experimentos acima.
