# Análise Geral do Projeto: Estado Atual e Próximos Passos

## 1. Visão Geral

O projeto consiste em um sistema de venda de ingressos baseado em uma arquitetura de microsserviços. A estrutura geral do projeto é sólida e bem definida, utilizando tecnologias como Spring Boot, Spring Cloud, React, Docker e RabbitMQ.

No entanto, o projeto no estado atual é um **boilerplate funcional** e **não uma aplicação completa**. A infraestrutura básica está no lugar, mas a lógica de negócio principal está incompleta ou ausente.

Este documento detalha os pontos críticos que precisam ser abordados para que a aplicação se torne funcional.

## 2. Pontos Críticos a Serem Corrigidos

### 2.1. Configuração Centralizada (Config Server)

- **Problema:** O `config-server` está configurado para usar um repositório Git de exemplo da Spring (`https://github.com/spring-cloud-samples/config-repo`).
- **Impacto:** Impede que a aplicação utilize sua própria configuração, tornando-a não funcional.
- **Solução:**
    1. Criar um novo repositório Git para armazenar os arquivos de configuração (`.yml`) de cada microsserviço.
    2. Migrar as configurações dos arquivos `application.yml` locais para o novo repositório.
    3. Atualizar o `application.yml` do `config-server` para apontar para o novo repositório.

### 2.2. Lógica de Negócio Incompleta

- **Problema:** A lógica de negócio principal está incompleta em vários serviços.
- **Exemplos:**
    - **`servico-pedidos`:**
        - A lógica para decrementar a quantidade de ingressos no `servico-eventos` após a criação de um pedido está comentada (`// TODO`).
        - Existe um bug na chamada ao `servico-eventos` para verificar a quantidade de ingressos.
    - **`payments-service`:** A lógica de processamento de pagamento é uma simulação. Não há integração com um gateway de pagamento real.
    - **`notifications-service`:** A lógica de envio de e-mail é um placeholder. Não há integração com um serviço de e-mail real.
- **Impacto:** A aplicação não consegue completar o fluxo principal de venda de ingressos.
- **Solução:** Implementar a lógica de negócio faltante em cada um dos serviços.

### 2.3. Segurança

- **Problema:** Segredos, como a chave de assinatura do JWT no `auth-service`, estão hardcoded nos arquivos `application.yml`.
- **Impacto:** Risco de segurança significativo.
- **Solução:** Externalizar todos os segredos e configurações sensíveis para o `config-server` e utilizar o Vault ou variáveis de ambiente para gerenciar os segredos.

### 2.4. Ausência de Testes

- **Problema:** Não há testes unitários, de integração ou de contrato.
- **Impacto:** Dificulta a manutenção e a evolução do código, além de aumentar o risco de bugs.
- **Solução:** Implementar uma estratégia de testes abrangente, incluindo:
    - Testes unitários com JUnit e Mockito.
    - Testes de integração com Testcontainers.
    - Testes de contrato com Spring Cloud Contract.

## 3. Próximos Passos Recomendados

1.  **Corrigir a configuração do `config-server`** para que a aplicação possa ser iniciada com sua própria configuração.
2.  **Implementar a lógica de negócio principal**, começando pelo fluxo de criação de pedido e decremento de ingressos.
3.  **Externalizar os segredos** para o `config-server`.
4.  **Adicionar testes** para garantir a qualidade e a manutenibilidade do código.
5.  **Desenvolver o frontend** para consumir os serviços do backend.
6.  **Implementar resiliência** com Circuit Breakers (Resilience4j) para lidar com falhas na comunicação entre os serviços.
7.  **Melhorar a observabilidade** com tracing distribuído (Micrometer Tracing) para facilitar o debug em um ambiente de microsserviços.
