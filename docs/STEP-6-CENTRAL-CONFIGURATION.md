# STEP-6 — Central Configuration com Spring Cloud Config e GitHub

## Objetivo e sequência

Esta etapa corresponde ao tema do capítulo 13 da terceira edição e parte da branch `codex/spring-security-keycloak`. A implementação fica em `codex/central-configuration`, preservando a segurança da etapa anterior.

O GitHub versiona configurações; Config Server lê o Git; cada aplicação consulta o Config Server ao iniciar. O cliente não baixa arquivos diretamente do GitHub. O servidor não usa Eureka para iniciar, evitando o ciclo Config Server → Eureka → Config Server.

| Aplicação (`spring.application.name`) | Arquivo base | Profiles disponíveis |
|---|---|---|
| `product` | `product.yml` | `product-local.yml`, `product-docker.yml` |
| `recommendation` | `recommendation.yml` | `recommendation-local.yml`, `recommendation-docker.yml` |
| `review` | `review.yml` | `review-local.yml`, `review-docker.yml` |
| `product-composite` | `product-composite.yml` | `product-composite-local.yml`, `product-composite-docker.yml` |
| `gateway` | `gateway.yml` | `gateway-local.yml`, `gateway-docker.yml` |
| `eureka-server` | `eureka-server.yml` | `eureka-server-local.yml`, `eureka-server-docker.yml` |

Todos ficam em `config-repo/`; `application.yml` dessa pasta contém propriedades comuns. O nome de configuração do composite é **product-composite**, não o nome do diretório `product-composite-service`. `api` e `util` são bibliotecas e não são clientes Config.

Os arquivos base preservam as configurações existentes e usam defaults para execução local. `local` explicita a identificação do ambiente; `docker` substitui endereços de banco, RabbitMQ, Eureka e JWKS por nomes da rede Compose. Não há um ambiente de produção pronto nesta aula. Para adicionar `dev`, por exemplo, crie `<application>-dev.yml` e ative `SPRING_PROFILES_ACTIVE=dev` no cliente.

## 1. Reproduzir a implementação

1. Inclua `spring-cloud:config-server` em `settings.gradle` e crie o módulo MVC com `spring-cloud-config-server`, actuator e starter-security. As versões vêm dos BOMs existentes.
2. Crie `ConfigServerApplication` com `@EnableConfigServer`. Configure porta 8888, URI Git, label, `search-paths: config-repo` e `clone-on-start: true`. O servidor deve iniciar sem depender de suas próprias configurações remotas.
3. Proteja os endpoints de configuração com Basic Auth; libere apenas health para o Compose. O usuário e senha do Config Server são diferentes dos clients Keycloak e do token GitHub.
4. Transfira as configurações completas de cada serviço para `config-repo/<application>.yml`, removendo `spring.application.name` desses arquivos. Adicione os arquivos por profile da tabela.
5. Adicione `spring-cloud-starter-config` aos seis executáveis clientes. Em cada `application.yml` local, mantenha nome da aplicação, profile, import, credenciais do servidor e label.
6. Use `spring.config.import: configserver:${CONFIG_SERVER_URL:http://localhost:8888}`. Sem `optional:`, a indisponibilidade do servidor impede a inicialização em vez de executar com configuração incompleta. Não é necessário `bootstrap.yml` nem o starter bootstrap legado.
7. No Compose, inicie Config Server antes do Eureka e dos demais clientes; passe `SPRING_PROFILES_ACTIVE=docker`. Remova os endereços duplicados do Compose, mantendo credenciais por ambiente e portas operacionais.
8. Dê ao Config Server seu próprio Dockerfile e `.dockerignore`, seguindo os demais módulos. Gere os JARs antes do build Docker.

Para revisar somente esta etapa:

```bash
git diff codex/spring-security-keycloak..codex/central-configuration
```

## 2. Publicar as configurações no GitHub

As branches desta aula estão publicadas no repositório do professor. Ao reproduzir em outro repositório, antes de usar o backend GitHub, é necessário que `config-repo/` exista no repositório e label indicados. Um commit local não está disponível ao container que clona pelo HTTPS.

Opção A: usar este monorepo e publicar as branches da aula, com permissão de escrita no remoto:

```bash
git push -u origin codex/rabbitmq-base
git push -u origin codex/spring-security-keycloak
git push -u origin codex/central-configuration
```

Nesse caso, `.env` deve conter:

```dotenv
CONFIG_GIT_URI=https://github.com/F290-LDMP/workshop-microservices.git
CONFIG_GIT_LABEL=codex/central-configuration
CONFIG_SERVER_USER=config
CONFIG_SERVER_PASSWORD=config-workshop
```

Opção B: criar no GitHub um repositório de configurações próprio, clonar e copiar **a pasta `config-repo/` inteira**, fazer commit e push na `main`. O servidor procura essa pasta, não arquivos soltos na raiz. Aponte `CONFIG_GIT_URI` para a URL HTTPS desse repositório e `CONFIG_GIT_LABEL=main`. Isso permite alterar configurações sem publicar código de aplicação.

Para repositório privado, preencha `CONFIG_GIT_USERNAME` com o usuário GitHub e `CONFIG_GIT_PASSWORD` com um token com leitura do repositório. Os valores são injetados no Config Server; clientes só recebem as credenciais HTTP do Config Server. Não coloque token na URI e não versione `.env`.

O `.env.example` usa o monorepo e a branch desta etapa como exemplo; a inicialização depende de publicá-la ou de substituir esses valores por um repositório próprio acessível. Se já existe `.env`, acrescente as variáveis sem sobrescrever credenciais dos bancos/RabbitMQ.

## 3. Subir e verificar a configuração antes das APIs

```bash
git switch codex/central-configuration
# Somente se ainda não existir .env:
cp .env.example .env
# Edite CONFIG_GIT_URI/LABEL conforme a seção anterior.
./gradlew clean build
docker compose up --build -d --wait config-server
curl -fsS localhost:8888/actuator/health
# 401 sem credenciais:
curl -i 'localhost:8888/product/docker/codex(_)central-configuration'
# Com os valores locais do exemplo:
curl -fsS -u config:config-workshop \
  'localhost:8888/product/docker/codex(_)central-configuration'
python3 scripts/smoke-config.py
docker compose up --build -d --wait
# Aguarde Keycloak responder ao endpoint OIDC do STEP-5.
python3 scripts/smoke-security.py
```

O endpoint segue `/{application}/{profile}/{label}`. Nos requests HTTP diretos, branch com `/` usa `(_)`: `codex/central-configuration` vira `codex(_)central-configuration`. Se usa um repositório na main, consulte `/product/docker/main`. Nos valores `CONFIG_GIT_LABEL` dos clientes/servidor use o nome Git normal, com `/`.

A resposta JSON contém `name`, `profiles`, `label`, `version` (SHA Git) e `propertySources`. Fontes anteriores têm maior prioridade; `product-docker.yml` deve preceder `product.yml` e o arquivo comum. Compare `/product/local/<label>` com `/product/docker/<label>` e encontre `spring.rabbitmq.host`.

Variáveis do processo podem sobrepor valores remotos: por exemplo `SERVER_PORT` continua determinando a porta operacional no Compose. Mudar um YAML no GitHub sem conferir essas variáveis pode parecer não ter efeito. Senhas de banco/Rabbit continuam em placeholders resolvidos nos clientes; não são substituídas por valores reais versionados.

O script de smoke lê `CONFIG_SERVER_URL`, `CONFIG_SERVER_USER`, `CONFIG_SERVER_PASSWORD` e `CONFIG_GIT_LABEL` do ambiente. Ele não lê `.env` automaticamente; seus defaults usam os valores didáticos e a branch deste monorepo. Exporte os valores quando utilizar outro repositório/credenciais:

```bash
export CONFIG_GIT_LABEL=main
python3 scripts/smoke-config.py
```

## 4. Experimento de alteração centralizada

1. No GitHub, altere `info.workshop.lesson` de `config-repo/application.yml` na label usada pela aula, faça commit/push.
2. Consulte novamente o endpoint Config Server. Confira o novo SHA e a propriedade, identificando o commit que forneceu a configuração.
3. Os clientes já iniciados mantêm os valores em memória. Nesta aula, reinicie os processos para reler as configurações; `docker compose restart gateway product-composite-service product-service recommendation-service review-service eureka-server` conserva as variáveis atuais dos containers.
4. Se alterou `.env` (URI, label, credenciais ou profile), use `docker compose up -d --force-recreate`, pois restart não atualiza environment. Mudança no próprio Config Server pode exigir sua recriação primeiro.
5. Para rollback, reverta o commit de configuração no GitHub e reinicie os clientes. Também é possível fixar `CONFIG_GIT_LABEL` em uma tag/commit, aplicando o mesmo valor no servidor e clientes.

Config Server consulta Git para servir configurações; não envia automaticamente mudanças aos processos. Não expomos `/actuator/refresh`, não adicionamos `@RefreshScope` e não configuramos Spring Cloud Bus. RabbitMQ permanece no fluxo de negócio da aula anterior.

## 5. Execução fora do Docker e testes sem GitHub

No host, o Config Server precisa receber a URI do Git antes de iniciar. Para um teste local do backend Git, com os arquivos já commitados nesta branch:

```bash
CONFIG_GIT_URI="file://$PWD" CONFIG_GIT_LABEL=codex/central-configuration \
  ./gradlew :spring-cloud:config-server:bootRun
```

O backend continua sendo Git, não `native`. O modo `file:` opera sobre o repositório local e pode fazer checkout de labels; use uma cópia descartável se for experimentar outras labels, para preservar sua árvore de trabalho. GitHub é o caminho principal da aula; este comando é uma alternativa de laboratório sem rede. Em outro terminal:

```bash
CONFIG_GIT_LABEL=codex/central-configuration SPRING_PROFILES_ACTIVE=local \
  ./gradlew :spring-cloud:eureka-server:bootRun
```

Os demais clientes seguem a mesma regra, com bancos/Rabbit e Keycloak acessíveis no host. Não execute os mesmos serviços simultaneamente no host e no Docker nas mesmas portas.

```bash
./gradlew build
./gradlew :spring-cloud:config-server:test
```

`ConfigServerGitTest` cria um Git temporário, copia os YAML reais, cria duas branches e inicia um Config Server HTTP real. Verifica autenticação, health, configuração comum, arquivos dos seis projetos, sobreposição de profile, seleção de label e label inexistente. Não depende do GitHub nem do Docker. Nos testes dos clientes, o Gradle aponta `spring.config.location` para os mesmos YAML base e desativa o Config Client; isso preserva os testes sem rede e sem duplicar os arquivos.

## Diagnóstico

| Sintoma | Verificação |
|---|---|
| Compose pede CONFIG_GIT_URI | Complete o `.env`; a variável é obrigatória |
| Clone falha / Config Server não inicia | URI, acesso à rede, repositório privado, token e branch publicada |
| 401 ao iniciar cliente | Credenciais HTTP CONFIG_SERVER_USER/PASSWORD devem coincidir |
| 404 para label | Label não foi publicada, nome incorreto ou falta conversão `(_)` no curl |
| Cliente não inicia sem Config Server | Comportamento intencional do import obrigatório; restaure o servidor |
| Cliente conecta em localhost dentro do container | Confira profile docker e arquivo `<application>-docker.yml` no Git |
| Alteração não aparece no processo | Confira SHA no servidor e reinicie/recrie o cliente |
| Aplicação desconhecida recebe configuração comum | `application.yml` é compartilhado; não use resposta não vazia como validação de nomes |

## Validação realizada em 25/10/2026

- `./gradlew build`: concluído.
- `./gradlew test --rerun-tasks`: 47 testes, zero falhas e zero ignorados, incluindo RabbitMQ/MongoDB/MySQL com Testcontainers.
- Compose completo iniciado; serviços com healthcheck saudáveis e realm Keycloak acessível.
- `smoke-config.py`: 12 combinações (seis aplicações × dois profiles) lidas do GitHub, label `codex/central-configuration`, com SHA de configuração `fe7bfd1719fd`.
- `smoke-security.py`: JWTs reais, 401/403 e autorização no Gateway e composite; criação, consulta, repetição e exclusão assíncronas concluídas.
- Label com `/` validada também na importação pelos clientes Gateway e composite.

O código permanece na branch `codex/central-configuration`; a etapa anterior está preservada em `codex/spring-security-keycloak`. O `.env` local recebeu somente as variáveis Config ausentes. Os smokes criam produtos com IDs aleatórios e os excluem ao concluir; volumes e DLQs anteriores são preservados.

## Referências

- [Livro — capítulo 13, terceira edição](https://github.com/PacktPublishing/Microservices-with-Spring-Boot-and-Spring-Cloud-Third-Edition/tree/main/Chapter13).
- [Spring Cloud Config 4.0.2 — Git, labels, segurança e Config Data](https://docs.spring.io/spring-cloud-config/docs/4.0.2/reference/html/).
- [STEP-5 — segurança preservada nesta etapa](STEP-5-SECURITY-KEYCLOAK.md).
