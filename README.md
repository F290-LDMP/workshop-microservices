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
├── util/
│   ├── build.gradle
│   └── src/main/java/br/com/fatecararas/util/.gitkeep
├── api/
│   ├── build.gradle
│   └── src/main/java/br/com/fatecararas/api/.gitkeep
├── spring-cloud/.gitkeep
└── microservices/.gitkeep
```

- `util`: diretório reservado aos utilitários compartilhados e ao tratamento de erros; depende de `api`.
- `api`: diretório reservado aos DTOs, às interfaces e às exceções dos contratos.
- `spring-cloud`: receberá os projetos Eureka e Gateway da equipe responsável.
- `microservices`: receberá product, review, recommendation e composite.

Os arquivos `.gitkeep` permitem versionar os diretórios ainda vazios. Esta base não contém classes Java, aplicações ou testes. As bibliotecas usam `java-library` e geram JARs comuns; não possuem `bootRun`.

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

O build inicial inclui somente `api` e `util`. Tarefas de compilação e testes podem indicar `NO-SOURCE`, pois o código será implementado durante o workshop. Um build bem-sucedido nesta fase valida a estrutura, não o comportamento dos futuros serviços.

## Convenções para as próximas etapas

- Pacote raiz e grupo Gradle: `br.com.fatecararas`.
- Versões alinhadas ao roteiro: Spring Boot 3.0.4, Spring Cloud 2022.0.2 e springdoc 2.0.2.
- Implemente as libs nos caminhos já preparados.
- Incorpore os serviços nos respectivos diretórios e inclua-os explicitamente em `settings.gradle` somente quando seus projetos estiverem presentes.
- Use o Wrapper da raiz para todos os módulos.
