# Product service

Implementação do grupo 1 do [STEP-1](../../docs/STEP-1.md), usando Java 17, Spring Boot 3.0.4 e Spring Cloud 2022.0.2.

## Execução

Este serviço já está incorporado ao monorepo e compila contra as libs `api` e `util` da raiz, descritas no [STEP-2-LIBS](../../docs/STEP-2-LIBS.md). Ele consome os contratos de `br.com.fatecararas.api` e os utilitários de `br.com.fatecararas.util` como dependências Gradle, sem cópias locais.

Inicie um MongoDB local na porta 27017 e o Eureka na porta 8761. Execute na raiz do repositório:

```bash
./gradlew :microservices:product-service:bootRun
```

Para executar individualmente sem Eureka:

```bash
EUREKA_ENABLED=false ./gradlew :microservices:product-service:bootRun
```

| Variável | Padrão |
| --- | --- |
| `SERVER_PORT` | `7001` |
| `MONGODB_URI` | `mongodb://localhost:27017/product-db` |
| `EUREKA_ENABLED` | `true` |
| `EUREKA_URL` | `http://localhost:8761/eureka/` |

O MongoDB precisa estar disponível na inicialização para criar o índice único de `productId` na coleção `products`. O endereço retornado usa a porta efetiva do servidor, inclusive com `SERVER_PORT=0`. O cliente não define o `serviceAddress` armazenado ou retornado.

## Endpoints

```bash
curl -i -X POST localhost:7001/product -H 'Content-Type: application/json' \
  --data '{"productId":1,"name":"Produto 1","weight":100}'
curl -i localhost:7001/product/1
curl -i localhost:7001/product/13
curl -i localhost:7001/product/0
curl -i -X DELETE localhost:7001/product/1
curl -i localhost:7001/actuator/health
```

POST e GET retornam HTTP 200 e o contrato `Product`. DELETE retorna HTTP 200 sem corpo e é idempotente para IDs inexistentes. IDs menores que 1 retornam 422 em todos os endpoints; POST duplicado retorna 422; GET inexistente retorna 404. Os erros de negócio contêm `timestamp`, `path`, `status`, `error` e `message`.

Os contratos em `br.com.fatecararas.api` pertencem ao módulo `api`; os utilitários HTTP e a configuração OpenAPI pertencem ao módulo `util`. O serviço consome ambos via dependências Gradle e registra os componentes compartilhados com `@Import`, sem cópias locais. O DTO `Product` segue os getters definidos no roteiro.

Com o serviço em execução, o Swagger fica em `/swagger-ui.html` e o JSON OpenAPI em `/v3/api-docs`.

## Verificação

```bash
./gradlew :microservices:product-service:test :microservices:product-service:bootJar
```

Os testes sobem o servidor em porta aleatória e verificam endpoints, formato dos erros, validação, duplicidade, exclusão e endereço da instância usando repository mockado. Não exigem MongoDB nem Eureka. A persistência real e o registro no dashboard devem ser verificados com esses serviços em execução; após iniciar o Eureka, a aplicação deve aparecer como `PRODUCT` com status `UP`.
