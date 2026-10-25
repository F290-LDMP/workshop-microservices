# STEP-5 — Spring Security e Keycloak

## Base e objetivo

Sequência local: `codex/rabbitmq-base` → `codex/spring-security-keycloak` → `codex/central-configuration`.
A base RabbitMQ foi preservada antes desta etapa. A segunda implementação parte desta branch, permitindo ensinar cada mudança separadamente com `git diff codex/rabbitmq-base..codex/spring-security-keycloak`.

Mantemos Java 17, Spring Boot 3.0.4 e Spring Cloud 2022.0.2 do workshop. O capítulo 12 da terceira edição introduz segurança; usamos Keycloak como provedor OAuth2/OIDC externo, em lugar do authorization server do exemplo do livro. Não é necessário adapter específico do Keycloak: as aplicações são Resource Servers OAuth2 padrão.

O cliente obtém um JWT no Keycloak e envia `Authorization: Bearer <token>` ao Gateway. Gateway e composite validam assinatura, emissor e expiração. O Gateway encaminha o header Authorization à rota existente; não é necessário TokenRelay para este fluxo Bearer. O composite valida novamente, inclusive quando acessado diretamente na porta 7100.

| Recurso | Permissão |
|---|---|
| GET `/product-composite` e subcaminhos | `SCOPE_product:read` |
| POST e DELETE nesses caminhos | `SCOPE_product:write` |
| `/actuator/health` e subcaminhos | Público para health checks |
| Swagger/OpenAPI no composite | Público; operações exigem token |
| Outros caminhos | Negados por padrão |

Scopes são permissões OAuth2 no claim `scope`; o Spring adiciona o prefixo `SCOPE_`. Não são realm roles. O realm contém dois clients confidenciais com service accounts: `workshop-reader` (leitura) e `workshop-writer` (leitura/escrita). O grant `client_credentials` representa uma aplicação; não há login de usuário nesta aula. Para usuários de uma interface web, a extensão é Authorization Code com PKCE.

## 1. Reproduzir as alterações no código

1. Em Gateway e composite, adicione `spring-boot-starter-oauth2-resource-server` ao `build.gradle`, sem versão explícita (BOM do Boot). Nos testes, adicione `spring-security-test`; Gateway também recebe `spring-boot-starter-test`.
2. Configure `spring.security.oauth2.resourceserver.jwt.issuer-uri` e `jwk-set-uri` nos dois `application.yml`.
3. No Gateway, crie `SecurityConfiguration` com `SecurityWebFilterChain` e `ServerHttpSecurity` (stack WebFlux). Não acrescente `starter-web` ou `util` ao Gateway.
4. No composite, crie `config/SecurityConfiguration` com `SecurityFilterChain` e `HttpSecurity` (MVC), sessão STATELESS e regras da tabela. CSRF está desabilitado porque a API usa Bearer explícito e não autenticação por cookie. O Gateway usa repositório de contexto sem persistência.
5. Importe `infra/keycloak/workshop-realm.json` no container com `start-dev --import-realm`. O arquivo declara client scopes e clients; não habilita password grant.
6. Adicione Keycloak ao Compose e configure os endereços JWT no Gateway/composite. Não altere Feign, DTOs ou o envelope RabbitMQ.
7. O customizador OpenAPI no composite registra o esquema Bearer para o botão **Authorize**. Os testes verificam as regras HTTP antes da execução da lógica de negócio.

Leia as classes completas e compare a branch anterior. As bibliotecas `api` e `util` não carregam dependências de segurança.

## 2. Executar o laboratório

```bash
git switch codex/spring-security-keycloak
# Somente se .env ainda não existir:
cp .env.example .env
./gradlew clean build
docker compose up --build -d --wait
# Keycloak não tem healthcheck no Compose; aguarde este endpoint responder:
curl -fsS http://localhost:8180/realms/workshop/.well-known/openid-configuration
python3 scripts/smoke-security.py
```

Keycloak: http://localhost:8180 (admin / admin-workshop, ou variáveis `KEYCLOAK_ADMIN_USER` e `KEYCLOAK_ADMIN_PASSWORD`). Realm `workshop`. Clients importados usam segredos didáticos `reader-secret` e `writer-secret`; o `.env` administrativo não altera esses segredos. O script aceita `KEYCLOAK_CLIENT_SECRET` se você mudar o client pelo console.

Keycloak 26.8.1 está fixado no Compose. `start-dev`, HTTP, armazenamento local do Keycloak e segredos versionados servem ao laboratório. Uma implantação externa exige HTTPS, segredos próprios e banco persistente apropriado. A importação inicial não sobrescreve um realm existente no volume: após editar o JSON, atualize o realm pelo console ou recrie apenas o volume Keycloak quando puder descartar esse realm. Não apague os volumes dos bancos para atualizar clients.

### Por que dois endereços?

O emissor do token é sempre `http://localhost:8180/realms/workshop`, definido pelo hostname público do Keycloak. Dentro do Docker, `localhost` é o próprio container; por isso a busca de chaves públicas usa `http://keycloak:8080/realms/workshop/protocol/openid-connect/certs`. Definir JWKS explicitamente evita descoberta pelo hostname público, mantendo a validação do issuer. Fora do Docker, os dois defaults usam localhost:8180. Não substitua o issuer pelo endereço interno: isso invalida tokens emitidos com o hostname público.

## 3. Experimento guiado — 401, 403 e autorização

```bash
# Sem autenticação: 401
curl -i localhost:8080/product-composite/0
# Token malformado: 401
curl -i localhost:8080/product-composite/0 -H 'Authorization: Bearer invalid'

READER_TOKEN=$(python3 scripts/keycloak-token.py reader)
# Leitura autorizada, mas ID inválido: 422 (a chamada chegou à aplicação)
curl -i localhost:8080/product-composite/0 -H "Authorization: Bearer $READER_TOKEN"
# Token válido sem permissão de escrita: 403
curl -i -X DELETE localhost:8080/product-composite/0 -H "Authorization: Bearer $READER_TOKEN"

export ACCESS_TOKEN=$(python3 scripts/keycloak-token.py writer)
# Escrita autorizada, ID inválido: 422
curl -i -X DELETE localhost:8080/product-composite/0 -H "Authorization: Bearer $ACCESS_TOKEN"
# Reexecuta CREATE/GET/duplicação/DELETE e verifica consistência eventual
python3 scripts/smoke-messaging.py
```

Tokens expiram em cinco minutos; obtenha outro antes de continuar. No Insomnia, crie uma variável `access_token` e configure Bearer nas requisições da aplicação da collection da aula anterior. Requests administrativos Rabbit continuam com Basic Auth próprio. No Swagger http://localhost:7100/swagger-ui.html use **Authorize** e cole somente o JWT. As chamadas dos roteiros anteriores agora precisam do header Bearer, inclusive na porta 7100.

Para visualizar a obtenção sem o script:

```bash
curl -fsS http://localhost:8180/realms/workshop/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=workshop-reader -d client_secret=reader-secret
```

## 4. Limites e testes

Os núcleos não publicam portas no host e continuam sem autenticação HTTP nesta etapa. Feign consulta esses serviços dentro da rede Docker; não há propagação do contexto SecurityContext para o executor paralelo, nem necessidade de Bearer nesses núcleos. Não exponha essas portas como se estivessem protegidas. Eureka e Management Rabbit têm acesso administrativo próprio ou local; esta etapa não implementa zero trust, audience específica ou TLS interno.

RabbitMQ continua autenticando produtores/consumidores por suas credenciais. JWT não é enviado no evento. A autorização ocorre antes de publicar, mantendo 202 e consistência eventual. Erros de autenticação/autorização são produzidos pelos filtros Spring Security e não pelo advice `HttpErrorInfo`.

```bash
./gradlew :spring-cloud:gateway:test :microservices:product-composite-service:test
python3 scripts/smoke-security.py
```

Os testes de filtros usam decoders mockados e cobrem ausência/token inválido, scopes, negação padrão e escrita sem CSRF. O smoke exige processos reais e valida JWTs do Keycloak, proteção direta do composite e a regressão RabbitMQ. O compose não aguarda o Keycloak ficar pronto; se o script falhar ao conectar, confira logs e aguarde o endpoint OIDC responder antes de repeti-lo.

## Referências

- [Livro, terceira edição](https://github.com/PacktPublishing/Microservices-with-Spring-Boot-and-Spring-Cloud-Third-Edition/tree/main/Chapter12).
- [Spring Security — JWT reativo](https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/jwt.html).
- [Keycloak — containers e importação](https://www.keycloak.org/server/containers).
- [Keycloak — hostname](https://www.keycloak.org/server/hostname).
