# Histórico de versões

## 1.0.2 — 2026-09-05

- Corrigida a liberação de frames após falhas do scanner, evitando bloqueio das leituras seguintes.
- Adicionada leitura alternativa offline com ZXing, incluindo tentativa com contraste invertido.
- Aumentada a resolução solicitada à câmera, com fallback conforme o aparelho e foco por toque.
- Isolados resultados atrasados da câmera e da galeria; imagens têm progresso, cancelamento e limite de espera.
- Gerador usa UTF-8 explícito, margem maior e resolução proporcional à densidade do código.
- QR Codes gerados são conferidos por ML Kit no original e em uma cópia reduzida antes de serem exibidos.
- Prévia sem suavização, botão para ampliar e gravação do PNG fora da thread da interface.
- Adicionados testes de recuperação, Unicode, contraste invertido, PNG salvo, JPEG rotacionado e frames YUV.

## 1.0.1 — 2026-08-28

- Corrigida a falha imediata ao abrir o APK release causada pela otimização dos componentes internos do ML Kit.
- Mantidas as correções visuais da aba Ajustes e do botão da lanterna.

## 1.0.0 — lançamento inicial

- Scanner offline por câmera e imagem, com múltiplos resultados e deduplicação.
- Correção de falso “Conteúdo personalizado” quando a câmera não aponta para um QR Code.
- Gerador estruturado com validação e leitura de retorno.
- Histórico, busca, filtros, ordenação, favoritos, seleção múltipla e Desfazer.
- Temas claro/escuro/sistema e layouts para telas compactas e expandidas.
- Detecção de URLs suspeitas e ações externas seguras.
- Conexão Wi-Fi mediante permissão e confirmação do sistema.
- Opção e comportamento de som removidos; somente vibração opcional.
- Política local, licenças e créditos de Richard Ittou.
- Aba “Ajustes” corrigida para permanecer em uma linha e seção “Sobre o QR Code Now” removida.
- Botão da lanterna reposicionado acima da orientação do scanner, sem sobreposição.
- APK release assinado para instalação direta em aparelhos Android 13+.

[1.0.2]: https://github.com/RichardXLR/Qr-Code-Now/releases/tag/v1.0.2
[1.0.1]: https://github.com/RichardXLR/Qr-Code-Now/releases/tag/v1.0.1
