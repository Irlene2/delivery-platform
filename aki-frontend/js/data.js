/* Catálogo local do AKI Delivery — imagens armazenadas em assets/cardapio/ */
const products=[
{id:1,name:'Aki Classic',category:'Hambúrgueres',description:'Pão brioche, blend artesanal, queijo, alface, tomate e molho especial.',price:24.90,image:'../assets/cardapio/01-aki-classic.png',tag:'Mais pedido'},
{id:2,name:'X-Bacon Aki',category:'Hambúrgueres',description:'Blend artesanal, queijo, bacon crocante e molho especial da casa.',price:29.90,image:'../assets/cardapio/02-x-bacon-aki.png',tag:'Favorito'},
{id:3,name:'Smash Duplo',category:'Hambúrgueres',description:'Dois smash burgers, cheddar cremoso, cebola e molho Aki.',price:34.90,image:'../assets/cardapio/03-smash-duplo.png',tag:'Novo'},
{id:4,name:'X-Egg Especial',category:'Hambúrgueres',description:'Carne, queijo, ovo, presunto, alface, tomate e maionese.',price:27.90,image:'../assets/cardapio/04-x-egg-especial.png'},
{id:5,name:'Batata Frita',category:'Porções',description:'Batatas douradas, crocantes e sequinhas.',price:14.90,image:'../assets/cardapio/05-batata-frita.png'},
{id:6,name:'Batata Cheddar & Bacon',category:'Porções',description:'Batata crocante coberta com cheddar e bacon.',price:19.90,image:'../assets/cardapio/06-batata-cheddar-bacon.png',tag:'Mais pedido'},
{id:7,name:'Onion Rings',category:'Porções',description:'Anéis de cebola empanados, dourados e crocantes.',price:17.90,image:'../assets/cardapio/07-onion-rings.png'},
{id:8,name:'Nuggets Crocantes',category:'Porções',description:'Nuggets crocantes acompanhados de molho especial.',price:16.90,image:'../assets/cardapio/08-nuggets-crocantes.png'},
{id:9,name:'Hot Dog Especial',category:'Hot Dogs',description:'Salsicha, purê, queijo, milho, batata palha e molhos.',price:22.90,image:'../assets/cardapio/09-hot-dog-especial.png'},
{id:10,name:'Hot Dog Bacon',category:'Hot Dogs',description:'Salsicha, bacon, queijo, milho, ervilha e batata palha.',price:26.90,image:'../assets/cardapio/10-hot-dog-bacon.png'},
{id:11,name:'Combo Aki Classic',category:'Combos',description:'Aki Classic, batata frita e refrigerante lata.',price:36.90,image:'../assets/cardapio/11-combo-aki-classic.png',tag:'Oferta'},
{id:12,name:'Combo X-Bacon',category:'Combos',description:'X-Bacon, batata cheddar com bacon e refrigerante.',price:45.90,image:'../assets/cardapio/12-combo-x-bacon.png'},
{id:13,name:'Combo Família',category:'Combos',description:'Dois hambúrgueres, batata grande e duas bebidas.',price:69.90,image:'../assets/cardapio/13-combo-familia.png',tag:'Economize'},
{id:14,name:'Milkshake Chocolate',category:'Bebidas',description:'Milkshake cremoso de chocolate com cobertura.',price:16.90,image:'../assets/cardapio/14-milkshake-chocolate.png'},
{id:15,name:'Milkshake Morango',category:'Bebidas',description:'Milkshake cremoso de morango com calda.',price:16.90,image:'../assets/cardapio/15-milkshake-morango.png'},
{id:16,name:'Refrigerante Lata',category:'Bebidas',description:'Coca-Cola, Guaraná ou Pepsi — 350 ml.',price:6.00,image:'../assets/cardapio/16-refrigerante-lata.png'},
{id:17,name:'Açaí Especial',category:'Sobremesas',description:'Açaí com banana, morango, granola e leite em pó.',price:21.90,image:'../assets/cardapio/17-acai-especial.png',tag:'Refrescante'},
{id:18,name:'Sorvete Cremoso',category:'Sobremesas',description:'Duas bolas de sorvete cremoso nos sabores chocolate, morango ou creme.',price:12.90,image:'../assets/cardapio/18-sorvete-cremoso.png'},
{id:19,name:'Pudim de Leite',category:'Sobremesas',description:'Pudim de leite condensado cremoso com calda de caramelo.',price:10.90,image:'../assets/cardapio/19-pudim-de-leite.png'},
{id:20,name:'Brownie com Sorvete',category:'Sobremesas',description:'Brownie de chocolate com sorvete e calda.',price:19.90,image:'../assets/cardapio/20-brownie-com-sorvete.png'},
{id:21,name:'Suco de Laranja',category:'Bebidas',description:'Suco natural de laranja, preparado na hora — 300 ml.',price:8.90,image:'../assets/cardapio/21-suco-de-laranja.png',tag:'Natural'},
{id:22,name:'Suco de Maracujá',category:'Bebidas',description:'Suco refrescante de maracujá — 300 ml.',price:9.90,image:'../assets/cardapio/22-suco-de-maracuja.png'},
{id:23,name:'Suco de Morango',category:'Bebidas',description:'Suco cremoso de morango, servido gelado — 300 ml.',price:10.90,image:'../assets/cardapio/23-suco-de-morango.png'},
{id:24,name:'Suco de Abacaxi com Hortelã',category:'Bebidas',description:'Suco natural de abacaxi com hortelã — 300 ml.',price:9.90,image:'../assets/cardapio/24-suco-de-abacaxi-com-hortela.png'}
];
const categories=[['Todos','restaurant_menu'],['Hambúrgueres','lunch_dining'],['Porções','fastfood'],['Hot Dogs','kebab_dining'],['Combos','local_fire_department'],['Bebidas','local_cafe'],['Sobremesas','icecream']];
const money=v=>Number(v).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
