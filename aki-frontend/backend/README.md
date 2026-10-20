# Backend inicial — AKI Delivery

API de referência em Node.js + Express e esquema inicial PostgreSQL. **Este backend está separado e NÃO está integrado ao frontend.** As rotas de login, cadastro e pedidos são apenas esqueletos; não salvam dados nem autenticam usuários.

## Requisitos
- Node.js 20+
- PostgreSQL (opcional para apenas iniciar as rotas demonstrativas; conexão ainda não está configurada no código)

## Executar
```bash
npm install
cp .env.example .env
npm run dev
```
Windows PowerShell: `Copy-Item .env.example .env`

API local: `http://localhost:3000`
- `GET /api/v1/health`
- `GET /api/v1/catalog`
- `GET /api/v1/orders`
- `POST /api/v1/orders` (retorna 501 por ainda não implementar persistência)
- `POST /api/v1/users/register` (retorna 501 por ainda não implementar persistência)
- `POST /api/v1/users/login` (retorna 501; autenticação não implementada)

## Banco de dados
`sql/schema.sql` contém um esboço inicial do schema PostgreSQL. Não é aplicado automaticamente. Revise e execute manualmente somente quando configurar o PostgreSQL.

## Próximas etapas
1. Configurar e testar conexão PostgreSQL.
2. Aplicar migrações e seeds.
3. Implementar validação, hash de senha e autenticação segura.
4. Implementar CRUD de produtos e pedidos com transações.
5. Só então integrar frontend e backend em uma etapa explícita.

Não use este esqueleto em produção: ainda não tem autenticação, autorização, persistência nem integração de pagamentos.
