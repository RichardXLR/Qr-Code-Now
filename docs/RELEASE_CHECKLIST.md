# Checklist de lançamento

Use esta lista em cada nova versão estável.

## Código e qualidade

- [ ] Atualizar `versionCode`, `versionName` e `CHANGELOG.md`.
- [ ] Executar `testDebugUnitTest`, `lintDebug` e `:benchmark:assemble`.
- [ ] Executar os testes instrumentados em dispositivo ou emulador compatível.
- [ ] Testar scanner, Photo Picker, geração, histórico, favoritos e créditos.
- [ ] Validar câmera real, lanterna, rotação, baixa iluminação e retorno do segundo plano.
- [ ] Confirmar estados vazios, tema claro/escuro e tamanhos de fonte maiores.

## Privacidade e segurança

- [ ] Confirmar que o manifesto final não contém permissão `INTERNET`.
- [ ] Confirmar backup e transferência de dados desativados.
- [ ] Revisar URLs suspeitas, intents externas e permissões em tempo de execução.
- [ ] Verificar que nenhum segredo, keystore, caminho local ou conteúdo sensível está rastreado pelo Git.

## Artefatos

- [ ] Gerar APK release com R8 e redução de recursos.
- [ ] Gerar AAB quando a versão for destinada à Play Store.
- [ ] Validar assinatura, alinhamento, instalação limpa e atualização sobre a versão anterior.
- [ ] Calcular e publicar o SHA-256 do APK distribuído.
- [ ] Testar a versão final em Android 13, 14, 15, 16 e 17, incluindo tela grande.

## Publicação

- [ ] Criar tag SemVer e GitHub Release com notas claras.
- [ ] Anexar somente o APK assinado e o arquivo de checksums à release.
- [ ] Atualizar capturas e textos da ficha da loja quando necessário.
- [ ] Revisar a declaração Data safety conforme `PRIVACY.md`.
- [ ] Confirmar que a automação Android CI terminou com sucesso.
