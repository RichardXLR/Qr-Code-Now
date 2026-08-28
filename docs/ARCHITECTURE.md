# Arquitetura do QR Code Now

## Princípios

O aplicativo é offline-first, orientado a estado imutável e separa regras de negócio de Android e da interface. Os ViewModels expõem `StateFlow`; Hilt fornece implementações para contratos do domínio.

## Camadas

- `domain`: modelos selados de conteúdo, parser, avaliação de URLs, gerador, contratos de repositório, scanner e ações externas.
- `data`: Room, DataStore, ML Kit, MediaStore e implementações dos repositórios.
- `presentation`: telas Jetpack Compose, estados de tela e ViewModels.
- `navigation`: destinos principais e rotas secundárias.
- `di`: módulos Hilt que ligam contratos às implementações.
- `ui`: cores, tipografia e temas da marca.
- `benchmark`: macrobenchmarks e geração de Baseline Profile em módulo isolado.

## Fluxo do scanner

```text
CameraX / Photo Picker
        ↓
QrScannerEngine (ML Kit local)
        ↓
deduplicação e pausa de análise
        ↓
QrContentParser + UrlSafety
        ↓
estado imutável da tela
        ↓
HistoryRepository / ação confirmada pelo usuário
```

A análise usa `KEEP_ONLY_LATEST`, é suspensa fora do ciclo de vida ativo e permanece pausada enquanto um resultado está sendo tratado.

## Persistência

Room mantém uma única fonte para histórico e favoritos. DataStore persiste somente preferências de interface e comportamento. O backup e a transferência automática dos dados do aplicativo estão desativados.

## Limites de segurança

- Não existe permissão de internet no manifesto final.
- Photo Picker evita permissão ampla de armazenamento.
- Links não HTTP/HTTPS e URLs consideradas suspeitas nunca são abertos automaticamente.
- Intents externas são centralizadas em `ExternalActionLauncher` e verificadas antes do envio.
- Imagens geradas são validadas por leitura antes de serem gravadas pelo MediaStore.
