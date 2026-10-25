# Aula prática — Feign nas consultas e RabbitMQ nas escritas

## Objetivo e duração

Roteiro para aproximadamente 45–60 minutos, partindo da aula anterior de OpenFeign. Ao final, o aluno deve conseguir explicar quem chama quem em uma consulta e em uma escrita, observar comandos acumulados no RabbitMQ e distinguir aceitação pelo broker de processamento pelo consumidor.

**Mensagem de abertura:** “O Feign continua resolvendo as consultas HTTP. Para criar ou excluir, o composite agora envia comandos ao RabbitMQ; cada serviço processa sua própria fila.”

| Operação | Caminho na estrutura atual | Resultado |
|---|---|---|
| GET | Insomnia → Gateway → composite → Feign → três núcleos → bancos | 200 com agregado; 404 se produto ausente |
| POST | Insomnia → Gateway → composite → RabbitTemplate → RabbitMQ → listeners → bancos | 202 após confirmação da publicação |
| DELETE | Insomnia → Gateway → composite → RabbitMQ → três listeners | 202; exclusão acontece depois |

Feign e RabbitMQ são usados em caminhos diferentes da mesma aplicação. O Feign não é o produtor AMQP. Se um núcleo HTTP estiver desligado, a consulta agregada pode falhar; o composite atual não devolve automaticamente um agregado parcial quando uma chamada Feign falha.

## 1. Preparação antes da aula — 10 minutos

Na raiz do repositório, com JDK 17 e Docker iniciados:

```bash
# Apenas se ainda não tiver .env; preserve um arquivo já configurado.
cp .env.example .env
./gradlew build
docker compose up --build -d --wait
docker compose ps
python3 scripts/smoke-messaging.py
```

O build Gradle gera os JARs antes do build Docker. Cada aplicação tem seu próprio `Dockerfile`; o Compose usa o diretório do módulo como contexto e copia seu JAR de `build/libs`. As bibliotecas compartilhadas já estão empacotadas no JAR executável.

O smoke deve terminar com `OK`. Ele usa IDs aleatórios e exclui o produto criado. Health checks positivos não garantem que todos os clientes Eureka já atualizaram seus registros; aguarde a descoberta antes de começar a apresentação.

Deixe abertas três janelas:

1. **Insomnia**, com a collection desta aula.
2. **RabbitMQ Management**, em http://localhost:15672, na aba **Queues and Streams**, vhost `/`.
3. **Terminal**, acompanhando os logs:

```bash
docker compose logs -f --tail=30 product-composite-service product-service review-service recommendation-service
```

Outros endereços: Eureka http://localhost:8761 e Swagger do composite http://localhost:7100/swagger-ui.html. Confirme PRODUCT-COMPOSITE, PRODUCT, REVIEW e RECOMMENDATION como UP no Eureka.

As portas 7001/7002/7003 dos núcleos são internas ao Docker. O Insomnia usa o Gateway em 8080; não cadastre URLs `localhost:7001` para este Compose. Apenas trocar o ambiente do Insomnia não publica portas nem cria rotas novas no Gateway.

## 2. Importar a collection

Arquivo: [insomnia/workshop-feign-rabbitmq.json](insomnia/workshop-feign-rabbitmq.json).

No Insomnia, escolha **Import → File**, selecione o JSON e abra **Workshop — Feign + RabbitMQ**. O arquivo utiliza o formato Insomnia JSON v4, ainda aceito pelo importador atual. Não havia uma collection do workshop versionada neste repositório; este arquivo passa a ser a referência para a aula.

Confira o **Base Environment**:

| Variável | Padrão | Uso |
|---|---|---|
| `base_url` | `http://localhost:8080` | Gateway, requisições da aplicação |
| `composite_url` | `http://localhost:7100` | Health do composite |
| `eureka_url` | `http://localhost:8761` | Registro das aplicações |
| `rabbit_url` | `http://localhost:15672` | API HTTP administrativa do RabbitMQ |
| `rabbit_user` / `rabbit_password` | `workshop` / `workshop` | Basic Auth apenas nas requisições administrativas |
| `product_id` | `31001` | Produto principal |
| `backlog_id` | `31002` | Consumidor desligado |
| `broker_id` | `31003` | Broker indisponível |
| `missing_id` | `1999999000` | Produto que deve permanecer inexistente |

Se você alterou as credenciais no `.env`, altere as duas variáveis do Rabbit no Insomnia. A collection não lê o `.env` automaticamente. Use IDs novos ou execute a limpeza da pasta 07 antes de repetir a aula.

As requisições têm testes em **Scripts → After-response**. Envie-as manualmente na ordem do roteiro. **Não execute a collection inteira em lote:** algumas requisições dependem de desligar/religar processos no terminal. 03.1 e 05.0 guardam as contagens iniciais das DLQs no ambiente para verificar o aumento, evitando confundir mensagens antigas com o teste atual. Os testes de consulta completa e contadores não têm polling automático; reenvie após alguns segundos quando necessário.

## 3. Retomar a aula de Feign — 5 minutos

Antes das chamadas, mostre estes pontos no código:

| Onde abrir | O que mostrar |
|---|---|
| `composite/client/ProductClient.java`, `ReviewClient.java`, `RecommendationClient.java` | `@FeignClient` usa os IDs registrados no Eureka |
| `ProductCompositeIntegrationService.get()` | Três consultas HTTP em paralelo; montagem de `ProductAggregate` |
| `ProductCompositeIntegrationService.create()` / `delete()` | Escritas agora usam `EventPublisher`, sem chamadas Feign de escrita |
| `util/messaging/EventPublisher.java` e `RabbitConfiguration.java` | RabbitTemplate, exchange, routing keys, confirmações e filas |
| `ReviewMessageListener.java` e `ReviewOperations.java` | Listener recebe comando e reutiliza a lógica de negócio |

Os três primeiros itens ficam em `microservices/product-composite-service/src/main/java/br/com/fatecararas/`; o suporte Rabbit fica em `util/src/main/java/br/com/fatecararas/util/messaging`. O listener e as operações ficam no pacote do respectivo núcleo.

Na pasta **00 — Preparação**, envie 00.1 a 00.5. Mostre as seis filas e anote a contagem inicial das DLQs. É normal haver mensagens de experiências anteriores; compare a variação das contagens e inspecione o payload.

### Mapa do Rabbit para explicar no painel

A exchange direct `products.commands` usa a routing key para escolher a fila:

| Routing key | Fila do consumidor | Fila de erro |
|---|---|---|
| `product` | `product.commands` | `product.dlq` |
| `recommendation` | `recommendation.commands` | `recommendation.dlq` |
| `review` | `review.commands` | `review.dlq` |

A exchange `products.dlx` encaminha rejeições para a DLQ correspondente. Cada núcleo consome somente sua fila de comandos.

- **Ready:** mensagens esperando entrega.
- **Unacked:** mensagens entregues cujo processamento ainda não foi confirmado.
- **Total:** Ready + Unacked.
- **Publish / Deliver / Ack:** taxas de publicação, entrega e confirmação. São taxas, portanto podem voltar a zero quando o movimento termina.

## 4. Criar pelo Rabbit e consultar por Feign — 10 minutos

Na pasta **01 — Rabbit escreve, Feign consulta**:

1. Envie **01.1**. Esperado: **202, sem corpo**. Há uma mensagem para product, uma para recommendation e uma para review.
2. Envie **01.2** imediatamente. Pode retornar **404** ou **200**, inclusive com coleções ainda incompletas. Se retornar tudo completo na primeira tentativa, o consumo apenas foi rápido.
3. Reenvie **01.3** até retornar **200** e os testes confirmarem uma review e uma recommendation.
4. Mostre `serviceAddresses` no JSON: as informações foram obtidas dos núcleos pela integração HTTP.
5. Envie **01.4** e compare com o painel RabbitMQ. Com consumidores ativos, Ready/Unacked geralmente voltam a zero rapidamente.
6. Envie **01.5** e **01.6**: repetir o mesmo conteúdo não duplica os itens.

**Evidência nos logs:** `Published`, seguido de `Consuming` e `Processed`, com `eventId`, `correlationId` e `productId`. Uma requisição produz mensagens com a mesma correlação e IDs de evento distintos. O GET não produz novos comandos.

**Pergunta à turma:** “Se recebi 202, posso afirmar que o produto e suas avaliações já estão no banco?” Resposta esperada: não; o broker confirmou a publicação, o consumo pode acontecer depois.

## 5. Tornar a fila visível: desligar e retomar o consumidor — 10 minutos

Este é o cenário principal para mostrar o Rabbit reagindo. Não dependa de capturar um gráfico que desaparece rapidamente.

```bash
docker compose stop review-service
```

Na pasta **02 — Consumidor desligado e retomada**:

1. Envie **02.1**. Esperado: **202**, mesmo com review-service desligado.
2. Envie **02.2**. Aguarde alguns segundos pelas estatísticas: `consumers = 0` e `messages_ready >= 1` em `review.commands`. Product e recommendation continuam processando suas próprias mensagens.
3. Abra a fila `review.commands` no Management. Mostre **Ready**, **Unacked**, **Total** e a ausência de consumidor.
4. Envie **02.3** para examinar o envelope. O modo `ack_requeue_true` devolve a mensagem à fila. Confira `eventType: CREATE`, `productId: 31002`, `data`, `eventId` e `correlationId`.

Se houver mais de dez mensagens antigas, aumente `count` no corpo da requisição de inspeção para localizar o evento atual; preserve `ackmode: ack_requeue_true`.

**Explique:** consultar uma mensagem desta forma não é uma observação inteiramente passiva: pode marcá-la como redelivered e mudar sua posição. A mensagem não é apagada pela requisição fornecida.

O GET agregado enquanto review-service estiver parado pode falhar ou aguardar timeout. Não use esse GET como prova de leitura parcial: a implementação exige sucesso das três chamadas HTTP.

Religue o consumidor:

```bash
docker compose start review-service
```

5. Reenvie **02.4** até `consumers >= 1`, Ready = 0 e Unacked = 0.
6. Reenvie **02.5** até o agregado ficar completo. Se necessário, aguarde a atualização do Eureka/LoadBalancer, que pode levar dezenas de segundos.

**Pergunta à turma:** “Quem entregou a review depois que o serviço voltou?” Resposta: o RabbitMQ manteve o comando na fila; o listener consumiu quando reconectou.

## 6. Erro de negócio posterior ao 202: DLQ — 7 minutos

Requer o produto do cenário 01 criado com o nome original.

Na pasta **03 — Conflito e DLQ**:

1. Envie **03.1** e anote a contagem atual de `product.dlq`.
2. Envie **03.2**. Ele usa o mesmo `product_id`, mas muda o nome para `Nome conflitante`. Esperado: **202**.
3. Reenvie **03.3** após alguns segundos. A contagem da DLQ deve aumentar.
4. Envie **03.4** (reenvie se necessário) e encontre o payload com o ID da aula e o nome conflitante. Mostre também `properties.headers.x-death`.
5. Envie **03.5**. Esperado: **200** com `Produto da aula`; o registro original foi preservado.

O broker aceitou a mensagem, mas o consumidor encontrou dados diferentes para a mesma chave. O erro permanente vai à DLQ sem novas tentativas. A resposta HTTP já terminou; ela não pode ser alterada retroativamente para 422. Não existe transação distribuída entre as três filas.

Na pasta **04 — Validação antes do Rabbit**, envie 04.1: nota 6 produz **422 antes de publicar**. Compare com o conflito assíncrono. 04.2 demonstra ID inválido e 04.3 demonstra 404 devolvido pela leitura Feign.

## 7. Extras, se houver tempo

### Mensagem inválida — 3 minutos

Na pasta **05 — Mensagem malformada**, envie **05.0** para registrar a contagem inicial, depois **05.1** para publicar. Reenvie **05.2** até a contagem aumentar e **05.3** para encontrar o payload inválido. A primeira usa a API HTTP **administrativa** do RabbitMQ para publicar um payload que não é JSON, com routing key `review`. HTTP 200 e `routed: true` indicam roteamento; o listener rejeita o conteúdo e ele aparece em `review.dlq`.

Esta publicação HTTP é uma ferramenta de demonstração. O projeto publica normalmente por AMQP na porta 5672, usando `RabbitTemplate`.

### Broker desligado: erro na publicação — 5 minutos

Faça este cenário antes da limpeza, mantendo o produto principal existente e todos os núcleos ativos:

```bash
docker compose stop rabbitmq
```

Envie **06.1**: esperado **503** com `HttpErrorInfo`, após a tentativa de conexão/confirmação. Os health checks que incluem Rabbit podem indicar DOWN durante a experiência; isso não significa que o processo HTTP foi encerrado.

O GET continua implementado com Feign, mas disponibilidade e descoberta também podem afetá-lo. No ensaio, foi observado 500 com `No servers available for service: review` durante a atualização do registro após reiniciar o núcleo. Não use uma leitura durante a falha do broker como garantia de isolamento total da estrutura atual.

Restaure o broker ao terminar:

```bash
docker compose start rabbitmq
```

Aguarde a reconexão, confira os núcleos no Eureka e reenvie **06.2** até obter **200** com o agregado completo. Depois prossiga para a limpeza. Não prometa rollback para uma falha no meio de várias publicações: parte dos comandos pode ter sido aceita antes do erro.

### Banco indisponível e retries — opcional, mais lento

```bash
docker compose stop mysql
```

Crie um agregado com um novo ID e uma review. A publicação pode ser aceita, enquanto o consumidor de review encontra o banco indisponível. Existem três tentativas totais com intervalos de 1 e 2 segundos, **além do tempo gasto nas tentativas de conexão SQL**. Esse cenário pode levar bem mais de três segundos; prefira o conflito de negócio como demonstração principal de DLQ.

```bash
docker compose start mysql
```

Religar o banco não retira mensagens já enviadas à DLQ. Elas precisam ser investigadas e republicadas depois de corrigir a causa.

## 8. Encerrar e preparar a repetição

Restaure os serviços de qualquer experiência antes da limpeza:

```bash
docker compose start rabbitmq mysql review-service
```

Envie **07.1**, **07.2** e **07.3**, todos com **202**. Aguarde **07.4** retornar **404** e confira as filas com **07.5**. Para confirmar que os itens do produto principal também foram removidos, execute os comandos abaixo; troque `31001` se alterou o ambiente:

```bash
docker compose exec -T review-service curl -fsS 'http://localhost:7003/review?productId=31001'
docker compose exec -T recommendation-service curl -fsS 'http://localhost:7002/recommendation?productId=31001'
```

Esperado: `[]` em ambos. Repita com `31002` para o cenário de backlog. DELETE dos produtos não limpa DLQs; as mensagens de erro permanecem disponíveis para análise. Não é necessário apagar volumes para repetir a aula: limpe os produtos ou selecione novos IDs e registre a contagem inicial das DLQs.

## 9. Diagnóstico rápido durante a apresentação

| Sintoma | Verificação e ação |
|---|---|
| Connection refused em 8080 | Docker ativo, `docker compose ps`; porta publicada no Gateway |
| 503 logo após iniciar | Confira Eureka e aguarde os clientes atualizarem a descoberta |
| 401 na API Rabbit | Credenciais do Base Environment devem coincidir com `.env` e com o usuário já existente no volume Rabbit |
| Fila sem estatísticas atualizadas | Aguarde alguns segundos e envie GET novamente; 202 não implica backlog visível com consumidor ativo |
| GET falha após desligar review | Esperado: Feign depende do núcleo HTTP; religue o serviço para consultar o agregado completo |
| DLQ já tinha mensagens | Compare a contagem antes/depois e inspecione o ID e conteúdo da mensagem da aula |
| Produto não aparece após POST | Confira filas, logs e DLQs; 202 não confirma gravação. Verifique conflitos de IDs usados anteriormente |
| Consumidor religado, GET ainda falha | Aguarde Eureka/LoadBalancer e confira health/logs do serviço; se aparecer `No servers available for service: review`, confirme o registro de REVIEW no Eureka e aguarde sua atualização |
| Comando vai à DLQ e banco já voltou | DLQ não tem replay automático; investigue antes de republicar |

**Plano de contingência para uma aula curta:** faça preparação, cenário 01, cenário 02 e a pergunta sobre o significado do 202. O cenário 02 mantém a mensagem parada de forma previsível, permitindo mostrar o envelope com calma.

## Referências de ferramentas

- [Importação e formato JSON v4 do Insomnia](https://developer.konghq.com/insomnia/import-export/).
- [Scripts e testes after-response no Insomnia](https://developer.konghq.com/insomnia/scripts/).
- [Referência da API HTTP administrativa do RabbitMQ](https://www.rabbitmq.com/docs/http-api-reference).
- [STEP-4: implementação, garantias e limites](STEP-4-RABBITMQ.md).
