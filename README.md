# workshop-microservices

Base Gradle multi-project para o workshop de microservices da FATEC Araras.

## Pré-requisitos

- JDK 17, com `JAVA_HOME` apontando para sua instalação. O Gradle usa a toolchain Java 17 para compilação e testes.
- Acesso à internet na primeira execução para baixar o Gradle e as dependências.
- Gradle 8.14.3 fornecido pelo Wrapper; não é necessário instalar Gradle.

## Estrutura

```text
workshop-microservices/
├── .gitignore
├── README.md
├── docs/
│   ├── STEP-1.md
│   ├── STEP-2-LIBS.md
│   └── STEP-2.md
├── settings.gradle
├── build.gradle
├── gradlew
├── gradlew.bat
├── gradle/
│   ├── gradle-daemon-jvm.properties
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── api/
│   ├── build.gradle
│   └── src/main/java/br/com/fatecararas/api/
│       ├── exceptions/                  # NotFoundException, InvalidInputException
│       ├── core/product/                # Product, ProductService
│       ├── core/recommendation/         # Recommendation, RecommendationService
│       ├── core/review/                 # Review, ReviewService
│       └── composite/product/           # ProductAggregate, RecommendationSummary,
│                                         # ReviewSummary, ServiceAddresses,
│                                         # ProductCompositeService
├── util/
│   ├── build.gradle
│   └── src/main/java/br/com/fatecararas/util/
│       ├── http/                        # ServiceUtil, HttpErrorInfo,
│       │                                # GlobalControllerExceptionHandler
│       └── openapi/                     # OpenApiConfiguration
├── spring-cloud/.gitkeep
└── microservices/
    ├── .gitkeep
    └── product-service/
        ├── README.md
        ├── build.gradle
        └── src/main/
            ├── java/br/com/fatecararas/microservices/core/product/
            │   ├── ProductServiceApplication.java
            │   ├── persistence/           # ProductEntity, ProductRepository
            │   └── services/              # ProductServiceImpl
            └── resources/application.yml
```

- `api`: contratos do workshop — 7 DTOs, 2 exceções de negócio e as 4 interfaces REST com anotações Swagger OpenAPI. Não depende de `util`; expõe `spring-web` e `swagger-annotations-jakarta` via configuração `api`.
- `util`: utilitários compartilhados — identificação da instância (`ServiceUtil`), formato de erro (`HttpErrorInfo`), tratamento global (`GlobalControllerExceptionHandler`) e metadados do OpenAPI (`OpenApiConfiguration`). Depende de `api`, sem ciclo.
- `spring-cloud`: receberá os projetos Eureka e Gateway da equipe responsável; contém apenas `.gitkeep`.
- `microservices`: o `product-service` do grupo 1 já está incorporado; review, recommendation e composite ainda serão adicionados. Consulte [as instruções do serviço](microservices/product-service/README.md).

As bibliotecas usam `java-library` e geram JARs comuns: não possuem classe `main`, servidor HTTP, `application.yml` nem `bootRun`. O `product-service` é a única aplicação presente e já consome as duas libs, conforme a seção 4 do [STEP-2-LIBS](docs/STEP-2-LIBS.md). O `.gitkeep` restante existe apenas para versionar `spring-cloud/`, que continua vazio.

## Documentação do workshop

- [STEP-1 — APIs, Eureka e Gateway](docs/STEP-1.md): contratos e tarefas das equipes.
- [STEP-2 — Bibliotecas compartilhadas](docs/STEP-2-LIBS.md): preparação da base, implementação de `util` e `api`, tratamento de erros e Swagger OpenAPI.
- [STEP-2 — Integração dos serviços](docs/STEP-2.md): OpenFeign, Docker e balanceamento de carga.

## Java usado pelo Gradle

`gradle/gradle-daemon-jvm.properties` fixa a JVM do daemon em Java 17, independentemente da versão apontada por `JAVA_HOME`. É necessário ter um JDK 17 instalado e detectável pelo Gradle; não há download automático de JDK configurado.

A toolchain em `build.gradle` também seleciona Java 17 para compilação e testes. Essas configurações têm papéis distintos: apenas a toolchain não impede que o próprio Gradle seja iniciado com uma JVM incompatível.

Se precisar selecionar o JDK no terminal com SDKMAN, execute `sdk use java <identificador-do-jdk-17-instalado>`. No IntelliJ, configure o Project SDK e o Gradle JVM para JDK 17.

## Verificação da base

Execute na raiz do repositório:

```bash
./gradlew --version
./gradlew projects
./gradlew clean build
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`. Se necessário, no Linux/macOS restaure a permissão com `chmod +x gradlew`.

O build inclui `api`, `util` e `microservices:product-service`. As duas libs compilam de verdade: `compileJava` executa e gera `api/build/libs/api-1.0.0-SNAPSHOT.jar` (13 classes) e `util/build/libs/util-1.0.0-SNAPSHOT.jar` (4 classes). Em ambas, `test` permanece `NO-SOURCE`, pois as libs não têm testes próprios; o comportamento HTTP é exercitado pelos testes do `product-service`, que sobem a aplicação com o repositório mockado e dispensam MongoDB e Eureka.

Saída real resumida dos dois primeiros comandos:

```text
$ ./gradlew --version
Gradle 8.14.3
Launcher JVM:  25.0.2 (Amazon.com Inc. 25.0.2+10-LTS)
Daemon JVM:    Compatible with Java 17, any vendor, nativeImageCapable=false (from gradle/gradle-daemon-jvm.properties)

$ ./gradlew projects
Root project 'workshop-microservices'
+--- Project ':api'
+--- Project ':util'
\--- Project ':microservices:product-service'
```

O `Launcher JVM` é a JVM do seu terminal e pode ser mais recente; o `Daemon JVM` deve indicar Java 17, comprovando que `gradle/gradle-daemon-jvm.properties` está em uso.

## Uso das libs

Ao incorporar um serviço MVC, adicione as duas libs ao `build.gradle` do projeto:

```groovy
implementation project(':api')
implementation project(':util')
implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${rootProject.springdocVersion}"
```

O starter `webmvc-ui` pertence à aplicação: as libs sozinhas não publicam `/v3/api-docs` nem `/swagger-ui.html`. Não adicione `util` ao Gateway, que é reativo.

O component scan padrão do serviço não encontra `br.com.fatecararas.util...`. Registre os componentes compartilhados na classe de aplicação:

```java
import org.springframework.context.annotation.Import;
import br.com.fatecararas.util.http.GlobalControllerExceptionHandler;
import br.com.fatecararas.util.http.ServiceUtil;
import br.com.fatecararas.util.openapi.OpenApiConfiguration;

@Import({ServiceUtil.class, GlobalControllerExceptionHandler.class, OpenApiConfiguration.class})
```

Implemente as interfaces de `api` nos controllers (`ProductService`, `ReviewService`, `RecommendationService`, `ProductCompositeService`) sem repetir `@GetMapping`/`@PostMapping`/`@DeleteMapping`. O detalhamento está nas seções 4 e 5 do [STEP-2-LIBS](docs/STEP-2-LIBS.md), incluindo o `application.yml` do springdoc e as portas do Swagger UI de cada serviço.

## Product service (STEP-1)

Serviço MVC na porta `7001`, com persistência MongoDB, registro no Eureka como `product` e Swagger em `/swagger-ui.html`. Ele é a referência prática da seção 4 do roteiro: depende de `api` e `util`, registra os componentes compartilhados com `@Import` e implementa `ProductService` sem repetir os mapeamentos.

```bash
./gradlew :microservices:product-service:test
./gradlew :microservices:product-service:bootRun
```

Consulte [as instruções do serviço](microservices/product-service/README.md) para variáveis de ambiente, endpoints e critérios de verificação.

## Convenções para as próximas etapas

- Pacote raiz e grupo Gradle: `br.com.fatecararas`.
- Versões alinhadas ao roteiro: Spring Boot 3.0.4, Spring Cloud 2022.0.2 e springdoc 2.0.2.
- As libs `api` e `util` já estão implementadas e versionadas; evite duplicar DTOs, interfaces, exceções ou o handler dentro dos serviços.
- Use o `ProductServiceApplication` como modelo de `@Import` e o `product-service/build.gradle` como modelo de dependências.
- Incorpore os serviços restantes em `microservices/` e inclua-os explicitamente em `settings.gradle` somente quando seus projetos estiverem presentes.
- Use o Wrapper da raiz para todos os módulos.
