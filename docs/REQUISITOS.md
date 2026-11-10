# Projeto: Shirokai Clear

Crie um mod chamado **Shirokai Clear**, desenvolvido em Java 21 para Minecraft 1.21.1 Fabric, compatível com Cobblemon 1.8.3.

O mod deve funcionar exclusivamente no servidor, sem exigir instalação no cliente.

## 1. Objetivo

Desenvolver um sistema automático e otimizado de limpeza de entidades para o servidor Cobblemon Shirokai.

O mod deve remover:

- Itens abandonados no chão.
- Pokémon selvagens que não estejam protegidos.
- Outras entidades somente se forem explicitamente habilitadas no futuro.

**REGRA MAIS IMPORTANTE: NENHUMA BATALHA PODE SER INTERROMPIDA PELA LIMPEZA.**

A proteção deve funcionar para batalhas entre jogadores, Pokémon selvagens, treinadores NPC, líderes de ginásio e qualquer outra batalha gerenciada pelo Cobblemon.

## 2. Proteção obrigatória das batalhas

Antes de remover qualquer Pokémon, verificar seu estado utilizando as APIs oficiais do Cobblemon 1.8.3.

Nunca remover:

- Pokémon participando de batalhas PvP.
- Pokémon participando de batalhas PvE.
- Pokémon envolvidos em batalhas contra Pokémon selvagens.
- Pokémon envolvidos em batalhas contra treinadores NPC.
- Pokémon envolvidos em batalhas de ginásio.
- Pokémon envolvidos em batalhas com múltiplos participantes.
- Pokémon pertencentes a jogadores.
- Pokémon que estejam seguindo jogadores ou enviados para fora da Poké Bola.
- Pokémon vinculados a uma batalha ativa, mesmo que não sejam o Pokémon atualmente em campo.

A proteção deve considerar todas as batalhas ativas, independentemente de quem iniciou o combate.

Não confiar apenas em nomes, tags ou proximidade dos jogadores para identificar batalhas.

**Se não for possível confirmar com segurança que um Pokémon está fora de batalha, ele NÃO deve ser removido.**

Verificar também possíveis mudanças de estado entre a seleção da entidade e sua remoção.

## 3. Compatibilidade com EasyNPC

O mod deve ser totalmente compatível com EasyNPC.

Nunca remover:

- NPCs criados pelo EasyNPC.
- Treinadores NPC.
- NPCs de ginásios.
- NPCs que executam comandos.
- NPCs utilizados em missões ou eventos.
- Entidades pertencentes ao sistema interno do EasyNPC.

Não identificar NPCs apenas pelo nome exibido.

Investigar como o EasyNPC registra suas entidades e implementar uma identificação segura utilizando classes, identificadores ou APIs disponíveis.

Se a identificação não puder ser validada, manter a entidade protegida.

**O Shirokai Clear não pode alterar, remover ou prejudicar NPCs do EasyNPC.**

## 4. Proteção adicional de Pokémon

Nunca remover automaticamente:

- Pokémon lendários.
- Pokémon míticos.
- Pokémon shiny.
- Pokémon pertencentes a jogadores.
- Pokémon em batalhas.
- Pokémon de treinadores NPC.
- Pokémon de ginásios.
- Pokémon vinculados a raids ou World Boss.
- Pokémon marcados como protegidos.
- Pokémon associados a sistemas de reprodução, caso existam entidades desse tipo no mundo.

Criar opções individuais para essas proteções.

A limpeza padrão deve atingir apenas Pokémon selvagens comuns, fora de batalha e sem qualquer vínculo especial.

## 5. Sistema de limpeza automática

Executar a limpeza em intervalos configuráveis.

Padrão: a cada 10 minutos.

Enviar avisos no chat antes da limpeza:

- 60 segundos antes.
- 30 segundos antes.
- 10 segundos antes.
- 5 segundos antes.
- 3 segundos antes.
- 2 segundos antes.
- 1 segundo antes.

Mensagem de exemplo:

`§b✦ Shirokai §8» §7Limpeza de entidades em §e30 segundos§7!`

Mensagem ao finalizar:

`§b✦ Shirokai §8» §aLimpeza concluída! §7Itens removidos: §f120 §8| §7Pokémon removidos: §f45`

Permitir configurar ou desativar cada aviso.

Não enviar mensagens individuais para cada entidade removida.

## 6. Comandos administrativos

Implementar:

`/shirokaiclear now`

Executa imediatamente a limpeza de itens e Pokémon elegíveis.

`/shirokaiclear items`

Limpa somente itens no chão.

`/shirokaiclear pokemon`

Limpa somente Pokémon selvagens elegíveis.

`/shirokaiclear reload`

Recarrega a configuração.

`/shirokaiclear status`

Mostra o tempo restante até a próxima limpeza e a quantidade removida na última execução.

`/shirokaiclear pause`

Pausa a limpeza automática.

`/shirokaiclear resume`

Retoma a limpeza automática.

`/shirokaiclear preview`

Calcula quantas entidades seriam removidas sem remover nenhuma.

Todos os comandos devem respeitar as mesmas regras de proteção. Mesmo uma limpeza manual nunca pode remover Pokémon em batalha ou NPCs protegidos.

## 7. LuckPerms

Implementar integração com Fabric Permissions API.

Permissão administrativa:

`shirokaiclear.admin`

Permitir também OP nível 2 como fallback, quando adequado.

Jogadores comuns não devem conseguir executar comandos administrativos.

## 8. Configuração JSON

Criar automaticamente:

`config/shirokaiclear.json`

Exemplo:

```json
{
  "enabled": true,
  "intervalSeconds": 600,
  "clearItems": true,
  "clearWildPokemon": true,
  "clearOtherEntities": false,

  "items": {
    "minimumAgeSeconds": 60,
    "protectNamedItems": true,
    "excludedItemIds": []
  },

  "pokemon": {
    "protectBattles": true,
    "protectPlayerPokemon": true,
    "protectNpcPokemon": true,
    "protectLegendary": true,
    "protectMythical": true,
    "protectShiny": true,
    "protectRaidBosses": true,
    "protectNamedPokemon": true,
    "protectTaggedPokemon": true,
    "minimumAgeSeconds": 120,
    "excludedSpecies": []
  },

  "npcs": {
    "protectEasyNPC": true,
    "protectAllNPCs": true
  },

  "worlds": {
    "enabledDimensions": [],
    "excludedDimensions": []
  },

  "announcements": {
    "enabled": true,
    "warningSeconds": [60, 30, 10, 5, 3, 2, 1],
    "showResults": true
  },

  "performance": {
    "maxEntitiesPerBatch": 100,
    "batchIntervalTicks": 1,
    "onlyLoadedChunks": true
  },

  "logging": {
    "enabled": true,
    "logProtectedEntities": false,
    "logSummary": true
  }
}
```

Todas as opções devem ser implementadas funcionalmente, não apenas adicionadas ao JSON.

Caso uma configuração dependa de dados indisponíveis na API, documentar a limitação e manter a proteção ativa.

## 9. Otimização

O mod deve ser adequado para servidores com muitos jogadores e Pokémon.

Requisitos:

- Não varrer todas as entidades a cada tick.
- Utilizar agendamento baseado no intervalo configurado.
- Processar a limpeza em lotes.
- Trabalhar somente com entidades de chunks carregados.
- Não carregar chunks novos para realizar a limpeza.
- Executar alterações de entidades na thread principal.
- Evitar operações bloqueantes.
- Não utilizar threads paralelas para manipular diretamente entidades do Minecraft.
- Revalidar a proteção de cada entidade imediatamente antes da remoção.
- Cancelar a remoção quando houver dúvidas sobre o estado de batalha.

A limpeza não pode provocar travamentos ou grandes picos de TPS.

## 10. Compatibilidade com outros mods

Considerar especialmente:

- Cobblemon 1.8.3.
- EasyNPC.
- Multiworld.
- LuckPerms.
- Shirokai Raids.
- Mods de ginásios e treinadores.

Não modificar os dados internos desses mods.

Se um Pokémon estiver vinculado a uma raid, evento ou NPC e essa informação não puder ser determinada com segurança, não removê-lo.

## 11. Testes obrigatórios

Criar e executar testes para os seguintes cenários:

1. Item comum abandonado é removido.
2. Item recém-jogado permanece protegido até atingir a idade mínima.
3. Pokémon selvagem comum fora de batalha é removido.
4. Pokémon selvagem em batalha contra jogador não é removido.
5. Pokémon em batalha PvP não é removido.
6. Pokémon em batalha contra treinador NPC não é removido.
7. Pokémon de treinador EasyNPC não é removido.
8. NPC EasyNPC não é removido.
9. Pokémon de ginásio não é removido.
10. Pokémon de jogador fora da Poké Bola não é removido.
11. Pokémon shiny não é removido.
12. Pokémon lendário não é removido.
13. Pokémon de raid não é removido.
14. Limpeza manual respeita todas as proteções.
15. Limpeza automática respeita todas as proteções.
16. Pokémon que inicia batalha durante uma limpeza em lotes permanece protegido.
17. Dimensões excluídas não são afetadas.
18. Reiniciar o servidor não duplica agendamentos.

Os testes de integração devem utilizar as versões reais das APIs, sem presumir métodos inexistentes.

## 12. Estrutura e entrega

- Mod ID: `shirokaiclear`.
- Nome: `Shirokai Clear`.
- Versão inicial: `1.0.0`.
- Minecraft: `1.21.1`.
- Fabric Loader compatível.
- Java: `21`.
- Ambiente: `server`.

Gerar:

- Código-fonte completo.
- Configuração JSON.
- README em português.
- Instruções de instalação.
- JAR compilado em `build/libs/`.
- Workflow GitHub Actions para compilar automaticamente.
- Release com o JAR após validação da build.

Não declarar compatibilidade ou testes aprovados sem verificação real.

**Prioridade absoluta: preservar todas as batalhas e todos os NPCs. É preferível deixar um Pokémon no mundo a remover uma entidade que possa estar envolvida em combate ou em um sistema especial.**