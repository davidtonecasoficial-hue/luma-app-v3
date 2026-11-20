# Luma Player (Android)

Aplicação nativa Android (Kotlin + Jetpack Compose + Material 3 + AndroidX Media3/ExoPlayer) com interface **Liquid Glass**, navegação inferior animada com indicador deslizante (*Sliding Pill Indicator*), transformação fluida para barra de pesquisa (*Morphing Search Bar*), leitor de música em segundo plano e leitor de vídeo integrado.

## Requisitos de Compilação e Execução

- **Plataforma:** Android 10 (API 29) ou superior recomendado (`minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`).
- **Linguagem:** Kotlin 2.x com Jetpack Compose BOM `2024.09.00`.
- **Dependências principais:**
  - `androidx.media3` (`1.5.1`): `media3-exoplayer`, `media3-session`, `media3-ui`, `media3-common`
  - `androidx.room` (`2.7.0`) com KSP
  - `androidx.datastore:datastore-preferences` (`1.1.7`)
  - `io.coil-kt:coil-compose` (`2.7.0`)
- **Comandos Gradle:**
  - Compilar APK Debug: `gradle :app:assembleDebug`
  - Executar testes unitários e Robolectric: `gradle :app:testDebugUnitTest`

## Formatos Suportados

A compatibilidade real depende dos descodificadores presentes no dispositivo Android (inspecionável diretamente em **Ajustes → Formatos suportados pelo dispositivo**):

- **Áudio:** MP3, AAC / M4A, FLAC, WAV (PCM), OGG / Vorbis, Opus, AMR; ALAC / AC-3 quando disponíveis nos codecs do dispositivo.
- **Vídeo:** MP4 (H.264/AVC, HEVC/H.265), WebM (VP8/VP9), MKV, MOV e AV1 (em dispositivos compatíveis).

## Limitações Conhecidas

- **Letras:** A aplicação funciona 100% offline e não consulta APIs externas de letras; apenas apresenta letras quando existirem em metadados locais.
- **Equalizador:** Depende do suporte de `android.media.audiofx.Equalizer` na sessão de áudio do dispositivo.
- **Importação de ficheiros:** Utiliza o seletor nativo do sistema (*Storage Access Framework*) com permissões persistentes de leitura, evitando permissões amplas de armazenamento.
