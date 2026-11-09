# Operação — procedimentos

Um documento por procedimento. Cada um responde **como se executa**, sob pressão
e sem improviso. É o terceiro tipo de documento do repositório:

| | Responde |
|---|---|
| `docs/architecture/decisions/` | *por que* se decidiu assim |
| `docs/dominio/` | *qual é a regra* hoje |
| **`docs/operacao/`** | *como se executa* |

## Os procedimentos

| Documento | Quando se abre | Base |
|---|---|---|
| [`exclusao-de-titular.md`](exclusao-de-titular.md) | um titular pede exclusão dos dados dele | ADR-013 |
| [`mensagem-na-fila-morta.md`](mensagem-na-fila-morta.md) | o alerta de fila morta disparou — ou apareceu sobra no fechamento | ADR-026 |
| [`recuperacao-de-acesso.md`](recuperacao-de-acesso.md) | o administrador de uma loja perdeu o acesso e o canal não resolve | ADR-029 |
| [`hospedagem-de-teste.md`](hospedagem-de-teste.md) | o link do cardápio precisa abrir fora da rede de casa — teste ou apresentação, sem custo | ADR-077 |

## Material de apoio

Não são procedimentos: são documentos que existem para serem **revisados por
alguém de fora**.

| Documento | Para quê |
|---|---|
| [`legitimo-interesse.md`](legitimo-interesse.md) | o teste de balanceamento das duas hipóteses de legítimo interesse — rascunho de engenharia, para revisão jurídica |
| [`revisao-juridica.md`](revisao-juridica.md) | a pauta da consulta com advogado: o que foi assumido, com que raciocínio, e o que muda conforme a resposta |

## O registro das passagens no navegador

Não é procedimento de operação do produto: é o registro dos testes feitos por gente, que várias
ADRs citam como prova. Para testar hoje, o caminho é o [`../como-testar.md`](../como-testar.md).

| Documento | O que guarda |
|---|---|
| [`passagem-humana.md`](passagem-humana.md) | como a pilha foi preparada para as passagens de outubro, na máquina com o Docker dentro do WSL, e o que a passagem de 19/10/2026 achou |

## Uma regra comum aos quatro procedimentos

Todos registram **quem autorizou, com que prova, quando**. O registro é
somente-inserção, como os ajustes de valor — e o nome de quem autorizou sobrevive
ao desligamento, anonimizado só depois dos cinco anos (ADR-013 §5).

Procedimento sem rastro é decisão sem dono.
