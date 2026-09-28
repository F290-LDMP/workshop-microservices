# STEP 2 — Preparação do monorepo + libs util e api + Swagger OpenAPI

> Laboratório de Desenvolvimento Multiplataforma — FATEC Antonio Brambilla

Esta é a primeira parte prática do [STEP-2](STEP-2.md). Partimos dos contratos do [STEP-1](STEP-1.md) para preparar o monorepo `workshop-microservices`, que receberá os projetos das cinco equipes. As instruções ficam em `docs/`, junto à base Gradle deste repositório.

> A estrutura inicial já está disponível na raiz. A seção 1 documenta como recriá-la; quem clonou esta base pode seguir para a implementação das libs nas seções 2 e 3. Execute os comandos Gradle na raiz do repositório, não dentro de `docs/`.

**Entrega desta etapa:** build Gradle multi-project, bibliotecas `util` e `api` implementadas e diretórios `spring-cloud` e `microservices` reservados para os alunos. Primeiro detalhamos `util`; depois, `api`. A integração real com OpenFeign, Docker e o balanceamento continuam no roteiro geral do STEP-2.

## 1. Organização e preparação do novo repositório

### 1.1 Responsabilidades das equipes

| Equipe | Destino no novo repositório | Responsabilidade |
|--------|----------------------------|------------------|
| 1 — product | `microservices/product-service` | Incorporar o serviço de produtos e consumir as libs |
| 2 — review | `microservices/review-service` | Incorporar o serviço de reviews e consumir as libs |
| 3 — recommendation | `microservices/recommendation-service` | Incorporar o serviço de recomendações e consumir as libs |
| 4 — composite | `microservices/product-composite-service` | Incorporar o composite e consumir os contratos dos núcleos |
| 5 — spring-cloud | `spring-cloud/eureka-server` e `spring-cloud/gateway` | Incorporar Discovery e Gateway |
| Todas, com orientação docente | `util`, `api` e build raiz | Estabilizar os contratos antes de integrar os serviços |

Aqui, **orquestrar os repositórios** significa reunir os códigos em um único repositório Git e coordenar sua compilação com Gradle. Isso ainda não inicia aplicações, bancos ou contêineres. O Gradle multi-project não sincroniza automaticamente alterações dos repositórios originais.

### 1.2 Versões para reproduzir o laboratório

| Ferramenta | Versão adotada |
|------------|----------------|
| Java | 17; compilação e testes usam a toolchain Java 17 |
| Gradle Wrapper | 8.14.3 |
| Spring Boot / BOM | 3.0.4 |
| Spring Cloud, quando os serviços forem incorporados | 2022.0.2 |
| springdoc-openapi | 2.0.2 |
| Swagger annotations Jakarta | 2.2.8 |

Boot 3.0.4 e springdoc 2.0.2 compõem a base de versões adotada neste roteiro. Fixamos as versões para a aula, sem propor uma atualização de plataforma. Todos devem alinhar os projetos a essa mesma base; se a turma já migrou, o conjunto Boot/Cloud/springdoc deve ser revisto em conjunto. Para Boot 3 use springdoc **2.x**, conforme a [documentação de compatibilidade](https://springdoc.org/v2/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).

Os quatro serviços de negócio usam **Spring MVC**, conforme o STEP-1. O Gateway continua com sua stack reativa. Não acrescente `spring-boot-starter-web` ao Gateway.

### 1.3 Estrutura inicial

```text
workshop-microservices/
├── .gitignore
├── README.md
├── docs/                      # roteiros do workshop
├── settings.gradle
├── build.gradle
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── util/
│   ├── build.gradle
│   └── src/main/java/br/com/fatecararas/util/
├── api/
│   ├── build.gradle
│   └── src/main/java/br/com/fatecararas/api/
├── spring-cloud/
│   └── .gitkeep
└── microservices/
    └── .gitkeep
```

Git não versiona diretórios vazios. Os `.gitkeep` são marcadores sem conteúdo; nesta entrega essas duas pastas **não contêm projetos**. As libs são JARs comuns, sem classe `main`, servidor HTTP ou `application.yml`.

Para recriar a base do zero em outro local, execute:

```bash
mkdir workshop-microservices
cd workshop-microservices
git init
mkdir -p util/src/main/java/br/com/fatecararas/util/http
mkdir -p util/src/main/java/br/com/fatecararas/util/openapi
mkdir -p api/src/main/java/br/com/fatecararas/api/exceptions
mkdir -p api/src/main/java/br/com/fatecararas/api/core/product
mkdir -p api/src/main/java/br/com/fatecararas/api/core/recommendation
mkdir -p api/src/main/java/br/com/fatecararas/api/core/review
mkdir -p api/src/main/java/br/com/fatecararas/api/composite/product
mkdir -p spring-cloud microservices
touch spring-cloud/.gitkeep microservices/.gitkeep
```

Todos os caminhos de arquivos e comandos seguintes são relativos à **raiz do monorepo**, um nível acima de `docs/`.

**Arquivo: `.gitignore`**

```gitignore
.gradle/
**/build/
.idea/
*.iml
.DS_Store
.env
```

**Arquivo: `settings.gradle`**

```groovy
rootProject.name = 'workshop-microservices'

include 'util', 'api'

// Os projetos dos alunos serão incluídos explicitamente quando forem copiados.
// Não inclua caminhos de serviços que ainda não existem.
```

**Arquivo: `build.gradle`**

```groovy
plugins {
    id 'base'
    id 'org.springframework.boot' version '3.0.4' apply false
    id 'io.spring.dependency-management' version '1.1.0' apply false
}

ext {
    springBootVersion = '3.0.4'
    springCloudVersion = '2022.0.2'
    springdocVersion = '2.0.2'
    swaggerAnnotationsVersion = '2.2.8'
}

allprojects {
    group = 'br.com.fatecararas'
    version = '1.0.0-SNAPSHOT'
    repositories { mavenCentral() }
}

subprojects {
    plugins.withId('java') {
        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(17)
            }
        }
        tasks.withType(JavaCompile).configureEach {
            options.encoding = 'UTF-8'
            options.release = 17
            options.compilerArgs += '-parameters'
        }
        tasks.withType(Test).configureEach { useJUnitPlatform() }
    }
}
```

### 1.4 Gradle Wrapper e primeiro commit

Além da toolchain de compilação, fixe a JVM que executa o Gradle para evitar que um Java mais recente no terminal cause erros como `Unsupported class file major version 69`.

**Arquivo: `gradle/gradle-daemon-jvm.properties`**

```properties
toolchainVersion=17
```

O Gradle selecionará um JDK 17 instalado localmente. A máquina deve ter esse JDK disponível; esta base não configura download automático de Java. Para gerar o Wrapper pela primeira vez, execute o Gradle com JDK 17 selecionado no terminal.


Depois de criar os arquivos Gradle das duas libs nas seções 2 e 3, gere o Wrapper a partir de uma instalação local do Gradle 8.14.3:

```bash
gradle wrapper --gradle-version 8.14.3 --distribution-type bin
chmod +x gradlew
./gradlew --version
./gradlew projects
```

Se não tiver Gradle instalado, copie `gradlew`, `gradlew.bat` e o diretório `gradle/wrapper` de um projeto da turma com Wrapper compatível com seu JDK. Execute esse Wrapper para gerar a versão acordada: `./gradlew wrapper --gradle-version 8.14.3 --distribution-type bin`. O JAR do Wrapper deve ser versionado; não basta copiar o arquivo `.properties`.

Ao final das implementações e verificações deste roteiro, escreva um `README.md` no novo repositório com: versões, comando `./gradlew clean build`, descrição de `util`/`api`, destinos das equipes e aviso de que os serviços ainda não foram incorporados. Então:

```bash
git add .gitignore README.md settings.gradle build.gradle gradlew gradlew.bat gradle util api spring-cloud microservices
git commit -m "Cria base do monorepo e bibliotecas compartilhadas"
```

Crie um repositório remoto vazio chamado `workshop-microservices` na conta/organização da turma. Substitua `<URL_DO_NOVO_REPOSITORIO>` pela URL desse repositório antes de executar:

```bash
git branch -M main
git remote add origin <URL_DO_NOVO_REPOSITORIO>
git push -u origin main
```

## 2. Primeiro: biblioteca util

### 2.1 Responsabilidade e dependências

A `util` concentra a identificação da instância, o formato de erro, o tratamento global das exceções e a configuração comum do OpenAPI.

```text
util/
├── build.gradle
└── src/main/java/br/com/fatecararas/util/
    ├── http/
    │   ├── ServiceUtil.java
    │   ├── HttpErrorInfo.java
    │   └── GlobalControllerExceptionHandler.java
    └── openapi/
        └── OpenApiConfiguration.java
```

**Ordem de leitura não é ordem de compilação:** `util` depende de `api` porque trata `NotFoundException` e `InvalidInputException`. Essas classes serão criadas na seção 3. Até lá, os imports estarão pendentes. O Gradle compilará `api` antes de `util` automaticamente.

```text
serviços MVC → util → api
serviços MVC → api
api          → Spring Web + anotações Swagger
```

A `api` não depende de `util`. Para documentar o erro sem formar um ciclo, as interfaces referenciam o schema `HttpErrorInfo` pelo nome; a configuração em `util` registra esse schema no OpenAPI de cada serviço.

**Arquivo: `util/build.gradle`**

```groovy
plugins { id 'java-library' }

dependencies {
    api platform("org.springframework.boot:spring-boot-dependencies:${rootProject.springBootVersion}")
    api project(':api')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation "org.springdoc:springdoc-openapi-starter-common:${rootProject.springdocVersion}"
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

Não aplique o plugin Spring Boot às libs: elas geram `jar`, não `bootJar`. O plugin `java-library` permite exportar dependências usadas nos contratos públicos por meio da configuração `api`.

### 2.2 ServiceUtil — endereço real da instância

Padronizamos o pacote `br.com.fatecararas.util.http`. Os grupos que usaram `dev.sdras.utils.http` no STEP-1 devem atualizar os imports ao incorporar o serviço. A porta vem do servidor iniciado, inclusive com `server.port=0`. Ignoramos eventos de um contexto filho de gerenciamento para não substituir a porta da aplicação pela porta do Actuator.

**Arquivo: `util/src/main/java/br/com/fatecararas/util/http/ServiceUtil.java`**

```java
package br.com.fatecararas.util.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ServiceUtil implements ApplicationListener<WebServerInitializedEvent> {
    private final ApplicationContext applicationContext;
    private volatile Integer serverPort;
    private volatile String serverIp;

    public ServiceUtil(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        if (event.getApplicationContext() != applicationContext) {
            return;
        }
        serverPort = event.getWebServer().getPort();
        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            serverIp = "unknown";
        }
    }

    public Integer getServerPort() { return serverPort; }
    public String getServerIp() { return serverIp; }

    public String getServerAddress() {
        if (serverPort == null) {
            throw new IllegalStateException("Servidor HTTP ainda não iniciado");
        }
        String host = serverIp.contains(":") ? "[" + serverIp + "]" : serverIp;
        return "http://" + host + ":" + serverPort;
    }
}
```

Use `getServerAddress()` ao montar a resposta da requisição, depois da inicialização. Esse endereço identifica a instância no laboratório; não é a URL pública do Gateway. A biblioteca não precisa de Eureka para funcionar.

### 2.3 HttpErrorInfo — contrato JSON dos erros

Preservamos exatamente os cinco campos do STEP-1. `status` é numérico, `error` contém a descrição HTTP e `timestamp` usa formato ISO-8601 com fuso. O construtor vazio e os setters também permitem desserializar o erro recebido de outro serviço na futura integração.

**Arquivo: `util/src/main/java/br/com/fatecararas/util/http/HttpErrorInfo.java`**

```java
package br.com.fatecararas.util.http;

import java.time.ZonedDateTime;
import org.springframework.http.HttpStatus;

public class HttpErrorInfo {
    private ZonedDateTime timestamp;
    private String path;
    private int status;
    private String error;
    private String message;

    public HttpErrorInfo() { }

    public HttpErrorInfo(HttpStatus status, String path, String message) {
        this.timestamp = ZonedDateTime.now();
        this.path = path;
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
    }

    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
```

### 2.4 GlobalControllerExceptionHandler — tratamento comum

O advice atua nos controllers MVC da aplicação que o importar. Ele transforma exceções de negócio em 404/422 e erros de leitura da requisição em 400. As implementações continuam responsáveis por lançar as exceções de negócio.

**Arquivo: `util/src/main/java/br/com/fatecararas/util/http/GlobalControllerExceptionHandler.java`**

```java
package br.com.fatecararas.util.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.api.exceptions.NotFoundException;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<HttpErrorInfo> notFound(
            NotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, request, ex.getMessage());
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<HttpErrorInfo> invalidInput(
            InvalidInputException ex, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<HttpErrorInfo> badRequest(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, request,
                "Requisição inválida: confira o JSON e os parâmetros obrigatórios");
    }

    private ResponseEntity<HttpErrorInfo> error(
            HttpStatus status, HttpServletRequest request, String message) {
        return ResponseEntity.status(status)
                .body(new HttpErrorInfo(status, request.getRequestURI(), message));
    }
}
```

| Situação | Exemplo | Resultado |
|----------|---------|-----------|
| Produto inexistente | implementação lança `NotFoundException` | 404 + `HttpErrorInfo` |
| Regra de negócio inválida | `productId < 1`, duplicidade ou `rate` fora de 0–5 | 422 + `HttpErrorInfo` |
| Parâmetro não numérico | `GET /product/abc` | 400 + `HttpErrorInfo` |
| Parâmetro obrigatório ausente | `GET /review` | 400 + `HttpErrorInfo` |
| Corpo JSON malformado | `POST /product` com `{` | 400 + `HttpErrorInfo` |
| Lista sem resultados | reviews/recommendations de produto sem registros | 200 + `[]` |

Não transforme indiscriminadamente `Exception` em 422: falhas inesperadas continuam sendo erros de servidor. Este advice padroniza os casos acima; erros de infraestrutura, filtros e rotas inexistentes não passam necessariamente por ele. Violações de unicidade do banco devem ser traduzidas pela implementação para `InvalidInputException`.

### 2.5 OpenApiConfiguration — metadados e schema de erro

A configuração é compartilhada, mas cada aplicação gera sua própria documentação. O título usa `spring.application.name`. `ModelConverters` registra o schema real de `HttpErrorInfo` para resolver as referências usadas nas interfaces.

**Arquivo: `util/src/main/java/br/com/fatecararas/util/openapi/OpenApiConfiguration.java`**

```java
package br.com.fatecararas.util.openapi;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import br.com.fatecararas.util.http.HttpErrorInfo;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    public OpenAPI workshopOpenApi(
            @Value("${spring.application.name:workshop-microservices}") String applicationName) {
        Components components = new Components();
        ModelConverters.getInstance().read(HttpErrorInfo.class)
                .forEach(components::addSchemas);
        return new OpenAPI()
                .info(new Info().title(applicationName + " API")
                        .version("1.0.0")
                        .description("Contratos do workshop de microservices — FATEC"))
                .components(components);
    }
}
```

## 3. Depois: biblioteca api

### 3.1 Estrutura e responsabilidade

A `api` reúne os DTOs de entrada/saída, as exceções de negócio e as interfaces HTTP que os quatro serviços implementam. Não contém repositories, controllers concretos, acesso a banco nem clientes Feign.

```text
api/
├── build.gradle
└── src/main/java/br/com/fatecararas/api/
    ├── exceptions/
    │   ├── NotFoundException.java
    │   └── InvalidInputException.java
    ├── core/
    │   ├── product/
    │   │   ├── Product.java
    │   │   └── ProductService.java
    │   ├── recommendation/
    │   │   ├── Recommendation.java
    │   │   └── RecommendationService.java
    │   └── review/
    │       ├── Review.java
    │       └── ReviewService.java
    └── composite/product/
        ├── ProductAggregate.java
        ├── RecommendationSummary.java
        ├── ReviewSummary.java
        ├── ServiceAddresses.java
        └── ProductCompositeService.java
```

**Arquivo: `api/build.gradle`**

```groovy
plugins { id 'java-library' }

dependencies {
    api platform("org.springframework.boot:spring-boot-dependencies:${rootProject.springBootVersion}")
    api 'org.springframework:spring-web'
    api "io.swagger.core.v3:swagger-annotations-jakarta:${rootProject.swaggerAnnotationsVersion}"
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

`spring-web` fornece as anotações dos contratos; não inicia um servidor. As dependências declaradas com `api` ficam disponíveis para quem implementa essas interfaces. Não há dependência de `util` aqui.

### 3.2 Exceções de negócio

Cada classe pública deve ficar em seu próprio arquivo. Depois de criar estas duas classes, os imports pendentes do handler estarão resolvidos.

**Arquivo: `api/src/main/java/br/com/fatecararas/api/exceptions/NotFoundException.java`**

```java
package br.com.fatecararas.api.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException() { super(); }
    public NotFoundException(String message) { super(message); }
    public NotFoundException(String message, Throwable cause) { super(message, cause); }
}
```

**Arquivo: `api/src/main/java/br/com/fatecararas/api/exceptions/InvalidInputException.java`**

```java
package br.com.fatecararas.api.exceptions;

public class InvalidInputException extends RuntimeException {
    public InvalidInputException() { super(); }
    public InvalidInputException(String message) { super(message); }
    public InvalidInputException(String message, Throwable cause) { super(message, cause); }
}
```

### 3.3 Entidades e DTOs: o que deve ser compartilhado

Os objetos `Product`, `Review` e `Recommendation` representam os dados expostos pela API. As **entidades de persistência permanecem nos serviços proprietários**, como definido no STEP-1. Assim o composite não passa a depender de MongoDB/JPA para consumir um produto.

| Objeto | Local | Observação |
|--------|-------|------------|
| `Product`, `Recommendation`, `Review` | `api/core/...` | DTOs compartilhados |
| Agregado, resumos e endereços | `api/composite/product` | DTOs compartilhados |
| `ProductEntity` | `microservices/product-service` | MongoDB; `id`, `version`, `productId`, `name`, `weight` |
| `RecommendationEntity` | `microservices/recommendation-service` | MongoDB; mapear `rating` da entidade para `rate` do DTO |
| `ReviewEntity` | `microservices/review-service` | JPA/MySQL; índices e versão ficam no serviço |
| Repositories e mappers | Respectivo serviço | Não entram nas libs |

### 3.4 DTOs dos serviços de núcleo

Os exemplos usam classes Java com construtor vazio, construtor completo, getters e setters, preservando o estilo de acesso `getProductId()` usado nas implementações. Não é necessário Lombok. As anotações `@Schema` documentam o modelo; **não validam regras de negócio**.

Os campos de endereço são somente de saída no Swagger. A implementação deve sempre calculá-los na instância que respondeu, sem confiar em um endereço enviado pelo cliente.

**Arquivo: `api/src/main/java/br/com/fatecararas/api/core/product/Product.java`**

```java
package br.com.fatecararas.api.core.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Produto")
public class Product {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    private String name;
    private int weight;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String serviceAddress;

    public Product() { }

    public Product(int productId, String name, int weight, String serviceAddress) {
        this.productId = productId;
        this.name = name;
        this.weight = weight;
        this.serviceAddress = serviceAddress;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
    public String getServiceAddress() { return serviceAddress; }
    public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }
}
```

**Arquivo: `api/src/main/java/br/com/fatecararas/api/core/recommendation/Recommendation.java`**

```java
package br.com.fatecararas.api.core.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Recomendação")
public class Recommendation {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    private int recommendationId;
    private String author;
    @Schema(description = "Nota de 0 a 5", example = "4", minimum = "0", maximum = "5")
    private int rate;
    private String content;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String serviceAddress;

    public Recommendation() { }

    public Recommendation(int productId, int recommendationId, String author, int rate, String content, String serviceAddress) {
        this.productId = productId;
        this.recommendationId = recommendationId;
        this.author = author;
        this.rate = rate;
        this.content = content;
        this.serviceAddress = serviceAddress;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getRecommendationId() { return recommendationId; }
    public void setRecommendationId(int recommendationId) { this.recommendationId = recommendationId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getRate() { return rate; }
    public void setRate(int rate) { this.rate = rate; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getServiceAddress() { return serviceAddress; }
    public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }
}
```

**Arquivo: `api/src/main/java/br/com/fatecararas/api/core/review/Review.java`**

```java
package br.com.fatecararas.api.core.review;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Review")
public class Review {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    private int reviewId;
    private String author;
    private String subject;
    private String content;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String serviceAddress;

    public Review() { }

    public Review(int productId, int reviewId, String author, String subject, String content, String serviceAddress) {
        this.productId = productId;
        this.reviewId = reviewId;
        this.author = author;
        this.subject = subject;
        this.content = content;
        this.serviceAddress = serviceAddress;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getServiceAddress() { return serviceAddress; }
    public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }
}
```

### 3.5 DTOs do composite

`RecommendationSummary` e `ReviewSummary` omitem o `productId`, já presente no agregado, e o endereço individual. `ServiceAddresses` mantém os nomes JSON `cmp`, `pro`, `rev` e `rec` do STEP-1. Quando não houver resultados, o composite deve preencher as listas com `List.of()`, produzindo `[]` no JSON.

**Arquivo: `api/src/main/java/br/com/fatecararas/api/composite/product/RecommendationSummary.java`**

```java
package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de recomendação")
public class RecommendationSummary {
    private int recommendationId;
    private String author;
    @Schema(description = "Nota de 0 a 5", example = "4", minimum = "0", maximum = "5")
    private int rate;
    private String content;

    public RecommendationSummary() { }

    public RecommendationSummary(int recommendationId, String author, int rate, String content) {
        this.recommendationId = recommendationId;
        this.author = author;
        this.rate = rate;
        this.content = content;
    }

    public int getRecommendationId() { return recommendationId; }
    public void setRecommendationId(int recommendationId) { this.recommendationId = recommendationId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getRate() { return rate; }
    public void setRate(int rate) { this.rate = rate; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

**Arquivo: `api/src/main/java/br/com/fatecararas/api/composite/product/ReviewSummary.java`**

```java
package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de review")
public class ReviewSummary {
    private int reviewId;
    private String author;
    private String subject;
    private String content;

    public ReviewSummary() { }

    public ReviewSummary(int reviewId, String author, String subject, String content) {
        this.reviewId = reviewId;
        this.author = author;
        this.subject = subject;
        this.content = content;
    }

    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

**Arquivo: `api/src/main/java/br/com/fatecararas/api/composite/product/ServiceAddresses.java`**

```java
package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Instâncias que atenderam à consulta")
public class ServiceAddresses {
    private String cmp;
    private String pro;
    private String rev;
    private String rec;

    public ServiceAddresses() { }

    public ServiceAddresses(String cmp, String pro, String rev, String rec) {
        this.cmp = cmp;
        this.pro = pro;
        this.rev = rev;
        this.rec = rec;
    }

    public String getCmp() { return cmp; }
    public void setCmp(String cmp) { this.cmp = cmp; }
    public String getPro() { return pro; }
    public void setPro(String pro) { this.pro = pro; }
    public String getRev() { return rev; }
    public void setRev(String rev) { this.rev = rev; }
    public String getRec() { return rec; }
    public void setRec(String rec) { this.rec = rec; }
}
```

**Arquivo: `api/src/main/java/br/com/fatecararas/api/composite/product/ProductAggregate.java`**

```java
package br.com.fatecararas.api.composite.product;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Produto com recomendações e reviews")
public class ProductAggregate {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    private String name;
    private int weight;
    private List<RecommendationSummary> recommendations;
    private List<ReviewSummary> reviews;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private ServiceAddresses serviceAddresses;

    public ProductAggregate() { }

    public ProductAggregate(int productId, String name, int weight, List<RecommendationSummary> recommendations, List<ReviewSummary> reviews, ServiceAddresses serviceAddresses) {
        this.productId = productId;
        this.name = name;
        this.weight = weight;
        this.recommendations = recommendations;
        this.reviews = reviews;
        this.serviceAddresses = serviceAddresses;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
    public List<RecommendationSummary> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationSummary> recommendations) { this.recommendations = recommendations; }
    public List<ReviewSummary> getReviews() { return reviews; }
    public void setReviews(List<ReviewSummary> reviews) { this.reviews = reviews; }
    public ServiceAddresses getServiceAddresses() { return serviceAddresses; }
    public void setServiceAddresses(ServiceAddresses serviceAddresses) { this.serviceAddresses = serviceAddresses; }
}
```

### 3.6 Interfaces REST com Swagger OpenAPI

Os mapeamentos ficam nos **métodos da interface**, com nomes explícitos em `@PathVariable` e `@RequestParam`. Os controllers concretos implementarão essas interfaces. `@Operation`, `@Tag` e `@ApiResponse` enriquecem a documentação gerada pelo springdoc.

Para padronizar a integração, adotamos os seguintes códigos de sucesso nesta etapa:

| Operação | Núcleos | Composite |
|----------|---------|-----------|
| GET | 200 com objeto ou lista | 200 com agregado |
| POST | 200 com DTO criado | 202 sem corpo, mantendo a convenção do STEP-1 |
| DELETE | 200 sem corpo | 202 sem corpo, mantendo a convenção do STEP-1 |

`@ResponseStatus` fixa o comportamento do composite; `@ApiResponse` apenas o documenta. O 202, por si só, não implementa mensageria. Erros 400/422 aparecem em todos os contratos; 404 só na consulta de produto/composite. DELETE é idempotente: ausência de registro não gera 404. Ajuste as implementações dos alunos a essas convenções antes de integrá-las.

A resposta de sucesso de GET/POST com retorno é inferida pelo springdoc a partir do tipo Java, inclusive `List<Review>` e `List<Recommendation>`. Respostas sem corpo usam `@Content` vazio. Os erros apontam para `#/components/schemas/HttpErrorInfo`, registrado pela `OpenApiConfiguration`.

**Arquivo: `api/src/main/java/br/com/fatecararas/api/core/product/ProductService.java`**

```java
package br.com.fatecararas.api.core.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "product", description = "Operações de product")
public interface ProductService {
    @Operation(summary = "Criar product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @PostMapping(value = "/product", consumes = "application/json", produces = "application/json")
    Product createProduct(@RequestBody Product body);

    @Operation(summary = "Consultar product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @GetMapping(value = "/product/{productId}", produces = "application/json")
    Product getProduct(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @PathVariable("productId") int productId);

    @Operation(summary = "Excluir product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída", content = @Content),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @DeleteMapping(value = "/product/{productId}")
    void deleteProduct(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @PathVariable("productId") int productId);
}
```

**Arquivo: `api/src/main/java/br/com/fatecararas/api/core/recommendation/RecommendationService.java`**

```java
package br.com.fatecararas.api.core.recommendation;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "recommendation", description = "Operações de recommendation")
public interface RecommendationService {
    @Operation(summary = "Criar recommendation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @PostMapping(value = "/recommendation", consumes = "application/json", produces = "application/json")
    Recommendation createRecommendation(@RequestBody Recommendation body);

    @Operation(summary = "Consultar recommendation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @GetMapping(value = "/recommendation", produces = "application/json")
    List<Recommendation> getRecommendations(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @RequestParam("productId") int productId);

    @Operation(summary = "Excluir recommendation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída", content = @Content),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @DeleteMapping(value = "/recommendation")
    void deleteRecommendations(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @RequestParam("productId") int productId);
}
```

**Arquivo: `api/src/main/java/br/com/fatecararas/api/core/review/ReviewService.java`**

```java
package br.com.fatecararas.api.core.review;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "review", description = "Operações de review")
public interface ReviewService {
    @Operation(summary = "Criar review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @PostMapping(value = "/review", consumes = "application/json", produces = "application/json")
    Review createReview(@RequestBody Review body);

    @Operation(summary = "Consultar review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @GetMapping(value = "/review", produces = "application/json")
    List<Review> getReviews(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @RequestParam("productId") int productId);

    @Operation(summary = "Excluir review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída", content = @Content),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @DeleteMapping(value = "/review")
    void deleteReviews(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @RequestParam("productId") int productId);
}
```

**Arquivo: `api/src/main/java/br/com/fatecararas/api/composite/product/ProductCompositeService.java`**

```java
package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "product-composite", description = "Operações de product-composite")
public interface ProductCompositeService {
    @Operation(summary = "Criar product-composite")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Solicitação aceita", content = @Content),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/product-composite", consumes = "application/json")
    void createProduct(@RequestBody ProductAggregate body);

    @Operation(summary = "Consultar product-composite")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @GetMapping(value = "/product-composite/{productId}", produces = "application/json")
    ProductAggregate getProduct(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @PathVariable("productId") int productId);

    @Operation(summary = "Excluir product-composite")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Solicitação aceita", content = @Content),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(value = "/product-composite/{productId}")
    void deleteProduct(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @PathVariable("productId") int productId);
}
```

## 4. Incorporar os projetos — atividade posterior dos alunos

> **Pare antes desta seção ao entregar apenas a base.** `spring-cloud` e `microservices` continuam contendo somente `.gitkeep`. As instruções seguintes são executadas por cada equipe quando seus projetos forem integrados.

### 4.1 Transferência dos repositórios

1. Cada equipe conclui o aceite do STEP-1 e registra o commit/tag de entrega.
2. Em um clone temporário do repositório da equipe, use `git archive` para exportar apenas os arquivos versionados. Registre a URL e o hash de origem no README do monorepo.
3. Extraia a exportação no destino correspondente. Para o grupo 5, extraia a estrutura que contém `eureka-server` e `gateway` dentro de `spring-cloud`, sem criar `spring-cloud/spring-cloud`.
4. Confira os arquivos antes de fazer commit no monorepo. Não importe `.git`, caches, saídas de build ou credenciais.

Exemplo para a equipe product; substitua os caminhos e a referência da entrega:

```bash
# Execute no clone do repositório product-service; HEAD pode ser uma tag acordada.
git archive --format=tar --output=/tmp/product-service-entrega.tar HEAD

# Execute agora na raiz do novo monorepo.
mkdir -p microservices/product-service
tar -xf /tmp/product-service-entrega.tar -C microservices/product-service
```

Esse procedimento importa um snapshot, sem o histórico Git original. A partir da integração, alterações destinadas ao trabalho integrado são entregues em branches/PRs do monorepo. Não clone repositórios aninhados nem use submódulos neste roteiro. Atualizações posteriores dos repositórios originais exigem uma nova integração explícita.

### 4.2 Incluir os projetos no Gradle

Acrescente cada `include` **somente quando o respectivo diretório contiver seu `build.gradle`**. Quando as cinco equipes tiverem concluído a transferência, o `settings.gradle` terá:

```groovy
rootProject.name = 'workshop-microservices'
include 'util', 'api'
include 'microservices:product-service'
include 'microservices:review-service'
include 'microservices:recommendation-service'
include 'microservices:product-composite-service'
include 'spring-cloud:eureka-server'
include 'spring-cloud:gateway'
```

Remova da cópia importada os `settings.gradle`/`settings.gradle.kts` e Wrappers próprios dos subprojetos; use sempre o Wrapper da raiz. Converta um eventual `build.gradle.kts` para a configuração Groovy adotada pela turma, evitando dois arquivos de build no mesmo módulo. Mantenha `src`, recursos, testes e dependências específicas de cada serviço.

### 4.3 Dependências dos quatro serviços de negócio

No `build.gradle` de cada serviço MVC, mantenha os starters de persistência apropriados e ajuste os plugins/dependências comuns conforme este modelo. Os plugins sem versão usam as versões declaradas no build raiz. Preserve configurações específicas de compilação e testes que o projeto já utiliza.

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${rootProject.springCloudVersion}"
    }
}

dependencies {
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${rootProject.springdocVersion}"
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // Preserve aqui o starter MongoDB ou JPA + driver MySQL do serviço.
}
```

O starter `webmvc-ui` pertence às **aplicações**: ele expõe o JSON OpenAPI e a interface Swagger. As libs sozinhas não disponibilizam `/v3/api-docs` nem `/swagger-ui.html`. Não adicione `util` ao Gateway: o starter MVC e o handler Servlet não se aplicam ao Gateway reativo. Eureka e Gateway mantêm as dependências do grupo 5 e as versões Boot/Cloud alinhadas à raiz.

### 4.4 Registrar os componentes compartilhados

O component scan padrão de uma aplicação em `br.com.fatecararas.microservices...` não encontra automaticamente `br.com.fatecararas.util...`. Em cada uma das quatro classes de aplicação, mantenha seu pacote/nome atual e acrescente estes imports e a anotação `@Import`:

```java
import org.springframework.context.annotation.Import;
import br.com.fatecararas.util.http.GlobalControllerExceptionHandler;
import br.com.fatecararas.util.http.ServiceUtil;
import br.com.fatecararas.util.openapi.OpenApiConfiguration;

// Coloque junto de @SpringBootApplication na classe já existente:
@Import({ServiceUtil.class, GlobalControllerExceptionHandler.class, OpenApiConfiguration.class})
```

Use apenas essa estratégia de registro; não acrescente também um scan amplo de `br.com.fatecararas` nem mantenha configurações duplicadas de OpenAPI/advice. Remova dos serviços as cópias locais de DTOs, interfaces, exceções, `HttpErrorInfo`, handler e `ServiceUtil`, atualizando os imports para as libs. Classes duplicadas com o mesmo pacote/nome podem causar comportamento dependente da ordem do classpath.

### 4.5 Implementar os contratos nos controllers existentes

Cada controller recebe `@RestController` e implementa a interface correspondente:

| Serviço | Interface |
|---------|-----------|
| product | `ProductService` |
| review | `ReviewService` |
| recommendation | `RecommendationService` |
| composite | `ProductCompositeService` |

Exemplo de **trecho a adaptar** no controller de product já criado pela equipe (não é um arquivo completo):

```java
@RestController
public class ProductServiceImpl implements ProductService {
    // Preserve o construtor, repository e mapper que a equipe já implementou.

    @Override
    public Product getProduct(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        ProductEntity entity = repository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException(
                        "No product found for productId: " + productId));
        return new Product(entity.getProductId(), entity.getName(),
                entity.getWeight(), serviceUtil.getServerAddress());
    }

    // Implemente também createProduct e deleteProduct conforme o STEP-1.
}
```

Neste trecho, `repository.findByProductId` retorna `Optional<ProductEntity>` e `serviceUtil` é injetado pelo construtor. Adapte ao repository/mapper concreto da equipe. Não repita `@GetMapping`/`@PostMapping`/`@DeleteMapping` no controller; os métodos herdam os mapeamentos da interface. Remova prefixos `@RequestMapping` antigos que duplicariam o caminho.

As validações continuam no serviço: identificadores positivos; `rate` de 0 a 5; duplicidade traduzida em 422. Reviews/recommendations sem registros retornam lista vazia. O composite permanece com o mock do STEP-1 nesta etapa; sua implementação real é o próximo trabalho.

O futuro client `@FeignClient` fica no composite, não na lib. A herança simples de contratos é descrita na [documentação do OpenFeign](https://docs.spring.io/spring-cloud-openfeign/docs/4.0.6/reference/html/#spring-cloud-feign-inheritance). O advice trata as exceções locais; a integração precisará traduzir os erros HTTP remotos para as exceções do contrato.

## 5. Swagger OpenAPI em execução

### 5.1 Configuração em cada serviço MVC

Mescle o bloco abaixo com o `application.yml` existente, preservando `server.port`, `spring.application.name`, banco e Eureka. Não crie chaves YAML duplicadas.

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
  override-with-generic-response: false
```

Desativamos a inclusão automática de respostas genéricas do advice para que o Swagger apresente os códigos explicitamente declarados em cada operação — por exemplo, sem adicionar 404 a toda consulta de lista. O JSON OpenAPI continua refletindo os schemas dos DTOs e o schema de erro registrado.

### 5.2 Endereços para conferência

| Serviço | Swagger UI | OpenAPI JSON |
|---------|------------|--------------|
| product | `http://localhost:7001/swagger-ui.html` | `http://localhost:7001/v3/api-docs` |
| recommendation | `http://localhost:7002/swagger-ui.html` | `http://localhost:7002/v3/api-docs` |
| review | `http://localhost:7003/swagger-ui.html` | `http://localhost:7003/v3/api-docs` |
| composite | `http://localhost:7000/swagger-ui.html` | `http://localhost:7000/v3/api-docs` |

O acesso inicial é direto ao serviço, com as portas do STEP-1. Se usar porta aleatória, consulte a porta efetiva no log. As rotas de negócio do Gateway não publicam automaticamente os recursos de Swagger; a agregação das documentações no Gateway é uma tarefa posterior.

## 6. Roteiro de verificação

### 6.1 Antes de receber os projetos dos alunos

Na raiz do novo monorepo, com as seções 1–3 concluídas:

```bash
./gradlew projects
./gradlew clean :api:build :util:build
./gradlew :util:dependencies --configuration compileClasspath
```

Esperado: apenas `api` e `util` como subprojetos; compilação sem ciclo; JARs em `api/build/libs` e `util/build/libs`; nenhuma exigência de `mainClass`, banco ou Eureka. Como o roteiro ainda não adicionou testes às libs, um resultado `test NO-SOURCE` **não comprova o comportamento HTTP**; essa verificação ocorre depois da incorporação dos controllers.

### 6.2 Depois que os serviços forem incorporados

Execute o build completo, prepare os bancos conforme os READMEs das equipes e inicie Eureka e os serviços em terminais separados:

```bash
./gradlew clean build
./gradlew :spring-cloud:eureka-server:bootRun
./gradlew :microservices:product-service:bootRun
# Outros terminais: recommendation-service, review-service e product-composite-service.
```

Confirme as operações documentadas e a resolução do schema de erro (requer `jq`):

```bash
curl -fsS localhost:7001/v3/api-docs | jq '.paths | keys'
curl -fsS localhost:7001/v3/api-docs | jq '.components.schemas.HttpErrorInfo'
curl -fsS localhost:7001/v3/api-docs | jq '.paths["/product/{productId}"].get.responses'
curl -fsS localhost:7002/v3/api-docs | jq '.paths["/recommendation"].get.responses["200"]'
```

Esperado: caminhos da aplicação atual, schema de erro com os cinco campos, GET de produto documentando 200/400/404/422 e GET de recommendation com retorno de lista. Abra também o Swagger UI e confira se ele carrega sem erro de resolução de `$ref`.

Roteiro HTTP para product (utilize um `productId` reservado para o teste):

```bash
curl -i -X POST localhost:7001/product   -H 'Content-Type: application/json'   -d '{"productId":900001,"name":"Produto do workshop","weight":100}'
# 200 + Product; serviceAddress preenchido pelo servidor.

curl -i localhost:7001/product/900001       # 200
curl -i localhost:7001/product/0            # 422 + HttpErrorInfo
curl -i localhost:7001/product/abc          # 400 + HttpErrorInfo
curl -i -X POST localhost:7001/product   -H 'Content-Type: application/json' -d '{' # 400 + HttpErrorInfo
curl -i -X DELETE localhost:7001/product/900001 # 200 sem corpo
curl -i localhost:7001/product/900001       # 404 + HttpErrorInfo
curl -i localhost:7003/review              # 400: productId ausente
curl -i 'localhost:7003/review?productId=900001' # 200 + [] se não há reviews
curl -i 'localhost:7002/recommendation?productId=900001' # 200 + [] se não há recomendações
```

Repita criação, consulta e exclusão para review/recommendation usando os payloads do STEP-1; confira duplicidade e `rate` inválido. No composite mockado, valide 1 → 200, 13 → 404 e 0 → 422, além de POST/DELETE → 202 sem corpo. Em todos os erros tratados, confira corpo e status HTTP, não apenas a descrição no Swagger.

### 6.3 Problemas comuns

| Sintoma | Conferência |
|---------|-------------|
| `Project with path ':api' could not be found` | Executar o Wrapper da raiz e conferir `settings.gradle` |
| `mainClass` não encontrada na lib | Remover o plugin Spring Boot de `api`/`util` |
| `NotFoundException` ainda não existe | Concluir a seção 3 antes do build de `util` |
| Erro com corpo padrão do Spring em vez de `HttpErrorInfo` | Verificar `@Import`, exceção lançada e remover advice local duplicado |
| Swagger sem operações | Controller concreto deve implementar a interface e estar no scan da aplicação |
| Schema de erro não resolvido | Importar `OpenApiConfiguration`; conferir `.components.schemas.HttpErrorInfo` |
| Campos duplicados ou getters incompatíveis | Remover DTOs locais e adotar as classes da lib |
| Gateway passa a iniciar como MVC | Remover starter-web e dependência de `util` do Gateway |

## 7. Critérios de aceite

**Base compartilhada — entrega atual:**

- [ ] Monorepo `workshop-microservices` com material de instruções em `docs/`.
- [ ] Gradle Wrapper versionado e build centralizado.
- [ ] Apenas `api` e `util` incluídos no build inicial.
- [ ] `spring-cloud` e `microservices` com somente `.gitkeep`, sem implementações fornecidas.
- [ ] `util` com `ServiceUtil`, `HttpErrorInfo`, handler MVC e configuração OpenAPI.
- [ ] `api` com duas exceções, sete DTOs e quatro interfaces completas e documentadas.
- [ ] Contratos preservam campos, tipos, nomes de métodos e endpoints do STEP-1.
- [ ] Nenhuma entidade JPA/MongoDB ou repository nas libs.
- [ ] Build das duas libs concluído e README com instruções de uso.

**Integração — entrega posterior de cada equipe:**

- [ ] Projeto colocado no destino acordado e incluído no Gradle raiz.
- [ ] Origem da entrega (URL e commit/tag) registrada no README.
- [ ] Cópias locais dos contratos/utilitários removidas e imports atualizados.
- [ ] Controllers implementando as interfaces e regras de erro do contrato.
- [ ] Swagger UI e JSON OpenAPI disponíveis nos quatro serviços MVC.
- [ ] Schema `HttpErrorInfo` resolvido e respostas HTTP conferidas.
- [ ] Eureka/Gateway preservam suas stacks e configurações do STEP-1.

## 8. Referências do workshop

- [README](../README.md): estrutura da base, pré-requisitos, comandos de verificação e convenções comuns.
- [STEP-1](STEP-1.md): contratos, entidades de persistência, tarefas por equipe e critérios de aceite.
- [STEP-2](STEP-2.md): arquitetura alvo, integração com OpenFeign, Docker e balanceamento de carga.

Este roteiro desenvolve os contratos definidos no STEP-1: usa MVC/Servlet no handler, mantém contratos síncronos, documenta também POST/DELETE e captura a porta por `WebServerInitializedEvent`. A implementação dos serviços e a orquestração em runtime permanecem sob responsabilidade das equipes, nas etapas seguintes.
