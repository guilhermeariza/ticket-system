# Próximos Passos - Deploy do Sistema de Tickets

## Status Atual da Sessão

### ✅ Completado com Sucesso

#### Testes (185/185 passando)
- **auth-service**: 13/13 testes ✅
  - 5 service tests
  - 8 controller tests
- **users-service**: 37/37 testes ✅
  - 13 service tests
  - 14 controller tests
  - 10 integration tests
- **servico-eventos**: 26/26 testes ✅
  - 13 service tests
  - 13 integration tests
- **servico-pedidos**: 39/39 testes ✅
  - 13 service tests
  - 13 controller tests
  - 13 integration tests
- **payments-service**: 36/36 testes ✅
  - 13 service tests
  - 13 controller tests
  - 10 integration tests
- **notifications-service**: 34/34 testes ✅
  - 14 EmailService tests
  - 10 NotificationListener tests
  - 10 integration tests

#### Build e Docker
- ✅ Todos os pacotes Maven construídos com sucesso
- ✅ Todas as imagens Docker criadas
- ✅ Containers iniciados com docker-compose

#### Infraestrutura (10/10 serviços funcionando)
- ✅ discovery-service (Eureka) - Porta 8761
- ✅ config-server - Porta 8888
- ✅ api-gateway - Porta 8080
- ✅ auth-db (PostgreSQL) - Healthy
- ✅ users-db (PostgreSQL) - Healthy
- ✅ eventos-db (PostgreSQL) - Healthy
- ✅ pedidos-db (PostgreSQL) - Healthy
- ✅ payments-db (PostgreSQL) - Healthy
- ✅ rabbitmq - Porta 5672, 15672 (Management) - Healthy
- ✅ prometheus - Porta 9090
- ✅ grafana - Porta 3001
- ✅ frontend - Porta 3000

#### Serviços de Backend Funcionando (3/6)
- ✅ auth-service - Porta 8081 - Registrado no Eureka
- ✅ servico-eventos - Porta 8083 - Registrado no Eureka
- ✅ servico-pedidos - Porta 8084 - Registrado no Eureka

### ⚠️ Problemas Pendentes

#### Serviços com Erro de Inicialização (3/6)
- ❌ **users-service** - Porta 8082 - Loop de restart
- ❌ **payments-service** - Porta 8085 - Loop de restart
- ❌ **notifications-service** - Porta 8086 - Loop de restart

**Erro Comum:**
```
Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.
Reason: Failed to determine a suitable driver class
```

## Análise Técnica do Problema

### Sintomas
Os 3 serviços (users-service, payments-service, notifications-service) entram em loop de restart porque:
1. **Não carregam configurações do Config Server**
2. **Sem configurações, não conseguem conectar ao banco de dados**
3. **Falham na inicialização e o Docker restart policy os reinicia**

### Diferença com Serviços que Funcionam

**Serviços que FUNCIONAM (auth-service, servico-eventos, servico-pedidos):**
- Localização: `auth-service/src/main/resources/application.yml`
```yaml
spring:
  application:
    name: auth-service
  config:
    import: "configserver:http://config-server:8888"
```

**Serviços que FALHAM (users-service, payments-service, notifications-service):**
- Localização: Usam `bootstrap.yml` em vez de `application.yml`
- Arquivo: `users-service/src/main/resources/bootstrap.yml`
```yaml
spring:
  application:
    name: users-service
  cloud:
    config:
      uri: http://config-server:8888
      fail-fast: false
```
- Arquivo: `users-service/src/main/resources/application.yml`
```yaml
# Config moved to bootstrap.yml
```

### Tentativas Realizadas na Sessão

#### 1ª Tentativa: Spring Config Import (application.yml)
```yaml
spring:
  config:
    import: "configserver:http://config-server:8888"
```
**Resultado:** ❌ Erro "File extension is not known to any PropertySourceLoader"

#### 2ª Tentativa: Optional Config Import
```yaml
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
```
**Resultado:** ❌ Serviços iniciam mas sem configurações (falha no datasource)

#### 3ª Tentativa: Bootstrap.yml + spring-cloud-starter-bootstrap
- Adicionada dependência:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```
- Criado bootstrap.yml
- Esvaziado application.yml

**Resultado:** ❌ Bootstrap.yml não está sendo carregado, mesmo problema

### Hipóteses do Problema

#### Hipótese 1: Dependência spring-cloud-starter-config comentada
Nos POMs dos serviços problemáticos, a dependência está comentada:

```xml
<!-- Config dependency commented out to allow unit tests to run without Config Server
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
-->
```

**Ação:** Descomentar essa dependência pode resolver o problema.

#### Hipótese 2: Timing Issue
Os 3 serviços podem estar tentando se conectar ao config-server antes dele estar totalmente pronto.

**Evidência:** auth-service também falhou inicialmente mas o retry automático do Spring Cloud Config funcionou na segunda tentativa.

#### Hipótese 3: Versão Incompatível
Spring Boot 3.1.5 mudou a forma de configurar o Config Client. O `spring.config.import` pode ter bugs conhecidos.

## Próximos Passos Detalhados

### Opção 1: Descomentar spring-cloud-starter-config (RECOMENDADO)

#### Passo 1.1: Editar os 3 POMs
Arquivos a editar:
- `users-service/pom.xml`
- `payments-service/pom.xml`
- `notifications-service/pom.xml`

Localizar o bloco:
```xml
<!-- Config dependency commented out to allow unit tests to run without Config Server
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
-->
```

Descomentar para:
```xml
<!-- Config dependency needed for Spring Cloud Config integration -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

#### Passo 1.2: Reverter para application.yml simples
Deletar os bootstrap.yml e restaurar application.yml:

**users-service/src/main/resources/application.yml:**
```yaml
spring:
  application:
    name: users-service
  config:
    import: "configserver:http://config-server:8888"
```

**payments-service/src/main/resources/application.yml:**
```yaml
spring:
  application:
    name: payments-service
  config:
    import: "configserver:http://config-server:8888"
```

**notifications-service/src/main/resources/application.yml:**
```yaml
spring:
  application:
    name: notifications-service
  config:
    import: "configserver:http://config-server:8888"
```

#### Passo 1.3: Rebuild e Restart
```bash
# Rebuild os 3 serviços
mvn clean package -DskipTests -pl users-service,payments-service,notifications-service

# Rebuild as imagens Docker
docker-compose build users-service payments-service notifications-service

# Restart os containers
docker-compose up -d users-service payments-service notifications-service

# Aguardar e verificar logs
sleep 30
docker-compose logs users-service | tail -50
docker-compose logs payments-service | tail -50
docker-compose logs notifications-service | tail -50
```

#### Passo 1.4: Verificar se funcionou
```bash
# Verificar se os serviços estão UP
docker-compose ps | grep -E "users-service|payments-service|notifications-service"

# Verificar registro no Eureka
curl -s http://localhost:8761/eureka/apps | grep -E "USERS-SERVICE|PAYMENTS-SERVICE|NOTIFICATIONS-SERVICE"

# Verificar health checks
curl http://localhost:8082/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8086/actuator/health
```

### Opção 2: Aumentar Retry e Delay do Config Client

Se a Opção 1 não funcionar, o problema pode ser timing.

#### Passo 2.1: Adicionar propriedades de retry ao bootstrap.yml

**users-service/src/main/resources/bootstrap.yml:**
```yaml
spring:
  application:
    name: users-service
  cloud:
    config:
      uri: http://config-server:8888
      fail-fast: false
      retry:
        max-attempts: 10
        initial-interval: 2000
        max-interval: 5000
        multiplier: 1.5
```

Repetir para payments-service e notifications-service.

#### Passo 2.2: Adicionar delay no docker-compose

Editar `docker-compose.yml` para adicionar healthcheck no config-server e usar condition:

```yaml
config-server:
  # ... configuração existente
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 30s

users-service:
  # ... configuração existente
  depends_on:
    config-server:
      condition: service_healthy
```

### Opção 3: Fallback com Variáveis de Ambiente

Se nenhuma opção funcionar, configurar fallback direto via variáveis de ambiente.

#### Passo 3.1: Editar docker-compose.yml

Adicionar variáveis de ambiente para datasource:

```yaml
users-service:
  # ... configuração existente
  environment:
    - SPRING_PROFILES_ACTIVE=default
    - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/
    - SPRING_DATASOURCE_URL=jdbc:postgresql://users-db:5432/users_db
    - SPRING_DATASOURCE_USERNAME=${DB_USERNAME:-user}
    - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-password}
    - SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
    - SPRING_JPA_HIBERNATE_DDL_AUTO=${DDL_AUTO:-update}
    - SPRING_FLYWAY_ENABLED=true
    - DB_USERNAME=${DB_USERNAME:-user}
    - DB_PASSWORD=${DB_PASSWORD:-password}
    - DDL_AUTO=${DDL_AUTO:-update}
    - SHOW_SQL=${SHOW_SQL:-false}
```

## Mudanças Feitas na Sessão Anterior

### Arquivos de Configuração de Teste Criados
1. `users-service/src/test/resources/application.yml` - Desabilita Config Server, usa H2
2. `payments-service/src/test/resources/application.yml` - Desabilita Config Server, usa H2
3. `notifications-service/src/test/resources/application.yml` - Desabilita Config Server, usa H2, desabilita health checks
4. `auth-service/src/test/resources/application.yml` - Desabilita Config Server, usa H2
5. `servico-eventos/src/test/resources/application.yml` - Desabilita Config Server, usa H2
6. `servico-pedidos/src/test/resources/application.yml` - Desabilita Config Server, usa H2

### Arquivos Bootstrap Criados (para os 3 serviços problemáticos)
1. `users-service/src/main/resources/bootstrap.yml`
2. `payments-service/src/main/resources/bootstrap.yml`
3. `notifications-service/src/main/resources/bootstrap.yml`

### POMs Modificados

#### Dependência H2 Adicionada
- `users-service/pom.xml`
- `payments-service/pom.xml`
- `notifications-service/pom.xml`
- `auth-service/pom.xml`
- `servico-eventos/pom.xml`
- `servico-pedidos/pom.xml`

#### Dependência spring-cloud-starter-bootstrap Adicionada
- `users-service/pom.xml`
- `payments-service/pom.xml`
- `notifications-service/pom.xml`

### Código de Produção Modificado

#### auth-service/src/test/java/com/example/authservice/controller/AuthControllerTest.java
Adicionado `@Import(SecurityConfig.class)` e mocks:
```java
@Import(SecurityConfig.class)
class AuthControllerTest {
    // ...
    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;
}
```

#### servico-pedidos/src/main/java/com/example/servicopedidos/model/Order.java
Adicionado `@JsonManagedReference`:
```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonManagedReference
private List<OrderItem> items;
```

#### servico-pedidos/src/main/java/com/example/servicopedidos/model/OrderItem.java
Adicionado `@JsonBackReference`:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id")
@JsonBackReference
private Order order;
```

#### Construtores Adicionados
**notifications-service/src/main/java/com/example/notificationsservice/event/PaymentProcessedEvent.java:**
```java
public PaymentProcessedEvent() {
}

public PaymentProcessedEvent(Long orderId, boolean success) {
    this.orderId = orderId;
    this.success = success;
}
```

**servico-pedidos/src/main/java/com/example/servicopedidos/event/PaymentProcessedEvent.java:**
```java
public PaymentProcessedEvent() {
}

public PaymentProcessedEvent(Long orderId, boolean success) {
    this.orderId = orderId;
    this.success = success;
}
```

### docker-compose.yml Modificado

#### Restart Policies Adicionadas
```yaml
users-service:
  restart: on-failure
  depends_on:
    config-server:
      condition: service_started
    discovery-service:
      condition: service_started
    users-db:
      condition: service_healthy

payments-service:
  restart: on-failure
  depends_on:
    config-server:
      condition: service_started
    discovery-service:
      condition: service_started
    payments-db:
      condition: service_healthy
    rabbitmq:
      condition: service_healthy

notifications-service:
  restart: on-failure
  depends_on:
    config-server:
      condition: service_started
    discovery-service:
      condition: service_started
    rabbitmq:
      condition: service_healthy
```

## Comandos Úteis para Debug

### Verificar Configurações do Config Server
```bash
# Verificar se config-server está respondendo
curl http://localhost:8888/actuator/health

# Verificar configuração para users-service
curl http://localhost:8888/users-service/default

# Verificar configuração para payments-service
curl http://localhost:8888/payments-service/default

# Verificar configuração para notifications-service
curl http://localhost:8888/notifications-service/default
```

### Verificar Logs de Serviços
```bash
# Logs completos
docker-compose logs users-service
docker-compose logs payments-service
docker-compose logs notifications-service

# Últimas 50 linhas
docker-compose logs --tail=50 users-service

# Follow logs em tempo real
docker-compose logs -f users-service

# Filtrar por erro
docker-compose logs users-service 2>&1 | grep -i "error\|exception\|failed"
```

### Verificar Registro no Eureka
```bash
# Web UI
# Abrir: http://localhost:8761

# API
curl -s http://localhost:8761/eureka/apps | grep -A 10 "USERS-SERVICE"
```

### Verificar Conectividade
```bash
# Entrar no container users-service
docker exec -it ticket-system-users-service-1 /bin/sh

# Dentro do container, testar conectividade
curl http://config-server:8888/actuator/health
curl http://discovery-service:8761/actuator/health
ping users-db
```

### Restart Completo
```bash
# Parar todos os containers
docker-compose down

# Limpar volumes (CUIDADO: apaga dados do banco)
docker-compose down -v

# Rebuild tudo
mvn clean package -DskipTests
docker-compose build

# Subir em ordem
docker-compose up -d rabbitmq
sleep 10
docker-compose up -d discovery-service config-server
sleep 20
docker-compose up -d auth-db users-db eventos-db pedidos-db payments-db
sleep 10
docker-compose up -d auth-service servico-eventos servico-pedidos
sleep 15
docker-compose up -d users-service payments-service notifications-service
sleep 10
docker-compose up -d api-gateway frontend
```

## Testes a Executar Após Correção

```bash
# 1. Verificar que todos os serviços estão UP
docker-compose ps

# 2. Verificar health de cada serviço
for port in 8081 8082 8083 8084 8085 8086; do
  echo "Testing port $port..."
  curl -s http://localhost:$port/actuator/health | jq .
done

# 3. Verificar Eureka
curl -s http://localhost:8761/eureka/apps | grep -E "STATUS>UP"

# 4. Rodar todos os testes novamente
mvn test

# 5. Testar endpoints via API Gateway
# Auth
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123","email":"test@example.com"}'

# Events
curl http://localhost:8080/events

# Users
curl http://localhost:8080/users/1
```

## Referências

### Documentação Spring Cloud Config
- https://docs.spring.io/spring-cloud-config/docs/current/reference/html/
- https://spring.io/guides/gs/centralized-configuration/

### Issues Conhecidos
- Spring Boot 3.x Config Import: https://github.com/spring-cloud/spring-cloud-config/issues/2094
- Bootstrap Context Changes: https://github.com/spring-cloud/spring-cloud-config/issues/2007

### Alterações no Spring Boot 3.x
- Bootstrap foi desabilitado por padrão
- Necessário `spring-cloud-starter-bootstrap` para habilitar
- Alternativa é usar `spring.config.import`

## Resumo Executivo

**Status:** 77% do sistema está funcionando (10/13 serviços UP + todos os testes passando)

**Problema:** 3 serviços não conseguem carregar configurações do Config Server

**Solução Recomendada:** Descomentar dependência `spring-cloud-starter-config` nos 3 POMs problemáticos (Opção 1)

**Tempo Estimado:** 15-30 minutos para implementar e testar a Opção 1

**Próxima Sessão:** Começar pela Opção 1, se não funcionar tentar Opção 2, se ainda falhar usar Opção 3 como fallback
