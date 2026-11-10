# Shirokai Clear 1.0.0 — pré-release

Mod server-side Java 21 para Minecraft 1.21.1/Fabric e Cobblemon 1.8.3+1.21.1.

Inclui limpeza automática em lotes, avisos, configuração JSON, comandos
administrativos e Fabric Permissions API integrada. Protege batalhas, equipes de
reserva, NPCs, donos, raids e origens desconhecidas. EasyNPC 7.17.1 foi carregado
no servidor de teste; as fontes de Shirokai Raids/Trainers foram auditadas.

Build e oito testes unitários passaram. O servidor dedicado executou fixtures
dos 18 cenários e uma batalha real do simulador Showdown, preservada pela limpeza.
Veja `docs/VALIDACAO.md` e `docs/results/` para o escopo e as evidências.

**Pré-release:** não foram validados combates com clientes, LuckPerms completo ou
os binários completos de Raids/Trainers. Vínculos externos ocultos de outros mods
exigem integração explícita; Pokémon de origem desconhecida são preservados.

Instale somente `Shirokai-Clear-1.0.0.jar` em `mods/` do servidor. Requer Fabric
Loader >=0.18.3, Fabric API >=0.116.7+1.21.1 e dependências oficiais do Cobblemon.
Não exige Shirokai Clear no cliente. Configuração criada automaticamente em
`config/shirokaiclear.json`. Consulte o README antes de habilitar em produção.
