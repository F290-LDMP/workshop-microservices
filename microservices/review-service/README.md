# Review service — módulo do workshop

Serviço da equipe review: implementar POST /review e GET/DELETE /review?productId=, consumir api/util e persistir avaliações. Java Eclipse Temurin 17 e versões definidas no build raiz. Nome Eureka: review. Porta padrão: 7003.

## Origem da entrega

- Repositório: https://github.com/jp8002/microservico-reviews
- Origem exata: consultar ORIGEM.md e integration-source.sha256. A adaptação foi importada da árvore de trabalho; registrar o commit final no PR.
- Equipe/integrantes: preencher antes do PR.

## Banco e execução

A entrega do roteiro utiliza MySQL. Prepare reviewsdb e usuário reviews com permissão de migrations. Não versione a senha. Na raiz do monorepo:

```bash
read -rs -p 'Senha do banco: ' DB_PASSWORD; echo; export DB_PASSWORD
export SPRING_PROFILES_ACTIVE=mysql,integracao
export EUREKA_URL=http://localhost:8761/eureka/
./gradlew :microservices:review-service:bootRun
```

Para MySQL, DB_URL padrão é jdbc:mysql://localhost:3306/reviewsdb. Para PostgreSQL local, remova mysql dos perfis; DB_URL padrão é jdbc:postgresql://localhost:5432/reviewsdb. DB_USER padrão é reviews; DB_PASSWORD é obrigatório; PORT padrão é 7003. Integração habilita Eureka. A conexão entre bancos não é migrada automaticamente.

Flyway V1/V2 criam/populam a tabela. V3 preserva dados e IDs públicos, adiciona id interno, version e unicidade productId/reviewId. Hibernate valida o schema. Um banco preexistente sem histórico Flyway exige avaliação explícita; não habilite baseline automático para contornar erros.

## Contrato

POST recebe productId, reviewId, author, subject e content e retorna 200 com Review e serviceAddress preenchido pelo servidor. O reviewId público é preservado. GET retorna lista, inclusive [] quando vazia. DELETE retorna 200 sem corpo, remove todas as avaliações do produto e é idempotente. Não consulta product-service para verificar existência do produto.

400 para requisições malformadas; 422 para identificadores inválidos, textos fora dos limites e duplicidade (productId, reviewId). HttpErrorInfo tem timestamp, path, status, error e message. GET de lista vazia não retorna 404.

Swagger: http://localhost:7003/swagger-ui.html
OpenAPI: http://localhost:7003/v3/api-docs

## Testes

Na raiz do monorepo, sem banco externo:

```bash
./gradlew :microservices:review-service:test
```

Usa H2 em modo PostgreSQL com servidor HTTP real em porta aleatória. Cobre CRUD, identidade pública, isolamento entre produtos, duplicidade, validações, migrations e OpenAPI. Para testar MySQL real, utilize um banco EXCLUSIVO de testes e configure REVIEW_TEST_DB_URL, REVIEW_TEST_DB_USER, REVIEW_TEST_DB_PASSWORD e REVIEW_TEST_MIGRATIONS=classpath:db/migration; execute a mesma task com --rerun-tasks. Não deixe perfis de execução mysql/integracao ativos na execução isolada de testes.

Com o review já iniciado:

```bash
python3 microservices/review-service/scripts/test-http.py
```

O script usa um productId aleatório, verifica que está vazio e limpa apenas os dados desse ID. O relatório JUnit fica em microservices/review-service/build/reports/tests/test/index.html.

## Eclipse

Importe o monorepo inteiro por Gradle → Existing Gradle Project, usando o Wrapper raiz e Temurin 17. Configure Lombok na IDE para as libs compartilhadas. Após sincronizar, execute se.magnus.api.core.review.ReviewApplication do módulo review-service. Defina DB_PASSWORD e SPRING_PROFILES_ACTIVE=mysql,integracao na configuração de execução. Para trabalhar sem Eureka, use apenas mysql ou nenhum perfil para PostgreSQL.

## Evidências da integração

Registrar no PR: build do monorepo, testes com MySQL, Swagger/erros, registro review UP no Eureka e fluxo Edge → Composite → núcleos. O teste H2 e o teste PostgreSQL isolado não comprovam o aceite MySQL nem a integração com outras equipes.
