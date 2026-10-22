# Painel do Restaurante

Spring Boot 3 (Java 21) + MySQL + frontend estático (Chart.js). Login com token assinado (HMAC-SHA256), senhas em BCrypt, bloqueio de 1 min após 5 falhas.

iFood e 99Food estão **desativados** (`Canal.disponivel = false`). Para ativar: troque para `true` em `Canal.java`, adicione os apps no `DataSeeder` e remova `INDISPONIVEIS` em `frontend/app.js`.

## Local
- Rápido (H2 + dados demo, login `admin` / `admin123`): `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- Com MySQL: `docker compose up --build` (login `admin`; a senha aparece no log da API)
- Frontend: `cd frontend && npx serve -l 3000`

## Variáveis do backend
| Variável | Uso |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão MySQL |
| `JWT_SECRET` | Obrigatória em produção, 32+ caracteres (`openssl rand -hex 32`) |
| `ADMIN_USER`, `ADMIN_PASSWORD` | Primeiro usuário (criado só se a tabela estiver vazia) |
| `CORS_ORIGINS` | URL do frontend na Vercel |
| `APP_SEED` | `true` carrega dados demo. Use `false` em produção |
| `TOKEN_HORAS` | Validade da sessão (padrão 12) |

## Hospedagem gratuita (verificada em 21/10/2026)
| Serviço | Grátis? | Limites |
|---|---|---|
| Vercel (frontend) | Sim | Plano Hobby |
| Render (backend) | Sim, para testes | 512 MB RAM, dorme após 15 min, ~1 min para acordar, 750 h/mês, disco apagado a cada deploy |
| Aiven MySQL | Sim, sem prazo | 1 GB, 1 CPU, pode desligar se ficar ocioso (religa manualmente) |
| TiDB Cloud Starter (MySQL) | Sim | 5 GiB, 50 mi RUs/mês; bloqueia conexões ao esgotar |
| Railway | Quase | Teste de 30 dias com US$ 5; depois US$ 1/mês de crédito |
| Fly.io | Não | Sem plano grátis para contas novas |

Render grátis não é indicado para produção. Para uso real, use um plano pago (Render Starter, Railway) ou uma VPS.

## Passo a passo
1. **GitHub**: crie um repositório e envie esta pasta inteira.
2. **Banco (Aiven)**: aiven.io → Create service → MySQL → plano Free. Copie host, porta, usuário e senha.
   URL JDBC: `jdbc:mysql://HOST:PORTA/defaultdb?sslMode=REQUIRED`
   (TiDB Cloud: `jdbc:mysql://HOST:4000/test?sslMode=VERIFY_IDENTITY`)
3. **Backend (Render)**: New → Web Service → conecte o repositório.
   - Language: Docker · Root Directory: `backend` · Instance Type: Free
   - Environment: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_USER`, `ADMIN_PASSWORD`, `APP_SEED=false`
   - Deploy. Anote a URL (`https://xxxx.onrender.com`).
4. **Frontend**: em `frontend/vercel.json`, troque `SEU-BACKEND.onrender.com` pela URL do passo 3. Faça commit e push.
5. **Vercel**: Add New → Project → mesmo repositório → Root Directory: `frontend` → Framework Preset: Other → Deploy.
6. **CORS**: na Render, defina `CORS_ORIGINS` com a URL da Vercel e faça redeploy.
7. **Acesso**: abra a URL da Vercel e entre com `ADMIN_USER` / `ADMIN_PASSWORD`. Use "Alterar senha".
   A primeira requisição após inatividade pode falhar (o backend está acordando). Tente de novo após 1 min.

## API
Todas exigem `Authorization: Bearer <token>`, exceto `POST /api/auth/login`.
| Método | Rota | Função |
|---|---|---|
| POST | `/api/auth/login` | `{usuario, senha}` → `{token, expiraEm}` |
| POST | `/api/auth/senha` | Troca a senha |
| GET | `/api/pedidos?abertos=&canal=&delivery=&data=` | Lista pedidos |
| POST | `/api/pedidos` | Cria pedido |
| PATCH | `/api/pedidos/{id}/status` | NOVO → EM_PREPARO → PRONTO → CONCLUIDO / CANCELADO |
| GET | `/api/mesas` | Mesas com conta aberta |
| POST | `/api/mesas/{n}/fechar` | Fecha a conta |
| GET/PATCH | `/api/apps`, `/api/apps/{id}` | Apps de entrega |
| GET | `/api/analises?periodo=DIARIO\|MENSAL&data=` | Análises |

## Pendências
- O backend não foi compilado no ambiente de criação (Maven Central bloqueado). Rode `mvn package` e corrija eventuais erros.
- Sem integração real com apps de entrega.
- Um único usuário administrador; sem cadastro de outros usuários.
