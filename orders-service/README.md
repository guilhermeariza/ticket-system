# Serviço de Pedidos

Este serviço gerencia o ciclo de vida de uma ordem de compra de ingressos.

## Responsabilidades

- **Criação de Pedidos:** Expõe um endpoint para que um usuário possa iniciar um processo de compra, criando um pedido com o status "PENDENTE".
- **Gerenciamento de Estado:** Controla o estado do pedido, que pode ser `PENDING`, `PAID`, `CANCELLED`, etc.
- **Comunicação Assíncrona:** Ao criar um pedido, publica um evento `OrderCreatedEvent` no RabbitMQ. Isso desacopla o fluxo de pagamento e notificação.
- **Consulta de Pedidos:** Permite que um usuário consulte seu histórico de pedidos.

## Diagrama de Classe

A entidade `Order` representa uma compra, contendo os itens do pedido.

```mermaid
classDiagram
    class Order {
        +Long id
        +String userId
        +BigDecimal totalAmount
        +OrderStatus status
        +LocalDateTime createdAt
        +List<OrderItem> items
    }

    class OrderItem {
        +Long id
        +Long ticketTypeId
        +int quantity
        +BigDecimal unitPrice
    }

    class OrderStatus {
        <<Enumeration>>
        PENDING
        PAID
        CANCELLED
    }

    Order "1" -- "*" OrderItem : contains
    Order -- OrderStatus
```

## Diagrama de Sequência: Criação de Pedido

Este diagrama mostra a criação de um pedido e a publicação de um evento no RabbitMQ.

```mermaid
sequenceDiagram
    participant U as Usuário (logado)
    participant FE as Frontend App
    participant GW as API Gateway
    participant OP as Serviço de Pedidos
    participant DB as Pedidos DB
    participant MQ as RabbitMQ

    U->>FE: Seleciona ingressos e clica em "Comprar"
    FE->>+GW: POST /orders-service/api/orders (com Token JWT)
    GW->>+OP: Encaminha a requisição
    Note over OP: Valida a requisição (ex: verifica se há ingressos suficientes)
    OP->>+DB: INSERT INTO orders (status='PENDING', ...)
    DB-->>-OP: Retorna o pedido criado com ID
    OP-->>-GW: Responde com os detalhes do pedido (201 Created)
    GW-->>-FE: Retorna a resposta
    FE->>U: Redireciona para a página de pagamento

    par
        OP->>MQ: Publica evento `OrderCreatedEvent`
    and
        Note over MQ: Mensagem fica na fila para ser consumida
    end
```
