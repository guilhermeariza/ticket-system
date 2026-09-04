# Configuração Manual do Usuário

Este documento descreve os passos de configuração que precisam ser realizados manualmente para completar a configuração do ambiente e preparar a aplicação para produção.

---

## 1. Configuração do `config-server` com Git (Opcional, para Produção)

Para o desenvolvimento, o `config-server` foi configurado para ler arquivos de um diretório local (`config-repo`), o que simplifica a execução. Para um ambiente de produção, é altamente recomendável usar um repositório Git para versionar e gerenciar as configurações de forma centralizada.

**Passos:**

1.  **Crie um Repositório Git:** Crie um novo repositório privado no GitHub, GitLab, Bitbucket, etc.

2.  **Envie os Arquivos de Configuração:** Adicione os arquivos do diretório local `config-repo` ao seu novo repositório Git e faça o push.

3.  **Atualize a Configuração do `config-server`:**
    -   Abra o arquivo `config-server/src/main/resources/application.yml`.
    -   Comente ou remova o perfil `native` e a configuração `search-locations`.
    -   Adicione a configuração do Git, apontando para o seu repositório. Se o repositório for privado, você precisará configurar credenciais de acesso.

    ```yaml
    spring:
      application:
        name: config-server
      cloud:
        config:
          server:
            git:
              uri: <URL_DO_SEU_REPOSITORIO_GIT>
              # username: <seu-usuario> # (se for privado)
              # password: <seu-token-de-acesso> # (se for privado)
    ```

4.  **Reconstrua a Imagem do `config-server`:**
    ```sh
    docker-compose build config-server
    ```

---

## 2. Integração com Gateway de Pagamento Real

O `payments-service` atualmente **simula** o processamento de pagamentos. Para processar transações reais, você precisa integrá-lo com um provedor de pagamentos como Stripe, Mercado Pago, etc.

**Passos:**

1.  **Escolha um Provedor:** Crie uma conta em um gateway de pagamento e obtenha suas chaves de API (API Key e Secret Key).

2.  **Adicione as Credenciais:** Armazene as chaves de forma segura no seu `config-server` (no arquivo `payments-service.yml` do seu repositório Git de configuração).

3.  **Implemente a Lógica de Integração:**
    -   Adicione a dependência do SDK do provedor de pagamento no `pom.xml` do `payments-service`.
    -   No `PaymentService.java`, substitua a simulação (`Thread.sleep`) pela lógica de chamada real à API do gateway de pagamento, usando o SDK.
    -   A variável `paymentSuccess` deve ser definida com base na resposta real do gateway.

---

## 3. Integração com Serviço de Envio de E-mail

O `notifications-service` **simula** o envio de notificações (apenas registra logs). Para enviar e-mails de verdade, você precisa integrá-lo com um serviço de e-mail transacional como Amazon SES, SendGrid ou usar um servidor SMTP.

**Passos:**

1.  **Configure um Provedor de E-mail:** Obtenha as credenciais SMTP (host, port, username, password) do seu provedor.

2.  **Adicione as Credenciais:** Armazene as credenciais de forma segura no seu `config-server` (no arquivo `notifications-service.yml`).

3.  **Implemente a Lógica de Envio:**
    -   Adicione a dependência `spring-boot-starter-mail` no `pom.xml` do `notifications-service`.
    -   Configure o `JavaMailSender` do Spring com as credenciais do seu provedor.
    -   No `NotificationListener.java`, injete o `JavaMailSender` e substitua a simulação (`log.info`) pela lógica real de envio de e-mail.
    -   Considere usar um motor de templates (como Thymeleaf ou FreeMarker) para criar e-mails HTML ricos.
