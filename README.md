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
│   ├── STEP-2.md
│   └── STEP-3-COMPOSITE.md
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
    └── product-composite-service/    # BFF com OpenFeign + Swagger
```

- `api`: contratos do workshop — 7 DTOs, 2 exceções de negócio e as 4 interfaces REST com anotações Swagger OpenAPI. Não depende de `util`; expõe `spring-web` e `swagger-annotations-jakarta` via configuração `api`.
- `util`: utilitários compartilhados — identificação da instância (`ServiceUtil`), formato de erro (`HttpErrorInfo`), tratamento global (`GlobalControllerExceptionHandler`) e metadados do OpenAPI (`OpenApiConfiguration`). Depende de `api`, sem ciclo.
- `spring-cloud`: receberá os projetos Eureka e Gateway da equipe responsável; contém apenas `.gitkeep`.
- `microservices`: contém `product-composite-service`; receberá product, review e recommendation quando integrados.

As bibliotecas usam `java-library` e geram JARs comuns: não possuem classe `main`, servidor HTTP, `application.yml` nem `bootRun`. O `product-composite-service` é uma aplicação Spring MVC executável, incluída como subprojeto Gradle. Os outros serviços e o stack spring-cloud ainda não estão incorporados.

O módulo `api` usa Lombok nos DTOs `Product`, `ProductAggregate`, `RecommendationSummary`, `ReviewSummary` e `ServiceAddresses`. A versão (`1.18.48`) fica centralizada em `build.gradle`; `api/build.gradle` configura o annotation processor e as dependências `compileOnly`/de teste. Lombok gera os accessors e construtores durante a compilação do JAR, portanto os serviços que consomem `api` não precisam adicioná-lo como dependência de runtime.

## Documentação do workshop

- [STEP-1 — APIs, Eureka e Gateway](docs/STEP-1.md): contratos e tarefas das equipes.
- [STEP-2 — Bibliotecas compartilhadas](docs/STEP-2-LIBS.md): preparação da base, implementação de `util` e `api`, tratamento de erros e Swagger OpenAPI.
- [STEP-2 — Integração dos serviços](docs/STEP-2.md): integração dos núcleos, Docker e balanceamento de carga.
- [STEP-3 — Implementação do product-composite](docs/STEP-3-COMPOSITE.md): BFF com OpenFeign, orquestração, erros remotos e Swagger.

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

O build inclui `api`, `util` e `microservices:product-composite-service`. O composite precisa do Eureka e dos três serviços de núcleo em execução para completar as chamadas. Consulte o STEP-3 para iniciar e conferir a aplicação.

Saída real resumida dos dois primeiros comandos:

```text
$ ./gradlew --version
Gradle 8.14.3
Launcher JVM:  25.0.2 (Amazon.com Inc. 25.0.2+10-LTS)
Daemon JVM:    Compatible with Java 17, any vendor, nativeImageCapable=false (from gradle/gradle-daemon-jvm.properties)

$ ./gradlew projects
Root project 'workshop-microservices'
+--- Project ':api'
+--- Project ':microservices'
|    \--- Project ':microservices:product-composite-service'
\--- Project ':util'
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

Implemente as interfaces de `api` nos controllers (`ProductService`, `ReviewService`, `RecommendationService`, `ProductCompositeService`) sem repetir `@GetMapping`/`@PostMapping`/`@DeleteMapping`. As seções 4 e 5 do [STEP-2-LIBS](docs/STEP-2-LIBS.md) explicam contratos, springdoc e como documentar operações, respostas de erro, exemplos e schemas; o [STEP-3-COMPOSITE](docs/STEP-3-COMPOSITE.md) mostra o resultado no BFF.

### Endereços dos serviços agregados

O `product-composite-service` configura em `application.yml` os **IDs de serviço** `product`, `recommendation` e `review`, que devem coincidir com o `spring.application.name` registrado por cada núcleo no Eureka. Os Feign clients usam esses IDs; Eureka/LoadBalancer resolve a instância e faz o balanceamento. Não configure `localhost` nem portas individuais para esses serviços no composite: isso contornaria a descoberta e não funcionaria ao escalar instâncias ou executar em Docker.

A porta definida para o composite é `7000`. Se ela estiver ocupada, inicie-o com `SERVER_PORT=7100 ./gradlew :microservices:product-composite-service:bootRun` e use a porta alternativa nas URLs do Swagger e dos endpoints.

Essa escolha segue o exemplo do [Capítulo 11](https://github.com/PacktPublishing/Microservices-with-Spring-Boot-and-Spring-Cloud-Fourth-Edition/tree/main/Chapter11): a integração usa destinos lógicos como `http://product`, `http://recommendation` e `http://review`, e o cliente com balanceamento os resolve pelo Eureka. No nosso projeto, os nomes são externalizados em `app.services` e referenciados pelos `@FeignClient`. Para outro nome de aplicação, defina `PRODUCT_SERVICE_ID`, `RECOMMENDATION_SERVICE_ID` ou `REVIEW_SERVICE_ID`; para mudar o endereço do Eureka, use `EUREKA_URL`.

```yaml
app:
  services:
    product: ${PRODUCT_SERVICE_ID:product}
    recommendation: ${RECOMMENDATION_SERVICE_ID:recommendation}
    review: ${REVIEW_SERVICE_ID:review}
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
```

## Convenções para as próximas etapas

- Pacote raiz e grupo Gradle: `br.com.fatecararas`.
- Versões alinhadas ao roteiro: Spring Boot 3.0.4, Spring Cloud 2022.0.2, springdoc 2.0.2 e Lombok 1.18.48.
- As libs `api` e `util` já estão implementadas e versionadas; evite duplicar DTOs, interfaces, exceções ou o handler dentro dos serviços.
- O `product-composite-service` é implementado neste repositório. Incorpore os demais serviços nos respectivos diretórios e inclua-os em `settings.gradle` quando presentes.
- Use o Wrapper da raiz para todos os módulos.
