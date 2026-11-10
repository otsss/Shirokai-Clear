# Validação — Shirokai Clear 1.0.0

Data: 10/11/2026. Distribuição inicial como pré-release.

## Ambiente e fontes reais

- Java Temurin 21.0.9, Gradle 8.14.3, Loom 1.11.8.
- Minecraft 1.21.1, Fabric Loader 0.18.3, Fabric API 0.116.7+1.21.1.
- Cobblemon `com.cobblemon:fabric:1.8.3+1.21.1` obtido do Maven oficial
  `https://artefacts.cobblemon.com/releases`.
- Código oficial Cobblemon, tag 1.8.3, commit
  `ffe005511dd0d3ba68377fa3f95906b3837d9a0e` em
  https://gitlab.com/cable-mc/cobblemon/-/tree/1.8.3.
- EasyNPC fornecido pelo usuário: `easy_npc-fabric-1.21.1-7.17.1.jar`,
  SHA-256 `16a10cf0d4f651bf3ed0db15883814643544a00720e27f22e352c2693a2e4f04`.
- Shirokai Raids: auditoria de fonte do commit
  `396af417d581bff1503244bf21f91e3ee0581950` de
  https://github.com/istwi/Shirokai-Raids.
- Shirokai Trainers: auditoria de fonte do commit
  `e6bd9cd2d45f6518c428797216b52c24d2783d03` de
  https://github.com/istwi/Shirokai-Trainers.

Os bloqueios de rede e de identificação de versão da investigação inicial foram
resolvidos. Os binários completos de Raids e Trainers NÃO foram carregados no
servidor de teste desta entrega. A compatibilidade desses dois mods tem auditoria
de fonte e fixtures baseadas em seus vínculos, não aceitação ponta a ponta.

## Build e testes unitários

`build remapIntegrationJar` concluiu com sucesso. Oito testes JUnit executados,
zero falhas, zero erros, zero ignorados. Cobrem configuração padrão, JSON inválido,
proteções obrigatórias, limites, nulls, dimensões, índice e isolamento de fronteiras.
O JAR de integração não faz parte do JAR distribuível.

JAR: `build/libs/Shirokai-Clear-1.0.0.jar`.
SHA-256: `6f5be1582fa17152397e17f2760457d9c2918721c334951a84388927991cd53e`.

## Testes de servidor

Servidor Fabric real de produção com Cobblemon e o JAR real EasyNPC 7.17.1.
Resultados estruturados em [results](results/). O harness usa entidades reais,
registro real de batalhas, classes oficiais de atores e Pokémon, sem mocks de APIs.

Os cenários 01–18 da especificação estão representados nas fixtures:

| Cenários | Como foram exercitados |
| --- | --- |
| 01–03 | Remoção real de item antigo e Pokémon natural comum; preservação de item recente |
| 04–07 | Batalhas construídas com `PokemonBattle`, atores jogador/NPC, equipes e reservas, inseridas temporariamente no registro; ator ligado a NPC EasyNPC real |
| 08 | NPC `easy_npc:humanoid` criado pelo tipo real, preservado pela limpeza |
| 09–10 | Vínculo de ginásio por marcação e Pokémon com proprietário |
| 11–13 | Shiny, lendário e tags de boss do Shirokai Raids sem `battleId` |
| 14–15 | Mesmo serviço e proteção nas execuções manual e automática |
| 16 | Batalha registrada depois do primeiro lote, antes do lote seguinte |
| 17 | Dimensão excluída preservada |
| 18 | Encerramento/recriação do serviço e novas execuções do processo no mesmo mundo |

Uma verificação adicional inicia uma batalha real de dois Pokémon pelo
`BattleRegistry.startBattle(..., false)`, usando o simulador Showdown do Cobblemon.
O harness segura as escolhas por 60 ticks, confirma `started=true` e `ended=false`,
executa a limpeza e verifica ambos os lados preservados. Também confirma que o
snapshot anterior foi invalidado pelo hook, apesar do início sem eventos públicos.
Essa batalha é encerrada pelo harness ao final; não é um teste de vitória/derrota.

Há verificações adicionais de prévia sem remoção, permissão de jogador comum,
registro de comando administrativo, origem desconhecida, reservas, opções de
proteção e pausa/retomada. A execução final completou **45 verificações**, com `success=true`; consulte os
nomes individuais nos JSONs.

## Limitações e aceitação ainda necessária

- Não houve clientes humanos conectados. As fixtures PvP/PvE/NPC não demonstram
  funcionamento da interface, execução de movimentos, troca de Pokémon ou vitória
  em todos os tipos de combate.
- Ginásios, reprodução e raids/eventos de terceiros com vínculos externos ocultos
  precisam de um veto por `ProtectionApi` ou de metadados identificáveis. Não há
  API universal para provar ausência de todo vínculo em mods desconhecidos.
- Não foi instalado um servidor LuckPerms completo; foram testados o fallback
  administrativo e a negação de jogador comum pela Fabric Permissions API.
- Não houve benchmark de TPS com muitos jogadores. O processamento de entidades
  é limitado por lote; a consulta às equipes das batalhas ocorre por lote.
- Não foi executado o plugin Multiworld; o filtro utiliza os IDs de dimensão
  nativos, sem carregar chunks.
- O modo de desenvolvimento remapeado encontrou metadados Kotlin intermediários
  incompatíveis com reflexão em `SpeciesAdditions`. Os testes passaram com os
  JARs de produção, sem alterar ou desativar verificações do Cobblemon.
- O servidor offline de teste registra falha de consulta de chaves públicas
  externas da Mojang e avisos de data fixer dos tipos EasyNPC. Isso não impediu
  inicialização, asserções ou encerramento com salvamento do mundo.

Por essas limitações, a entrega é uma **pré-release**, com política conservadora.
Antes de produção, valide os combates com clientes e os JARs exatos de todos os
mods do servidor, incluindo as situações de vínculo oculto descritas no README.

O wrapper foi regenerado pelo Gradle 8.14.3 e seu JAR conferido contra o SHA-256
publicado oficialmente. A fixture adicional inicialmente encontrou uma exceção
ao tentar limpar um nome via `PokemonEntity.setCustomName(null)` do Cobblemon;
foi corrigida para usar `Pokemon.setNickname(null)`. A execução final passou.
