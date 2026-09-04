# Pré-requisitos para o Ambiente de Desenvolvimento

Este arquivo detalha todo o software e as configurações necessárias na sua máquina local para que o projeto **Sistema de Venda de Ingressos** funcione corretamente.

---

## 1. Ferramentas de Containerização

A nossa arquitetura é totalmente baseada em contêineres, o que simplifica a configuração do ambiente. Você precisará do Docker e do Docker Compose.

- **Docker:** É a plataforma de contêineres que nos permite empacotar e rodar as aplicações em ambientes isolados.
- **Docker Compose:** É a ferramenta usada para orquestrar múltiplos contêineres, como os nossos vários microsserviços, bancos de dados e outras ferramentas.

### Como Instalar

- **Windows/macOS:** A maneira mais fácil é instalar o [**Docker Desktop**](https://www.docker.com/products/docker-desktop/), que já inclui o Docker Engine, o Docker CLI e o Docker Compose.

### Como Verificar a Instalação

Abra seu terminal e execute os seguintes comandos:

```sh
docker --version
# Exemplo de saída: Docker version 24.0.5, build 24.0.5-0ubuntu1~22.04.1

docker-compose --version
# Exemplo de saída: Docker Compose version v2.21.0
```

---

## 2. Ambiente de Desenvolvimento Java

Os microsserviços do backend são construídos com Java e Maven.

- **Java Development Kit (JDK):** O projeto está configurado para usar **Java 17**.
- **Maven:** É a nossa ferramenta de build e gerenciamento de dependências para os projetos Java.

### Como Instalar

- **Java 17:** Recomendamos o uso de um gerenciador de versões Java como o [SDKMAN!](https://sdkman.io/) (para Linux/macOS) ou baixar diretamente do [site da Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) ou de uma distribuição como a [Eclipse Temurin (Adoptium)](https://adoptium.net/).
- **Maven:** Pode ser instalado via gerenciadores de pacote (como `apt`, `brew`) ou seguindo o [guia de instalação oficial](https://maven.apache.org/install.html).

### Como Verificar a Instalação

```sh
java -version
# Exemplo de saída: openjdk version "17.0.8" 2023-07-18

mvn -version
# Exemplo de saída: Apache Maven 3.8.1
```

> **Importante:** Certifique-se de que as variáveis de ambiente `JAVA_HOME` e `M2_HOME` (ou `MAVEN_HOME`) estão configuradas corretamente e que os executáveis `java` e `mvn` estão no `PATH` do seu sistema.

---

## 3. Ambiente de Desenvolvimento Frontend

O frontend é uma Single Page Application (SPA) e requer o Node.js e o npm.

- **Node.js:** É o ambiente de execução JavaScript que nos permite rodar a aplicação React localmente.
- **npm (Node Package Manager):** Vem junto com o Node.js e é usado para gerenciar as dependências do frontend.

### Como Instalar

- Recomendamos usar um gerenciador de versões como o [**nvm** (Node Version Manager)](https://github.com/nvm-sh/nvm) para instalar o Node.js. Isso facilita a troca entre diferentes versões do Node.js. A versão **18.x (LTS)** é recomendada.
- Alternativamente, baixe diretamente do [site oficial do Node.js](https://nodejs.org/).

### Como Verificar a Instalação

```sh
node -v
# Exemplo de saída: v18.17.1

npm -v
# Exemplo de saída: 9.6.7
```

---

## 4. Verificação de Portas de Rede

O `docker-compose` irá expor várias portas na sua máquina local. Certifique-se de que as seguintes portas **não estão em uso** por outras aplicações:

- `3000`: Frontend React
- `8080`: API Gateway
- `8761`: Discovery Service (Eureka)
- `15672` e `5672`: RabbitMQ
- `9090`: Prometheus
- `3001`: Grafana
- `8081` a `8086`: Microsserviços Java
