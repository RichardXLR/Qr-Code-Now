# Validação da versão 1.0.2

## Escopo da correção

Leitura intermitente pela câmera e por imagens, inclusive de QR Codes gerados no próprio aplicativo. O scanner continua offline e limitado a QR Codes.

## Resultado em 5 de setembro de 2026

- **25 testes unitários aprovados.**
- **4 testes instrumentados aprovados** no emulador Android 17/API 37: `OK (4 tests)`, execução final de 12,255 segundos.
- [Android CI aprovado](https://github.com/RichardXLR/Qr-Code-Now/actions/runs/34003359006): testes unitários, lint, APK debug, compilação dos testes Android e módulo benchmark.
- APK release 1.0.2 / versionCode 3 compilado com R8 e redução de recursos; assinatura e alinhamento de páginas de 16 KB verificados.
- Atualização do APK instalado aceita pelo Android, preservando a assinatura anterior. Abertura, navegação, geração validada e salvamento de PNG verificados no release.
- Manifesto final sem `INTERNET`. Lint sem erros; avisos não bloqueantes de versões novas e sugestões de extensões Kotlin, sem atualização de dependências neste patch.

A execução inicial foi prejudicada por ANRs do sistema do emulador. Depois da inicialização e da instalação do APK de teste recompilado, a suíte Android completa passou. A matriz física Android 13–17 e testes ópticos em celulares não foram executados nesta revisão.

## Regressões automatizadas

- 25 testes unitários aprovados: parsers, geração/retorno, Unicode, limites de conteúdo, histórico, recuperação do scanner e descarte de resultados atrasados.
- Testes Android com ML Kit real: geração em resolução original/reduzida, PNG salvo pelo app, JPEG rotacionado, transparência, contraste invertido e recuperação depois de uma imagem vazia.
- Teste Android de frames YUV reais via ImageReader/ImageWriter: três leituras consecutivas pelo caminho usado na câmera, sem simular o resultado do reconhecedor.
- O CI compila os testes instrumentados; a execução deles exige um Android conectado e não é realizada pelo job atual do GitHub Actions.

## Como repetir

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:assembleRelease
```

Para conferir no aparelho, crie um QR, salve o PNG e abra-o em **Scanner → Imagem**. Repita após ler uma imagem vazia e após retornar do segundo plano. Para câmera, fotografe o código em outro visor ou impresso, mantendo toda a margem visível, e teste foco por toque e lanterna.

## Limites

Frames sintéticos verificam o processamento, não o foco óptico, a iluminação nem o hardware de cada fabricante. A aprovação automatizada não substitui ensaios em câmera física. Códigos incompletos, danificados ou pequenos/desfocados demais podem ser irrecuperáveis. Conteúdo reconhecido também pode conter dados inválidos; reconhecer PIX, por exemplo, não valida um pagamento.

O gerador limita o conteúdo a 2.000 bytes UTF-8 e só exibe o resultado após conferir sua leitura com ZXing e ML Kit. Acentos e emojis podem ocupar vários bytes por caractere. Se a validação falhar, pede-se reduzir o conteúdo em vez de disponibilizar um código que o leitor não conseguiu reconhecer.
