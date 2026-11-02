# Hospedagem de teste, sem custo

**Status:** proposta, 01/11/2026 · **Para:** a entrega de 15/11/2026 e os testes com o celular
de outra pessoa · **Não é** hospedagem de produção.

O objetivo é um só: **o link do cardápio abrir no celular de quem não está na rede da casa**,
para o teste e para a apresentação, sem gastar nada. Os preços e limites abaixo foram
**conferidos em 01/11/2026** nas páginas dos fornecedores; planos gratuitos mudam sem aviso
(a Oracle cortou o dela pela metade em 2026), então confira de novo antes de usar.

## O que a pilha pede

Cinco serviços Java (gateway, `identity`, `merchant`, `catalog`, `order`), PostgreSQL, MongoDB
e RabbitMQ — os perfis `marco2` e `pedidos`. No Compose cada serviço Java tem até 512 MB, então
a pilha cabe em **uns 4 a 5 GB de memória**. Isso descarta os planos gratuitos de "um serviço
pequeno que dorme" para o back, e deixa duas opções reais.

## Opção A — recomendada para 15/11: o computador de quem apresenta, com túnel da Cloudflare

**Custo zero, sem conta e sem cartão.** O *Quick Tunnel* da Cloudflare dá um endereço público
`https://<palavras-aleatórias>.trycloudflare.com` que aponta para uma porta do computador. A pilha
continua no Docker Desktop de quem apresenta, exatamente como hoje.

**Limites, da documentação da Cloudflare:** até 200 requisições simultâneas (passa disso, 429),
sem garantia de disponibilidade, e o endereço **muda a cada vez** que o túnel sobe. Só funciona
com o computador ligado e o túnel aberto. Para um teste e uma apresentação, basta.

São **dois túneis**: um para o gateway (a API) e um para o front. O front chama a API pelo
endereço público, e o gateway precisa aceitar a origem do front no CORS.

1. Instale o `cloudflared` (Windows: `winget install --id Cloudflare.cloudflared`).
2. Abra o túnel do front **primeiro**, para saber o endereço dele:
   `cloudflared tunnel --url http://localhost:4173` → anote o `https://…trycloudflare.com` (B).
3. Abra o túnel da API, em outro terminal: `cloudflared tunnel --url http://localhost:8080` →
   anote o endereço (A).
4. No `.env`: `CORS_ALLOWED_ORIGINS=http://localhost:5173,<B>` e recrie o gateway
   (`docker compose --profile marco2 --profile pedidos up -d gateway`).
5. Gere o front apontando para a API pública e sirva o build:
   ```powershell
   cd frontend
   $env:VITE_API_BASE_URL = "<A>"; npm run build
   npx vite preview --port 4173 --strictPort
   ```
6. Abra `<B>` no celular, entre, e copie o link do cardápio em Configurações: ele já sai com o
   endereço público.

**O que falta no código para isto funcionar** (tarefa do time, abaixo): o Vite 7 recusa
endereço que não seja `localhost` (*"Blocked request. This host is not allowed"*). É preciso
liberar `.trycloudflare.com` em `preview.allowedHosts` no `vite.config.ts`. Não foi feito
nesta rodada para não mexer na configuração sem o time validar.

## Opção B — se quiserem um endereço que fica no ar: a VM gratuita da Oracle

O plano *Always Free* da Oracle Cloud dá uma VM ARM (Ampere A1) de **2 OCPUs e 12 GB** — o limite
atual é 1.500 horas de OCPU e 9.000 GB-hora por mês —, com 200 GB de disco. Cabe a pilha
inteira, com o mesmo `docker compose`. As imagens que usamos (Temurin, PostgreSQL, MongoDB,
RabbitMQ) têm versão ARM.

**Os poréns:** o cadastro pede **cartão de crédito** para verificar a identidade (não cobra,
mas o banco pode reservar um valor por uns dias; cartão pré-pago e virtual não servem); a
região escolhida pode ficar dias sem capacidade (*"out of host capacity"*); a Oracle pode
recolher a VM se ela ficar sete dias com CPU, rede e memória abaixo de 20%; e não há suporte.
O endereço público continua sendo um túnel da Cloudflare (o mesmo da opção A, na VM) — um
endereço fixo e bonito pede domínio, e domínio custa.

**Esforço:** um dia de uma pessoa, contando o cadastro, a VM, o Docker e o primeiro deploy.

## O que não recomendamos para o back

Os planos gratuitos de PaaS (Render e parecidos) **dormem** o serviço depois de 15 minutos sem
acesso e levam cerca de um minuto para acordar; o PostgreSQL gratuito do Render **expira em 30
dias** e tem 1 GB. Com cinco serviços Java que se chamam entre si, cada um dormindo por conta
própria, o primeiro pedido da apresentação seria o que acorda a pilha. Para o **front**
sozinho, Cloudflare Pages, Netlify ou Vercel servem de graça — mas com a opção A o front já
sai do mesmo computador, e um serviço a mais é uma configuração a mais.

## Cuidados, em qualquer opção

- **Senhas de verdade no `.env`** da máquina exposta, e não as de exemplo. O `.env` nunca vai
  para o Git.
- `DELIVERY_CODIGO_NA_TELA=true` **só** no teste, e **só com contas de teste**: com ela, qualquer
  pessoa cria conta com qualquer telefone **e troca a senha de qualquer conta** pelo "Esqueci
  minha senha", porque o código aparece na tela. Com o endereço público aberto, quem souber um
  telefone cadastrado entra na conta dele. Use números de teste, e desligue a bandeira assim que o
  teste acabar.
- Feche os túneis depois do teste. Endereço `trycloudflare.com` é público enquanto estiver
  aberto.

## Fontes

- Cloudflare — [Quick Tunnels (TryCloudflare)](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/)
- Oracle — [Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/resourceref.htm)
- [FreeTierWatch — a redução do plano da Oracle](https://buttondown.com/freetierwatch/archive/oracle-quietly-halved-its-famous-free-tier-and/)
- [Space-Node — limites e cadastro do Always Free em 2026](https://space-node.net/blog/oracle-cloud-always-free-limits-2026)
- Render — [Deploy for Free](https://render.com/docs/free)
