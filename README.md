# RefriCenter — Java / Spring Boot

Migração do backend Python/Django para Java 21 + Spring Boot 3.

## Stack

| Componente | Tecnologia |
|---|---|
| Framework | Spring Boot 3.3 |
| ORM | Spring Data JPA + Hibernate |
| Banco | PostgreSQL |
| Autenticação | JWT (stateless) |
| Migrations | Flyway |
| Build | Gradle |

## Como rodar

```bash
# 1. Variáveis de ambiente necessárias
export DATABASE_URL=jdbc:postgresql://localhost:5432/refricenter
export DB_USER=postgres
export DB_PASSWORD=senha
export JWT_SECRET=sua-chave-secreta-de-32-caracteres-minimo

# 2. Build e execução
./gradlew bootRun
```

O servidor sobe na porta **8000** (configurável via `PORT`).

## Mudança importante no frontend

O backend Django usava **sessão com cookie**. O Java usa **JWT**.

Após o login, o frontend recebe um campo `token` na resposta. É necessário:

1. Armazenar o token (`localStorage.setItem('token', data.token)`)
2. Enviar em todas as requisições: `Authorization: Bearer <token>`

### Exemplo de configuração Axios:

```js
// Após login
const { data } = await axios.post('/api/auth/login', { username, password });
localStorage.setItem('token', data.token);
axios.defaults.headers.common['Authorization'] = `Bearer ${data.token}`;

// Ao iniciar o app
const token = localStorage.getItem('token');
if (token) axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
```

## Estrutura

```
src/main/java/com/refricenter/
├── config/          # SecurityConfig, WebConfig
├── security/        # JwtTokenProvider, JwtAuthenticationFilter
├── model/           # Entidades JPA (20 classes)
├── repository/      # Interfaces Spring Data JPA
├── service/         # Lógica de negócio
├── controller/      # Controllers REST (mesmas URLs do Django)
└── dto/             # DTOs de request/response
```

## Endpoints equivalentes

Todos os endpoints `/api/*` são os mesmos do Django. Veja `assistencias/urls.py` original para referência.

## Módulos a implementar pelo time

- [ ] Importação/exportação Excel (estoque e clientes) — usar Apache POI já no build.gradle
- [ ] Integração completa com Focus NFe (FiscalController tem estrutura, falta a chamada HTTP)
- [ ] Backup/restauração do banco
- [ ] Relatórios detalhados (RelatorioController)
- [ ] Envio de email de notificações
