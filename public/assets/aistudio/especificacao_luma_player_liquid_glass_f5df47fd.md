# Especificação de Produto e Prompt de Desenvolvimento --- Aplicativo de Música e Vídeo Liquid Glass

## 1. Visão geral

Criar um aplicativo multimédia moderno, fluido e altamente interativo,
com foco principal na reprodução de música e suporte adicional à
reprodução de vídeo. A identidade visual deve combinar o conceito
**Liquid Glass** (vidro líquido) com uma experiência simples, elegante e
funcional.

O resultado deve parecer um produto comercial acabado, e não apenas um
protótipo visual. Todas as áreas interativas devem funcionar, os estados
devem ser coerentes e a aplicação deve tratar erros de forma clara.

**Nome provisório:** Luma Player\
O nome deve ficar centralizado numa configuração para poder ser alterado
facilmente.

### Objetivos principais

-   Reproduzir ficheiros de áudio locais em vários formatos.
-   Reproduzir ficheiros de vídeo com uma interface visual coerente com
    o leitor de música.
-   Apresentar biblioteca, pesquisa, playlists, favoritos, histórico e
    fila de reprodução.
-   Oferecer temas claro e escuro, com transições suaves.
-   Aplicar Liquid Glass de forma consistente, sem prejudicar
    legibilidade ou desempenho.
-   Ter animações suaves, controlos responsivos e feedback visual e
    tátil.
-   Funcionar de forma robusta mesmo quando um ficheiro está danificado,
    não é suportado ou desaparece do dispositivo.

------------------------------------------------------------------------

## 2. Referências visuais fornecidas

Usar as imagens anexadas pelo utilizador como referências de direção
artística:

1.  **Cartão de reprodução translúcido azul:** referência para o
    mini-player e os controlos de reprodução flutuantes.
2.  **Três ecrãs de aplicação musical:** referência para Home, Now
    Playing e My Library, incluindo cartões, playlists e navegação
    inferior.
3.  **Imagem "LIQUID GLASS":** referência principal para o efeito de
    vidro líquido, transparências, reflexos, contornos luminosos e
    formas fluidas.
4.  **Leitor minimalista branco:** referência para uma alternativa
    clara, com capa grande, tipografia limpa, waveform e controlos
    simples.
5.  **Cartão "Golden Hour":** referência para um cartão de reprodução
    horizontal, com capa, progresso, favorito e controlos.

Não copiar cegamente os ecrãs. Criar uma identidade original inspirada
neles, mantendo a consistência entre todos os ecrãs.

------------------------------------------------------------------------

## 3. Direção visual e sistema de design

### 3.1 Liquid Glass

O Liquid Glass deve ser uma característica central da aplicação.

Aplicar, conforme o contexto:

-   Superfícies translúcidas com desfoque do conteúdo por trás.
-   Gradientes subtis em azul, violeta e ciano; permitir outras cores de
    destaque.
-   Bordas finas semitransparentes.
-   Reflexos e brilhos discretos nas extremidades.
-   Sombras suaves para criar profundidade.
-   Cartões com cantos arredondados e camadas visuais.
-   Elementos que pareçam vidro moldado ou líquido, sem exagerar nos
    efeitos.

**Regra de qualidade:** o efeito nunca pode tornar textos, ícones ou
botões difíceis de ler. Em dispositivos sem suporte adequado a blur,
usar uma alternativa visual leve com transparência, gradiente e
contorno.

### 3.2 Temas

Implementar três opções:

-   **Claro:** superfícies claras, texto escuro e vidro
    branco/translúcido.
-   **Escuro:** fundo azul-marinho/preto, superfícies escuras
    translúcidas e texto claro.
-   **Sistema:** acompanhar automaticamente o tema do dispositivo.

O seletor de tema deve mudar realmente a aplicação inteira, incluindo
barras, cartões, menus, ícones, diálogos, leitor de música e leitor de
vídeo. A alteração deve ter uma transição suave, sem piscar o ecrã nem
reiniciar a aplicação.

Guardar a preferência do utilizador entre sessões.

### 3.3 Tipografia, ícones e espaçamento

-   Usar uma família tipográfica moderna e muito legível.
-   Manter hierarquia clara entre título, artista, metadados e texto
    secundário.
-   Usar ícones consistentes, com áreas de toque suficientemente
    grandes.
-   Respeitar áreas seguras, recortes do ecrã e navegação do sistema.
-   Evitar excesso de elementos decorativos e informação amontoada.

### 3.4 Movimento e animações

As animações devem ser fluidas e curtas, sem atrasar ações:

-   Transições entre páginas com movimento e opacidade subtis.
-   Cartões com feedback ao toque.
-   Botões Play/Pause com transformação animada entre os dois estados.
-   Capa do álbum com animação discreta durante a reprodução, se
    ativada.
-   Barra de progresso atualizada em tempo real.
-   Expansão do mini-player para o leitor completo.
-   Mudança de tema com transição suave.
-   Animação de entrada e saída de menus, diálogos e folhas inferiores.
-   Feedback tátil opcional nos controlos principais.

Respeitar a opção de acessibilidade "reduzir movimento" do sistema. Não
usar animações pesadas ou contínuas que gastem bateria sem necessidade.

------------------------------------------------------------------------

## 4. Estrutura de navegação

Usar uma navegação principal simples, com barra inferior em estilo
Liquid Glass.

Separadores recomendados:

1.  **Início**
2.  **Pesquisar**
3.  **Biblioteca**
4.  **Histórico**
5.  **Perfil / Definições**

A barra inferior deve indicar claramente o separador ativo, animar a
seleção e manter-se visualmente consistente nos ecrãs principais. Em
ecrãs de reprodução imersiva, pode ser ocultada.

### 4.1 Início

Apresentar:

-   Saudação contextual, sem obrigar a recolher dados pessoais.
-   Secção "Continuar a ouvir".
-   Reproduzido recentemente.
-   Playlists recentes.
-   Favoritos.
-   Músicas adicionadas recentemente.
-   Atalhos para Álbuns, Artistas, Pastas e Vídeos.
-   Cartão de destaque com a música atualmente em reprodução.

Se a biblioteca estiver vazia, apresentar uma ilustração/estado vazio e
um botão claro para importar ou procurar ficheiros.

### 4.2 Pesquisar

Permitir pesquisa local por:

-   Título da música.
-   Nome do artista.
-   Álbum.
-   Nome do ficheiro.
-   Pasta.
-   Playlist.

A pesquisa deve atualizar os resultados de forma rápida, ignorar
diferenças entre maiúsculas/minúsculas e apresentar uma mensagem útil
quando não houver resultados.

Adicionar filtros por tipo: Tudo, Músicas, Álbuns, Artistas, Playlists e
Vídeos.

### 4.3 Biblioteca

Organizar a biblioteca com separadores ou filtros:

-   Músicas
-   Álbuns
-   Artistas
-   Pastas
-   Playlists
-   Favoritos
-   Vídeos

Disponibilizar ordenação por nome, artista, álbum, data de adição e
duração. Permitir alternar entre visualização em lista e grelha.

### 4.4 Histórico

Mostrar músicas e vídeos reproduzidos recentemente, com data ou
indicação temporal. Permitir limpar o histórico, individualmente ou por
completo, após confirmação.

### 4.5 Perfil e definições

Incluir:

-   Tema: Claro, Escuro ou Sistema.
-   Cor de destaque.
-   Qualidade visual / efeitos Liquid Glass.
-   Animações e opção de reduzir movimento.
-   Preferências de reprodução.
-   Gestão da biblioteca e pastas.
-   Permissões e privacidade.
-   Formatos suportados pelo dispositivo.
-   Informações da aplicação, versão e licenças.

------------------------------------------------------------------------

## 5. Leitor de música

O leitor completo é uma das áreas mais importantes da aplicação.

### 5.1 Ecrã "A reproduzir"

Elementos:

-   Capa do álbum grande, com cantos arredondados.
-   Título da música.
-   Nome do artista e álbum.
-   Botão de favorito.
-   Barra de progresso com posição atual e duração total.
-   Possibilidade de arrastar a barra para avançar ou recuar.
-   Botões Anterior, Play/Pause e Seguinte.
-   Reprodução aleatória.
-   Repetir tudo, repetir uma música e desligado.
-   Acesso à fila.
-   Acesso a letras, quando existirem fontes de letras legalmente
    disponíveis.
-   Menu de opções da faixa.

### 5.2 Comportamento dos controlos

-   Play inicia a reprodução e muda imediatamente para Pause quando o
    áudio começa.
-   Pause interrompe o áudio e muda para Play.
-   Anterior e Seguinte respeitam a fila e o modo aleatório.
-   Repetição deve respeitar os três estados definidos.
-   O favorito deve alternar entre ativo e inativo e persistir.
-   A barra de progresso deve refletir a posição real do áudio.
-   Ao selecionar outra música, atualizar capa, título, artista, duração
    e fila sem misturar metadados da faixa anterior.
-   Desativar ou indicar claramente controlos que não sejam aplicáveis.

### 5.3 Mini-player

Manter um mini-player acessível nos ecrãs principais quando existir uma
faixa carregada.

Mostrar:

-   Miniatura da capa.
-   Título e artista.
-   Play/Pause.
-   Indicador de progresso.
-   Ação para expandir o leitor completo.

O mini-player deve poder ser dispensado sem apagar a música da
biblioteca. Ao tocar nele, abrir o leitor completo com uma transição
suave.

### 5.4 Fila de reprodução

Permitir:

-   Ver a faixa atual e as próximas.
-   Reordenar faixas por arrastar e largar.
-   Remover faixas da fila.
-   Adicionar músicas à fila.
-   Reproduzir a seguir.
-   Limpar a fila após confirmação.
-   Guardar uma fila como playlist.

### 5.5 Funcionalidades adicionais de áudio

Implementar, se a plataforma permitir:

-   Temporizador para parar a reprodução.
-   Equalizador com predefinições e controlos manuais.
-   Temporizador de sono.
-   Velocidade de reprodução.
-   Reprodução em segundo plano.
-   Controlos na notificação e no ecrã bloqueado.
-   Compatibilidade com controlos de auscultadores e Bluetooth.
-   Retomar a última sessão de reprodução.
-   Transição gradual entre faixas (crossfade), com opção para desligar.
-   Normalização de volume, se tecnicamente suportada.

Não simular funcionalidades que não estejam implementadas. Se uma função
depender do sistema ou do formato, explicar isso na interface.

------------------------------------------------------------------------

## 6. Compatibilidade de formatos de áudio

O objetivo é suportar o maior número possível de formatos, mas não
prometer "qualquer formato" sem validar a capacidade real do
descodificador e do sistema operativo.

Preparar uma arquitetura extensível de descodificação e testar, no
mínimo, os formatos comuns:

-   MP3
-   AAC / M4A
-   WAV
-   FLAC
-   OGG / Vorbis
-   Opus
-   AIFF
-   ALAC, quando suportado pela biblioteca escolhida
-   WMA, quando suportado por descodificador adicional

Considerar também áudio incorporado em contentores de vídeo, quando a
plataforma permitir.

### Regras de compatibilidade

-   Detetar o formato antes de iniciar a reprodução.
-   Ler metadados e capa incorporada quando disponíveis.
-   Se não houver capa, mostrar uma capa padrão elegante.
-   Se o formato não for suportado, explicar o motivo e não bloquear a
    aplicação.
-   Se o ficheiro estiver danificado, apresentar erro recuperável.
-   Não alterar nem converter o ficheiro original sem autorização.
-   Não carregar ficheiros inteiros para a memória quando for possível
    fazer leitura em fluxo.
-   Tratar ficheiros grandes e bibliotecas extensas sem congelar a
    interface.

------------------------------------------------------------------------

## 7. Leitor de vídeo

Criar um leitor de vídeo com a mesma identidade visual Liquid Glass, mas
adaptado à visualização de vídeo.

### 7.1 Ecrã do leitor

-   Vídeo em primeiro plano.
-   Controlos sobrepostos em vidro translúcido.
-   Play/Pause.
-   Avançar e recuar.
-   Barra de progresso.
-   Tempo atual e duração.
-   Controlo de volume.
-   Controlo de brilho, se permitido pelo sistema.
-   Seleção de orientação e ecrã inteiro.
-   Bloqueio de controlos para evitar toques acidentais.
-   Velocidade de reprodução.
-   Seleção de faixa de áudio e legendas, quando disponíveis.
-   Opção de ajustar o vídeo ao ecrã, manter proporção ou ampliar.

Os controlos devem desaparecer automaticamente após alguns segundos sem
interação e reaparecer ao tocar no ecrã.

### 7.2 Formatos de vídeo

Suportar os formatos e codecs que a plataforma e o motor de reprodução
consigam descodificar. Testar, no mínimo, MP4 (H.264/AAC) e, conforme a
plataforma, MKV, WebM, MOV e outros contentores/codec comuns.

Distinguir contentor de codec: a extensão do ficheiro, por si só, não
garante compatibilidade.

### 7.3 Biblioteca de vídeos

-   Listar vídeos locais.
-   Mostrar miniatura, nome, duração e resolução quando disponível.
-   Pesquisar, ordenar e filtrar.
-   Guardar posição de reprodução para continuar mais tarde.
-   Permitir eliminar apenas com confirmação e respeitando as permissões
    do sistema.
-   Não confundir vídeos com músicas na biblioteca.

------------------------------------------------------------------------

## 8. Importação, ficheiros e permissões

A aplicação deve permitir escolher ficheiros através do seletor de
documentos do sistema, sempre que possível.

-   Solicitar apenas as permissões necessárias.
-   Explicar por que uma permissão é solicitada.
-   Não pedir acesso amplo ao armazenamento se o seletor do sistema
    resolver a necessidade.
-   Permitir importar um ficheiro ou vários ficheiros.
-   Detetar duplicados sem apagar automaticamente.
-   Atualizar a biblioteca quando os ficheiros forem removidos ou
    movidos.
-   Guardar referências persistentes aos ficheiros quando a plataforma
    permitir.
-   Não enviar ficheiros pessoais para servidores.
-   Tratar cancelamento de seleção, acesso negado e ficheiro
    indisponível.

------------------------------------------------------------------------

## 9. Funcionalidades inovadoras recomendadas

Implementar estas ideias de forma progressiva, priorizando estabilidade:

1.  **Smart Mix local:** criar uma mistura a partir de músicas
    semelhantes por artista, álbum ou género, usando apenas metadados
    disponíveis.
2.  **Mood Collections:** coleções configuráveis como Chill, Focus,
    Workout e Road Trip, sem depender de serviços externos.
3.  **Visualizador de áudio:** animação discreta sincronizada com o som,
    com opção para desligar.
4.  **Capa dinâmica:** usar cores da capa do álbum para adaptar
    subtilmente o gradiente do leitor.
5.  **Sleep Timer inteligente:** parar a música após um período
    escolhido ou no fim da faixa/playlist.
6.  **Retomar reprodução:** reabrir a última música ou vídeo no ponto em
    que foi interrompido.
7.  **Gestos configuráveis:** por exemplo, deslizar na capa para mudar
    de faixa; permitir desativar para evitar ações acidentais.
8.  **Modo minimalista:** ocultar elementos secundários e manter apenas
    capa, título e controlos essenciais.
9.  **Estatísticas locais:** músicas mais reproduzidas e tempo de
    audição, com opção para limpar os dados.
10. **Widgets e controlos externos:** implementar apenas se forem
    suportados pela plataforma-alvo.

Não inventar dados de artistas, letras, capas ou estatísticas. Não
depender de APIs externas sem explicar, documentar e tratar falhas de
rede.

------------------------------------------------------------------------

## 10. Plataforma obrigatória: Android

**Este projeto deve ser desenvolvido exclusivamente para Android.** Não criar uma versão web, iOS ou desktop como entrega principal.

### Requisitos obrigatórios da plataforma

- **Sistema operativo:** Android 10 (API 29) ou superior.
- **Linguagem principal:** Kotlin.
- **Interface:** Jetpack Compose, com componentes reutilizáveis e estado reativo.
- **Arquitetura:** Android nativo, organizada em camadas e com separação entre interface, lógica, dados e motores multimédia.
- **Reprodução multimédia:** utilizar bibliotecas Android estáveis e adequadas para áudio e vídeo, como AndroidX Media3/ExoPlayer, quando compatíveis com os formatos pretendidos.
- **Funcionamento:** biblioteca local e reprodução de ficheiros devem funcionar offline, sem exigir ligação à Internet.
- **Entrega principal:** ficheiro APK instalável em dispositivos Android.
- **Compilação:** fornecer também o código-fonte e instruções para gerar o APK, incluindo as versões necessárias de Android SDK, Gradle e dependências.

A aplicação deve ser testada em dispositivos Android reais ou emuladores com Android 10 ou superior. Adaptar a interface a diferentes tamanhos de ecrã, densidades, recortes e versões do Android.

Não substituir funcionalidades nativas por páginas web. Se uma dependência ou funcionalidade não for compatível com a versão mínima do Android, escolher uma alternativa compatível ou documentar claramente a limitação.

## 12. Arquitetura técnica e qualidade

Usar uma implementação Android nativa moderna com Kotlin e Jetpack Compose.
Separar claramente:

-   Interface e componentes visuais.
-   Estado da aplicação.
-   Motor de reprodução de áudio.
-   Motor de reprodução de vídeo.
-   Acesso a ficheiros e permissões.
-   Base de dados local.
-   Metadados e capas.
-   Preferências e temas.

### Requisitos técnicos

-   Usar um motor de áudio/vídeo estável e compatível com a plataforma.
-   Manter a reprodução independente do ciclo de vida dos ecrãs.
-   Usar serviço de reprodução em segundo plano quando exigido pelo
    Android.
-   Atualizar a interface através de estado reativo, evitando estados
    duplicados e contraditórios.
-   Executar operações demoradas fora da thread principal.
-   Implementar tratamento centralizado de erros.
-   Guardar preferências e playlists localmente.
-   Não introduzir dependências abandonadas ou incompatíveis.
-   Fixar versões das dependências e documentar a configuração do
    projeto.
-   Respeitar acessibilidade, tamanhos de texto e contraste.
-   Evitar consumo excessivo de bateria, CPU e memória.

### Princípios de implementação

-   Não criar botões decorativos sem ação.
-   Não apresentar dados falsos para preencher a interface.
-   Não deixar páginas vazias sem estado vazio e orientação ao
    utilizador.
-   Não usar links, menus ou controlos que não funcionem.
-   Não bloquear a interface durante leitura de ficheiros ou
    carregamento de capas.
-   Não perder a posição da música ao mudar de ecrã.
-   Não reiniciar a reprodução ao alternar tema.
-   Não apagar ficheiros do utilizador sem confirmação explícita.

------------------------------------------------------------------------

## 11. Acessibilidade e experiência de utilização

-   Contraste adequado nos dois temas.
-   Suporte a tamanhos de letra do sistema.
-   Descrições acessíveis para botões e ícones.
-   Áreas de toque confortáveis.
-   Compatibilidade com leitor de ecrã.
-   Não depender apenas da cor para comunicar estados.
-   Respeitar a preferência de movimento reduzido.
-   Mensagens de erro claras, em português.
-   Confirmação para ações destrutivas.
-   Estados de carregamento e estados vazios bem desenhados.

Idioma inicial: **Português**. Preparar a estrutura para traduções
futuras.

------------------------------------------------------------------------

## 13. Testes obrigatórios e critérios de aceitação

A aplicação só deve ser considerada pronta quando os fluxos abaixo forem
testados.

### 12.1 Testes funcionais

-   Importar um ficheiro de áudio e reproduzi-lo.
-   Pausar e retomar.
-   Avançar e recuar na faixa.
-   Mudar de música.
-   Usar aleatório e os três modos de repetição.
-   Adicionar e remover favoritos.
-   Criar, renomear e eliminar playlists.
-   Reordenar a fila.
-   Fechar e reabrir a aplicação sem perder dados locais.
-   Reproduzir em segundo plano, quando suportado.
-   Reproduzir um vídeo e usar os seus controlos.
-   Continuar um vídeo a partir da última posição.
-   Alternar entre tema claro, escuro e sistema.
-   Usar pesquisa e filtros.
-   Lidar com ficheiros removidos, danificados e não suportados.

### 12.2 Testes visuais e de interação

-   Verificar todos os ecrãs em diferentes dimensões.
-   Verificar o Liquid Glass em fundos claros e escuros.
-   Confirmar que textos e ícones permanecem legíveis.
-   Confirmar que os botões apresentam estados pressionado, ativo,
    desativado e carregando quando aplicável.
-   Confirmar que as animações não bloqueiam a interação.
-   Testar navegação rápida entre ecrãs.
-   Testar o comportamento com movimento reduzido.

### 12.3 Testes de estabilidade e desempenho

-   Testar biblioteca pequena e biblioteca grande.
-   Testar ficheiros longos e de tamanho elevado.
-   Testar ficheiros com metadados incompletos.
-   Testar chamadas telefónicas/interrupções de áudio e retorno à
    aplicação.
-   Testar falta de espaço e permissões negadas.
-   Verificar fugas de memória e falhas durante rotação/orientação.
-   Verificar que a interface não congela durante a leitura da
    biblioteca.

### Critérios de aceitação

-   Nenhum botão principal pode ser apenas visual.
-   Nenhum erro recuperável pode encerrar inesperadamente a aplicação.
-   A reprodução não pode parar apenas porque o utilizador mudou de
    separador.
-   A troca de tema deve atualizar todos os componentes.
-   O mini-player e o leitor completo devem apresentar o mesmo estado de
    reprodução.
-   Os dados da biblioteca, favoritos e playlists devem persistir.
-   Formatos não suportados devem produzir uma mensagem clara, não uma
    falha silenciosa.
-   A aplicação deve compilar e passar os testes automatizados
    disponíveis antes da entrega.

------------------------------------------------------------------------

## 14. Entregáveis esperados da inteligência artificial

Não entregar apenas imagens ou um protótipo estático. Entregar:

1.  Projeto completo e organizado.
2.  Código-fonte de todos os ecrãs e componentes.
3.  Motor funcional de reprodução de áudio.
4.  Motor funcional de reprodução de vídeo.
5.  Sistema de biblioteca, pesquisa, playlists, favoritos e histórico.
6.  Temas claro, escuro e sistema.
7.  Animações e componentes Liquid Glass.
8.  Gestão de permissões e erros.
9.  Testes básicos e instruções para os executar.
10. Instruções de compilação e execução.
11. Lista transparente de formatos realmente suportados.
12. Lista de limitações conhecidas, se existirem.

Se alguma funcionalidade não puder ser implementada no ambiente
disponível, não a simular. Explicar a limitação e apresentar a
alternativa técnica mais próxima.

------------------------------------------------------------------------

## 15. Instrução final para a IA desenvolvedora

Constrói uma aplicação multimédia completa, elegante e original, com
prioridade para a experiência de reprodução. Usa as imagens fornecidas
como referência visual, especialmente o Liquid Glass azul/violeta e os
exemplos de leitores de música apresentados.

Antes de começar, define a arquitetura e a estrutura de pastas. Depois
implementa por etapas, mantendo o projeto compilável em cada etapa:

1.  Estrutura do projeto e sistema de design.
2.  Navegação e ecrãs principais.
3.  Biblioteca e importação de ficheiros.
4.  Reprodução de áudio e mini-player.
5.  Fila, playlists, favoritos e histórico.
6.  Reprodução de vídeo.
7.  Temas, animações e Liquid Glass.
8.  Acessibilidade, tratamento de erros e testes.
9.  Otimização e documentação.

**Prioridade absoluta:** funcionalidade real, estabilidade, fluidez e
consistência visual. O Liquid Glass deve ser bonito, mas a aplicação
deve continuar rápida, legível e fácil de usar. Não declarar que está
tudo funcional sem executar testes e verificar os fluxos principais.
