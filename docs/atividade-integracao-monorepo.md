# Atividade — Integração dos projetos ao monorepo de microservices

## Objetivo

Integrar o projeto desenvolvido pelo grupo ao repositório compartilhado, realizando os ajustes necessários para que ele compile, execute e se comunique corretamente com os demais serviços.

O repositório já possui as bibliotecas `api` e `util` e o `product-composite-service` implementado com OpenFeign. Cada grupo deverá incorporar seu projeto a essa estrutura e entregar a integração por meio de um Pull Request (PR).

## 1. Preparar o repositório

Clone o repositório:

```bash
git clone https://github.com/F290-LDMP/workshop-microservices.git
cd workshop-microservices
git checkout -b integracao/nome-do-grupo
```

Consulte o `../README.md` e os roteiros disponíveis na pasta `docs`:

- `STEP-1.md`: contratos e responsabilidades dos serviços.
- `STEP-2-LIBS.md`: utilização das bibliotecas compartilhadas.
- `STEP-2.md`: integração dos projetos.
- `STEP-3-COMPOSITE.md`: funcionamento atual do composite e testes de integração.

Utilize **Java 17 e o Gradle Wrapper da raiz**. Mantenha as versões de dependências alinhadas ao projeto.

## 2. Incluir o projeto na pasta correta

Cada grupo deverá adicionar somente o projeto ou os projetos sob sua responsabilidade:

| Projeto | Pasta de destino |
|---|---|
| product-service | `microservices/product-service` |
| review-service | `microservices/review-service` |
| recommendation-service | `microservices/recommendation-service` |
| eureka-service | `spring-cloud/eureka-service` |
| edge-service | `spring-cloud/edge-service` |

Nesta atividade, `eureka-service` e `edge-service` correspondem aos projetos chamados de `eureka-server` e `gateway` no tutorial.

Copie o código-fonte, o `../build.gradle` e as configurações necessárias. Não inclua a pasta `.git` do projeto original, arquivos de IDE, caches ou diretórios de build.

Registre o módulo no `../settings.gradle` da raiz. Exemplo:

```groovy
include 'microservices:product-service'
```

Para projetos de infraestrutura:

```groovy
include 'spring-cloud:eureka-service'
include 'spring-cloud:edge-service'
```

Adicione apenas os módulos efetivamente entregues pelo grupo, preservando os que já existem.

## 3. Integrar as bibliotecas e os contratos

Os serviços **product, review e recommendation** devem utilizar as bibliotecas compartilhadas. No `../build.gradle` do módulo, inclua:

```groovy
implementation project(':api')
implementation project(':util')
implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${rootProject.springdocVersion}"
```

Implemente a interface correspondente da biblioteca `api`:

| Serviço | Interface |
|---|---|
| product-service | `ProductService` |
| review-service | `ReviewService` |
| recommendation-service | `RecommendationService` |

Também será necessário:

- Substituir DTOs e exceções locais pelas classes da biblioteca `api`.
- Remover cópias locais dos utilitários compartilhados.
- Importar `ServiceUtil`, `GlobalControllerExceptionHandler` e `OpenApiConfiguration`, conforme o `STEP-2-LIBS.md`.
- Preservar endpoints, campos JSON, tipos e códigos HTTP definidos nos contratos.
- Preencher `serviceAddress` com o endereço real da instância.
- Manter entidades e repositórios de persistência dentro do próprio serviço.
- Disponibilizar Swagger UI e `/v3/api-docs`.

**Exceção para infraestrutura:** Eureka e Edge não implementam os contratos de negócio. O Edge utiliza uma stack reativa e **não deve receber a biblioteca `util` nem o starter MVC**, conforme o tutorial. A exigência de utilização conjunta de `api` e `util` aplica-se aos serviços de negócio.

## 4. Ajustar as configurações de execução

Utilize as seguintes configurações como referência:

| Aplicação | `spring.application.name` | Porta local |
|---|---|---|
| product-service | `product` | 7001 |
| recommendation-service | `recommendation` | 7002 |
| review-service | `review` | 7003 |
| product-composite-service, já existente | `product-composite` | 7000 |
| eureka-service | `eureka-server` | 8761 |
| edge-service | `gateway` | 8080 |

Nos clientes Eureka, configure o endereço do servidor:

```yaml
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
```

Os nomes `product`, `review` e `recommendation` precisam coincidir com os utilizados pelo composite para descoberta dos serviços.

Responsabilidades específicas:

- **Product e Recommendation:** configurar a conexão com MongoDB.
- **Review:** configurar a conexão com MySQL.
- **Eureka:** habilitar o servidor e configurar `registerWithEureka: false` e `fetchRegistry: false`.
- **Edge:** configurar a rota `/product-composite/**` para `lb://product-composite` e disponibilizar `/actuator/health`.

Documente as variáveis de ambiente, os bancos e os comandos necessários para executar o projeto. Não versione credenciais pessoais.

## 5. Validar a integração

Na raiz do monorepo, execute:

```bash
./gradlew projects
./gradlew clean build
```

No Windows, utilize `gradlew.bat`.

Depois, inicie os bancos, o Eureka, os serviços de negócio, o composite e o Edge. Execute cada aplicação em um terminal separado, utilizando o caminho do módulo. Exemplos:

```bash
./gradlew :spring-cloud:eureka-service:bootRun
./gradlew :microservices:product-service:bootRun
./gradlew :microservices:product-composite-service:bootRun
./gradlew :spring-cloud:edge-service:bootRun
```

**O build não inicia automaticamente todas as aplicações.** Combine com os demais grupos a disponibilidade dos módulos necessários para testar o fluxo completo.

Confira:

- Aplicações iniciando sem falhas de configuração ou de conexão.
- Clientes registrados como **UP** no Eureka.
- Criação, consulta e exclusão funcionando conforme os contratos.
- Respostas de erro corretas: 400 para requisições malformadas, 404 para produto inexistente e 422 para entradas inválidas de negócio.
- Review e Recommendation retornando `200` com `[]` quando não houver registros.
- Swagger disponível nos serviços de negócio.
- Fluxo completo funcionando pelo Edge: **Edge → Composite → Product, Review e Recommendation**.

Utilize os exemplos do `STEP-3-COMPOSITE.md` para criar, consultar e excluir um agregado, acessando a porta `8080` pelo Edge. No composite, POST e DELETE retornam `202`; GET retorna `200` quando o produto existe.

## 6. Documentar e abrir o Pull Request

Inclua no README do módulo:

- Nome do grupo e integrantes.
- Link do repositório original e commit ou tag utilizado na integração.
- Configurações e comandos de execução.
- Dependências de banco de dados.
- Testes realizados e evidências dos resultados.

Faça commit, envie a branch e abra um Pull Request para a branch principal do repositório compartilhado.

Título sugerido:

```text
Integração do product-service — Grupo X
```

Na descrição do PR, apresente os ajustes realizados, as instruções para validação e as evidências de funcionamento. Antes da entrega, atualize sua branch e resolva eventuais conflitos.

## Entrega no Teams

Cada grupo deverá **anexar ou inserir o link do Pull Request nesta tarefa**, identificando os integrantes e os serviços entregues. As evidências de build e execução deverão estar disponíveis no PR.

## Critérios de conclusão

- Projeto incluído na pasta correta e registrado no Gradle da raiz.
- Build do monorepo concluído com sucesso.
- Bibliotecas e contratos compartilhados utilizados conforme a responsabilidade do serviço.
- Configurações de banco, Eureka e rotas funcionando.
- Serviço integrado ao fluxo existente, sem falhas nos cenários de validação.
- Instruções suficientes para outra pessoa executar e testar a entrega.
- Pull Request aberto e link entregue no Teams.

**A entrega deve demonstrar a integração funcionando; apenas copiar o projeto para o repositório não conclui a atividade.**
