# Como contribuir

Obrigado pelo interesse em melhorar o QR Code Now.

## Antes de começar

1. Procure uma issue existente ou abra uma proposta explicando a mudança.
2. Faça um fork e crie uma branch curta a partir de `main`, como `feat/photo-picker` ou `fix/scanner-lifecycle`.
3. Preserve o funcionamento offline, a privacidade e a compatibilidade com Android 13 ou superior.

## Padrões do projeto

- Escreva código Kotlin claro, formatado e sem credenciais ou caminhos locais.
- Mantenha regras de negócio em `domain`, integrações em `data` e estado de interface nos ViewModels.
- Toda ação externa deve validar o conteúdo e verificar se há um aplicativo capaz de atendê-la.
- Adicione ou atualize testes para parsers, segurança, banco, ViewModels e fluxos Compose afetados.
- Use recursos em `strings.xml` para textos exibidos ao usuário.

## Verificação local

No Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug :benchmark:assemble
```

No Linux ou macOS:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug :benchmark:assemble
```

## Pull requests

Descreva o problema, a solução, os testes executados e inclua capturas quando houver mudança visual. Mantenha cada pull request focado em uma única finalidade.
