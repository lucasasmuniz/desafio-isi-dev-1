# Desafio Técnico Fullstack - Backend (Instituto SENAI de Inovação)

Este repositório contém a implementação para o [desafio técnico da vaga de Desenvolvedor Fullstack Júnior](https://github.com/isi-tics/desafio-isi-dev-1). O backend da aplicação foi desenvolvido em Java com Spring Boot, e o frontend foi desenvolvido com Typescript e React. A aplicação simula um fluxo básico de vendas, focando no gerenciamento de produtos e na aplicação de descontos com regras de negócio complexas.


## Backend

### 1. Tecnologias Utilizadas

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

### 2. Setup e Execução (Como Rodar o Projeto)

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

### 3. Estrutura do Projeto

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

### 4. Decisões Técnicas e Arquitetura

Durante o desenvolvimento, algumas decisões de arquitetura foram tomadas para atender aos requisitos e garantir a qualidade do código:

* **Tratamento de Erros Centralizado:** Foi implementado um `@ControllerAdvice` (`ExceptionHandlerController`) para interceptar exceções lançadas pela camada de serviço. Exceções customizadas e semânticas (ex: `ResourceNotFoundException`, `ResourceConflictException`) são mapeadas para os códigos de status HTTP corretos (`404`, `409`, etc.), garantindo respostas de erro padronizadas e claras.

* **Modelo de Descontos Separado:** Para atender à regra de negócio de "desconto percentual direto" e "cupom promocional", optou-se por criar duas entidades de aplicação distintas (`ProductCouponApplication` e `ProductDirectDiscountApplication`). Essa abordagem mantém o modelo de dados explícito e evita sobrecarregar uma única tabela com responsabilidades mistas.

* **Listagem Avançada com JPA Specifications:** Para o endpoint `GET /products`, que exige múltiplos filtros opcionais, foi implementado o padrão `Specification` do Spring Data JPA. Isso permite a construção de queries dinâmicas de forma segura e modular, evitando a concatenação de strings SQL/JPQL e resultando em um código mais limpo e manutenível. Para evitar o problema de N+1 queries, a busca por descontos é feita em lote (`batch fetch`) apenas para os produtos da página atual.

* **Remoção de desconto ao realizar uma operação de PATCH no valor do produtor** Como não especificado nos requisitos, foi implementada a regra de negócio de que qualquer alteração no preço de um produto `(PATCH /products/{id})` ou alteração no tipo ou valor de um cupom `(PATCH /coupons/{id})` remove automaticamente qualquer desconto ativo no produto ou que esteja utilizando o cupom alterado. Esta decisão garante a consistência dos dados e evita que produtos fiquem com preços finais diferentes do que deveria ser.

* **Filtragem de Cupons Válidos:** O endpoint `GET /coupons` foi desenvolvido com um parâmetro opcional `?onlyValid=true` para permitir que o cliente da API liste apenas os cupons que estão dentro do período de validade e que ainda possuem usos disponíveis.

### 5. Documentação da API (Endpoints)

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