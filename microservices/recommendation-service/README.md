# Recommendation Service

## Objetivo
O `recommendation-service` expõe a API de recomendações do cenário de microserviços, armazenando registros em MongoDB e registrando-se no Eureka como o serviço `recommendation`.

## Grupo e integrantes
- Grupo: 3
- Integrantes: [preencher]

## Tecnologias
- Java 17
- Spring Boot 3.2.x
- Spring Cloud 2023.0.x
- Spring Data MongoDB
- Netflix Eureka Client
- Springdoc OpenAPI

## Requisitos
- JDK 17 instalado
- MongoDB acessível em `mongodb://localhost:27017`
- Eureka disponível em `http://localhost:8761/eureka/`

## Variáveis de ambiente
- `MONGODB_URI`: conexão do MongoDB, padrão `mongodb://localhost:27017/recommendation-db`
- `EUREKA_URL`: URL do Eureka, padrão `http://localhost:8761/eureka/`

## Porta
- `7002`

## Como executar
1. Inicie o MongoDB local.
2. Inicie o Eureka.
3. Execute:

```powershell
./gradlew.bat :microservices:recommendation-service:bootRun
```

## Swagger
- UI: `http://localhost:7002/swagger-ui/index.html`
- OpenAPI: `http://localhost:7002/v3/api-docs`

## Testes
```powershell
./gradlew.bat :microservices:recommendation-service:test
```

## Evidências executadas
Até o momento, o projeto foi configurado para compilar no monorepo e a estrutura de Gradle foi integrada com os módulos compartilhados. O restante da validação depende da presença de MongoDB e Eureka em execução localmente.
