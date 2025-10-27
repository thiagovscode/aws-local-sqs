# SQS + LocalStack — Guia de Comandos (AWS CLI)

## Sumário

* [Pré-requisitos](#pré-requisitos)
* [Subindo o LocalStack](#subindo-o-localstack)
* [Configurando o AWS CLI para o LocalStack](#configurando-o-aws-cli-para-o-localstack)
* [Filas usadas neste projeto](#filas-usadas-neste-projeto)
* [Criar filas](#criar-filas)
* [Descobrir URL da fila](#descobrir-url-da-fila)
* [Listar filas](#listar-filas)
* [Enviar mensagens](#enviar-mensagens)
* [Receber e apagar mensagens](#receber-e-apagar-mensagens)
* [Purgar filas](#purgar-filas)
* [Apagar filas](#apagar-filas)
* [Rodar a aplicação com profile `local`](#rodar-a-aplicação-com-profile-local)
* [Dicas e solução de problemas](#dicas-e-solução-de-problemas)

---

## Pré-requisitos

* **Docker** (para rodar o LocalStack)
* **AWS CLI** (v2 recomendado)
* **Java 17** e **Maven** (para rodar a app)
* (Opcional) **awslocal** (`pip install awscli-local`) — wrapper que injeta `--endpoint-url` automaticamente

---

## Subindo o LocalStack

```bash
docker run -d --name localstack \
  -p 4566:4566 -p 4571:4571 \
  -e SERVICES=sqs \
  -e EDGE_PORT=4566 \
  localstack/localstack:latest
```

> Porta padrão do gateway: `http://localhost:4566`

---

## Configurando o AWS CLI para o LocalStack

### Opção A — usar `awslocal`

Todos os comandos SQS ficam assim:

```bash
awslocal sqs <subcomando> ...
```

### Opção B — usar `aws` com `--endpoint-url`

```bash
aws --endpoint-url http://localhost:4566 sqs <subcomando> ...
```

### (Opcional) Profile dedicado

```bash
aws configure --profile localstack
# Access Key ID: test
# Secret Access Key: test
# Region: us-east-1
# Output: json
```

Depois:

```bash
aws --profile localstack --endpoint-url http://localhost:4566 sqs <subcomando> ...
```

> O LocalStack usa conta fake **`000000000000`**.

---

## Filas usadas neste projeto

* **Entrada:** `pizza-order`
* **Saída:** `pizza-order-out`

---

## Criar filas

Com **awslocal**:

```bash
awslocal sqs create-queue --queue-name pizza-order
awslocal sqs create-queue --queue-name pizza-order-out
```

Com **aws**:

```bash
aws --endpoint-url http://localhost:4566 sqs create-queue --queue-name pizza-order
aws --endpoint-url http://localhost:4566 sqs create-queue --queue-name pizza-order-out
```

---

## Descobrir URL da fila

```bash
awslocal sqs get-queue-url --queue-name pizza-order
# ou
aws --endpoint-url http://localhost:4566 sqs get-queue-url --queue-name pizza-order
```

Formato esperado:

```
http://localhost:4566/000000000000/pizza-order
```

---

## Listar filas

```bash
awslocal sqs list-queues
# ou
aws --endpoint-url http://localhost:4566 sqs list-queues
```

---

## Enviar mensagens

### JSON (recomendado — compatível com desserialização automática)

**Bash / Git Bash / Linux / macOS:**

```bash
aws --endpoint-url http://localhost:4566 sqs send-message \
  --queue-url http://localhost:4566/000000000000/pizza-order \
  --message-body '{"id":"123","placaOuChassi":"ABC1D23","produto":"pizza"}'
```

**PowerShell (Windows):**

```powershell
aws --endpoint-url http://localhost:4566 sqs send-message `
  --queue-url http://localhost:4566/000000000000/pizza-order `
  --message-body "{\"id\":\"123\",\"placaOuChassi\":\"ABC1D23\",\"produto\":\"pizza\"}"
```

### Texto livre (não recomendado, apenas se o listener tratar)

```bash
aws --endpoint-url http://localhost:4566 sqs send-message \
  --queue-url http://localhost:4566/000000000000/pizza-order \
  --message-body "id: 123\nplaca ou chassi: ABC1D23\nproduto: pizza"
```

---

## Receber e apagar mensagens

### Receber (long poll 10s)

```bash
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/pizza-order-out \
  --wait-time-seconds 10 --max-number-of-messages 10
```

### Apagar uma mensagem (usa o `ReceiptHandle` retornado no receive)

```bash
awslocal sqs delete-message \
  --queue-url http://localhost:4566/000000000000/pizza-order-out \
  --receipt-handle "<RECEIPT_HANDLE_AQUI>"
```

---

## Purgar filas

> **Purge** remove **todas** as mensagens **visíveis e invisíveis** (em processamento). Útil quando mensagens inválidas estão causando retrys.

```bash
awslocal sqs purge-queue \
  --queue-url http://localhost:4566/000000000000/pizza-order

awslocal sqs purge-queue \
  --queue-url http://localhost:4566/000000000000/pizza-order-out
```

---

## Apagar filas

```bash
awslocal sqs delete-queue --queue-url http://localhost:4566/000000000000/pizza-order
awslocal sqs delete-queue --queue-url http://localhost:4566/000000000000/pizza-order-out
```

---

## Rodar a aplicação com profile `local`

### Via Maven (sem testes)

```bash
mvn clean package -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Rodando o JAR (atenção à ordem dos argumentos)

**PowerShell / CMD:**

```powershell
java -Dspring.profiles.active=local -jar "target\sqs-app-0.0.1-SNAPSHOT.jar"
# ou
java -jar "target\sqs-app-0.0.1-SNAPSHOT.jar" --spring.profiles.active=local
```

**Bash / Linux / macOS / Git Bash:**

```bash
java -Dspring.profiles.active=local -jar target/sqs-app-0.0.1-SNAPSHOT.jar
```

> Logs iniciais devem mostrar: `The following 1 profile is active: "local"`.

---

## Dicas e solução de problemas

* **JSON inválido** ao consumir:

    * Erro típico: `Unrecognized token 'Pedido'…`
    * Envie **JSON válido** (ex.: `{"id":"123",...}`) ou use listener que recebe `String` e **faz parse manual**.
    * Faça **purge** da fila para limpar mensagens antigas ruins:

      ```bash
      awslocal sqs purge-queue --queue-url http://localhost:4566/000000000000/pizza-order
      ```

* **Caminho do JAR no Windows/Git Bash**:

    * No Git Bash, use **barra normal** `/` (não `\`) ou **aspas** no caminho:

      ```bash
      java -Dspring.profiles.active=local -jar target/sqs-app-0.0.1-SNAPSHOT.jar
      ```

      ou

      ```powershell
      java -Dspring.profiles.active=local -jar "target\sqs-app-0.0.1-SNAPSHOT.jar"
      ```

* **Dois `WebClient` beans** (auth/api):

    * Use `@Qualifier("authClient")` e `@Qualifier("apiClient")` no `ExternalApiClient`.
    * Opcional: marque `apiClient` como `@Primary`.

* **Dois `SqsAsyncClient` beans**:

    * Separe por perfil: `@Profile("local")` para LocalStack, `@Profile("!local")` para AWS real.
    * Ou `@Primary`/`@Qualifier`.

* **URLs rápidas (padrão LocalStack)**:

    * `http://localhost:4566/000000000000/<queue-name>`

---

### Comandos essenciais (cola na marra 🧾)

```bash
# criar filas
awslocal sqs create-queue --queue-name pizza-order
awslocal sqs create-queue --queue-name pizza-order-out

# enviar JSON válido (Bash)
aws --endpoint-url http://localhost:4566 sqs send-message \
  --queue-url http://localhost:4566/000000000000/pizza-order \
  --message-body '{"id":"123","placaOuChassi":"ABC1D23","produto":"pizza"}'

# purgar
awslocal sqs purge-queue --queue-url http://localhost:4566/000000000000/pizza-order

# receber da saída
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/pizza-order-out \
  --wait-time-seconds 10 --max-number-of-messages 10
```

---

