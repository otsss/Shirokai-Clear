package dev.shirokai.clear;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.spawning.SpawnCause;
import com.cobblemon.mod.common.api.spawning.detail.SpawnPool;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.actor.*;
import com.cobblemon.mod.common.battles.ai.RandomBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.authlib.GameProfile;
import dev.shirokai.clear.mixin.BattleRegistryAccess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Real dedicated-server objects and APIs. Battle-state fixtures are not a client combat acceptance test. */
public final class IntegrationTest implements ModInitializer {
    private final List<String> passed=new ArrayList<>();
    private MinecraftServer server; private ServerLevel world; private FakePlayer player;
    private int ticks; private boolean done;
    private PokemonBattle live; private PokemonEntity liveA,liveB; private ClearService liveService;
    @Override public void onInitialize() {
        if(!Boolean.getBoolean("shirokaiclear.integration")) throw new IllegalStateException("Test harness cannot run on a production server");
        try { Files.deleteIfExists(Path.of("integration-result.json")); } catch(Exception e) {throw new RuntimeException(e);}
        ServerLifecycleEvents.SERVER_STARTED.register(s->{server=s;world=s.overworld();world.setChunkForced(0,0,true);});
        ServerTickEvents.END_SERVER_TICK.register(s->{
            if(world==null || done || ++ticks<40) return;
            try {
                if(ticks==40) { run(); return; }
                if(liveService!=null) liveService.tick();
                if(ticks<100) return;
                check(live!=null && live.getStarted() && !live.getEnded(),"batalha real do Showdown iniciada e mantida ativa");
                check(!liveA.isRemoved() && !liveB.isRemoved() && liveService.last().pokemon()==0,"limpeza preserva ambos os lados durante batalha real");
                live.end(); liveA.discard(); liveB.discard(); done=true;finish(true,null);server.halt(false);
            } catch(Throwable error) {done=true;error.printStackTrace();finish(false,error.toString());server.halt(false);}

        });
    }
    private void check(boolean value,String name) { if(!value) throw new AssertionError(name);passed.add(name); }
    private PokemonEntity pokemon(String species) {
        var p=PokemonProperties.Companion.parse(species+" level=10").createEntity(world);
        p.setPos(0.5,200,0.5); world.addFreshEntity(p);p.tickCount=10000;
        p.setSpawnCause(new SpawnCause(new PlayerSpawner(player,new SpawnPool("clear_test")),player));
        return p;
    }
    private ItemEntity item(int age) {var e=new ItemEntity(world,0.5,200,0.5,new ItemStack(Items.APPLE));world.addFreshEntity(e);e.tickCount=age;return e;}
    private ClearConfig config() {var c=new ClearConfig();c.announcements.enabled=false;c.logging.enabled=false;return c;}
    private ClearService service(ClearConfig c,Entity... entities) {var service=new ClearService(server,ShirokaiClear.LOG,c);for(var e:entities)service.loaded(e);return service;}
    private void drain(ClearService service) {for(int i=0;i<200 && service.busy();i++)service.tick();if(service.busy())throw new AssertionError("Job não terminou");}
    private boolean protectedPokemon(PokemonEntity p) {return Eligibility.reason(p,config(),new BattleGuard())!=null;}
    private BattlePokemon member(PokemonEntity p) {return new BattlePokemon(p.getPokemon(),p.getPokemon(),ignored->kotlin.Unit.INSTANCE);}
    private TrainerBattleActor trainer(PokemonEntity... team) {return new TrainerBattleActor("Fixture treinador",UUID.randomUUID(),Arrays.stream(team).map(this::member).toList(),new RandomBattleAI());}
    private PokemonBattle register(BattleActor left,BattleActor right) {
        var b=new PokemonBattle(BattleFormat.Companion.getGEN_9_SINGLES(),new BattleSide(left),new BattleSide(right));
        BattleRegistryAccess.shirokaiBattles().put(b.getBattleId(),b);return b;
    }
    private void unregister(PokemonBattle b) {BattleRegistryAccess.shirokaiBattles().remove(b.getBattleId());}
    private void run() throws Exception {
        player=FakePlayer.get(world,new GameProfile(UUID.randomUUID(),"ClearTest"));
        check(new BattleGuard().reliable(),"accessor do registro Cobblemon 1.8.3 aplicado");
        var old=item(2000);var recent=item(0);var c=config();var s=service(c,old,recent);
        s.start(ClearService.Mode.ALL,false);drain(s);
        check(old.isRemoved(),"01 item abandonado removido");check(!recent.isRemoved(),"02 item recente protegido");recent.discard();
        var wild=pokemon("magikarp");check(Eligibility.reason(wild,c,new BattleGuard())==null,"pré-condição: selvagem natural elegível");
        s=service(c,wild);s.start(ClearService.Mode.POKEMON,false);drain(s);check(wild.isRemoved(),"03 selvagem comum removido");
        wild=pokemon("magikarp");var foe=pokemon("rattata");
        var playerActor=new PlayerBattleActor(player.getUUID(),List.of(member(foe)));
        var battle=register(playerActor,trainer(wild));
        check(protectedPokemon(wild),"04 selvagem contra jogador protegido pelo registro sem battleId");unregister(battle);
        battle=register(new PlayerBattleActor(UUID.randomUUID(),List.of(member(wild))),new PlayerBattleActor(UUID.randomUUID(),List.of(member(foe))));
        check(protectedPokemon(wild)&&protectedPokemon(foe),"05 equipes PvP protegidas");unregister(battle);
        battle=register(trainer(wild),trainer(foe));check(protectedPokemon(wild),"06 treinador NPC protegido");unregister(battle);
        var npcType=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("easy_npc:humanoid"));
        var npc=npcType.create(world);if(npc==null)throw new AssertionError("EasyNPC humanoid ausente");npc.setPos(0,200,0);world.addFreshEntity(npc);
        var anchor=npc;
        class NpcActor extends TrainerBattleActor implements com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor<net.minecraft.world.entity.LivingEntity> {
            NpcActor(PokemonEntity p){super("EasyNPC fixture",UUID.randomUUID(),List.of(member(p)),new RandomBattleAI());}
            public net.minecraft.world.entity.LivingEntity getEntity(){return (net.minecraft.world.entity.LivingEntity)anchor;}
            public net.minecraft.world.phys.Vec3 getInitialPos(){return anchor.position();}
        }
        battle=register(new NpcActor(wild),trainer(foe));check(protectedPokemon(wild),"07 equipe de ator ligado a EasyNPC protegida");unregister(battle);
        s=service(c,npc);s.start(ClearService.Mode.ALL,false);drain(s);check(!npc.isRemoved(),"08 EasyNPC real 7.17.1 preservado");
        wild.addTag("shirokai_gym");check(protectedPokemon(wild),"09 ginásio protegido");wild.removeTag("shirokai_gym");
        wild.setOwnerUUID(player.getUUID());check(protectedPokemon(wild),"10 Pokémon enviado por jogador protegido");wild.setOwnerUUID(null);
        var shiny=pokemon("magikarp shiny=true");check(protectedPokemon(shiny),"11 shiny protegido");shiny.discard();
        var legendary=pokemon("rayquaza");check(protectedPokemon(legendary),"12 lendário protegido");legendary.discard();
        wild.addTag("shirokai_raids_boss");check(protectedPokemon(wild),"13 boss público Shirokai Raids protegido sem battleId");
        var guarded=wild;s=service(c,guarded);s.start(ClearService.Mode.ALL,false);drain(s);check(!guarded.isRemoved(),"14 manual preserva raid");
        c.intervalSeconds=1;s=service(c,guarded);for(int i=0;i<25;i++)s.tick();check(!guarded.isRemoved()&&s.last().pokemon()==0,"15 automática preserva raid");
        guarded.removeTag("shirokai_raids_boss");var first=item(2000);c.performance.maxEntitiesPerBatch=1;
        s=service(c,first,guarded);s.start(ClearService.Mode.ALL,false);s.tick();
        check(first.isRemoved(),"lote inicial executado antes da mudança de batalha");
        battle=register(trainer(guarded),trainer(foe));drain(s);check(!guarded.isRemoved(),"16 batalha iniciada entre lotes preservada");unregister(battle);
        c=config();c.worlds.excludedDimensions.add("minecraft:overworld");s=service(c,guarded);s.start(ClearService.Mode.ALL,false);drain(s);check(!guarded.isRemoved(),"17 dimensão excluída preservada");
        c=config();c.intervalSeconds=1;var a=item(2000);s=service(c,a);s.stop();var replacement=service(c,a);
        for(int i=0;i<25;i++)replacement.tick();check(a.isRemoved()&&replacement.last().items()==1&&!s.busy(),"18 ciclo stop/start isola agendamentos (reinício real verificado por segunda execução)");
        c=config();var preview=pokemon("magikarp");s=service(c,preview);s.start(ClearService.Mode.ALL,true);drain(s);check(!preview.isRemoved(),"preview não remove");
        var reserve=pokemon("rattata");battle=register(trainer(preview,reserve),trainer(foe));check(protectedPokemon(reserve),"reserva de batalha protegida");unregister(battle);
        preview.setSpawnCause(null);check(protectedPokemon(preview),"origem desconhecida protegida");
        preview.discard();reserve.discard();guarded.discard();foe.discard();npc.discard();
        var dispatcher=server.getCommands().getDispatcher();boolean denied=false;
        try {dispatcher.execute("shirokaiclear now",player.createCommandSourceStack().withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
        check(denied,"jogador comum sem permissão não executa limpeza");
        check(dispatcher.execute("shirokaiclear status",server.createCommandSourceStack())==1,"status administrativo registrado");
        var optional=pokemon("magikarp shiny=true");var opts=config();opts.pokemon.protectShiny=false;
        check(Eligibility.reason(optional,opts,new BattleGuard())==null,"opção shiny desativada permite selvagem confirmado");
        optional.getPokemon().setShiny(false);optional.tickCount=0;
        check(Eligibility.reason(optional,opts,new BattleGuard())!=null,"idade mínima de Pokémon aplicada");optional.tickCount=10000;
        optional.setCustomName(net.minecraft.network.chat.Component.literal("Protegido"));
        check(Eligibility.reason(optional,opts,new BattleGuard())!=null,"nome protege Pokémon");opts.pokemon.protectNamedPokemon=false;
        check(Eligibility.reason(optional,opts,new BattleGuard())==null,"opção de nome funciona");optional.getPokemon().setNickname(null);
        optional.addTag("ordinary_marker");check(Eligibility.reason(optional,opts,new BattleGuard())!=null,"tag genérica protege");opts.pokemon.protectTaggedPokemon=false;
        check(Eligibility.reason(optional,opts,new BattleGuard())==null,"opção de tag genérica funciona");optional.removeTag("ordinary_marker");
        optional.addTag("shirokai_raid_boss");check(Eligibility.reason(optional,opts,new BattleGuard())!=null,"tag legada de raid continua obrigatória com tags genéricas desativadas");optional.removeTag("shirokai_raid_boss");
        opts.pokemon.excludedSpecies.add("magikarp");check(Eligibility.reason(optional,opts,new BattleGuard())!=null,"espécie excluída");opts.pokemon.excludedSpecies.clear();
        optional.getPokemon().getPersistentData().putBoolean("breeding",true);check(Eligibility.reason(optional,opts,new BattleGuard())!=null,"reprodução com metadados protegida");optional.getPokemon().getPersistentData().remove("breeding");
        var protectedIntegration=optional;ProtectionApi.register(entity->entity==protectedIntegration);
        check(Eligibility.reason(optional,opts,new BattleGuard())!=null,"veto adicional de integração respeitado");optional.discard();
        var namedItem=item(2000);namedItem.getItem().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Nome"));
        opts=config();check(Eligibility.reason(namedItem,opts,new BattleGuard())!=null,"nome protege item");opts.items.protectNamedItems=false;
        check(Eligibility.reason(namedItem,opts,new BattleGuard())==null,"opção de nome de item funciona");opts.items.excludedItemIds.add("minecraft:apple");
        check(Eligibility.reason(namedItem,opts,new BattleGuard())!=null,"ID de item excluído");namedItem.discard();
        var resumedItem=item(2000);opts=config();opts.intervalSeconds=1;s=service(opts,resumedItem);s.pause();
        for(int i=0;i<30;i++)s.tick();check(!resumedItem.isRemoved(),"pause impede limpeza automática");s.resume();for(int i=0;i<25;i++)s.tick();
        check(resumedItem.isRemoved(),"resume retoma limpeza automática");
        liveA=pokemon("magikarp");liveB=pokemon("magikarp");liveB.setPos(4,200,0);
        class WaitingWildActor extends PokemonBattleActor {
            WaitingWildActor(PokemonEntity entity) {super(entity.getUUID(),member(entity),64,new RandomBattleAI());}
            @Override public void onChoiceRequested() {} // Test holds choices; the real simulator remains in battle.
        }
        var before=new BattleGuard();
        BattleRegistry.startBattle(BattleFormat.Companion.getGEN_9_SINGLES(),new BattleSide(new WaitingWildActor(liveA)),new BattleSide(new WaitingWildActor(liveB)),false)
            .ifSuccessful(b->{live=b;return kotlin.Unit.INSTANCE;});
        check(live!=null,"início real sem eventos retornou batalha");
        check(before.protects(liveA.getUUID(),liveA.getPokemon().getUuid()),"snapshot invalidado quando uma batalha inicia sem eventos");
        liveService=service(config(),liveA,liveB);liveService.start(ClearService.Mode.ALL,false);

    }
    private void finish(boolean success,String error) {
        var json=new JsonObject();json.addProperty("success",success);json.addProperty("error",error);
        json.addProperty("scope","dedicated server API fixtures; no live clients");json.add("passed",new Gson().toJsonTree(passed));
        try {Files.writeString(Path.of("integration-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(json));}catch(Exception e){throw new RuntimeException(e);}
        System.out.println("SHIROKAI_CLEAR_INTEGRATION "+json);
    }
}
