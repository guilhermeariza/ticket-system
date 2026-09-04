# RELATÓRIO DE ANÁLISE COMPLETA E PENDÊNCIAS - SISTEMA DE TICKETS/EVENTOS

**Data:** 02 de novembro de 2025
**Versão:** 1.0
**Estado do Projeto:** PROTÓTIPO FUNCIONAL

---

## SUMÁRIO EXECUTIVO

Este projeto implementa um sistema de venda de ingressos baseado em arquitetura de microsserviços com Spring Boot (Java 17), React, Docker, RabbitMQ, PostgreSQL e ferramentas de observabilidade (Prometheus/Grafana).

**STATUS ATUAL:** O projeto está em estado **FUNCIONAL BÁSICO**, com a infraestrutura core implementada e funcionalidades principais operacionais, mas requer melhorias significativas em segurança, testes, validações e features completas para ser considerado production-ready.

**ESFORÇO ESTIMADO PARA PRODUÇÃO:** 4-6 semanas com 2 desenvolvedores

---

## ÍNDICE

1. [Estrutura Geral do Projeto](#1-estrutura-geral-do-projeto)
2. [Problemas de Código Existentes](#2-problemas-de-codigo-existentes)
3. [Problemas de Segurança](#3-problemas-de-seguranca)
4. [Funcionalidades Incompletas](#4-funcionalidades-incompletas)
5. [Falta de Testes](#5-falta-de-testes)
6. [Problemas de Configuração](#6-problemas-de-configuracao)
7. [Documentação](#7-documentacao)
8. [Problemas de Performance](#8-problemas-de-performance)
9. [Dependências e Package Management](#9-dependencias-e-package-management)
10. [Inconsistências no Código](#10-inconsistencias-no-codigo)
11. [Features Planejadas mas Não Implementadas](#11-features-planejadas-mas-nao-implementadas)
12. [Problemas de Integração Entre Módulos](#12-problemas-de-integracao-entre-modulos)
13. [Problemas Adicionais Detectados](#13-problemas-adicionais-detectados)
14. [Matriz de Prioridades](#14-matriz-de-prioridades)
15. [Pontos Positivos do Projeto](#15-pontos-positivos-do-projeto)
16. [Checklist de Produção](#16-checklist-de-producao)
17. [Conclusão](#17-conclusao)

---

## 1. ESTRUTURA GERAL DO PROJETO

### 1.1 Arquitetura de Microsserviços

#### Serviços de Infraestrutura
- `discovery-service` (Eureka Server) - Porta 8761
- `config-server` (Spring Cloud Config) - Porta 8888
- `api-gateway` (Spring Cloud Gateway) - Porta 8080

#### Serviços de Domínio
- `auth-service` (Autenticação JWT) - Porta 8081
- `users-service` (Gestão de usuários) - Porta 8082
- `servico-eventos` (Gestão de eventos/ingressos) - Porta 8083
- `servico-pedidos` (Gestão de pedidos) - Porta 8084
- `payments-service` (Processamento de pagamentos) - Porta 8085
- `notifications-service` (Notificações) - Porta 8086

#### Frontend
- React SPA - Porta 3000

#### Infraestrutura de Suporte
- RabbitMQ (Message Broker) - Portas 5672, 15672
- PostgreSQL (5 instâncias separadas para cada serviço)
- Prometheus (Métricas) - Porta 9090
- Grafana (Dashboards) - Porta 3001

### 1.2 Tecnologias Utilizadas

#### Backend
- Spring Boot 3.1.5
- Spring Cloud 2022.0.4
- Java 17
- PostgreSQL 13
- JWT (jjwt 0.11.5)
- RabbitMQ
- Spring Data JPA
- OpenFeign (comunicação entre serviços)

#### Frontend
- React 18.2.0
- React Router DOM 7.9.4
- Material-UI (@mui/material 7.3.4)
- Axios/Fetch API

#### DevOps
- Docker & Docker Compose
- Maven
- Node.js 18

---

## 2. PROBLEMAS DE CÓDIGO EXISTENTES

### 2.1 TODOs e Código Incompleto

**LOCALIZAÇÃO:** `notifications-service/src/main/java/com/example/notificationsservice/service/EmailService.java:12`

```java
// TODO: Integrate with a real email sending service like JavaMailSender, SendGrid, etc.
```

**IMPACTO:** ⚠️ ALTO
**DESCRIÇÃO:** O serviço de notificações apenas imprime mensagens no console, não envia emails reais.

**SOLUÇÃO NECESSÁRIA:**
- Integrar com JavaMailSender ou serviço externo (SendGrid, AWS SES)
- Criar templates de email
- Configurar SMTP

---

### 2.2 Lógica de Negócio Incompleta

**PROBLEMA:** Incompatibilidade entre contrato frontend/backend na criação de pedidos

**Frontend (`BuyTicketPage.js`):**
```javascript
body: JSON.stringify({ ticketTypeId, quantity }),
```

**Backend (`servico-pedidos/src/main/java/com/example/servicopedidos/controller/OrderController.java:31`):**
```java
@PostMapping
public Order createOrder(@RequestBody Order order) {
    return orderService.createOrder(order);
}
```

**IMPACTO:** 🔴 CRÍTICO
**DESCRIÇÃO:** O frontend envia apenas `ticketTypeId` e `quantity`, mas o backend espera um objeto `Order` completo com `userId`, `items`, etc.

**SOLUÇÃO NECESSÁRIA:**
```java
// Criar DTO específico
public class CreateOrderRequest {
    private Long ticketTypeId;
    private Integer quantity;
}

@PostMapping
public Order createOrder(
    @RequestHeader("X-User-Id") String userId,
    @RequestBody CreateOrderRequest request
) {
    return orderService.createOrder(userId, request);
}
```

---

### 2.3 Gestão de UserId Inconsistente

**LOCALIZAÇÃO:** `api-gateway` e controllers dos serviços

**PROBLEMA:** O Gateway extrai o `userId` do JWT e adiciona ao header `X-User-Id`:

```java
ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
    .header("X-User-Id", userId)
    .build();
```

**MAS:** Nenhum controller captura este header com `@RequestHeader("X-User-Id")`

**IMPACTO:** 🔴 CRÍTICO
**DESCRIÇÃO:** O campo `userId` dos pedidos fica null ou precisa ser enviado pelo frontend (INSEGURO!)

**SOLUÇÃO NECESSÁRIA:**
- Adicionar `@RequestHeader("X-User-Id") String userId` em todos os endpoints que precisam do usuário
- Criar um interceptor/filter para extrair automaticamente

---

## 3. PROBLEMAS DE SEGURANÇA

### 3.1 Credenciais Hardcoded

**SEVERIDADE:** 🔴 CRÍTICA

**LOCALIZAÇÃO:** Múltiplos arquivos em `config-repo/`

**Credenciais expostas:**
- Database password: `password` (em todos os serviços)
- Database username: `user` (em todos os serviços)
- JWT Secret: `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970`

**ARQUIVOS AFETADOS:**
- `config-repo/auth-service.yml`
- `config-repo/servico-eventos.yml`
- `config-repo/servico-pedidos.yml`
- `config-repo/users-service.yml`
- `config-repo/payments-service.yml`
- `api-gateway/src/main/resources/application.yml`

**RECOMENDAÇÃO:**
```yaml
# Usar variáveis de ambiente
spring:
  datasource:
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**ALTERNATIVAS:**
- Spring Cloud Vault
- Docker Secrets
- AWS Secrets Manager

---

### 3.2 CSRF Desabilitado

**LOCALIZAÇÃO:** `auth-service/src/main/java/com/example/authservice/security/SecurityConfig.java:38`

```java
.csrf(csrf -> csrf.disable())
```

**IMPACTO:** ⚠️ MÉDIO
**DESCRIÇÃO:** Vulnerável a ataques CSRF. Embora seja comum em APIs REST com JWT, é importante documentar esta decisão.

---

### 3.3 CORS Configurado para Desenvolvimento

**LOCALIZAÇÃO:** `api-gateway/src/main/resources/application.yml:12`

```yaml
allowedOrigins: "http://localhost:3000"
```

**IMPACTO:** ⚠️ MÉDIO
**DESCRIÇÃO:** OK para desenvolvimento, mas precisa ser configurado dinamicamente para produção.

**SOLUÇÃO:**
```yaml
allowedOrigins: ${ALLOWED_ORIGINS:http://localhost:3000}
```

---

### 3.4 Falta de Validação de Entrada

**IMPACTO:** ⚠️ ALTO

**PROBLEMA:** Não há anotações de validação (`@Valid`, `@NotNull`, `@NotEmpty`, `@Size`) em nenhum DTO ou entidade.

**EXEMPLOS SEM VALIDAÇÃO:**
- `AuthController.register()` - não valida username/password
- `OrderController.createOrder()` - não valida dados do pedido
- `EventController.createEvent()` - não valida dados do evento

**SOLUÇÃO NECESSÁRIA:**
```java
// Adicionar dependência
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

// Usar validações
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}

@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    // ...
}
```

---

### 3.5 Falta de Rate Limiting

**IMPACTO:** ⚠️ ALTO
**DESCRIÇÃO:** Não há proteção contra brute force ou DDoS nos endpoints de autenticação.

**RECOMENDAÇÃO:**
- Implementar Bucket4j ou Redis Rate Limiter
- Configurar no API Gateway

---

### 3.6 Logs com Dados Sensíveis

**PROBLEMA:** `show-sql: true` está habilitado em produção, podendo expor dados sensíveis nos logs.

**SOLUÇÃO:**
```yaml
jpa:
  show-sql: false # Produção
  properties:
    hibernate:
      format_sql: false
```

---

## 4. FUNCIONALIDADES INCOMPLETAS

### 4.1 Serviço de Pagamentos

**STATUS:** ⚠️ Simulação completa

**LOCALIZAÇÃO:** `payments-service/src/main/java/com/example/paymentsservice/service/PaymentService.java`

```java
// Simulate payment processing
boolean paymentSuccess = true;
```

**FALTANDO:**
- ❌ Integração com gateway de pagamento real (Stripe, Mercado Pago)
- ❌ Geração de transaction IDs reais
- ❌ Tratamento de falhas de pagamento
- ❌ Webhooks para confirmação de pagamento
- ❌ Refund/cancelamento
- ❌ Validação de cartão
- ❌ 3D Secure

---

### 4.2 Serviço de Notificações

**STATUS:** ⚠️ Apenas placeholder

**FALTANDO:**
- ❌ Integração com serviço de email (JavaMailSender, SendGrid, AWS SES)
- ❌ Templates de email (Thymeleaf, Freemarker)
- ❌ Configuração SMTP
- ❌ Notificações push
- ❌ Histórico de notificações enviadas
- ❌ Retry em caso de falha
- ❌ Suporte a múltiplos canais (email, SMS, push)

---

### 4.3 Autenticação

**IMPLEMENTADO:**
- ✅ Registro de usuário
- ✅ Login com JWT
- ✅ Validação de token no gateway

**FALTANDO:**
- ❌ Refresh token
- ❌ Recuperação de senha
- ❌ Verificação de email
- ❌ Logout (blacklist de tokens)
- ❌ Roles/Permissions (authorization)
- ❌ OAuth2/Social login
- ❌ Two-factor authentication (2FA)
- ❌ Session management
- ❌ Password policy enforcement

---

### 4.4 Gerenciamento de Usuários

**IMPLEMENTADO:**
- ✅ CRUD básico

**FALTANDO:**
- ❌ Paginação
- ❌ Busca e filtragem
- ❌ Perfil de usuário completo
- ❌ Upload de avatar
- ❌ Preferências de usuário
- ❌ Histórico de atividades
- ❌ Gerenciamento de endereços
- ❌ GDPR compliance (exportação/exclusão de dados)

---

### 4.5 Gestão de Eventos

**IMPLEMENTADO:**
- ✅ CRUD de eventos
- ✅ Tipos de ingressos
- ✅ Controle de quantidade
- ✅ Decremento de estoque

**FALTANDO:**
- ❌ Busca e filtragem (por data, localização, categoria)
- ❌ Categorias de eventos
- ❌ Upload de imagens/banners
- ❌ Reserva temporária de ingressos
- ❌ Gestão de assentos numerados
- ❌ Cancelamento de eventos
- ❌ Devolução de ingressos
- ❌ Check-in de participantes
- ❌ QR Code para ingressos
- ❌ Ingressos em lote (batch)
- ❌ Promoções e cupons de desconto
- ❌ Waiting list

---

### 4.6 Gestão de Pedidos

**IMPLEMENTADO:**
- ✅ Criação de pedido
- ✅ Listagem de pedidos
- ✅ Atualização de status via RabbitMQ

**FALTANDO:**
- ❌ Filtragem por usuário (userId)
- ❌ Histórico de pedidos com paginação
- ❌ Cancelamento de pedido
- ❌ Timeouts para pedidos pendentes
- ❌ Invoice/Recibo em PDF
- ❌ Exportação de pedidos (CSV, Excel)
- ❌ Estatísticas de vendas
- ❌ Relatórios gerenciais

---

### 4.7 Frontend

**IMPLEMENTADO:**
- ✅ Login/Registro
- ✅ Listagem de eventos
- ✅ Detalhes do evento
- ✅ Compra de ingressos (com bugs)
- ✅ Visualização de pedidos

**FALTANDO:**
- ❌ Protected routes (rotas autenticadas)
- ❌ Loading states consistentes
- ❌ Tratamento de erros global
- ❌ Carrinho de compras
- ❌ Checkout multi-step
- ❌ Perfil de usuário
- ❌ Histórico detalhado de pedidos
- ❌ Responsividade completa (mobile)
- ❌ Testes E2E
- ❌ Dark mode
- ❌ Internacionalização (i18n)
- ❌ Acessibilidade (a11y)
- ❌ SEO otimizado
- ❌ PWA (Progressive Web App)

---

## 5. FALTA DE TESTES

### 5.1 Backend

**STATUS:** 🔴 ZERO testes implementados

**EVIDÊNCIA:**
```bash
find "./*/src/test/java" -type d
# Resultado: diretórios vazios
```

**FALTANDO:**
- ❌ Testes unitários (JUnit + Mockito)
- ❌ Testes de integração (@SpringBootTest + Testcontainers)
- ❌ Testes de contrato (Spring Cloud Contract)
- ❌ Testes de API (RestAssured)
- ❌ Testes de performance (JMeter, Gatling)
- ❌ Testes de segurança (OWASP ZAP)

**IMPACTO:** 🔴 CRÍTICO - Não há garantia de qualidade do código.

**RECOMENDAÇÃO:**

```java
// Exemplo de teste unitário
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventServiceClient eventServiceClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        Order order = new Order();
        when(orderRepository.save(any())).thenReturn(order);

        // When
        Order result = orderService.createOrder(order);

        // Then
        assertNotNull(result);
        verify(orderRepository).save(order);
    }
}

// Exemplo de teste de integração
@SpringBootTest
@Testcontainers
class OrderControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\"1\",\"items\":[]}"))
            .andExpect(status().isOk());
    }
}
```

---

### 5.2 Frontend

**STATUS:** ⚠️ Estrutura presente mas sem testes implementados

**EVIDÊNCIA:** `package.json` contém `@testing-library/react` mas sem arquivos de teste em `src/`

**FALTANDO:**
- ❌ Testes unitários (Jest + React Testing Library)
- ❌ Testes de componentes
- ❌ Testes E2E (Cypress, Playwright)
- ❌ Testes de acessibilidade (jest-axe)
- ❌ Visual regression tests (Chromatic, Percy)

**RECOMENDAÇÃO:**

```javascript
// Exemplo de teste de componente
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import LoginPage from './LoginPage';

test('should login successfully', async () => {
  render(
    <BrowserRouter>
      <LoginPage />
    </BrowserRouter>
  );

  fireEvent.change(screen.getByLabelText(/username/i), {
    target: { value: 'testuser' }
  });

  fireEvent.change(screen.getByLabelText(/password/i), {
    target: { value: 'password123' }
  });

  fireEvent.click(screen.getByRole('button', { name: /login/i }));

  expect(await screen.findByText(/welcome/i)).toBeInTheDocument();
});
```

---

## 6. PROBLEMAS DE CONFIGURAÇÃO

### 6.1 Hibernate DDL-Auto

**LOCALIZAÇÃO:** Todos os `application.yml` dos serviços

```yaml
jpa:
  hibernate:
    ddl-auto: update
```

**IMPACTO:** ⚠️ MÉDIO
- ✅ OK para desenvolvimento
- 🔴 INACEITÁVEL para produção
- ⚠️ Risco de perda de dados

**RECOMENDAÇÃO:** Usar Flyway ou Liquibase para migrations versionadas.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    baseline-on-migrate: true
```

```sql
-- src/main/resources/db/migration/V1__create_tables.sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    total_price DECIMAL(10,2),
    status VARCHAR(50),
    created_at TIMESTAMP
);
```

---

### 6.2 Prometheus - Actuator Não Configurado

**PROBLEMA:** O `prometheus.yml` está configurado para scrape do endpoint `/actuator/prometheus`, mas as dependências do Actuator e Micrometer **NÃO ESTÃO** nos pom.xml dos serviços.

**IMPACTO:** 🔴 CRÍTICO - O Prometheus não conseguirá coletar métricas dos serviços.

**SOLUÇÃO:** Adicionar em TODOS os `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

---

### 6.3 Variáveis de Ambiente Faltando

**PROBLEMA:** Não há uso de variáveis de ambiente para configurações específicas de ambiente (dev, staging, prod).

**EXEMPLOS:**
- Conexões de banco de dados hardcoded
- URLs de serviços hardcoded no frontend (`http://localhost:8080`)
- Credenciais em texto plano

**SOLUÇÃO:**

```yaml
# application.yml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/orders}
    username: ${DATABASE_USERNAME:user}
    password: ${DATABASE_PASSWORD:password}
```

```javascript
// Frontend - .env
REACT_APP_API_URL=http://localhost:8080

// src/config.js
export const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
```

---

### 6.4 Falta de Profiles Spring

**PROBLEMA:** Apenas um profile "default" é usado. Não há profiles para dev/staging/prod.

**SOLUÇÃO:**

```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: true
logging:
  level:
    root: DEBUG

# application-prod.yml
spring:
  jpa:
    show-sql: false
logging:
  level:
    root: WARN
```

```bash
# Executar com profile
java -jar app.jar --spring.profiles.active=prod
```

---

## 7. DOCUMENTAÇÃO

### 7.1 Documentação Existente

**ARQUIVOS PRESENTES:**
- ✅ `README.md` - Visão geral com diagramas Mermaid (EXCELENTE)
- ✅ `COMO_EXECUTAR.md` - Guia passo a passo (BOM)
- ✅ `PREREQUISITOS.md` - Lista de requisitos (BOM)
- ✅ `ANALISE_GERAL_DO_PROJETO.md` - Análise anterior
- ✅ `ANALISE_E_PROXIMOS_PASSOS.md` - Roadmap
- ✅ READMEs individuais em cada serviço

**QUALIDADE:** ✅ BOA - A documentação existente é bem estruturada.

---

### 7.2 Documentação Faltando

#### APIs
- ❌ Swagger/OpenAPI documentation
- ❌ Endpoints documentados com `@Operation`, `@ApiResponse`
- ❌ Collection do Postman/Insomnia
- ❌ Exemplos de request/response

**SOLUÇÃO:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

```java
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    @PostMapping
    @Operation(
        summary = "Create a new order",
        description = "Creates a new order for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Order createOrder(@RequestBody OrderRequest request) {
        // ...
    }
}
```

Acesso: `http://localhost:8080/swagger-ui.html`

---

#### Arquitetura
- ❌ Diagramas de sequência para fluxos complexos
- ❌ Diagramas de banco de dados (ER diagrams)
- ❌ ADRs (Architecture Decision Records)
- ❌ Documentação de decisões técnicas

---

#### Desenvolvimento
- ❌ Guia de contribuição (`CONTRIBUTING.md`)
- ❌ Code style guide
- ❌ Guia de troubleshooting
- ❌ FAQ

**RECOMENDAÇÃO:** Criar `CONTRIBUTING.md`:

```markdown
# Guia de Contribuição

## Setup do Ambiente
1. Clone o repositório
2. Instale as dependências
3. Execute os testes

## Padrões de Código
- Use Java 17
- Siga Google Java Style Guide
- Cobertura de testes mínima: 80%

## Processo de Pull Request
1. Crie uma branch a partir de `main`
2. Faça suas alterações
3. Execute os testes
4. Crie um PR com descrição detalhada
```

---

#### Operação
- ❌ Runbooks para incidentes
- ❌ Guia de deploy
- ❌ Guia de monitoramento
- ❌ Guia de backup/restore
- ❌ Disaster recovery plan

---

## 8. PROBLEMAS DE PERFORMANCE

### 8.1 N+1 Query Problem

**LOCALIZAÇÃO:** `Event` e `TicketType` com relacionamento `@OneToMany`

```java
@OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonManagedReference
private List<TicketType> ticketTypes;
```

**PROBLEMA:** Com `FetchType.LAZY` (padrão), ao listar eventos, cada `ticketTypes` pode causar uma query adicional.

**IMPACTO:** ⚠️ MÉDIO - Performance degradada com muitos eventos

**SOLUÇÃO:**

```java
// Repository
@Query("SELECT e FROM Event e LEFT JOIN FETCH e.ticketTypes")
List<Event> findAllWithTicketTypes();

// Ou usar @EntityGraph
@EntityGraph(attributePaths = {"ticketTypes"})
List<Event> findAll();
```

---

### 8.2 Falta de Cache

**IMPACTO:** ⚠️ MÉDIO

**PROBLEMA:** Não há cache configurado para:
- Listagem de eventos (consultas repetidas)
- Dados de usuários
- Configurações

**SOLUÇÃO:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}

// Service
@Cacheable(value = "events", key = "#id")
public Event getEventById(Long id) {
    return eventRepository.findById(id).orElseThrow();
}

@CacheEvict(value = "events", key = "#event.id")
public Event updateEvent(Event event) {
    return eventRepository.save(event);
}
```

---

### 8.3 Falta de Índices no Banco

**PROBLEMA:** Não há definição explícita de índices nas entidades.

**ÍNDICES NECESSÁRIOS:**
- `User.username` (busca frequente, login)
- `Event.date` (filtragem por data)
- `Order.userId` (filtragem por usuário)
- `TicketType.eventId` (foreign key)

**SOLUÇÃO:**

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true)
})
public class User {
    // ...
}

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_event_date", columnList = "date"),
    @Index(name = "idx_event_status", columnList = "status")
})
public class Event {
    // ...
}

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created", columnList = "created_at")
})
public class Order {
    // ...
}
```

---

### 8.4 Connection Pool Não Configurado

**PROBLEMA:** Não há configuração de pool de conexões (HikariCP). Usar valores default pode não ser ideal.

**SOLUÇÃO:**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

---

### 8.5 Sem Circuit Breaker

**PROBLEMA:** O `servico-pedidos` faz chamada síncrona para `servico-eventos` via Feign sem circuit breaker.

```java
Integer availableQuantity = eventServiceClient.getAvailableQuantity(item.getTicketTypeId());
```

Se `servico-eventos` estiver down, o `servico-pedidos` vai falhar completamente.

**IMPACTO:** ⚠️ ALTO - Cascata de falhas

**SOLUÇÃO:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

```java
@FeignClient(name = "servico-eventos", fallback = EventServiceFallback.class)
public interface EventServiceClient {
    @GetMapping("/api/events/{id}/available")
    Integer getAvailableQuantity(@PathVariable Long id);
}

@Component
public class EventServiceFallback implements EventServiceClient {
    @Override
    public Integer getAvailableQuantity(Long id) {
        // Retornar valor default ou lançar exceção específica
        throw new ServiceUnavailableException("Event service is temporarily unavailable");
    }
}
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      eventService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
```

---

## 9. DEPENDÊNCIAS E PACKAGE MANAGEMENT

### 9.1 Versões do Backend

**Spring Boot:** 3.1.5 (Outubro 2023)
**Spring Cloud:** 2022.0.4

**STATUS:** ⚠️ Versões um pouco desatualizadas mas ainda suportadas.

**ÚLTIMA VERSÃO ESTÁVEL (2025):**
- Spring Boot 3.3.x
- Spring Cloud 2023.0.x

**RECOMENDAÇÃO:** Atualizar em ambiente de staging primeiro.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
</parent>

<properties>
    <spring-cloud.version>2023.0.0</spring-cloud.version>
</properties>
```

---

### 9.2 Versões do Frontend

**React:** 18.2.0 (OK)
**Material-UI:** 7.3.4 (versão mais recente)
**React Router:** 7.9.4

**PROBLEMA POTENCIAL:** ⚠️ React Router 7.9.4 pode ser uma versão futura/instável. A versão estável atual é 6.x.

**RECOMENDAÇÃO:** Verificar se esta versão existe:

```bash
npm list react-router-dom
```

Se for um erro, corrigir para:

```json
{
  "dependencies": {
    "react-router-dom": "^6.20.0"
  }
}
```

---

### 9.3 Vulnerabilidades de Segurança

**PROBLEMA:** Não há evidência de scans de segurança (Snyk, OWASP Dependency Check, Trivy).

**RECOMENDAÇÃO:** Executar regularmente:

```bash
# Backend - Maven
mvn org.owasp:dependency-check-maven:check

# Frontend - npm
npm audit
npm audit fix

# Docker images
docker scan <image-name>
```

**Integrar com CI/CD:**

```yaml
# .github/workflows/security.yml
name: Security Scan
on: [push, pull_request]
jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run OWASP Dependency Check
        run: mvn org.owasp:dependency-check-maven:check
      - name: Run npm audit
        run: npm audit
```

---

### 9.4 Dependências Não Utilizadas

**RECOMENDAÇÃO:** Auditar dependências:

```bash
# Maven
mvn dependency:analyze

# npm
npm prune
npx depcheck
```

---

## 10. INCONSISTÊNCIAS NO CÓDIGO

### 10.1 Nomenclatura Inconsistente

**Problema 1:** Mix de português e inglês

- `servico-eventos` vs `events-service`
- `servico-pedidos` vs `orders-service`

**RECOMENDAÇÃO:** Padronizar para inglês:

- `servico-eventos` → `events-service`
- `servico-pedidos` → `orders-service`

---

**Problema 2:** Entidades User duplicadas

- `auth-service/model/User.java`
- `users-service/model/User.java`

Duas entidades diferentes com o mesmo nome mas estruturas diferentes:
- Auth User: `id`, `username`, `password`
- Users User: `id`, `username`, `email`

**IMPACTO:** ⚠️ MÉDIO - Confusão e possível duplicação de dados

**RECOMENDAÇÃO:** Decidir arquitetura:

**Opção 1:** User único no `users-service`
- `auth-service` apenas faz autenticação e chama `users-service` via Feign
- Um único source of truth

**Opção 2:** Separar conceitos
- `auth-service/Credentials` (username, password)
- `users-service/UserProfile` (email, nome, etc.)

---

### 10.2 Uso Inconsistente de DTOs

**PROBLEMA:** Alguns endpoints usam entidades diretamente (`@RequestBody User user`), outros usam DTOs (`LoginRequest`).

**RECOMENDAÇÃO:** Sempre usar DTOs para entrada/saída de APIs.

```java
// ❌ Ruim - expõe entidade diretamente
@PostMapping
public User createUser(@RequestBody User user) {
    return userService.save(user);
}

// ✅ Bom - usa DTOs
@PostMapping
public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
    User user = userService.create(request);
    return UserMapper.toResponse(user);
}
```

---

### 10.3 Tratamento de Erros

**PROBLEMA:** Não há `@ControllerAdvice` global para tratamento de erros. Cada controller trata erros de forma diferente.

```java
// Alguns retornam ResponseEntity.notFound()
// Outros lançam RuntimeException
// Não há padrão
```

**SOLUÇÃO:** Criar exception handler global:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            LocalDateTime.now(),
            errors
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> errors;

    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this(status, message, timestamp, null);
    }
}
```

---

### 10.4 Uso de Optional

**PROBLEMA:** Alguns métodos retornam `Optional<>`, outros retornam `null`, e alguns lançam exceções.

```java
// Inconsistente
public Optional<Order> getOrderById(Long id) {
    return orderRepository.findById(id);
}

// No controller
Order order = orderRepository.findById(id)
    .orElseThrow(() -> new RuntimeException("Order not found"));
```

**RECOMENDAÇÃO:** Criar custom exceptions:

```java
// Custom exception
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s not found with id: %d", resource, id));
    }
}

// Service
public Order getOrderById(Long id) {
    return orderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Order", id));
}
```

---

## 11. FEATURES PLANEJADAS MAS NÃO IMPLEMENTADAS

### 11.1 Resiliência

**Mencionado em:** `ANALISE_E_PROXIMOS_PASSOS.md`

```
- Implementar resiliência com Circuit Breakers (Resilience4j)
```

**STATUS:** ❌ Não implementado

---

### 11.2 Observabilidade Avançada

**Mencionado em:** `ANALISE_E_PROXIMOS_PASSOS.md`

```
- Tracing Distribuído: Integrar o Micrometer Tracing
```

**STATUS:** ❌ Não implementado. Nem mesmo métricas básicas do Actuator estão configuradas.

**SOLUÇÃO:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

---

### 11.3 Migração de Banco de Dados

**Mencionado em:** `ANALISE_E_PROXIMOS_PASSOS.md`

```
- Substituir ddl-auto: update por Flyway ou Liquibase
```

**STATUS:** ❌ Não implementado

---

### 11.4 Frontend Completo

**Mencionado em:** `ANALISE_E_PROXIMOS_PASSOS.md`

```
- Desenvolvimento de UI/UX: Construir a interface completa
- Carrinho de compras
- Checkout
```

**STATUS:** ⚠️ Apenas estrutura básica implementada

---

## 12. PROBLEMAS DE INTEGRAÇÃO ENTRE MÓDULOS

### 12.1 OrderRequest vs Order Entity

**PROBLEMA:** Frontend envia `OrderRequest` simplificado, mas backend espera `Order` completo.

**Frontend (`BuyTicketPage.js`):**
```javascript
body: JSON.stringify({ ticketTypeId, quantity })
```

**Backend (`OrderController.java`):**
```java
public Order createOrder(@RequestBody Order order)
```

**IMPACTO:** 🔴 CRÍTICO - A compra de ingressos NÃO FUNCIONA como está.

**SOLUÇÃO COMPLETA:**

```java
// DTO
@Data
public class CreateOrderRequest {
    @NotNull(message = "Ticket type ID is required")
    private Long ticketTypeId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Maximum 10 tickets per order")
    private Integer quantity;
}

@Data
public class OrderResponse {
    private Long id;
    private String userId;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}

// Controller
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody CreateOrderRequest request
    ) {
        Order order = orderService.createOrder(userId, request);
        return ResponseEntity.ok(OrderMapper.toResponse(order));
    }
}

// Service
@Service
@Transactional
public class OrderService {

    public Order createOrder(String userId, CreateOrderRequest request) {
        // Buscar ticket type e validar disponibilidade
        TicketType ticketType = eventServiceClient.getTicketType(request.getTicketTypeId());

        if (ticketType.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientTicketsException();
        }

        // Criar order
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        // Criar order item
        OrderItem item = new OrderItem();
        item.setTicketTypeId(request.getTicketTypeId());
        item.setQuantity(request.getQuantity());
        item.setPrice(ticketType.getPrice());
        item.setOrder(order);

        order.setItems(List.of(item));
        order.calculateTotalPrice();

        // Salvar
        order = orderRepository.save(order);

        // Publicar evento
        orderEventPublisher.publishOrderCreated(order);

        return order;
    }
}
```

---

### 12.2 UserId Não Propagado

**PROBLEMA:** Gateway extrai `userId` do JWT e adiciona ao header `X-User-Id`, mas nenhum controller captura este header.

**IMPACTO:** 🔴 CRÍTICO - O campo `userId` do `Order` fica null ou precisa ser enviado pelo frontend (INSEGURO!)

**SOLUÇÃO:** Já mostrada na seção 12.1

**ALTERNATIVA:** Criar um resolver automático:

```java
// Annotation
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}

// Resolver
@Component
public class UserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String userId = request.getHeader("X-User-Id");

        if (userId == null) {
            throw new UnauthorizedException("User ID not found in request");
        }

        return userId;
    }
}

// Configuração
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserIdArgumentResolver userIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userIdArgumentResolver);
    }
}

// Uso
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
    @CurrentUserId String userId,
    @Valid @RequestBody CreateOrderRequest request
) {
    // userId é extraído automaticamente do header
}
```

---

### 12.3 Comunicação RabbitMQ

**IMPLEMENTADO:**
- ✅ `servico-pedidos` publica `OrderCreatedEvent`
- ✅ `payments-service` consome `OrderCreatedEvent`
- ✅ `payments-service` publica `PaymentProcessedEvent`
- ✅ `servico-pedidos` consome `PaymentProcessedEvent`
- ✅ `notifications-service` consome `PaymentProcessedEvent`

**PROBLEMAS:**
1. ❌ Não há Dead Letter Queue (DLQ) configurada
2. ❌ Não há retry policy configurada
3. ❌ Não há monitoramento de mensagens não processadas
4. ❌ Serialização pode falhar sem tratamento adequado

**SOLUÇÃO:**

```yaml
# application.yml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          multiplier: 2
          max-interval: 10000
        default-requeue-rejected: false
```

```java
// Config
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_QUEUE = "order.created";
    public static final String ORDER_DLQ = "order.created.dlq";
    public static final String ORDER_EXCHANGE = "order.exchange";

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", ORDER_DLQ)
            .build();
    }

    @Bean
    public Queue orderDLQ() {
        return new Queue(ORDER_DLQ, true);
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
            .to(orderExchange())
            .with("order.created");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

// Listener com tratamento de erros
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("Processing order: {}", event.getOrderId());
            // Processar...
        } catch (Exception e) {
            log.error("Error processing order: {}", event.getOrderId(), e);
            throw new AmqpRejectAndDontRequeueException("Failed to process order", e);
        }
    }

    // Monitor DLQ
    @RabbitListener(queues = RabbitMQConfig.ORDER_DLQ)
    public void handleDLQ(Message message) {
        log.error("Message sent to DLQ: {}", new String(message.getBody()));
        // Alertar equipe, salvar para análise, etc.
    }
}
```

---

### 12.4 Feign Client sem Fallback

**LOCALIZAÇÃO:** `servico-pedidos/client/EventServiceClient.java`

```java
@FeignClient(name = "servico-eventos")
public interface EventServiceClient {
    // ...
}
```

**PROBLEMA:** Sem fallback para quando o servico-eventos estiver indisponível.

**SOLUÇÃO:** Já mostrada na seção 8.5

---

## 13. PROBLEMAS ADICIONAIS DETECTADOS

### 13.1 Falta de .gitignore na Raiz

**EVIDÊNCIA:** Apenas `.idea/.gitignore` existe

**IMPACTO:** ⚠️ MÉDIO - Arquivos de build (`target/`, `node_modules/`) podem ser commitados por engano.

**SOLUÇÃO:** Criar `.gitignore` na raiz:

```gitignore
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml

# Node
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*
build/
dist/

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Logs
*.log
logs/

# Environment
.env
.env.local
.env.*.local

# Spring
spring.log

# Database
*.db
*.sqlite
```

---

### 13.2 Senha de Grafana Fraca

**LOCALIZAÇÃO:** `docker-compose.yml:220`

```yaml
environment:
  - GF_SECURITY_ADMIN_PASSWORD=password
```

**SEVERIDADE:** ⚠️ MÉDIA (desenvolvimento OK, produção NÃO)

**SOLUÇÃO:**

```yaml
environment:
  - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-password}
```

---

### 13.3 RabbitMQ com Credenciais Default

**LOCALIZAÇÃO:** `docker-compose.yml:38-44`

```yaml
rabbitmq:
  image: "rabbitmq:3.8-management"
```

Usa credenciais default `guest/guest`.

**SEVERIDADE:** ⚠️ MÉDIA (desenvolvimento OK, produção NÃO)

**SOLUÇÃO:**

```yaml
rabbitmq:
  image: "rabbitmq:3.8-management"
  environment:
    - RABBITMQ_DEFAULT_USER=${RABBITMQ_USER:-guest}
    - RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASS:-guest}
```

---

### 13.4 Frontend com URL Hardcoded

**LOCALIZAÇÃO:** Múltiplos arquivos no frontend

```javascript
const API_BASE_URL = 'http://localhost:8080';
```

**IMPACTO:** ⚠️ MÉDIO - Não funcionará em outros ambientes sem rebuild.

**SOLUÇÃO:** Usar variável de ambiente (já mostrada na seção 6.3)

---

### 13.5 Falta de Health Checks no Docker

**PROBLEMA:** O `docker-compose.yml` não define `healthcheck` para os serviços.

**IMPACTO:** ⚠️ MÉDIO - Serviços podem ser considerados "up" mesmo se não estiverem prontos.

**SOLUÇÃO:**

```yaml
services:
  auth-service:
    build: ./auth-service
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    depends_on:
      db-auth:
        condition: service_healthy
      discovery-service:
        condition: service_healthy

  db-auth:
    image: postgres:13
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U user"]
      interval: 10s
      timeout: 5s
      retries: 5
```

---

### 13.6 Ordem de Inicialização dos Serviços

**PROBLEMA:** O `depends_on` no Docker Compose não garante que o serviço esteja pronto, apenas que foi iniciado.

**SOLUÇÃO:** Usar `wait-for-it.sh` ou implementar retry logic:

```bash
# wait-for-it.sh
#!/bin/bash
set -e

host="$1"
shift
cmd="$@"

until curl -f "$host"; do
  >&2 echo "Service $host is unavailable - sleeping"
  sleep 1
done

>&2 echo "Service $host is up - executing command"
exec $cmd
```

```dockerfile
# Dockerfile
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

CMD ["/wait-for-it.sh", "http://config-server:8888/actuator/health", "java", "-jar", "app.jar"]
```

**ALTERNATIVA:** Usar Spring Retry:

```java
@Configuration
@EnableRetry
public class RetryConfig {
}

@Service
public class ConfigService {

    @Retryable(
        value = {RestClientException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public Config fetchConfig() {
        // Buscar configuração do config-server
    }
}
```

---

## 14. MATRIZ DE PRIORIDADES

### 🔴 CRÍTICO (P0) - Resolver Imediatamente

| # | Pendência | Impacto | Esforço | Localização |
|---|-----------|---------|---------|-------------|
| 1 | **Credenciais hardcoded** | 🔴 Crítico | 4h | `config-repo/*.yml`, `api-gateway/application.yml` |
| 2 | **Bug: Compra de ingressos não funciona** | 🔴 Crítico | 8h | `servico-pedidos/OrderController.java`, `frontend/BuyTicketPage.js` |
| 3 | **Bug: UserId não propagado** | 🔴 Crítico | 4h | Todos os controllers |
| 4 | **Falta de testes** | 🔴 Crítico | 40h | Todos os serviços |
| 5 | **Prometheus sem métricas** | 🔴 Crítico | 4h | Todos os `pom.xml` |

**Total Esforço P0:** ~60 horas (~1.5 semanas)

---

### ⚠️ ALTO (P1) - Resolver Antes de Produção

| # | Pendência | Impacto | Esforço | Localização |
|---|-----------|---------|---------|-------------|
| 6 | **Validações de entrada** | ⚠️ Alto | 8h | Todos os controllers |
| 7 | **Global Exception Handler** | ⚠️ Alto | 4h | Criar em cada serviço |
| 8 | **Serviço de Email real** | ⚠️ Alto | 8h | `notifications-service` |
| 9 | **Gateway de Pagamento real** | ⚠️ Alto | 16h | `payments-service` |
| 10 | **Migração de BD (Flyway)** | ⚠️ Alto | 16h | Todos os serviços |
| 11 | **Circuit Breaker** | ⚠️ Alto | 8h | `servico-pedidos` |
| 12 | **Health checks Docker** | ⚠️ Alto | 4h | `docker-compose.yml` |
| 13 | **Autenticação completa** | ⚠️ Alto | 16h | `auth-service` |
| 14 | **.gitignore** | ⚠️ Alto | 0.5h | Raiz do projeto |

**Total Esforço P1:** ~80.5 horas (~2 semanas)

---

### 🟡 MÉDIO (P2) - Melhorias Importantes

| # | Pendência | Impacto | Esforço |
|---|-----------|---------|---------|
| 15 | **Cache (Redis)** | 🟡 Médio | 8h |
| 16 | **Índices de BD** | 🟡 Médio | 4h |
| 17 | **Paginação** | 🟡 Médio | 8h |
| 18 | **Dead Letter Queue** | 🟡 Médio | 4h |
| 19 | **API Documentation (Swagger)** | 🟡 Médio | 8h |
| 20 | **Frontend: Protected Routes** | 🟡 Médio | 4h |
| 21 | **Frontend: Carrinho de Compras** | 🟡 Médio | 16h |
| 22 | **Profiles Spring (dev/prod)** | 🟡 Médio | 4h |
| 23 | **Atualizar dependências** | 🟡 Médio | 8h |

**Total Esforço P2:** ~64 horas (~1.5 semanas)

---

### 🟢 BAIXO (P3) - Bom Ter

| # | Pendência | Impacto | Esforço |
|---|-----------|---------|---------|
| 24 | **Tracing Distribuído** | 🟢 Baixo | 16h |
| 25 | **Dashboards Grafana** | 🟢 Baixo | 8h |
| 26 | **Testes E2E** | 🟢 Baixo | 24h |
| 27 | **CI/CD Pipeline** | 🟢 Baixo | 16h |
| 28 | **Logs Centralizados (ELK)** | 🟢 Baixo | 24h |
| 29 | **Feature: Upload de Imagens** | 🟢 Baixo | 8h |
| 30 | **Feature: Assentos Numerados** | 🟢 Baixo | 32h |

**Total Esforço P3:** ~128 horas (~3 semanas)

---

### RESUMO DE ESFORÇO TOTAL

| Prioridade | Horas | Semanas (1 dev) | Semanas (2 devs) |
|------------|-------|-----------------|------------------|
| P0 (Crítico) | 60h | 1.5 | 0.75 |
| P1 (Alto) | 80.5h | 2 | 1 |
| P2 (Médio) | 64h | 1.5 | 0.75 |
| P3 (Baixo) | 128h | 3 | 1.5 |
| **TOTAL** | **332.5h** | **8 semanas** | **4 semanas** |

**RECOMENDAÇÃO:** Focar em P0 + P1 para tornar o projeto production-ready (4-6 semanas com 2 desenvolvedores).

---

## 15. PONTOS POSITIVOS DO PROJETO

Apesar dos problemas identificados, o projeto tem vários pontos fortes:

### Arquitetura e Design
1. ✅ **Arquitetura bem definida** - Microsserviços bem separados por domínio
2. ✅ **Service Discovery** - Eureka configurado corretamente
3. ✅ **Configuração centralizada** - Config Server funcional
4. ✅ **API Gateway** - Roteamento e JWT validation implementados
5. ✅ **Comunicação assíncrona** - RabbitMQ funcionando com eventos

### Infraestrutura
6. ✅ **Containerização completa** - Tudo roda com Docker Compose
7. ✅ **Separação de bancos** - Cada serviço com seu próprio PostgreSQL
8. ✅ **Observabilidade planejada** - Prometheus/Grafana incluídos
9. ✅ **Message Broker** - RabbitMQ com eventos bem modelados

### Código
10. ✅ **Transacionalidade** - `@Transactional` usado corretamente
11. ✅ **Relacionamentos JPA** - Modelados corretamente (Event/TicketType, Order/OrderItem)
12. ✅ **DTOs para autenticação** - LoginRequest/RegisterRequest bem implementados
13. ✅ **Feign Clients** - Comunicação síncrona bem abstraída

### Documentação
14. ✅ **Documentação inicial excelente** - README com diagramas Mermaid
15. ✅ **Guias de execução** - COMO_EXECUTAR.md e PREREQUISITOS.md
16. ✅ **Diagramas de arquitetura** - Visão clara da estrutura

### Frontend
17. ✅ **Stack moderno** - React 18 com Material-UI
18. ✅ **Roteamento** - React Router configurado
19. ✅ **Integração com API** - Chamadas para o backend implementadas

---

## 16. CHECKLIST DE PRODUÇÃO

### Backend - Segurança
- [ ] Externalizar todas as credenciais para variáveis de ambiente
- [ ] Configurar Spring Cloud Vault ou AWS Secrets Manager
- [ ] Implementar rate limiting nos endpoints de autenticação
- [ ] Adicionar validações (`@Valid`) em todos os endpoints
- [ ] Implementar HTTPS/TLS
- [ ] Configurar CORS para produção
- [ ] Desabilitar `show-sql` em produção
- [ ] Implementar auditoria de segurança
- [ ] Configurar WAF (Web Application Firewall)

### Backend - Funcionalidades
- [ ] Corrigir bug de compra de ingressos (OrderRequest)
- [ ] Implementar propagação de UserId
- [ ] Integrar gateway de pagamento real
- [ ] Integrar serviço de email real
- [ ] Implementar refresh token
- [ ] Implementar recuperação de senha
- [ ] Implementar roles/permissions
- [ ] Adicionar cancelamento de pedidos
- [ ] Implementar QR Code para ingressos

### Backend - Qualidade
- [ ] Implementar testes unitários (mínimo 80% cobertura)
- [ ] Implementar testes de integração
- [ ] Implementar testes de contrato
- [ ] Adicionar exception handler global em todos os serviços
- [ ] Padronizar nomenclatura (inglês)
- [ ] Criar DTOs para todos os endpoints
- [ ] Implementar logging estruturado (JSON)
- [ ] Adicionar correlation IDs para tracing

### Backend - Performance
- [ ] Implementar cache (Redis) para eventos
- [ ] Adicionar índices no banco de dados
- [ ] Implementar paginação em listagens
- [ ] Configurar connection pool adequadamente
- [ ] Otimizar queries (resolver N+1)
- [ ] Implementar Circuit Breaker (Resilience4j)
- [ ] Configurar Feign timeouts
- [ ] Implementar connection pooling para Feign

### Backend - Observabilidade
- [ ] Adicionar Actuator em todos os serviços
- [ ] Adicionar Micrometer para métricas Prometheus
- [ ] Implementar tracing distribuído (Zipkin/Jaeger)
- [ ] Configurar health checks nos containers
- [ ] Criar dashboards no Grafana
- [ ] Configurar alertas (AlertManager)
- [ ] Implementar logging centralizado (ELK)
- [ ] Adicionar business metrics customizadas

### Backend - Banco de Dados
- [ ] Implementar Flyway para migrations
- [ ] Mudar `ddl-auto` para `validate` em produção
- [ ] Criar migrations para schema existente
- [ ] Adicionar índices necessários
- [ ] Configurar backup automatizado
- [ ] Implementar connection pooling
- [ ] Configurar read replicas (se necessário)
- [ ] Documentar schema (ER diagrams)

### Backend - Mensageria
- [ ] Configurar Dead Letter Queue (DLQ)
- [ ] Implementar retry policy
- [ ] Adicionar monitoring de filas
- [ ] Implementar idempotência nos consumers
- [ ] Configurar prefetch adequadamente
- [ ] Adicionar circuit breaker para publishers
- [ ] Implementar event versioning

### Frontend
- [ ] Externalizar URL da API (variável de ambiente)
- [ ] Implementar protected routes
- [ ] Adicionar error boundary global
- [ ] Implementar loading states consistentes
- [ ] Criar carrinho de compras
- [ ] Implementar checkout multi-step
- [ ] Adicionar testes unitários
- [ ] Adicionar testes E2E (Cypress/Playwright)
- [ ] Otimizar bundle size (code splitting)
- [ ] Implementar PWA (se aplicável)
- [ ] Adicionar analytics (Google Analytics)
- [ ] Melhorar responsividade mobile
- [ ] Implementar acessibilidade (WCAG 2.1)
- [ ] Adicionar internacionalização (i18n)
- [ ] Implementar dark mode

### DevOps - CI/CD
- [ ] Criar `.gitignore` completo
- [ ] Configurar GitHub Actions / Jenkins
- [ ] Implementar build automatizado
- [ ] Implementar testes automatizados no CI
- [ ] Configurar deploy automatizado
- [ ] Implementar scans de segurança (OWASP, Snyk)
- [ ] Configurar Docker image scanning
- [ ] Implementar blue-green deployment
- [ ] Configurar canary releases
- [ ] Adicionar smoke tests pós-deploy

### DevOps - Infraestrutura
- [ ] Configurar Kubernetes (ou ECS/EKS)
- [ ] Implementar auto-scaling
- [ ] Configurar load balancer
- [ ] Implementar service mesh (Istio, se necessário)
- [ ] Configurar secrets management (Vault)
- [ ] Implementar backup automatizado
- [ ] Configurar disaster recovery
- [ ] Documentar runbooks para incidentes
- [ ] Criar ambientes (dev, staging, prod)
- [ ] Implementar network policies

### Documentação
- [ ] Adicionar Swagger/OpenAPI em todos os serviços
- [ ] Criar collection Postman/Insomnia
- [ ] Documentar ADRs (Architecture Decision Records)
- [ ] Criar guia de contribuição (CONTRIBUTING.md)
- [ ] Documentar code style guide
- [ ] Criar FAQ
- [ ] Documentar troubleshooting comum
- [ ] Criar runbooks operacionais
- [ ] Documentar processo de deploy
- [ ] Criar diagramas de sequência
- [ ] Documentar schema do banco (ER diagrams)

### Conformidade e Legal
- [ ] Implementar GDPR compliance (se aplicável)
- [ ] Adicionar termos de uso
- [ ] Adicionar política de privacidade
- [ ] Implementar consent management
- [ ] Adicionar exportação de dados
- [ ] Implementar direito ao esquecimento
- [ ] Configurar retenção de logs
- [ ] Documentar data flow

---

## 17. CONCLUSÃO

### Estado Atual do Projeto

O projeto **Sistema de Venda de Ingressos** apresenta uma arquitetura de microsserviços bem estruturada e uma base sólida para desenvolvimento. A infraestrutura core está funcional, com:

✅ **Pontos Fortes:**
- Service discovery (Eureka)
- Configuração centralizada (Config Server)
- API Gateway com autenticação JWT
- Comunicação assíncrona via RabbitMQ
- Frontend React básico operacional
- Containerização completa com Docker Compose
- Documentação inicial de qualidade

🔴 **Pontos Críticos:**
- **Segurança comprometida** com credenciais hardcoded
- **Ausência total** de testes automatizados
- **Features críticas incompletas** (pagamento, notificações)
- **Bug bloqueante** no fluxo de compra (incompatibilidade frontend/backend)
- **Falta de métricas/observabilidade** (Prometheus sem dados)

### Classificação

**ESTADO ATUAL:** 🟡 PROTÓTIPO FUNCIONAL

O projeto demonstra:
- ✅ Conhecimento sólido de arquitetura de microsserviços
- ✅ Uso adequado de tecnologias modernas (Spring Cloud, React, Docker)
- ✅ Boa separação de responsabilidades
- ⚠️ Funcionalidades básicas implementadas mas com bugs
- 🔴 Não está pronto para produção

### Roadmap para Produção

#### Fase 1: Correções Críticas (1-2 semanas)
**Objetivo:** Tornar o sistema minimamente funcional e seguro

- Corrigir bug de compra de ingressos
- Externalizar credenciais
- Implementar propagação de userId
- Adicionar Actuator/Micrometer
- Implementar exception handling global
- Adicionar validações básicas

#### Fase 2: Testes e Qualidade (2-3 semanas)
**Objetivo:** Garantir confiabilidade

- Implementar testes unitários (80% cobertura)
- Implementar testes de integração
- Adicionar testes E2E no frontend
- Configurar CI/CD básico

#### Fase 3: Features Completas (2-3 semanas)
**Objetivo:** Completar funcionalidades críticas

- Integrar gateway de pagamento real
- Implementar serviço de email real
- Completar autenticação (refresh token, recuperação de senha)
- Implementar carrinho de compras no frontend
- Adicionar paginação e filtragem

#### Fase 4: Performance e Resiliência (1-2 semanas)
**Objetivo:** Preparar para carga de produção

- Implementar cache (Redis)
- Adicionar Circuit Breakers
- Configurar índices de banco
- Implementar Flyway
- Configurar health checks

#### Fase 5: Observabilidade e Operação (1-2 semanas)
**Objetivo:** Permitir monitoramento e troubleshooting

- Configurar dashboards Grafana
- Implementar tracing distribuído
- Configurar alertas
- Documentar runbooks
- Implementar logging estruturado

### Esforço Estimado

| Fase | Esforço (1 dev) | Esforço (2 devs) |
|------|-----------------|------------------|
| Fase 1 | 2 semanas | 1 semana |
| Fase 2 | 3 semanas | 1.5 semanas |
| Fase 3 | 3 semanas | 1.5 semanas |
| Fase 4 | 2 semanas | 1 semana |
| Fase 5 | 2 semanas | 1 semana |
| **TOTAL** | **12 semanas** | **6 semanas** |

**RECOMENDAÇÃO:** Com 2 desenvolvedores dedicados, o projeto pode estar production-ready em **6 semanas** seguindo o roadmap acima.

### Recomendações Imediatas

**PRIORIDADE MÁXIMA (Esta Semana):**
1. ✅ Criar `.gitignore` para evitar commits acidentais
2. 🔴 Corrigir bug de compra de ingressos (bloqueante)
3. 🔴 Externalizar credenciais (segurança crítica)
4. 🔴 Implementar captura de userId (segurança)
5. ⚠️ Adicionar Actuator/Micrometer (observabilidade básica)

**PRÓXIMAS 2 SEMANAS:**
6. Implementar exception handling global
7. Adicionar validações de entrada
8. Implementar testes unitários básicos (50%+ cobertura)
9. Configurar health checks no Docker
10. Documentar APIs com Swagger

### Métricas do Projeto

| Métrica | Valor Atual | Valor Desejado |
|---------|-------------|----------------|
| Cobertura de Testes | 0% | 80%+ |
| Bugs Críticos | 3 | 0 |
| Vulnerabilidades de Segurança | 7+ | 0 |
| APIs Documentadas | 0% | 100% |
| Microsserviços com Métricas | 0/9 | 9/9 |
| Features Completas | ~40% | 100% |

### Conclusão Final

Este é um **projeto promissor** com uma arquitetura sólida e tecnologias adequadas. A documentação inicial e a estrutura demonstram planejamento cuidadoso. No entanto, é necessário um esforço significativo em **segurança, testes e funcionalidades completas** antes de ser considerado production-ready.

**VEREDICTO:** 🟡 **BOM COMEÇO, REQUER MAIS TRABALHO**

Com dedicação e seguindo o roadmap proposto, este projeto pode se tornar um sistema robusto e escalável de venda de ingressos.

---

**Documento gerado em:** 02 de novembro de 2025
**Versão:** 1.0
**Próxima revisão recomendada:** Após implementação da Fase 1

---

## APÊNDICE A - Comandos Úteis

### Verificar Dependências Desatualizadas
```bash
# Maven
mvn versions:display-dependency-updates

# npm
npm outdated
```

### Executar Scans de Segurança
```bash
# OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check

# npm audit
npm audit
npm audit fix

# Snyk
snyk test
```

### Análise de Código
```bash
# SonarQube
mvn sonar:sonar

# Checkstyle
mvn checkstyle:check

# ESLint
npm run lint
```

### Testes
```bash
# Executar todos os testes
mvn test

# Executar com cobertura
mvn test jacoco:report

# Frontend
npm test
npm test -- --coverage
```

### Docker
```bash
# Build e start
docker-compose up --build

# Ver logs de um serviço
docker-compose logs -f auth-service

# Restart um serviço
docker-compose restart auth-service

# Limpar tudo
docker-compose down -v
```

---

## APÊNDICE B - Recursos e Links Úteis

### Spring Boot
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring Security](https://docs.spring.io/spring-security/reference/index.html)

### Testes
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Testcontainers](https://www.testcontainers.org/)

### Observabilidade
- [Prometheus Documentation](https://prometheus.io/docs/introduction/overview/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Micrometer Documentation](https://micrometer.io/docs)

### Best Practices
- [12 Factor App](https://12factor.net/)
- [Microservices Patterns](https://microservices.io/patterns/index.html)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

---

**FIM DO RELATÓRIO**