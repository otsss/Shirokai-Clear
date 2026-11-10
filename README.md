# Shirokai Clear

Mod exclusivamente de servidor para **Minecraft 1.21.1**, **Java 21**, **Fabric** e
**Cobblemon 1.8.3+1.21.1**. Mod ID: `shirokaiclear`, versão `1.0.0`.
O cliente não precisa instalar Shirokai Clear.

Remove itens abandonados e Pokémon selvagens comuns elegíveis em lotes.
O intervalo padrão é de 10 minutos, com avisos em 60, 30, 10, 5, 3, 2 e 1 segundos.
As mensagens de aviso e resultado aceitam configuração no JSON.

## Instalação

1. Use Java 21, Fabric Loader **0.18.3 ou superior**, Fabric API
   **0.116.7+1.21.1 ou superior**, Cobblemon **1.8.3+1.21.1** e suas dependências
   oficiais, incluindo Fabric Language Kotlin.
2. Coloque somente `Shirokai-Clear-1.0.0.jar` em `mods/` do servidor.
   Não instale o sources JAR ou o JAR de integração.
3. Inicie o servidor. O arquivo `config/shirokaiclear.json` será criado automaticamente.
4. Ajuste a configuração e use `/shirokaiclear reload`.

LuckPerms é opcional. Fabric Permissions API 0.3.1 está incluída no mod.
EasyNPC não é dependência obrigatória: seus tipos nunca são alvos da limpeza.
A versão binária usada na validação é **EasyNPC Fabric 7.17.1 para 1.21.1**.

## Comandos

Todos exigem `shirokaiclear.admin`, com OP nível 2 como fallback da Permissions API.
Uma decisão explícita do provedor de permissões prevalece sobre o fallback.

| Comando | Função |
| --- | --- |
| `/shirokaiclear now` | Agenda para o próximo lote a limpeza de itens e Pokémon elegíveis |
| `/shirokaiclear items` | Limpa somente itens elegíveis |
| `/shirokaiclear pokemon` | Limpa somente Pokémon elegíveis |
| `/shirokaiclear preview` | Conta elegíveis em lotes, sem remover; resultado somente para quem pediu |
| `/shirokaiclear status` | Mostra estado, contagem da última limpeza e tempo restante |
| `/shirokaiclear reload` | Valida e recarrega o JSON; se inválido, mantém a configuração anterior |
| `/shirokaiclear pause` | Pausa o agendamento automático; não cancela um lote em andamento |
| `/shirokaiclear resume` | Retoma o agendamento com um intervalo completo |

Existe no máximo um trabalho de limpeza/prévia por servidor. Comandos manuais
respeitam os filtros e proteções do JSON mesmo com agendamento automático pausado.
`enabled=false` desativa apenas o agendamento automático.

## Proteções e limites deliberados

- Verifica `battleId`, estado de batalha, busy locks, donos, lojas party/PC/NPC,
  tethering, clones de batalha, entidades persistentes/invulneráveis, montadas,
  atreladas, sem IA e não capturáveis.
- Consulta todas as batalhas registradas, incluindo originais, cópias, reservas e
  entidades de origem dos atores. Não depende de quem iniciou a batalha.
- Cobblemon 1.8.3 não oferece enumeração pública de batalhas. Um accessor Mixin
  **somente de leitura e específico dessa versão** expõe o registro; outro hook
  invalida snapshots quando uma batalha começa, mesmo por início sem eventos.
  Falha na consulta protege o Pokémon. Falha ao aplicar os Mixins impede iniciar
  o servidor, evitando executar limpeza com cobertura incompleta.
- Só entidades da classe exata `PokemonEntity` e tipo `cobblemon:pokemon` são
  candidatas. Subclasses, proxies, NPCs, projéteis e outros tipos não são removidos.
- Raids Shirokai usam `shirokai_raids_boss` e `shirokai_raid_boss`; essas tags
  são vetos obrigatórios, inclusive quando o boss público não tem `battleId`.
  As equipes do Shirokai Trainers são protegidas pelo registro e seus atores;
  Pokémon sem origem natural confirmada continuam protegidos fora da batalha.
- Metadados persistentes não vazios ou tags com `raid`, `boss`, `npc`, `trainer`,
  `gym`, `breed`, `pasture`, `event`, `protect` ou `quest` sempre impedem remoção.
  Isso é uma proteção adicional; nomes/tags nunca são a única prova de batalha.
- Apenas origem confirmada no `PlayerSpawner` padrão do Cobblemon é elegível.
  Pokémon criados por comandos, restaurados do disco ou de origem desconhecida
  são preservados. Isso pode reduzir bastante a limpeza após reiniciar o servidor.
- Não existe API universal para detectar associações ocultas de mods de terceiros.
  Integrações de reprodução/eventos podem registrar vetos adicionais por
  `ProtectionApi.register(Predicate<PokemonEntity>)`; erro no veto também protege.
  Não se declara compatibilidade universal com mods que ocultam seus vínculos.
- Lendários, míticos, shiny, nomes e tags genéricas possuem opções individuais.
  Proteções de batalhas, jogadores, NPCs e raids são **obrigatórias**: tentar
  desativá-las rejeita a configuração. `clearOtherEntities=true` e
  `onlyLoadedChunks=false` também são rejeitados até existirem implementações seguras.

## Configuração

Consulte [o exemplo completo](config/shirokaiclear.json). Campos omitidos recebem
os padrões; valores nulos ou fora dos limites rejeitam o carregamento.
Uma configuração inicial inválida deixa a limpeza desativada, preserva o arquivo
e permite recuperação por `reload` depois da correção.

`minimumAgeSeconds` usa ticks carregados da entidade nesta sessão, não tempo de
relógio; a idade reinicia ao recarregar a entidade. É uma escolha conservadora.
`items.excludedItemIds` aceita IDs completos; `pokemon.excludedSpecies` aceita
`cobblemon:magikarp` ou `magikarp`. Exclusões de dimensão prevalecem sobre inclusões.
Todas as dimensões carregadas são indexadas, incluindo dimensões de Multiworld,
sem carregar chunks para limpeza.

`warningSeconds` permite ativar/desativar cada antecedência individualmente.
Mensagens usam `{seconds}`, `{items}` e `{pokemon}` e aceitam os códigos `§`.
`announcements.enabled` controla avisos/resultados globais; a prévia continua
respondendo ao administrador. `showResults` controla apenas o resultado global.

O índice de candidatos é atualizado pelos eventos de carregamento/descarregamento.
Cada trabalho captura uma fronteira constante, sem copiar todas as entidades.
O limite de lote conta entidades **inspecionadas**, inclusive as protegidas.
Todas as alterações ocorrem na thread principal. Não há benchmark de TPS sob
carga real; ajuste `maxEntitiesPerBatch` e `batchIntervalTicks` ao servidor.

## Desenvolvimento e validação

```sh
./gradlew build
```

Gera o JAR distribuível e o sources JAR em `build/libs/`, executando os testes
unitários. O wrapper fixa Gradle 8.14.3 com checksum SHA-256.

A fixture dedicada usa as APIs e binários reais, sem ser incluída no JAR final:

```sh
export EASY_NPC_JAR=/caminho/easy_npc-fabric-1.21.1-7.17.1.jar
./gradlew build remapIntegrationJar -PeasyNpcJar="$EASY_NPC_JAR"
python3 scripts/prepare-test-server.py
python3 scripts/run-integration.py
```

O preparador cria somente um servidor de teste em `run-production/`, offline,
porta 25575, com EULA aceita para a execução local. Requer Python 3, curl,
`JAVA_HOME` apontando para um JDK 21 e o cache populado pela build. Verifica
checksums do instalador, loader, bibliotecas e Minecraft. Não aponte para um
servidor de produção. O JAR EasyNPC esperado tem SHA-256 fixado no script.

O resultado é `run-production/integration-result.json`. **Verifique `success` e
as verificações executadas**. O script runner remove resultados antigos e
rejeita arquivo ausente, falha ou cenários obrigatórios não executados, mesmo se
o Java encerrar com código zero. A execução `runIntegration` de desenvolvimento está sujeita
à falha de metadados Kotlin do Cobblemon remapeado; use o servidor acima.

Consulte [a evidência e os limites dos testes](docs/VALIDACAO.md) e
[a especificação original](docs/REQUISITOS.md). Testes de modelos de batalha
não comprovam combates completos com clientes, interface ou plugins de terceiros.
