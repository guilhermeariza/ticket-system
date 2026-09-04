
# Como Executar o Projeto

Este guia fornece o passo a passo para clonar, construir e executar a arquitetura completa do **Sistema de Venda de Ingressos**.

> **Atenção:** Antes de prosseguir, certifique-se de que você atende a todos os itens listados no arquivo `PREREQUISITOS.md`.

---

### Passo 1: Clonar o Repositório

Primeiro, clone o código-fonte do projeto do repositório Git para a sua máquina local.

```sh
git clone <URL_DO_REPOSITORIO>
cd ticket-system
```

---

### Passo 2: Construir os Microsserviços Java

Antes de criar as imagens Docker, precisamos compilar o código Java e empacotar cada microsserviço em um arquivo `.jar`. O Maven fará isso por nós.

Na raiz do projeto (`ticket-system`), execute o comando:

```sh
mvn clean install
```

**O que este comando faz?**
- `clean`: Remove os artefatos de builds anteriores (como a pasta `target`).
- `install`: Compila o código-fonte, executa os testes e empacota o resultado em um arquivo `.jar` dentro da pasta `target` de cada microsserviço.

> **Nota:** A primeira execução deste comando pode demorar vários minutos, pois o Maven precisa baixar todas as dependências do projeto (as "libs") da internet.

---

### Passo 3: Construir as Imagens e Iniciar os Contêineres

Com os arquivos `.jar` gerados, o Docker Compose pode agora construir as imagens de cada serviço e iniciar todos os contêineres definidos no arquivo `docker-compose.yml`.

```sh
docker-compose up --build -d
```

**O que este comando faz?**
- `up`: Inicia os contêineres.
- `--build`: Força a reconstrução das imagens Docker. É importante usar esta flag sempre que houver uma alteração no código-fonte (seja no backend ou no frontend).
- `-d` (detached mode): Executa os contêineres em segundo plano, liberando o seu terminal.

---

### Passo 4: Verificar se Tudo está Funcionando

Após alguns instantes (pode levar de 1 a 2 minutos para todos os serviços Spring Boot iniciarem e se registrarem no Eureka), você pode verificar o status de todos os contêineres.

```sh
docker-compose ps
```

Você deverá ver uma lista de todos os serviços com o status `Up` ou `running`. Se algum serviço estiver com o status `Exit` ou `restarting`, você pode investigar o problema olhando os logs do contêiner específico:

```sh
# Exemplo para ver os logs do servico-pedidos
docker-compose logs -f servico-pedidos
```

---

### Passo 5: Acessar as Aplicações

Com tudo em execução, você pode acessar as diferentes partes do sistema pelo seu navegador:

- **Aplicação Principal (Frontend):** [http://localhost:3000](http://localhost:3000)
- **API Gateway (ponto de entrada para APIs):** [http://localhost:8080](http://localhost:8080)
- **Service Discovery (Eureka):** [http://localhost:8761](http://localhost:8761)
- **Fila de Mensagens (RabbitMQ):** [http://localhost:15672](http://localhost:15672) (login: `guest` / `guest`)
- **Métricas (Prometheus):** [http://localhost:9090](http://localhost:9090)
- **Dashboards (Grafana):** [http://localhost:3001](http://localhost:3001) (login: `admin` / `password`)

---

### Passo 6: Como Parar o Ambiente

Para parar e remover todos os contêineres, redes e volumes criados pelo Docker Compose, execute o seguinte comando na raiz do projeto:

```sh
docker-compose down
```

Se você quiser apenas parar os contêineres sem removê-los, use `docker-compose stop`.
