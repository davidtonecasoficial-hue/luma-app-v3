# L --- Animação da Navegação Inferior e Pesquisa

## Especificação de interface e comportamento para Android

**Objetivo:** reproduzir na aplicação L a navegação inferior observada
no vídeo de referência, incluindo a animação do indicador de separador
ativo e a transformação da área inferior quando o utilizador abre a
Pesquisa.

------------------------------------------------------------------------

## 1. Nome da animação

O efeito principal pode ser descrito como:

**Animated Bottom Navigation com Sliding Pill Indicator e Morphing
Search Bar.**

Em português:

**Barra de navegação inferior animada com indicador deslizante e
transformação para campo de pesquisa.**

Não existe necessariamente um único nome universal para este conjunto de
efeitos. A descrição técnica acima identifica os movimentos que devem
ser implementados.

### Elementos da animação

1.  **Sliding Pill Indicator (indicador deslizante):** uma cápsula
    arredondada e translúcida desloca-se suavemente para destacar o
    separador selecionado.
2.  **Animated Selection (seleção animada):** o ícone e o texto do
    separador ativo mudam de cor, tamanho ou opacidade de forma suave.
3.  **Morphing Transition (transição de transformação):** ao abrir a
    Pesquisa, a navegação inferior transforma-se numa área de pesquisa
    fixa na parte inferior.
4.  **Shared Container Transition (transição de contentor partilhado):**
    a mudança entre os estados deve parecer contínua, como se os
    elementos pertencessem ao mesmo componente.
5.  **Fade and Slide:** os elementos que entram ou saem combinam
    deslocamento vertical discreto com alteração de opacidade.

------------------------------------------------------------------------

## 2. Estrutura da navegação inferior

A barra principal deve conter:

-   **Início:** abre a página inicial.
-   **Biblioteca:** abre a coleção multimédia local.
-   **Ajustes:** abre as definições.
-   **Pesquisa:** abre a página de pesquisa.

A Pesquisa aparece como um botão circular separado, à direita da cápsula
que agrupa os outros separadores, conforme a referência visual.

### Estado normal

-   A barra fica fixa na parte inferior da interface, respeitando a área
    segura do Android.
-   Início, Biblioteca e Ajustes ficam agrupados numa cápsula
    arredondada.
-   O botão Pesquisa fica separado, mas alinhado verticalmente com a
    cápsula.
-   O separador atual recebe um fundo em formato de cápsula, com
    animação deslizante.
-   Os ícones e rótulos permanecem alinhados e legíveis.
-   A barra não deve cobrir o conteúdo nem o Mini Player.

### Comportamento do indicador

Quando o utilizador toca em Início, Biblioteca ou Ajustes:

1.  O indicador em formato de cápsula desloca-se horizontalmente até ao
    novo separador.
2.  O movimento deve ser suave, com aceleração e desaceleração naturais.
3.  O ícone e o rótulo do separador selecionado recebem destaque.
4.  O separador anterior perde o destaque sem saltos bruscos.
5.  O conteúdo da página muda com uma transição discreta e consistente.

**Importante:** o indicador deve deslizar entre posições; não deve
desaparecer e reaparecer de forma brusca em cada toque.

------------------------------------------------------------------------

## 3. Animação ao abrir a Pesquisa

Ao tocar no botão circular de Pesquisa, a interface deve entrar num
estado próprio de pesquisa.

### Sequência visual pretendida

1.  O botão circular de Pesquisa recebe um feedback visual breve.
2.  A cápsula da navegação inferior começa a desaparecer com uma
    transição suave.
3.  Os elementos da navegação reduzem a opacidade e deslocam-se
    ligeiramente, sem movimentos exagerados.
4.  A área de pesquisa aparece na parte inferior, no lugar visual da
    navegação.
5.  O campo de pesquisa expande-se suavemente na horizontal.
6.  O ícone de lupa e o texto de orientação aparecem dentro do campo.
7.  Um botão circular auxiliar aparece à esquerda do campo, conforme o
    desenho de referência.
8.  O Mini Player continua visível acima da área de pesquisa, caso
    exista uma faixa em reprodução.

A transição deve dar a sensação de que a barra inferior se transforma na
pesquisa, e não de que uma nova barra aparece abruptamente.

### Estado da Pesquisa

No estado de pesquisa, a parte inferior deve apresentar:

-   Um botão circular auxiliar à esquerda.
-   Um campo de pesquisa arredondado à direita.
-   Ícone de lupa dentro do campo.
-   Texto de orientação: **"Músicas, Álbuns ou Artistas"**.
-   Área de toque suficientemente grande para utilização confortável.
-   Campo preparado para receber texto e abrir o teclado do Android
    quando selecionado.

O botão circular auxiliar deve ter uma função definida no produto. Pode
abrir filtros ou opções de pesquisa. Não deve ser apenas decorativo. Se
não existir uma função implementada, não deve parecer um controlo ativo.

------------------------------------------------------------------------

## 4. Funções da Pesquisa

### 4.1 Pesquisa de músicas

-   Pesquisar pelo título da música.
-   Mostrar correspondências à medida que o utilizador escreve.
-   Permitir abrir ou reproduzir um resultado.
-   Apresentar artista, álbum e duração quando disponíveis.

### 4.2 Pesquisa de álbuns

-   Pesquisar pelo nome do álbum.
-   Mostrar a capa do álbum quando existir.
-   Mostrar o artista associado.
-   Abrir a página do álbum e permitir reproduzir as suas faixas.

### 4.3 Pesquisa de artistas

-   Pesquisar pelo nome do artista.
-   Mostrar os resultados correspondentes.
-   Abrir a página do artista.
-   Apresentar os álbuns e músicas locais associados, quando os
    metadados permitirem.

### 4.4 Pesquisa de vídeos

A pesquisa também deve poder localizar vídeos existentes na biblioteca
local.

-   Pesquisar pelo nome ou título do ficheiro.
-   Mostrar miniatura, duração e nome quando disponíveis.
-   Permitir abrir o vídeo no leitor da aplicação.

### 4.5 Resultados e estados

A Pesquisa deve apresentar estados distintos:

-   **Estado inicial:** campo vazio e orientação para pesquisar.
-   **A escrever:** resultados filtrados de acordo com o texto
    introduzido.
-   **Com resultados:** lista organizada por músicas, álbuns, artistas e
    vídeos.
-   **Sem resultados:** mensagem clara de que não foram encontradas
    correspondências.
-   **Biblioteca vazia:** explicar que ainda não foram encontrados
    ficheiros multimédia.
-   **A atualizar:** mostrar indicador de progresso durante a
    atualização da biblioteca.
-   **Erro:** apresentar uma mensagem compreensível e uma ação de
    recuperação.

A pesquisa local deve funcionar offline, utilizando os dados já
indexados na biblioteca. Não deve exigir Internet para localizar músicas
e vídeos guardados no dispositivo.

------------------------------------------------------------------------

## 5. Comportamento do botão auxiliar

O botão circular que aparece à esquerda do campo de pesquisa deve ter
uma finalidade consistente.

**Comportamento recomendado: filtros de pesquisa.**

Ao tocar, abre um pequeno painel ou menu com filtros:

-   Tudo
-   Músicas
-   Álbuns
-   Artistas
-   Vídeos

Quando um filtro estiver ativo, a Pesquisa deve apresentar apenas o tipo
de conteúdo selecionado.

O painel deve fechar depois da seleção ou quando o utilizador tocar fora
dele. O filtro selecionado deve ficar visualmente identificado.

Se a referência visual tiver outra função para este botão, adaptar a
ação sem alterar a estrutura da animação.

------------------------------------------------------------------------

## 6. Fechar a Pesquisa e regressar à navegação

Ao sair da Pesquisa:

1.  O teclado fecha, se estiver aberto.
2.  O campo de pesquisa reduz-se suavemente.
3.  O botão auxiliar desaparece com uma transição discreta.
4.  A barra de navegação inferior reaparece.
5.  O indicador deslizante regressa ao separador anteriormente ativo ou
    ao separador definido pela navegação.
6.  O conteúdo da página anterior deve ser preservado quando adequado.

O utilizador não deve perder o texto da pesquisa ao alternar
acidentalmente entre ecrãs. Definir uma regra consistente para manter ou
limpar a consulta ao sair.

------------------------------------------------------------------------

## 7. Mini Player e navegação

O Mini Player deve funcionar independentemente do separador selecionado.

-   Permanecer visível durante a navegação, quando houver reprodução
    ativa.
-   Ficar acima da barra inferior ou do campo de pesquisa.
-   Nunca sobrepor o campo de pesquisa.
-   Manter capa, título, artista, progresso e controlos atualizados.
-   Permitir abrir o leitor completo.
-   Não reiniciar a música quando o utilizador muda de separador.
-   Adaptar a posição quando o teclado do Android aparece.

Quando o espaço vertical for reduzido, a interface deve ajustar o
conteúdo de forma responsiva, sem esconder controlos essenciais.

------------------------------------------------------------------------

## 8. Especificações de movimento

Os valores abaixo são referências iniciais de implementação, não medidas
extraídas do vídeo. Ajustar após testes no dispositivo.

  Elemento                         Duração inicial sugerida Movimento
  ------------------------------ -------------------------- -------------------------------
  Indicador deslizante                          250--350 ms Deslocamento horizontal suave
  Destaque do ícone e rótulo                    150--220 ms Cor e opacidade
  Saída da navegação                            180--250 ms Fade + pequeno deslocamento
  Entrada do campo de pesquisa                  250--350 ms Expansão + fade
  Botão auxiliar                                180--250 ms Fade + escala discreta
  Regresso à navegação                          250--350 ms Transição inversa

### Regras de animação

-   Usar curvas de aceleração e desaceleração naturais, como
    `FastOutSlowIn` ou uma curva equivalente.
-   Evitar efeitos elásticos exagerados.
-   Evitar saltos de layout.
-   Não animar dimensões de forma que provoque reposicionamentos
    bruscos.
-   Sincronizar a saída de um componente com a entrada do outro.
-   Respeitar a opção **Reduzir Movimento**: nesse modo, reduzir as
    deslocações e usar transições simples de opacidade ou mudanças
    imediatas.
-   Manter uma taxa de atualização fluida sem bloquear a thread
    principal.

------------------------------------------------------------------------

## 9. Requisitos técnicos Android

-   Implementar a navegação com estado claro e único para o separador
    selecionado.
-   Manter um estado próprio para o modo normal e para o modo de
    pesquisa.
-   Não criar várias barras inferiores sobrepostas.
-   Gerir corretamente o foco do campo de texto e o teclado virtual.
-   Ajustar a interface quando o teclado é aberto, respeitando o
    comportamento de redimensionamento do ecrã.
-   Preservar o estado da reprodução ao navegar.
-   Garantir que os botões têm ações reais e descrições acessíveis.
-   Suportar diferentes tamanhos de ecrã e áreas seguras do sistema.
-   Evitar recriar toda a página quando apenas o separador muda.
-   Manter a pesquisa local integrada com a biblioteca multimédia
    indexada.

------------------------------------------------------------------------

## 10. Critérios de aceitação

-   [ ] O indicador ativo desliza suavemente entre Início, Biblioteca e
    Ajustes.
-   [ ] O separador ativo é identificável por ícone, rótulo e fundo.
-   [ ] A Pesquisa abre ao tocar no botão circular separado.
-   [ ] A barra inferior transforma-se visualmente na área de pesquisa.
-   [ ] O campo apresenta o texto "Músicas, Álbuns ou Artistas".
-   [ ] O botão auxiliar abre filtros funcionais.
-   [ ] A pesquisa encontra músicas, álbuns, artistas e vídeos locais.
-   [ ] Os resultados atualizam-se de forma eficiente enquanto o
    utilizador escreve.
-   [ ] A pesquisa funciona sem ligação à Internet.
-   [ ] O teclado não cobre o campo de pesquisa nem os resultados
    essenciais.
-   [ ] O Mini Player permanece funcional e não fica sobreposto.
-   [ ] Ao sair da Pesquisa, a navegação regressa com uma transição
    suave.
-   [ ] A opção Reduzir Movimento é respeitada.
-   [ ] Não existem saltos, cintilações ou elementos duplicados durante
    as transições.

------------------------------------------------------------------------

## 11. Instrução final para a IA de desenvolvimento

Analisa o projeto Android existente e implementa esta navegação inferior
animada e o modo de Pesquisa como uma evolução da interface atual da
aplicação L.

Preserva o design, os temas, o Liquid Glass e o Mini Player existentes.
Reproduz o comportamento visual descrito neste documento: indicador em
cápsula que desliza entre separadores e transformação suave da navegação
inferior num campo de pesquisa.

Implementa a pesquisa real sobre a biblioteca multimédia local. Não uses
resultados fictícios, botões sem ação ou uma pesquisa que dependa
obrigatoriamente da Internet.

Antes de alterar o código, identifica a arquitetura e os componentes de
navegação existentes. Reutiliza-os sempre que possível, evita
duplicações e testa todos os estados e transições.

**Resultado esperado:** uma navegação inferior elegante, fluida e
responsiva, com indicador deslizante e uma transição de transformação
para uma Pesquisa local completa, integrada com a experiência Liquid
Glass da aplicação L.
