package com.deliveryplatform.catalog.infrastructure.semeadura;

import java.util.List;

/**
 * O cardápio de exemplo do protótipo ({@code frontend/src/aki/exemplos.ts}), agora gravado
 * no banco da "Pizzaria da Fixture" — ADR-059, emenda de 01/11/2026. Até então ele só existia
 * no modo demonstração do front, sem servidor; o modo saiu da tela de entrada e a loja de teste
 * passou a ser a da fixture.
 *
 * <p><b>As fotos são links, não arquivos.</b> O {@code imagemRef} do produto aceita uma URL
 * absoluta, e o front a usa como está ({@code urlDaApi}). Todas são de licença livre: Unsplash ou
 * Wikimedia Commons. As da Commons usam o endereço {@code Special:FilePath}, que a própria
 * Commons mantém e que redireciona para o arquivo. Sem internet a foto não carrega, e o cardápio
 * mostra a imagem padrão — o resto funciona.
 */
final class CardapioDeDemonstracao {

    record Item(String nome, String descricao, String preco, String imagem) {
    }

    record Secao(String nome, List<Item> itens) {
    }

    /** Em ordem: a ordem da lista é a ordem do cardápio. */
    static final List<Secao> SECOES = List.of(
            new Secao("Hambúrgueres", List.of(
                    new Item("Aki Classic", "Pão brioche, blend artesanal, queijo, alface, tomate e molho especial.", "24.90",
                            "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=600&q=75"),
                    new Item("X-Bacon Aki", "Blend artesanal, queijo, bacon crocante e molho especial da casa.", "29.90",
                            "https://images.unsplash.com/photo-1553979459-d2229ba7433b?auto=format&fit=crop&w=600&q=75"),
                    new Item("Smash Duplo", "Dois smash burgers, cheddar cremoso, cebola e molho Aki.", "34.90",
                            "https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=600&q=75"),
                    new Item("X-Egg Especial", "Carne, queijo, ovo, presunto, alface, tomate e maionese.", "27.90",
                            "https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5?auto=format&fit=crop&w=600&q=75")
            )),
            new Secao("Porções", List.of(
                    new Item("Batata Frita", "Batatas douradas, crocantes e sequinhas.", "14.90",
                            "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=600&q=75"),
                    new Item("Batata Cheddar & Bacon", "Batata crocante coberta com cheddar e bacon.", "19.90",
                            "https://images.unsplash.com/photo-1585109649139-366815a0d713?auto=format&fit=crop&w=600&q=75"),
                    new Item("Onion Rings", "Anéis de cebola empanados, dourados e crocantes.", "17.90",
                            "https://images.unsplash.com/photo-1639024471283-03518883512d?auto=format&fit=crop&w=600&q=75"),
                    new Item("Nuggets Crocantes", "Nuggets crocantes acompanhados de molho especial.", "16.90",
                            "https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=600&q=75")
            )),
            new Secao("Hot Dogs", List.of(
                    new Item("Hot Dog Especial", "Salsicha, purê, queijo, milho, batata palha e molhos.", "22.90",
                            "https://commons.wikimedia.org/wiki/Special:FilePath/Hot_Dog_with_Fries_(5076896494)_(2).jpg?width=600"),
                    new Item("Hot Dog Bacon", "Salsicha, bacon, queijo, milho, ervilha e batata palha.", "26.90",
                            "https://commons.wikimedia.org/wiki/Special:FilePath/Chicago-style_Hot_Dog.jpg?width=600")
            )),
            new Secao("Combos", List.of(
                    new Item("Combo Aki Classic", "Aki Classic, batata frita e refrigerante lata.", "36.90",
                            "https://images.unsplash.com/photo-1571091718767-18b5b1457add?auto=format&fit=crop&w=600&q=75"),
                    new Item("Combo X-Bacon", "X-Bacon, batata cheddar com bacon e refrigerante.", "45.90",
                            "https://images.unsplash.com/photo-1550317138-10000687a72b?auto=format&fit=crop&w=600&q=75"),
                    new Item("Combo Família", "Dois hambúrgueres, batata grande e duas bebidas.", "69.90",
                            "https://images.unsplash.com/photo-1565299507177-b0ac66763828?auto=format&fit=crop&w=600&q=75")
            )),
            new Secao("Bebidas", List.of(
                    new Item("Milkshake Chocolate", "Milkshake cremoso de chocolate com cobertura.", "16.90",
                            "https://images.unsplash.com/photo-1572490122747-3968b75cc699?auto=format&fit=crop&w=600&q=75"),
                    new Item("Milkshake Morango", "Milkshake cremoso de morango com calda.", "16.90",
                            "https://images.unsplash.com/photo-1553787499-6f9133860278?auto=format&fit=crop&w=600&q=75"),
                    new Item("Refrigerante Lata", "Coca-Cola, Guaraná ou Pepsi — 350 ml.", "6.00",
                            "https://images.unsplash.com/photo-1629203849820-fdd70d49c38e?auto=format&fit=crop&w=600&q=75"),
                    new Item("Suco de Laranja", "Suco natural de laranja, preparado na hora — 300 ml.", "8.90",
                            "https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=600&q=75"),
                    new Item("Suco de Maracujá", "Suco refrescante de maracujá — 300 ml.", "9.90",
                            "https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=600&q=75"),
                    new Item("Suco de Morango", "Suco cremoso de morango, servido gelado — 300 ml.", "10.90",
                            "https://images.unsplash.com/photo-1553530666-ba11a7da3888?auto=format&fit=crop&w=600&q=75"),
                    new Item("Suco de Abacaxi com Hortelã", "Suco natural de abacaxi com hortelã — 300 ml.", "9.90",
                            "https://commons.wikimedia.org/wiki/Special:FilePath/Pineapple.jpg?width=600")
            )),
            new Secao("Sobremesas", List.of(
                    new Item("Açaí Especial", "Açaí com banana, morango, granola e leite em pó.", "21.90",
                            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/A%C3%A7a%C3%AD.jpg/600px-A%C3%A7a%C3%AD.jpg"),
                    new Item("Sorvete Cremoso", "Duas bolas de sorvete cremoso nos sabores chocolate, morango ou creme.", "12.90",
                            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/12/Ice_Cream_Scoops.JPG/600px-Ice_Cream_Scoops.JPG"),
                    new Item("Pudim de Leite", "Pudim de leite condensado cremoso com calda de caramelo.", "10.90",
                            "https://commons.wikimedia.org/wiki/Special:FilePath/Pudim_de_Leite.jpg?width=600"),
                    new Item("Brownie com Sorvete", "Brownie de chocolate com sorvete e calda.", "19.90",
                            "https://images.unsplash.com/photo-1564355808539-22fda35bed7e?auto=format&fit=crop&w=600&q=75")
            ))
    );

    /** O produto que ganha os grupos de opções que a demonstração mostrava. */
    static final String COM_OPCOES = "Aki Classic";

    private CardapioDeDemonstracao() {
    }
}
