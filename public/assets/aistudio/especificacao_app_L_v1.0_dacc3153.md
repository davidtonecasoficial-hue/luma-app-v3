# Especificação do Aplicativo --- L (v1.0)

**Documento de requisitos funcionais, visuais e técnicos para
desenvolvimento Android**

-   **Versão do documento:** 1.0
-   **Plataforma principal:** Android
-   **Nome provisório:** L
-   **Estilo visual:** Liquid Glass, moderno, minimalista, fluido e leve
-   **Referência visual:** imagem fornecida pelo utilizador, com fundo
    abstracto azul/roxo e barra de navegação inferior em vidro
    translúcido.

------------------------------------------------------------------------

## 1. Visão geral do produto

Criar um aplicativo Android de música e multimédia que reúna, num único
lugar, os ficheiros de áudio e vídeo existentes no armazenamento do
telemóvel. A experiência deve ser simples, rápida, elegante e intuitiva,
inspirada na facilidade de utilização de leitores multimédia como o Lark
Player, mas com identidade visual própria.

O aplicativo deve localizar ficheiros de música em formatos compatíveis,
organizar a biblioteca automaticamente e permitir ao utilizador
pesquisar, reproduzir, consultar o histórico e gerir as suas
preferências.

A interface deve usar o conceito **Liquid Glass**: superfícies
translúcidas, desfoque subtil, reflexos controlados, contornos finos,
profundidade e animações suaves. O efeito não pode prejudicar a
legibilidade nem tornar a aplicação pesada.

## 2. Objectivos da versão 1.0

Na primeira versão, a aplicação deve:

1.  Apresentar uma abertura animada com a letra **L**.
2.  Mostrar automaticamente uma saudação conforme a hora local do
    telemóvel.
3.  Solicitar a permissão Android adequada para consultar os ficheiros
    multimédia.
4.  Fazer uma varredura da biblioteca multimédia acessível no
    dispositivo.
5.  Reunir os ficheiros de áudio e vídeo compatíveis numa biblioteca
    central.
6.  Separar e organizar músicas e vídeos, sem esconder a possibilidade
    de encontrar tudo num só lugar.
7.  Permitir pesquisa, reprodução, pausa, avanço, retrocesso e controlo
    de volume.
8.  Guardar e apresentar o histórico de reprodução.
9.  Apresentar estatísticas simples da biblioteca e da utilização.
10. Disponibilizar um perfil com definições e informações da aplicação.
11. Ter uma barra inferior Liquid Glass com cinco áreas funcionais.
12. Manter o consumo de memória, bateria e recursos o mais baixo
    possível.

## 3. Identidade visual e referência Liquid Glass

### 3.1 Direcção artística

Usar como referência a imagem enviada:

-   Fundo abstracto com formas orgânicas e gradientes azul, azul-escuro,
    violeta e pequenos reflexos lilás.
-   Barra de navegação horizontal, flutuante, arredondada e translúcida.
-   Ícones lineares brancos, simples e consistentes.
-   Texto branco ou cinzento-claro, com contraste suficiente.
-   Reflexos e brilhos suaves nas superfícies de vidro.
-   Cantos arredondados e espaçamento generoso.
-   Aparência premium, mas sem excesso de elementos decorativos.

A imagem é uma referência de estilo, não uma tela para copiar
literalmente. Adaptar a composição ao contexto de um leitor de música.

### 3.2 Regras do efeito Liquid Glass

-   Aplicar transparência e desfoque sobretudo em cartões, menus,
    controlos e barra inferior.
-   Usar uma camada de fundo ligeiramente escurecida atrás de elementos
    importantes para garantir contraste.
-   Usar bordas claras subtis e reflexos discretos.
-   Evitar blur excessivo, sombras pesadas, partículas constantes ou
    animações permanentes que gastem bateria.
-   Em dispositivos modestos, reduzir ou desligar efeitos de blur e
    reflexos, mantendo cores e transparências simples.
-   Respeitar a opção do Android para reduzir animações e efeitos
    visuais.

### 3.3 Paleta sugerida

-   Azul profundo: `#080D2B`
-   Azul principal: `#2447D8`
-   Azul luminoso: `#416BFF`
-   Violeta: `#7657D6`
-   Lilás de reflexo: `#B9A5FF`
-   Branco: `#FFFFFF`
-   Texto secundário: `#C6CBE0`

Manter a paleta consistente e não usar demasiadas cores numa mesma tela.

## 4. Abertura da aplicação (Splash Screen)

### 4.1 Composição

A abertura deve ter fundo azul-escuro com gradientes suaves. No centro,
apresentar a letra **L** dentro de uma forma de vidro líquido, com
transparência, contorno subtil e reflexo animado.

Abaixo da letra, apresentar uma saudação e uma frase curta.

### 4.2 Saudação automática

A aplicação deve consultar a hora local do dispositivo, sem depender de
Internet:

  Hora local        Saudação
  ----------------- ------------
  05:00 até 11:59   Bom dia!
  12:00 até 17:59   Boa tarde!
  18:00 até 04:59   Boa noite!

Texto secundário sugerido: **"É bom ter-te de volta."**

A saudação deve actualizar-se de acordo com a hora local. Não fixar uma
saudação no código.

### 4.3 Sequência de animação sugerida

1.  O fundo aparece com fade-in.
2.  A letra L entra com escala suave e ligeiro movimento vertical.
3.  Um reflexo percorre a superfície de vidro.
4.  A saudação aparece com fade-in e pequeno deslocamento.
5.  A frase secundária aparece discretamente.
6.  A abertura termina com uma transição suave para a página inicial.

As durações devem ser curtas e a animação não deve atrasar
desnecessariamente o acesso à música. Se a biblioteca já estiver pronta,
entrar directamente na página inicial.

## 5. Organização da biblioteca multimédia

### 5.1 Varredura automática

Na primeira utilização, depois de explicar a finalidade e obter a
permissão necessária, a aplicação deve consultar os ficheiros multimédia
disponíveis no dispositivo Android.

A implementação deve privilegiar **MediaStore**, a API oficial do
Android para descobrir e consultar conteúdos multimédia. Não pedir
acesso total a todos os ficheiros se a aplicação só precisa de áudio e
vídeo.

A aplicação deve:

-   Encontrar ficheiros de áudio compatíveis, incluindo MP3 e outros
    formatos suportados pelo leitor.
-   Encontrar ficheiros MP4 e outros vídeos suportados.
-   Reunir os resultados numa biblioteca central.
-   Identificar nome, duração, tamanho, formato, localização
    disponibilizada pelo sistema e, quando existir, artista, álbum e
    capa.
-   Evitar entradas duplicadas.
-   Ignorar ficheiros incompletos, temporários ou que não possam ser
    reproduzidos.
-   Permitir actualizar a biblioteca através de uma nova varredura.
-   Actualizar a biblioteca quando o sistema indicar que os conteúdos
    multimédia mudaram.

**Nota importante sobre MP4:** MP4 é normalmente um contentor de vídeo e
áudio. Na versão 1.0, os ficheiros MP4 devem aparecer na secção de
vídeos e poder ser reproduzidos com imagem e som. Se o utilizador quiser
apenas ouvir o som de um MP4, disponibilizar um modo de reprodução em
segundo plano quando tecnicamente suportado. Não converter nem extrair
áudio silenciosamente.

### 5.2 Permissões e privacidade

-   Usar as permissões de leitura de áudio e vídeo adequadas à versão do
    Android.
-   Considerar as permissões separadas introduzidas nas versões recentes
    do Android, como `READ_MEDIA_AUDIO` e `READ_MEDIA_VIDEO`, e as
    alterações de versões posteriores.
-   Pedir apenas as permissões necessárias e explicar por que são
    usadas.
-   Se o utilizador negar uma permissão, explicar o que ficará
    indisponível e permitir continuar com o conteúdo autorizado.
-   Não enviar ficheiros, nomes de músicas ou dados pessoais para
    servidores.
-   Não fazer upload automático de conteúdo.
-   Não apagar, mover ou alterar ficheiros do utilizador sem uma acção
    explícita.
-   Respeitar o acesso limitado a fotografias/vídeos ou conteúdos
    seleccionados quando aplicável à versão do Android.

### 5.3 Estrutura da biblioteca

A página inicial deve ter:

-   Título e saudação contextual.
-   Pesquisa rápida.
-   Secção "Reproduzido recentemente".
-   Secção "As tuas músicas".
-   Secção "Os teus vídeos".
-   Secção "Pastas" ou "Todos os ficheiros".
-   Estado vazio claro quando ainda não houver ficheiros.
-   Acção "Actualizar biblioteca" acessível.

Cada música deve poder mostrar capa, título, artista e duração. Quando
não existir capa, usar uma imagem de capa padrão elegante. Cada vídeo
deve mostrar miniatura, nome e duração.

## 6. Barra inferior de navegação --- referência da imagem

A barra deve ficar na parte inferior do ecrã, flutuante, com formato de
cápsula, cantos totalmente arredondados e aparência Liquid Glass.

Deve ter cinco separadores, seguindo a referência visual:

1.  **Home** --- biblioteca e conteúdos recentes.
2.  **Search** --- pesquisa de músicas, vídeos, artistas, álbuns e
    pastas.
3.  **Analytics** --- estatísticas da biblioteca e actividade de
    reprodução.
4.  **History** --- histórico de reprodução.
5.  **Profile** --- perfil, preferências e informações da aplicação.

### 6.1 Comportamento visual

-   Ícones lineares e rótulos legíveis.
-   Separador activo claramente identificado por brilho, fundo ou
    indicador discreto.
-   Animação curta ao mudar de separador.
-   Barra com fundo translúcido, desfoque subtil, borda fina e reflexo
    discreto.
-   Respeitar a área segura do ecrã e não cobrir os controlos do leitor.
-   Em ecrãs pequenos, manter os cinco destinos acessíveis sem comprimir
    os rótulos excessivamente.

### 6.2 Interacção obrigatória

Todos os cinco botões devem funcionar realmente. Cada botão deve abrir a
sua respectiva página, actualizar o estado seleccionado e permitir
regressar às outras áreas. Não criar botões meramente decorativos.

## 7. Explicação detalhada de cada separador

### 7.1 Home

É a página principal e o ponto de entrada da aplicação.

Deve conter:

-   Saudação "Bom dia", "Boa tarde" ou "Boa noite", conforme a hora.
-   Nome ou marca da aplicação.
-   Campo ou ícone de pesquisa.
-   Cartão "A tocar agora", se existir reprodução activa.
-   Lista de músicas adicionadas ou encontradas recentemente.
-   Atalho para todos os áudios.
-   Atalho para todos os vídeos.
-   Atalho para pastas.
-   Botão para actualizar/rescanear a biblioteca.
-   Mensagem de orientação quando a biblioteca estiver vazia.

Ao tocar numa música, iniciar a reprodução. Ao tocar num vídeo, abrir o
leitor de vídeo.

### 7.2 Search

A pesquisa deve ser funcional e rápida.

Permitir pesquisar por:

-   Nome da música ou vídeo.
-   Nome do ficheiro.
-   Artista.
-   Álbum.
-   Pasta.
-   Formato, quando aplicável.

Apresentar resultados enquanto o utilizador escreve, com pequenos
atrasos controlados para evitar pesquisas repetidas. Incluir filtros
"Tudo", "Músicas" e "Vídeos". Mostrar uma mensagem clara quando não
houver resultados.

### 7.3 Analytics

Esta secção deve apresentar informações úteis e simples, sem inventar
dados.

Exemplos:

-   Número total de músicas encontradas.
-   Número total de vídeos encontrados.
-   Duração total da biblioteca de áudio, quando calculável.
-   Número de reproduções registadas pela aplicação.
-   Conteúdos mais reproduzidos, com base no histórico local.
-   Actividade recente.

Os dados devem ser calculados a partir da biblioteca e do histórico
real. Não apresentar estatísticas falsas. Evitar gráficos pesados;
preferir cartões pequenos e gráficos simples, se forem úteis.

### 7.4 History

Mostrar os conteúdos reproduzidos recentemente, por ordem cronológica.

Cada item deve apresentar:

-   Capa ou miniatura.
-   Título.
-   Artista ou nome do ficheiro.
-   Data ou indicação temporal da última reprodução, quando disponível.
-   Acção para reproduzir novamente.

Disponibilizar a opção de limpar o histórico, sempre com confirmação. O
histórico deve ser guardado localmente.

### 7.5 Profile

Esta área deve ser desenhada minuciosamente e com o mesmo cuidado visual
da barra de navegação da imagem de referência.

No topo, apresentar:

-   Avatar ou ícone de perfil.
-   Nome do utilizador, com possibilidade de editar um nome local.
-   Texto secundário, por exemplo "A tua experiência musical".
-   Cartão Liquid Glass com resumo da biblioteca.

Abaixo, organizar opções em grupos visuais:

**Biblioteca** - Gerir biblioteca. - Actualizar varredura. - Pastas
incluídas, se a arquitectura permitir. - Formatos suportados. - Estado
das permissões de áudio e vídeo.

**Leitor** - Reprodução em segundo plano. - Retomar reprodução
anterior. - Reprodução automática da próxima faixa. - Modo de
repetição. - Reprodução aleatória. - Preferências de áudio disponíveis
no dispositivo.

**Aparência** - Tema escuro. - Intensidade dos efeitos Liquid Glass:
normal ou reduzida. - Reduzir animações. - Preferência de cor, se
implementada.

**Armazenamento e privacidade** - Espaço aproximado ocupado pela
biblioteca indexada, quando calculável. - Explicação de que os ficheiros
permanecem no dispositivo. - Gerir permissões. - Limpar apenas cache e
miniaturas geradas pela aplicação. - Limpar histórico, com confirmação.

**Sobre** - Nome da aplicação. - Versão 1.0. - Informações de
desenvolvimento. - Política de privacidade. - Licenças de bibliotecas de
terceiros, se existirem. - Verificar actualizações, caso exista
mecanismo de actualização.

Cada opção deve abrir uma página, diálogo ou controlo funcional
adequado. Não mostrar opções que não estejam implementadas. Não usar
interruptores decorativos.

## 8. Ecrã de reprodução

O leitor deve ser limpo, com a capa em destaque e controlos acessíveis.

Para áudio: - Capa do álbum ou arte padrão. - Título e artista. - Barra
de progresso com tempo actual e duração. - Anterior, reproduzir/pausar e
seguinte. - Aleatório e repetição. - Fila de reprodução. - Controlos na
notificação e no ecrã bloqueado, quando suportados pelo Android. -
Reprodução em segundo plano.

Para vídeo: - Área de vídeo adaptável ao ecrã. - Reproduzir/pausar. -
Barra de progresso. - Controlo de volume e brilho, se implementado. -
Rotação e ecrã inteiro. - Retomar reprodução, quando possível.

A reprodução deve usar componentes nativos ou bibliotecas Android
adequadas, como AndroidX Media3/ExoPlayer, depois de avaliar
compatibilidade e tamanho final.

## 9. Requisitos de interactividade --- 100%

A aplicação deve ser realmente funcional, não apenas uma demonstração
visual.

-   Todos os botões, ícones de acção, cartões clicáveis, menus,
    separadores e controlos devem executar a acção indicada.
-   Mostrar estados de carregamento, sucesso, erro, ausência de
    resultados e ausência de permissões.
-   Dar resposta visual ao toque, sem atrasos exagerados.
-   Impedir toques repetidos que provoquem operações duplicadas.
-   Usar navegação consistente e permitir voltar correctamente.
-   Guardar as preferências e o histórico localmente.
-   Não deixar controlos sem acção.
-   Não simular resultados nem apresentar dados fictícios.

## 10. Desempenho: aplicação leve e limpa

A prioridade é abrir rapidamente, navegar sem travamentos e consumir
poucos recursos.

Requisitos:

-   Evitar bibliotecas desnecessárias e dependências pesadas.
-   Não carregar todas as capas e miniaturas de uma só vez.
-   Usar carregamento paginado ou por lotes na biblioteca.
-   Gerar e guardar miniaturas em cache.
-   Fazer a varredura em segundo plano, com progresso visível.
-   Não bloquear a interface durante a leitura dos ficheiros.
-   Evitar animações contínuas quando o ecrã não está visível.
-   Libertar recursos do leitor quando deixam de ser necessários.
-   Não manter serviços em segundo plano sem necessidade.
-   Evitar actualizações constantes da interface.
-   Adaptar os efeitos visuais ao desempenho do dispositivo.
-   Testar em telemóveis com pouca memória e processadores modestos.

A aplicação deve priorizar fluidez e estabilidade em vez de efeitos
visuais excessivos.

## 11. Requisitos técnicos Android

-   Desenvolver como aplicação Android nativa.
-   Utilizar Kotlin e Jetpack Compose, salvo justificação técnica para
    outra arquitectura.
-   Usar MediaStore para consultar áudio e vídeo.
-   Usar Media3 para reprodução, se adequado.
-   Usar corrotinas e processamento fora da thread principal.
-   Guardar preferências com DataStore.
-   Guardar histórico e metadados necessários numa base de dados local,
    por exemplo Room.
-   Implementar permissões conforme a versão do Android e o nível de API
    alvo.
-   Suportar diferentes tamanhos de ecrã e densidades.
-   Respeitar os ciclos de vida do Android.
-   Não exigir ligação à Internet para procurar e reproduzir ficheiros
    locais.

## 12. Estados e mensagens que devem existir

Criar interfaces claras para:

-   Primeira abertura.
-   Pedido de permissões.
-   Permissão recusada.
-   Varredura em andamento.
-   Varredura concluída.
-   Biblioteca vazia.
-   Ficheiro incompatível ou indisponível.
-   Erro de reprodução.
-   Pesquisa sem resultados.
-   Histórico vazio.
-   Conteúdo removido do dispositivo.
-   Falha ao carregar miniatura.

As mensagens devem ser curtas, em português, e indicar ao utilizador o
que pode fazer a seguir.

## 13. Critérios de aceitação da versão 1.0

A versão 1.0 só deve ser considerada pronta quando:

-   A aplicação instala e abre correctamente em Android.
-   A abertura apresenta a letra L e a saudação correcta.
-   A saudação usa a hora local do telemóvel.
-   A aplicação pede permissões adequadas e explica a sua finalidade.
-   A varredura encontra os ficheiros de áudio e vídeo autorizados.
-   MP3 e MP4 compatíveis aparecem nas secções correctas.
-   Não existem duplicados na biblioteca.
-   A pesquisa apresenta resultados reais.
-   A reprodução de áudio funciona e continua em segundo plano quando
    configurada.
-   O leitor de vídeo funciona para ficheiros compatíveis.
-   O histórico regista reproduções reais.
-   Analytics apresenta dados calculados.
-   Os cinco separadores inferiores funcionam.
-   Todas as opções visíveis no Profile funcionam.
-   O utilizador consegue actualizar a biblioteca.
-   A aplicação lida correctamente com permissões recusadas.
-   A interface não bloqueia durante a varredura.
-   O desempenho é aceitável em dispositivos Android modestos.
-   Os ficheiros pessoais não são enviados para servidores.

## 14. Fora do âmbito obrigatório da versão 1.0

Não implementar como requisito obrigatório, a menos que o projecto seja
posteriormente expandido:

-   Streaming de música pela Internet.
-   Conta online ou sincronização na nuvem.
-   Download de músicas de serviços externos.
-   Conversão de formatos ou extracção de áudio de vídeos.
-   Equalizador avançado que dependa de efeitos não disponíveis no
    dispositivo.
-   Letras sincronizadas online.

Estas funções podem ser consideradas em versões futuras, sem comprometer
a leveza da primeira versão.

## 15. Instrução final para a inteligência artificial de desenvolvimento

Constrói a aplicação Android **L v1.0** seguindo esta especificação como
requisito do produto, não apenas como inspiração visual.

Antes de finalizar, implementa e testa cada fluxo funcional. A imagem de
referência deve orientar o estilo Liquid Glass da barra inferior e dos
elementos translúcidos, mas a aplicação deve ter uma identidade própria
de leitor multimédia.

**Prioridades absolutas, por ordem:**

1.  Funcionamento correcto da biblioteca e da reprodução local.
2.  Interactividade real de todos os botões e separadores.
3.  Desempenho leve, estabilidade e baixo consumo de recursos.
4.  Design Liquid Glass elegante, legível e adaptável.
5.  Animações suaves e abertura personalizada com a letra L.

Não entregar apenas um protótipo estático. Entregar uma aplicação
Android funcional, com estados reais, permissões correctas, navegação
completa e tratamento de erros.
