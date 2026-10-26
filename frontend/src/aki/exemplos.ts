/**
 * Os produtos de exemplo do protótipo (`aki_delivery/js/data.js`), usados no
 * modo demonstração e na área do cliente, que ainda não tem rota no back-end.
 */
export interface ProdutoDeExemplo {
  readonly id: string;
  readonly nome: string;
  readonly tipo: string;
  readonly descricao: string;
  readonly preco: number;
  readonly imagem: string;
  readonly selo: string;
}

export const PRODUTOS_DE_EXEMPLO: readonly ProdutoDeExemplo[] = [
  {
    id: '1',
    nome: 'Aki Classic',
    tipo: 'Hambúrgueres',
    descricao: 'Pão brioche, blend artesanal, queijo, alface, tomate e molho especial.',
    preco: 24.9,
    imagem:
      'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=600&q=75',
    selo: 'Mais pedido',
  },
  {
    id: '2',
    nome: 'X-Bacon Aki',
    tipo: 'Hambúrgueres',
    descricao: 'Blend artesanal, queijo, bacon crocante e molho especial da casa.',
    preco: 29.9,
    imagem:
      'https://images.unsplash.com/photo-1553979459-d2229ba7433b?auto=format&fit=crop&w=600&q=75',
    selo: 'Favorito',
  },
  {
    id: '3',
    nome: 'Smash Duplo',
    tipo: 'Hambúrgueres',
    descricao: 'Dois smash burgers, cheddar cremoso, cebola e molho Aki.',
    preco: 34.9,
    imagem:
      'https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=600&q=75',
    selo: 'Novo',
  },
  {
    id: '4',
    nome: 'X-Egg Especial',
    tipo: 'Hambúrgueres',
    descricao: 'Carne, queijo, ovo, presunto, alface, tomate e maionese.',
    preco: 27.9,
    imagem:
      'https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '5',
    nome: 'Batata Frita',
    tipo: 'Porções',
    descricao: 'Batatas douradas, crocantes e sequinhas.',
    preco: 14.9,
    imagem:
      'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '6',
    nome: 'Batata Cheddar & Bacon',
    tipo: 'Porções',
    descricao: 'Batata crocante coberta com cheddar e bacon.',
    preco: 19.9,
    imagem:
      'https://images.unsplash.com/photo-1585109649139-366815a0d713?auto=format&fit=crop&w=600&q=75',
    selo: 'Mais pedido',
  },
  {
    id: '7',
    nome: 'Onion Rings',
    tipo: 'Porções',
    descricao: 'Anéis de cebola empanados, dourados e crocantes.',
    preco: 17.9,
    imagem:
      'https://images.unsplash.com/photo-1639024471283-03518883512d?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '8',
    nome: 'Nuggets Crocantes',
    tipo: 'Porções',
    descricao: 'Nuggets crocantes acompanhados de molho especial.',
    preco: 16.9,
    imagem:
      'https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '9',
    nome: 'Hot Dog Especial',
    tipo: 'Hot Dogs',
    descricao: 'Salsicha, purê, queijo, milho, batata palha e molhos.',
    preco: 22.9,
    imagem:
      'https://img0.didiglobal.com/static/soda_public/img_54443e1df42a6c452fceaab6bd1c54e6.jpg',
    selo: '',
  },
  {
    id: '10',
    nome: 'Hot Dog Bacon',
    tipo: 'Hot Dogs',
    descricao: 'Salsicha, bacon, queijo, milho, ervilha e batata palha.',
    preco: 26.9,
    imagem:
      'https://www.thedailymeal.com/img/gallery/10-creative-hot-dog-ideas-for-your-next-backyard-bash-slideshow/2_bacon%20dog_andiphoto-Shutterstock.jpg',
    selo: '',
  },
  {
    id: '11',
    nome: 'Combo Aki Classic',
    tipo: 'Combos',
    descricao: 'Aki Classic, batata frita e refrigerante lata.',
    preco: 36.9,
    imagem:
      'https://images.unsplash.com/photo-1571091718767-18b5b1457add?auto=format&fit=crop&w=600&q=75',
    selo: 'Oferta',
  },
  {
    id: '12',
    nome: 'Combo X-Bacon',
    tipo: 'Combos',
    descricao: 'X-Bacon, batata cheddar com bacon e refrigerante.',
    preco: 45.9,
    imagem:
      'https://images.unsplash.com/photo-1550317138-10000687a72b?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '13',
    nome: 'Combo Família',
    tipo: 'Combos',
    descricao: 'Dois hambúrgueres, batata grande e duas bebidas.',
    preco: 69.9,
    imagem:
      'https://images.unsplash.com/photo-1565299507177-b0ac66763828?auto=format&fit=crop&w=600&q=75',
    selo: 'Economize',
  },
  {
    id: '14',
    nome: 'Milkshake Chocolate',
    tipo: 'Bebidas',
    descricao: 'Milkshake cremoso de chocolate com cobertura.',
    preco: 16.9,
    imagem:
      'https://images.unsplash.com/photo-1572490122747-3968b75cc699?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '15',
    nome: 'Milkshake Morango',
    tipo: 'Bebidas',
    descricao: 'Milkshake cremoso de morango com calda.',
    preco: 16.9,
    imagem:
      'https://images.unsplash.com/photo-1553787499-6f9133860278?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '16',
    nome: 'Refrigerante Lata',
    tipo: 'Bebidas',
    descricao: 'Coca-Cola, Guaraná ou Pepsi — 350 ml.',
    preco: 6.0,
    imagem:
      'https://images.unsplash.com/photo-1629203849820-fdd70d49c38e?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '19',
    nome: 'Suco de Laranja',
    tipo: 'Bebidas',
    descricao: 'Suco natural de laranja, preparado na hora — 300 ml.',
    preco: 8.9,
    imagem:
      'https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=600&q=75',
    selo: 'Natural',
  },
  {
    id: '20',
    nome: 'Suco de Maracujá',
    tipo: 'Bebidas',
    descricao: 'Suco refrescante de maracujá — 300 ml.',
    preco: 9.9,
    imagem:
      'https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '21',
    nome: 'Suco de Morango',
    tipo: 'Bebidas',
    descricao: 'Suco cremoso de morango, servido gelado — 300 ml.',
    preco: 10.9,
    imagem:
      'https://images.unsplash.com/photo-1553530666-ba11a7da3888?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
  {
    id: '22',
    nome: 'Suco de Abacaxi com Hortelã',
    tipo: 'Bebidas',
    descricao: 'Suco natural de abacaxi com hortelã — 300 ml.',
    preco: 9.9,
    imagem:
      'https://espaconatelie.com.br/wp-content/uploads/2024/09/Suco-de-abacaxi-com-hortela.jpg',
    selo: '',
  },
  {
    id: '17',
    nome: 'Açaí Especial',
    tipo: 'Sobremesas',
    descricao: 'Açaí com banana, morango, granola e leite em pó.',
    preco: 21.9,
    imagem:
      'https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/A%C3%A7a%C3%AD.jpg/600px-A%C3%A7a%C3%AD.jpg',
    selo: 'Refrescante',
  },
  {
    id: '23',
    nome: 'Sorvete Cremoso',
    tipo: 'Sobremesas',
    descricao: 'Duas bolas de sorvete cremoso nos sabores chocolate, morango ou creme.',
    preco: 12.9,
    imagem:
      'https://upload.wikimedia.org/wikipedia/commons/thumb/1/12/Ice_Cream_Scoops.JPG/600px-Ice_Cream_Scoops.JPG',
    selo: '',
  },
  {
    id: '24',
    nome: 'Pudim de Leite',
    tipo: 'Sobremesas',
    descricao: 'Pudim de leite condensado cremoso com calda de caramelo.',
    preco: 10.9,
    imagem: 'https://live.staticflickr.com/3311/3544225805_df46b4d51e_o.jpg',
    selo: '',
  },
  {
    id: '18',
    nome: 'Brownie com Sorvete',
    tipo: 'Sobremesas',
    descricao: 'Brownie de chocolate com sorvete e calda.',
    preco: 19.9,
    imagem:
      'https://images.unsplash.com/photo-1564355808539-22fda35bed7e?auto=format&fit=crop&w=600&q=75',
    selo: '',
  },
];
export const TIPOS_DE_EXEMPLO: readonly (readonly [string, string])[] = [
  ['Hambúrgueres', 'lunch_dining'],
  ['Porções', 'fastfood'],
  ['Hot Dogs', 'kebab_dining'],
  ['Combos', 'local_fire_department'],
  ['Bebidas', 'local_cafe'],
  ['Sobremesas', 'icecream'],
];
