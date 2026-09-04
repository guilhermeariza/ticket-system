# Serviço de Eventos

Este serviço é o coração do negócio, responsável por gerenciar todos os aspectos relacionados a eventos e ingressos.

## Responsabilidades

- **CRUD de Eventos:** Permite a criação, leitura, atualização e exclusão de eventos.
- **Gerenciamento de Ingressos:** Controla os tipos de ingressos para cada evento, seus preços e a quantidade disponível.
- **Consulta Pública:** Expõe endpoints públicos para que os usuários possam listar eventos disponíveis e ver os detalhes de um evento específico.
- **Controle de Estoque:** Fornece mecanismos para consultar a disponibilidade de ingressos.

## Diagrama de Classe

As principais entidades são `Event` e `TicketType`, mostrando uma relação de um-para-muitos.

```mermaid
classDiagram
    class Event {
        +Long id
        +String name
        +String description
        +LocalDateTime date
        +String location
        +List<TicketType> ticketTypes
    }

    class TicketType {
        +Long id
        +String name (e.g., "VIP", "Pista")
        +BigDecimal price
        +int totalQuantity
        +int availableQuantity
    }

    Event "1" -- "*" TicketType : has
```

## Diagrama de Sequência: Listagem de Eventos

Este é um fluxo simples e público para qualquer visitante do site.

```mermaid
sequenceDiagram
    participant U as Visitante
    participant FE as Frontend App
    participant GW as API Gateway
    participant ES as Serviço de Eventos
    participant DB as Eventos DB

    U->>FE: Acessa a página de eventos
    FE->>+GW: GET /events-service/api/events
    GW->>+ES: Encaminha a requisição
    ES->>+DB: SELECT * FROM events WHERE date > NOW()
    DB-->>-ES: Retorna a lista de eventos futuros
    ES-->>-GW: Responde com a lista de eventos (JSON)
    GW-->>-FE: Retorna a lista
    FE->>U: Renderiza a lista de eventos na tela
```
