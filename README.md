# Desafio Técnico Fullstack - Backend (Instituto SENAI de Inovação)

Este repositório contém a implementação para o [desafio técnico da vaga de Desenvolvedor Fullstack Júnior](https://github.com/isi-tics/desafio-isi-dev-1). O [backend](#inicio-backend) da aplicação foi desenvolvido em Java com Spring Boot, e o [frontend](#inicio-frontend) foi desenvolvido com Typescript e React. A aplicação simula um fluxo básico de vendas, focando no gerenciamento de produtos e na aplicação de descontos com regras de negócio complexas.

## Execução com Docker

O projeto está containerizado com Docker.

### Pré-requisitos
- Docker
- Docker Compose

### Passos para Executar

1.  Na raiz do projeto, execute o comando para construir as imagens e subir os contêineres:
    ```bash
    docker-compose up -d --build
    ```
2.  Aguarde o processo de build ser finalizado.
3.  Acesse o frontend em `http://localhost:5173`.
4.  Acesse a API do backend em `http://localhost:8080`.

<h2 id="inicio-backend">Backend</h2>

### Tecnologias Utilizadas

- **Java 21**: Versão LTS mais recente da linguagem.
- **Spring Boot 3.5**: Framework principal para construção da aplicação.
- **Spring Web**: Para a construção dos endpoints RESTful.
- **Spring Data JPA**: Para a camada de persistência de dados.
- **Spring Validation**: Para validação de requests recebidas
- **Maven**: Gerenciador de dependências e build do projeto.
- **H2 Database**: Banco de dados relacional em memória para desenvolvimento e testes.
- **Jackson**: Para serialização/deserialização de JSON, incluindo o módulo `JavaTimeModule` para datas ISO-8601.
- **JSON Patch (com.github.fge:json-patch)**: Biblioteca para implementação de atualizações parciais (`PATCH`) seguindo a RFC 6902.
- **JUnit 5 & Mockito**: Para a suíte de testes unitários da camada de serviço.
- **JaCoCo**: Para uma cobertura de testes completa.

### Setup e Execução (Como Rodar o Projeto)

#### Pré-requisitos
- **Java JDK 21** (ou superior)
- **Apache Maven 3.8** (ou superior)

#### Passos para Executar

1.  **Clone o repositório:**
    ```bash
    git clone <url-do-seu-repositorio>
    ```

2.  **Navegue até a pasta do backend:**
    ```bash
    cd senai-isi-challenge/backend
    ```

3.  **Execute a aplicação com Maven:**
    ```bash
    mvn spring-boot:run
    ```

A API estará disponível em `http://localhost:8080`. O banco de dados H2 é configurado em memória e pode ser acessado em `http://localhost:8080/h2-console` com as configurações padrão do Spring Boot.

#### Variáveis de Ambiente
Atualmente, o projeto não requer variáveis de ambiente para ser executado, pois utiliza configurações padrão e um banco de dados em memória.

#### Postman collections
No diretório do projeto, foi disponibilizado a collection e o arquivo das variáveis de ambiente do Postman, contendo todos os endpoints disponíveis, utilizada para testes do backend.

#### Seed de Dados
O projeto utiliza um arquivo data.sql para popular o banco de dados na inicialização, criando 10 produtos e 15 cupons para facilitar testes e demonstrações.

### Estrutura do Projeto

O projeto segue uma arquitetura em camadas.

```
└── br/com/lmuniz/desafio/senai/
    ├── controllers/        # Camada de API: expõe os endpoints REST
    │   └── exceptions/     # Handler global de exceções (@ControllerAdvice)
    ├── domains/            # Camada de domínio
    │   ├── dtos/           # Data transfer objects (para requests e responses)
    │   │   ├── coupons/
    │   │   ├── discounts/
    │   │   └── products/
    │   ├── entities/       # Entidades JPA que mapeiam o banco de dados
    │   └── enums/          # Enumerações de negócio (ex: CouponEnum)
    ├── repositories/       # Camada de acesso a dados (Interfaces Spring Data JPA)
    │   └── specifications/ # Lógica para queries dinâmicas (JPA Specifications)
    |── serializers/        # Lógica para desserialização customizada de JSON (interpretar formatos brasileiros e internacionais de decimal)
    ├── services/           # Camada de serviço, onde reside a lógica de negócio
    │   └── exceptions/     # Exceções customizadas
    └── utils/              # Classes utilitárias (ex: normalização do Strings)
```

### Decisões Técnicas e Arquitetura

Durante o desenvolvimento, algumas decisões de arquitetura foram tomadas para atender aos requisitos e garantir a qualidade do código:

* **Tratamento de Erros Centralizado:** Foi implementado um `@ControllerAdvice` (`ExceptionHandlerController`) para interceptar exceções lançadas pela camada de serviço. Exceções customizadas e semânticas (ex: `ResourceNotFoundException`, `ResourceConflictException`) são mapeadas para os códigos de status HTTP corretos (`404`, `409`, etc.), garantindo respostas de erro padronizadas e claras.

* **Modelo de Descontos Separado:** Para atender à regra de negócio de "desconto percentual direto" e "cupom promocional", optou-se por criar duas entidades de aplicação distintas (`ProductCouponApplication` e `ProductDirectDiscountApplication`). Essa abordagem mantém o modelo de dados explícito e evita sobrecarregar uma única tabela com responsabilidades mistas.

* **Listagem Avançada com JPA Specifications:** Para o endpoint `GET /products`, que exige múltiplos filtros opcionais, foi implementado o padrão `Specification` do Spring Data JPA. Isso permite a construção de queries dinâmicas de forma segura e modular, evitando a concatenação de strings SQL/JPQL e resultando em um código mais limpo e manutenível. Para evitar o problema de N+1 queries, a busca por descontos é feita em lote (`batch fetch`) apenas para os produtos da página atual.

* **Remoção de desconto ao realizar uma operação de PATCH no valor do produtor** Como não especificado nos requisitos, foi implementada a regra de negócio de que qualquer alteração no preço de um produto `(PATCH /products/{id})` ou alteração no tipo ou valor de um cupom `(PATCH /coupons/{id})` remove automaticamente qualquer desconto ativo no produto ou que esteja utilizando o cupom alterado. Esta decisão garante a consistência dos dados e evita que produtos fiquem com preços finais diferentes do que deveria ser.

* **Filtragem de Cupons Válidos:** O endpoint `GET /coupons` foi desenvolvido com um parâmetro opcional `?onlyValid=true` para permitir que o cliente da API liste apenas os cupons que estão dentro do período de validade e que ainda possuem usos disponíveis.

### Documentação da API (Endpoints)

A API está disponível no base path `/api/v1`.

#### Endpoints de Produtos
| Verbo HTTP | Rota                                    | Descrição                                         |
|:-----------|:----------------------------------------|:----------------------------------------------------|
| `GET`      | `/products`                             | Lista, filtra e pagina todos os produtos.           |
| `POST`     | `/products`                             | Cria um novo produto.                               |
| `GET`      | `/products/{id}`                        | Busca os detalhes de um produto específico.         |
| `PATCH`    | `/products/{id}`                        | Atualiza parcialmente um produto (JSON Patch).      |
| `DELETE`   | `/products/{id}`                        | Inativa (soft-delete) um produto.                   |
| `POST`     | `/products/{id}/restore`                | Reativa um produto inativado.                       |
| `POST`     | `/products/{id}/discount/coupon`        | Aplica um cupom de desconto a um produto.           |
| `POST`     | `/products/{id}/discount/percent`       | Aplica um desconto percentual direto a um produto.  |
| `DELETE`   | `/products/{id}/discount`               | Remove qualquer desconto ativo de um produto.       |

#### Endpoints de Cupons
| Verbo HTTP | Rota                 | Descrição                               |
|:-----------|:---------------------|:----------------------------------------|
| `GET`      | `/coupons`           | Lista todos os cupons (com filtro opcional). |
| `POST`     | `/coupons`           | Cria um novo cupom.                     |
| `GET`      | `/coupons/{id}`      | Busca os detalhes de um cupom específico. |
| `PATCH`    | `/coupons/{id}`      | Atualiza parcialmente um cupom (JSON Patch). |
| `DELETE`   | `/coupons/{id}`      | Inativa (soft-delete) um cupom.         |

#### Query Param para `GET /coupons/{id}`

O endpoint de listagem de cupons aceita o parâmetro de filtragem `onlyValid`:

##### Exemplo:

Requisição
```
http://localhost:8080/coupons?onlyValid=true
```

Resposta
```
[
    {
        "id": 1,
        "code": "promo10",
        "type": "percent",
        "value": 10.00,
        "oneShot": false,
        "maxUses": 100,
        "validFrom": "2025-06-23T17:31:03.576387Z",
        "validUntil": "2025-07-24T17:31:03.576387Z"
    },
    {
        "id": 2,
        "code": "desconto25",
        "type": "percent",
        "value": 25.00,
        "oneShot": false,
        "maxUses": 50,
        "validFrom": "2025-06-14T17:31:03.576387Z",
        "validUntil": "2025-07-14T17:31:03.576387Z"
    },
    {
        "id": 4,
        "code": "relampago5",
        "type": "percent",
        "value": 5.00,
        "oneShot": false,
        "maxUses": null,
        "validFrom": "2025-06-24T16:31:03.576387Z",
        "validUntil": "2025-07-24T18:31:03.576387Z"
    }
]
```


#### Query Params para `GET /products`

O endpoint de listagem de produtos aceita os seguintes parâmetros para filtragem, ordenação e paginação:

| Parâmetro | Tipo | Descrição | Exemplo |
| :--- | :--- | :--- | :--- |
| `page` | `integer` | Define o número da página (começando em 0). Padrão: `0`. | `?page=0` |
| `size` | `integer` | Define a quantidade de itens por página. Padrão: `20`. | `?size=10` |
| `sort` | `string` | Define o campo de ordenação e a direção. Formato: `campo,direcao`. | `?sort=stock,desc` |
| `search` | `string` | Busca textual no nome, nome normalizado e na descrição do produto. | `?search=café` |
| `minPrice` | `decimal` | Filtra produtos com preço maior ou igual ao valor informado. | `?minPrice=46.00` |
| `maxPrice` | `decimal` | Filtra produtos com preço menor ou igual ao valor informado. | `?maxPrice=2000.00` |
| `hasDiscount` | `boolean` | Filtra produtos que possuem algum desconto ativo quando `true`. | `?hasDiscount=true` |
| `withCouponApplied`| `boolean` | Filtra produtos que possuem um desconto vindo de um **cupom** (`true`). | `?withCouponApplied=false`|
| `onlyOutOfStock`| `boolean` | Se `true`, retorna apenas produtos com estoque igual a zero. | `?onlyOutOfStock=false` |
| `includeDeleted` | `boolean` | Se `true`, inclui produtos inativados (soft-deleted) na busca. | `?includeDeleted=true` | 

##### Exemplo:

Requisição
```
http://localhost:8080/products?page=0&size=10&withCouponApplied=false&hasDiscount=true&maxPrice=2000&minPrice=46&onlyOutOfStock=false&search=cafe&includeDeleted=true&sort=stock,asc
```

Resposta
```
{
    "content": [
        {
            "id": 6,
            "name": "Moedor de Café Manual",
            "description": "Lâminas de cerâmica para uma moagem precisa.",
            "stock": 40,
            "isOutOfStock": false,
            "price": 89.90,
            "finalPrice": 26.97,
            "discount": {
                "type": "percent",
                "value": 70.00,
                "appliedAt": "2025-06-24T14:07:08.532380Z"
            },
            "hasCouponApplied": false,
            "createdAt": "2025-06-24T14:05:46.734298Z",
            "updatedAt": null
        },
        {
            "id": 1,
            "name": "Cafeteira Elétrica Mondial",
            "description": "Prepara até 20 xícaras de café.",
            "stock": 50,
            "isOutOfStock": false,
            "price": 199.90,
            "finalPrice": 179.91,
            "discount": {
                "type": "percent",
                "value": 10.00,
                "appliedAt": "2025-06-24T14:06:40.567611Z"
            },
            "hasCouponApplied": true,
            "createdAt": "2025-06-24T14:05:46.734298Z",
            "updatedAt": null
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 10,
        "sort": {
            "empty": false,
            "unsorted": false,
            "sorted": true
        },
        "offset": 0,
        "unpaged": false,
        "paged": true
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 2,
    "numberOfElements": 2,
    "first": true,
    "size": 10,
    "number": 0,
    "sort": {
        "empty": false,
        "unsorted": false,
        "sorted": true
    },
    "empty": false
}
```

#### Dificuldades encontradas:
* Acabei demorando um pouco nos endpoints de `PATCH`, por conta de sempre estar trabalhando com `PUT`. O `PATCH` é um pouco mais trabalhoso do que o `PUT`, porque não temos o auxilio de ferramentas como o `jakarta validation`, então temos que fazer todas as validações na mão.
* Por conta desse problema com o tempo não consegui realizar os testes unitários e de integração por completo.

#### Testes

* Mesmo após o encerramento da data de entrega, resolvi realizar os testes para deixar o projeto completo.
* Os testes unitários foram realizados com o auxílio do plugin JaCoCo para uma cobertura de testes aprofundada.

`![Relatório de Cobertura JaCoCo](https://github.com/user-attachments/assets/c6fb3ab1-0d46-49ed-9786-269f784dc983)`

<h2 id="inicio-frontend">Frontend</h2>

### Screenshots

#### Tela principal
![Image](https://github.com/user-attachments/assets/9850be5c-1e7f-4942-aa12-055ffaeeaaca)

#### Tela principal (Filtro por texto e por produtos com desconto)
![Image](https://github.com/user-attachments/assets/9902770f-6330-43e2-bc95-48464ac082b0)

#### Tela de cadastro (Tratamento de exceções)
![Image](https://github.com/user-attachments/assets/8726e0d4-e904-462f-8966-39e786fa0feb)

#### Dialog de desconto direto
![Image](https://github.com/user-attachments/assets/52301600-0a9e-417e-a132-3820284a1580)

#### Dialog de cupom de desconto
![Image](https://github.com/user-attachments/assets/1b15fe6c-d324-4fee-8e70-e095deca61e4)

### Tecnologias Utilizadas

-   **React 18+**: Biblioteca principal para o desenvolvimento da interface.
-   **TypeScript**: Por conta da tipagem estática.
-   **Vite**: Ambiente de desenvolvimento rápido e fácil de mexer.
-   **React Router DOM**: Para o gerenciamento de rotas e navegação (client-side routing).
-   **Axios**: Para realizar as chamadas HTTP para a API backend de forma organizada.
-   **CSS Puros / Módulos CSS**: Para estilização.

### Setup e Execução Local

##### Pré-requisitos

-   **Node.js v20.x** (ou superior)
-   **Yarn**

##### Passos para Rodar

1.  **Clone o repositório** (caso ainda não tenha feito).

2.  **Navegue até a pasta do frontend:**
    ```bash
    cd senai-isi-challenge/frontend
    ```

3.  **Instale as dependências:**
    ```bash
    yarn install
    ```

4.  **Execute a aplicação em modo de desenvolvimento:**
    ```bash
    yarn dev
    ```

A aplicação estará em `http://localhost:5173`.

##### Variáveis de Ambiente

Para que o frontend consiga se comunicar com o backend, crie um arquivo `.env` na raiz da pasta `frontend/` com base no `.env.example` fornecido no diretório.

**`.env.example`**
```
VITE_BACKEND_URL=
```

**`.env` (Exemplo de preenchimento)**
```
VITE_BACKEND_URL=http://localhost:8080
```

### Estrutura do Projeto

O projeto segue uma arquitetura componentizada,

```
/src
├── assets/      # Armazena arquivos estáticos como os ícones SVG utilizados na interface.
├── components/  # Contém componentes de UI genéricos e reutilizáveis (ex: Button, Input, PageHeader).
├── models/      # Define as interfaces e tipos TypeScript que representam os DTOs e modelos de dados.
├── routes/      # Contém os componentes que funcionam como as páginas principais da aplicação (ex: ProductsCatalog).
├── service/     # Centraliza a lógica de comunicação com a API backend, utilizando uma instância do Axios.
└── utils/       # Agrupa funções utilitárias, como métodos de formatação, validações e constantes do sistema.
```

### Decisões de Arquitetura e Design

* **Comunicação com API:** As chamadas à API foram centralizadas em um módulo (`/api`) que utiliza uma instância pré-configurada do `axios`. Isso facilita a manutenção, a adição de interceptors (para tratamento de token ou erros) e evita a repetição da URL base do backend no código.

* **Roteamento com Layout Persistente:** Foi utilizado o `react-router-dom` para criar um sistema de rotas aninhadas. Um componente `Layout` central renderiza a `Sidebar` e o `Header` de forma persistente, enquanto o componente `<Outlet />` renderiza dinamicamente o conteúdo da página atual, uma abordagem moderna e eficiente para SPAs.

* **Melhoria de Usabilidade (UX) na Deleção de Produtos e Descontos:** Ao meu ver, o fluxo, que entendi pelo mockup, para inativar um produto e remover um desconto poderia causar confusão no usuário. Para criar uma experiência de usuário mais clara e segura, repensei a funcionalidade:

1. A ação principal de inativar o produto foi mantida no ícone de lixeira na tabela, um padrão de mercado facilmente reconhecível.

2. A ação secundária de remover um desconto foi tornada mais contextual. O usuário agora passa o mouse sobre o "badge" do desconto, que se transforma em um botão de remoção. Isso torna a ação intencional e evita que um desconto seja removido por engano.

### Dificuldades encontradas:
* No frontend tive muito problema com tempo, sinto que poderia ter feito uma arquitetura de componentes reutilizáveis melhor.