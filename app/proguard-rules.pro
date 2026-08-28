-dontwarn org.conscrypt.**

# O ML Kit registra e resolve componentes internos em tempo de execução. A otimização
# agressiva do R8/AGP 9 pode fundir esses providers e fazer getClient() retornar nulo.
-keep class com.google.mlkit.** { *; }
