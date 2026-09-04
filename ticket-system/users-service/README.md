# Users Service

Este serviço é responsável pelo gerenciamento completo dos dados dos usuários (CRUD - Create, Read, Update, Delete).

## Responsabilidades

- **CRUD de Usuários:** Fornece endpoints para criar, ler, atualizar e deletar perfis de usuários.
- **Gerenciamento de Perfil:** Permite que os usuários atualizem suas próprias informações, como nome, e-mail, etc.
- **Consulta de Dados:** Permite que outros serviços consultem informações de usuários por ID.

## Diagrama de Classe

A entidade principal é a `UserProfile`, que contém informações públicas e semi-públicas do usuário.

```mermaid
classDiagram
    class UserProfile {
        +Long id
        +String userId (foreign key from Auth Service)
        +String firstName
        +String lastName
        +String email
        +Date registrationDate
    }
```

## Diagrama de Sequência: Atualização de Perfil

Este diagrama mostra o fluxo para um usuário atualizar suas informações de perfil.

```mermaid
sequenceDiagram
    participant U as Usuário (logado)
    participant FE as Frontend App
    participant GW as API Gateway
    participant US as Users Service
    participant DB as Users DB

    U->>FE: Edita seu perfil e salva
    FE->>+GW: PUT /users-service/api/users/{userId} (com Token JWT)
    Note over GW: Valida o Token JWT e autoriza a requisição
    GW->>+US: Encaminha a requisição
    US->>+DB: Busca o perfil do usuário pelo {userId}
    DB-->>-US: Retorna o perfil atual
    US->>US: Atualiza os campos do perfil com os novos dados
    US->>+DB: Salva (UPDATE) o perfil atualizado
    DB-->>-US: Confirma a atualização
    US-->>-GW: Responde com o perfil atualizado (200 OK)
    GW-->>-FE: Retorna a resposta
    FE->>U: Exibe mensagem de sucesso
```
