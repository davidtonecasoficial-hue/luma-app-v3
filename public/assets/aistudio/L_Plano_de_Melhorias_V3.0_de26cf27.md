# L --- Plano de Melhorias e Funcionalidades em Falta

## Especificação de evolução para a versão 3.0

**Plataforma:** Android\
**Tipo de aplicação:** Leitor de música e vídeo offline\
**Objetivo:** Evoluir a aplicação L, preservando a identidade visual
atual, o estilo Liquid Glass e a navegação existente, enquanto se
melhoram a usabilidade, a biblioteca multimédia, a reprodução e a
estabilidade.

------------------------------------------------------------------------

## 1. Objetivo geral

A aplicação já apresenta uma identidade visual consistente, com fundo em
gradiente, cartões translúcidos, efeito Liquid Glass, mini player e
leitor de música com capa circular.

A próxima versão deve aperfeiçoar o que já existe, sem reconstruir a
interface desnecessariamente. O foco deve estar em quatro pilares:

1.  **Design:** interface premium, legível e consistente.
2.  **Inteligência:** biblioteca multimédia automática e bem organizada.
3.  **Experiência:** reprodução de áudio e vídeo completa e intuitiva.
4.  **Desempenho:** fluidez, estabilidade e baixo consumo de bateria.

> Importante: as imagens de referência mostram a aparência da aplicação,
> mas não comprovam quais funcionalidades já estão implementadas. Antes
> de desenvolver cada item, verificar o estado real do código para
> evitar duplicações.

------------------------------------------------------------------------

## 2. Correções prioritárias da interface

### 2.1 Contraste e legibilidade

-   [ ] Corrigir textos escuros que ficam pouco visíveis sobre fundos
    escuros ou translúcidos.
-   [ ] Definir cores de texto adaptáveis aos temas Claro, Escuro e
    Sistema.
-   [ ] Garantir contraste suficiente em títulos, descrições, ícones,
    botões e opções desativadas.
-   [ ] Verificar a legibilidade dos textos sobre cartões com
    transparência e desfoque.
-   [ ] Manter uma hierarquia visual clara entre títulos, subtítulos e
    informações secundárias.

### 2.2 Espaçamento e sobreposições

-   [ ] Corrigir elementos que se sobrepõem ao Mini Player ou à barra de
    navegação.
-   [ ] Garantir que o conteúdo possa ser percorrido até ao fim, sem
    ficar escondido atrás dos controlos fixos.
-   [ ] Respeitar as áreas seguras do Android, incluindo a barra de
    estado e a navegação do sistema.
-   [ ] Uniformizar margens, espaçamentos, alturas e cantos dos cartões.
-   [ ] Verificar a interface em diferentes tamanhos de ecrã e
    densidades.

### 2.3 Carrosséis e cartões

-   [ ] Uniformizar o tamanho e o alinhamento dos cartões.
-   [ ] Melhorar a transição entre cartões ao deslizar.
-   [ ] Evitar cortes inadequados de títulos e nomes de artistas.
-   [ ] Apresentar capas reais quando disponíveis.
-   [ ] Definir um comportamento visual adequado quando não existir
    capa.
-   [ ] Garantir que tocar num cartão abre ou reproduz o conteúdo
    correspondente.

### 2.4 Mini Player

-   [ ] Manter o Mini Player acessível nas páginas principais.
-   [ ] Evitar que cubra conteúdo, botões ou a navegação inferior.
-   [ ] Mostrar capa, título, artista, progresso e controlos essenciais.
-   [ ] Permitir reproduzir/pausar, avançar, voltar, abrir o leitor
    completo e fechar o Mini Player.
-   [ ] Atualizar imediatamente as informações quando a faixa mudar.
-   [ ] Manter o estado correto ao navegar entre separadores.

------------------------------------------------------------------------

## 3. Leitor de música

### 3.1 Controlos e reprodução

-   [ ] Garantir os controlos de reproduzir, pausar, faixa anterior e
    seguinte.
-   [ ] Implementar modos aleatório e repetição: desligado, repetir uma
    faixa e repetir a fila.
-   [ ] Criar uma fila de reprodução editável.
-   [ ] Permitir adicionar, remover e reorganizar faixas na fila.
-   [ ] Guardar a última posição de reprodução quando aplicável.
-   [ ] Restaurar a sessão de reprodução após fechar e voltar a abrir a
    aplicação.
-   [ ] Mostrar corretamente a duração, o tempo decorrido e o tempo
    restante.
-   [ ] Permitir procurar uma posição específica na música através da
    barra de progresso.

### 3.2 Reprodução em segundo plano

-   [ ] Continuar a reprodução quando o utilizador sai da aplicação.
-   [ ] Continuar a reprodução com o ecrã desligado.
-   [ ] Apresentar controlos na notificação do Android.
-   [ ] Integrar os controlos do ecrã bloqueado quando suportados.
-   [ ] Suportar comandos de auriculares e dispositivos Bluetooth.
-   [ ] Tratar chamadas, interrupções de áudio e perda de foco de áudio.
-   [ ] Usar o mecanismo Android adequado para reprodução em segundo
    plano, incluindo serviço de reprodução e notificações conforme a
    versão do Android.

### 3.3 Capa e animações

-   [ ] Rodar a capa durante a reprodução, se a opção estiver ativa.
-   [ ] Pausar a rotação quando a música estiver pausada.
-   [ ] Criar transições suaves entre capas.
-   [ ] Sincronizar animações com o estado real da reprodução.
-   [ ] Evitar animações pesadas que aumentem o consumo de bateria.
-   [ ] Mostrar uma capa padrão elegante quando não existir imagem
    incorporada no ficheiro.

------------------------------------------------------------------------

## 4. Biblioteca multimédia automática

### 4.1 Varredura do dispositivo

A aplicação deve encontrar e organizar os ficheiros multimédia locais
através das APIs oficiais do Android, preferencialmente a **MediaStore
API**, respeitando as permissões aplicáveis à versão do sistema.

-   [ ] Solicitar as permissões adequadas para áudio e vídeo.
-   [ ] Explicar ao utilizador por que motivo o acesso é necessário.
-   [ ] Fazer a primeira varredura da biblioteca.
-   [ ] Detetar novos ficheiros multimédia sem obrigar a repetir sempre
    uma varredura completa.
-   [ ] Atualizar a biblioteca quando os ficheiros forem adicionados,
    removidos ou alterados.
-   [ ] Mostrar o progresso da varredura.
-   [ ] Mostrar a quantidade de ficheiros encontrados.
-   [ ] Informar quando ocorrerem erros ou quando o acesso estiver
    limitado.
-   [ ] Permitir iniciar uma varredura manual.

### 4.2 Formatos

Verificar o suporte de leitura e reprodução dos formatos pretendidos, de
acordo com os descodificadores disponíveis no Android e com a
implementação escolhida.

**Áudio a considerar:** - MP3 - M4A / AAC - FLAC - WAV - OGG - OPUS -
Outros formatos compatíveis com o motor de reprodução utilizado.

**Vídeo a considerar:** - MP4 - MKV - MOV - WEBM - 3GP - Outros formatos
compatíveis com o dispositivo e o motor de reprodução.

> A extensão do ficheiro, por si só, não garante que o conteúdo seja
> reproduzível. A aplicação deve validar o formato e o codec e
> apresentar uma mensagem clara quando não houver suporte.

### 4.3 Organização da biblioteca

-   [ ] Separar Música e Vídeos.
-   [ ] Criar secções para Todas as músicas, Artistas, Álbuns, Pastas e
    Géneros, quando os metadados estiverem disponíveis.
-   [ ] Criar uma secção para vídeos.
-   [ ] Ordenar por nome, artista, álbum, data de adição e duração.
-   [ ] Permitir alternar entre visualização em lista e grelha.
-   [ ] Mostrar capas e miniaturas quando disponíveis.
-   [ ] Apresentar uma imagem padrão quando não houver capa ou
    miniatura.
-   [ ] Permitir atualizar ou reexaminar a biblioteca.

### 4.4 Metadados e duplicados

-   [ ] Ler título, artista, álbum, duração, género e imagem
    incorporada, quando disponíveis.
-   [ ] Permitir editar metadados, se tecnicamente possível e com
    autorização adequada.
-   [ ] Identificar possíveis ficheiros duplicados.
-   [ ] Nunca eliminar ficheiros automaticamente.
-   [ ] Mostrar a localização do ficheiro quando essa informação estiver
    disponível.
-   [ ] Tratar corretamente ficheiros sem metadados ou com metadados
    incompletos.

### 4.5 Pesquisa

-   [ ] Pesquisar músicas por título.
-   [ ] Pesquisar por artista e álbum.
-   [ ] Pesquisar vídeos por nome.
-   [ ] Permitir filtrar por tipo de conteúdo.
-   [ ] Mostrar resultados rapidamente.
-   [ ] Apresentar um estado vazio útil quando não houver resultados.

------------------------------------------------------------------------

## 5. Leitor de vídeo

O leitor de vídeo deve manter a mesma identidade visual da aplicação,
mas adaptar os controlos às necessidades de visualização.

-   [ ] Criar um leitor de vídeo em ecrã inteiro.
-   [ ] Implementar reproduzir, pausar, avançar e recuar.
-   [ ] Permitir procurar uma posição na linha temporal.
-   [ ] Controlar brilho e volume através de gestos, com indicação
    visual.
-   [ ] Permitir ajustar a velocidade de reprodução, por exemplo 0,5x,
    1x, 1,25x, 1,5x e 2x, conforme o motor de vídeo.
-   [ ] Oferecer modos de proporção e ajuste do vídeo, como ajustar ao
    ecrã, preencher e manter proporção.
-   [ ] Suportar legendas externas SRT, se possível.
-   [ ] Permitir bloquear os controlos para evitar toques acidentais.
-   [ ] Guardar a posição do último vídeo e permitir retomar a
    reprodução.
-   [ ] Suportar orientação horizontal e vertical.
-   [ ] Implementar Picture-in-Picture quando suportado e autorizado.
-   [ ] Apresentar mensagens claras para ficheiros incompatíveis ou
    danificados.

------------------------------------------------------------------------

## 6. Funcionalidades adicionais

### 6.1 Equalizador

-   [ ] Criar um equalizador acessível a partir do leitor.
-   [ ] Disponibilizar presets, por exemplo Normal, Bass Boost,
    Clássico, Dance e Vocal.
-   [ ] Permitir ajustar bandas manualmente, se o motor de áudio
    suportar.
-   [ ] Adicionar controlo de reforço de graves, quando disponível.
-   [ ] Não apresentar controlos que não tenham efeito real no áudio.

### 6.2 Letras de músicas

-   [ ] Apresentar letras quando existirem fontes ou ficheiros
    disponíveis.
-   [ ] Suportar letras incorporadas nos ficheiros.
-   [ ] Considerar ficheiros de letras locais, como LRC.
-   [ ] Sincronizar as linhas com o tempo da música quando houver
    marcações temporais.
-   [ ] Apresentar uma mensagem adequada quando não existirem letras.
-   [ ] Não prometer letras online sem implementar uma fonte e as
    respetivas condições de acesso.

### 6.3 Playlists e favoritos

-   [ ] Criar playlists personalizadas.
-   [ ] Renomear e eliminar playlists.
-   [ ] Adicionar e remover músicas.
-   [ ] Reordenar músicas dentro de uma playlist.
-   [ ] Criar uma lista de favoritos.
-   [ ] Permitir reproduzir uma playlist inteira.
-   [ ] Guardar as alterações localmente.

### 6.4 Histórico e recomendações locais

-   [ ] Guardar o histórico de reprodução.
-   [ ] Mostrar músicas reproduzidas recentemente.
-   [ ] Permitir limpar o histórico.
-   [ ] Criar uma secção de reprodução recente.
-   [ ] Usar o histórico local para sugestões simples, sem exigir
    ligação à Internet.

### 6.5 Temporizador

-   [ ] Permitir definir um tempo para parar a reprodução.
-   [ ] Oferecer opções rápidas e temporizador personalizado.
-   [ ] Mostrar o tempo restante.
-   [ ] Permitir cancelar ou alterar o temporizador.

### 6.6 Gestos e acessibilidade

-   [ ] Definir gestos simples para ações frequentes.
-   [ ] Permitir configurar ou desativar gestos quando necessário.
-   [ ] Respeitar a opção Reduzir Movimento.
-   [ ] Garantir que os controlos continuam acessíveis sem gestos.
-   [ ] Suportar tamanhos de texto do sistema.
-   [ ] Fornecer descrições acessíveis para botões e ícones.

------------------------------------------------------------------------

## 7. Liquid Glass e personalização

O efeito Liquid Glass deve ser aplicado com consistência, sem prejudicar
a leitura nem o desempenho.

-   [ ] Uniformizar o nível de transparência dos cartões.
-   [ ] Ajustar o desfoque conforme o tema e a capacidade do
    dispositivo.
-   [ ] Manter bordas e reflexos discretos.
-   [ ] Evitar excesso de brilho e elementos decorativos.
-   [ ] Garantir que o texto permanece legível em qualquer fundo.
-   [ ] Aplicar corretamente as cores de destaque selecionadas.
-   [ ] Fazer com que o tema Sistema acompanhe a configuração do
    Android.
-   [ ] Guardar as preferências do utilizador.
-   [ ] Garantir que a opção Reduzir Movimento desativa ou reduz
    animações não essenciais.
-   [ ] Disponibilizar uma alternativa visual mais leve para
    dispositivos menos potentes.

### Animações

-   [ ] Usar transições suaves entre páginas.
-   [ ] Animar a abertura e o fecho de cartões e painéis.
-   [ ] Evitar movimentos bruscos.
-   [ ] Sincronizar animações com as ações reais.
-   [ ] Evitar executar animações desnecessárias quando a aplicação
    estiver em segundo plano.

------------------------------------------------------------------------

## 8. Definições e experiência do utilizador

-   [ ] Organizar as definições por categorias.
-   [ ] Mostrar claramente quando uma opção está ativa ou desativada.
-   [ ] Explicar as opções menos óbvias.
-   [ ] Guardar as preferências imediatamente.
-   [ ] Criar uma secção Sobre a aplicação.
-   [ ] Mostrar a versão instalada.
-   [ ] Disponibilizar uma opção para limpar a cache, sem apagar músicas
    ou vídeos pessoais.
-   [ ] Pedir confirmação antes de ações destrutivas.
-   [ ] Criar estados vazios, mensagens de erro e indicadores de
    carregamento consistentes.

------------------------------------------------------------------------

## 9. Desempenho, estabilidade e consumo

-   [ ] Evitar carregar todas as capas e miniaturas de uma só vez.
-   [ ] Usar cache de imagens e miniaturas.
-   [ ] Carregar listas de forma eficiente.
-   [ ] Evitar repetir consultas desnecessárias à MediaStore.
-   [ ] Manter a interface fluida durante a varredura.
-   [ ] Evitar bloqueios da interface principal.
-   [ ] Controlar o consumo de memória.
-   [ ] Reduzir o consumo de bateria com animações e tarefas em segundo
    plano.
-   [ ] Tratar erros de descodificação e ficheiros inválidos.
-   [ ] Testar a aplicação com bibliotecas pequenas e muito grandes.
-   [ ] Testar com pouco espaço livre e memória limitada.
-   [ ] Verificar o comportamento após reiniciar o dispositivo.
-   [ ] Verificar a estabilidade após longos períodos de reprodução.

------------------------------------------------------------------------

## 10. Compatibilidade e permissões Android

-   [ ] Definir as versões mínima e alvo do Android.
-   [ ] Implementar permissões de áudio e vídeo de acordo com a versão
    do Android.
-   [ ] Considerar as permissões de acesso a imagens e vídeos quando
    forem necessárias para funcionalidades específicas.
-   [ ] Não pedir acesso total aos ficheiros se a MediaStore for
    suficiente.
-   [ ] Explicar como recuperar o acesso quando uma permissão for
    recusada.
-   [ ] Testar em diferentes versões do Android.
-   [ ] Verificar notificações e reprodução em segundo plano nas versões
    suportadas.
-   [ ] Tratar corretamente restrições de bateria e execução em segundo
    plano.

------------------------------------------------------------------------

## 11. Plano de implementação recomendado

A implementação deve ser feita por etapas, para reduzir erros e permitir
testar cada conjunto de funcionalidades.

### Fase 1 --- Correções fundamentais

1.  Corrigir contraste e legibilidade.
2.  Corrigir sobreposições e espaçamentos.
3.  Aperfeiçoar o Mini Player.
4.  Rever a navegação e as áreas seguras do Android.
5.  Testar os temas Claro, Escuro e Sistema.

### Fase 2 --- Biblioteca multimédia

1.  Rever permissões Android.
2.  Completar a integração com MediaStore.
3.  Implementar atualização incremental da biblioteca.
4.  Organizar músicas, vídeos, artistas, álbuns e pastas.
5.  Melhorar pesquisa, metadados e tratamento de duplicados.

### Fase 3 --- Reprodução

1.  Completar controlos do leitor de música.
2.  Implementar fila de reprodução e playlists.
3.  Garantir reprodução em segundo plano.
4.  Integrar notificações e controlos externos.
5.  Completar o leitor de vídeo.

### Fase 4 --- Funcionalidades premium

1.  Equalizador.
2.  Letras sincronizadas.
3.  Temporizador.
4.  Gestos.
5.  Visualizador de áudio.
6.  Ambiente dinâmico baseado na capa.

### Fase 5 --- Otimização e testes

1.  Otimizar carregamento de imagens.
2.  Reduzir consumo de memória e bateria.
3.  Testar diferentes dispositivos e versões Android.
4.  Corrigir falhas e problemas de reprodução.
5.  Validar acessibilidade e estabilidade.

------------------------------------------------------------------------

## 12. Critérios de conclusão da versão 3.0

A versão 3.0 só deve ser considerada concluída quando:

-   [ ] A interface estiver legível em todos os temas.
-   [ ] Não existirem sobreposições entre conteúdo, Mini Player e
    navegação.
-   [ ] A biblioteca detetar e organizar os ficheiros multimédia
    autorizados.
-   [ ] A pesquisa apresentar resultados corretos.
-   [ ] O leitor de música funcionar em primeiro e segundo plano.
-   [ ] Os controlos de notificação e ecrã bloqueado funcionarem nos
    dispositivos suportados.
-   [ ] O leitor de vídeo executar os formatos compatíveis.
-   [ ] Playlists, favoritos e histórico guardarem os dados
    corretamente.
-   [ ] As definições forem persistentes.
-   [ ] A opção Reduzir Movimento for respeitada.
-   [ ] A aplicação não apresentar falhas graves durante testes
    prolongados.
-   [ ] O desempenho se mantiver aceitável em dispositivos Android de
    recursos limitados.

------------------------------------------------------------------------

## 13. Instruções importantes para a inteligência artificial ou equipa de desenvolvimento

1.  Analisar primeiro o projeto e o código existente.
2.  Identificar o que já está implementado e o que está apenas
    representado visualmente.
3.  Não recriar funcionalidades que já funcionam.
4.  Preservar o nome, a identidade visual, os ecrãs e a navegação
    atuais.
5.  Não substituir o Liquid Glass por um design completamente diferente.
6.  Implementar funcionalidades reais; não criar botões decorativos sem
    ação.
7.  Usar APIs Android adequadas e respeitar as permissões.
8.  Não eliminar ficheiros pessoais do utilizador.
9.  Não introduzir serviços online obrigatórios numa aplicação cujo
    objetivo principal é funcionar offline.
10. Testar cada fase antes de avançar para a seguinte.
11. Se uma funcionalidade não for tecnicamente suportada, explicar a
    limitação em vez de simular que funciona.
12. Manter o código organizado, modular e preparado para futuras
    atualizações.

**Resultado esperado:** uma aplicação Android de música e vídeo offline,
com interface Liquid Glass premium, biblioteca multimédia automática,
reprodução completa, controlos modernos, boa acessibilidade e desempenho
estável.
