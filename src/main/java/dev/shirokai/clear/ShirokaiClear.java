package dev.shirokai.clear;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import me.lucko.fabric.api.permissions.v0.Permissions;
import java.nio.file.Path;

public final class ShirokaiClear implements ModInitializer {
    public static final Logger LOG=LoggerFactory.getLogger("ShirokaiClear");
    private static final Path CONFIG=FabricLoader.getInstance().getConfigDir().resolve("shirokaiclear.json");
    private ClearService service;
    private boolean configurationHealthy;
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try { service=new ClearService(server,LOG,ClearConfig.load(CONFIG)); configurationHealthy=true; }
            catch (Exception error) {
                var disabled=new ClearConfig();disabled.enabled=false;disabled.clearItems=false;disabled.clearWildPokemon=false;
                service=new ClearService(server,LOG,disabled);configurationHealthy=false;
                LOG.error("Limpeza desativada: configuração inválida; arquivo preservado. Corrija e use reload",error);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { if(service!=null) service.stop(); service=null; });
        ServerEntityEvents.ENTITY_LOAD.register((entity,world) -> { if(service!=null) service.loaded(entity); });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,world) -> { if(service!=null) service.unloaded(entity); });
        ServerTickEvents.END_SERVER_TICK.register(server -> { if(service!=null) service.tick(); });
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment) -> {
            var root=Commands.literal("shirokaiclear").requires(Permissions.require("shirokaiclear.admin",2));
            for(String name : new String[]{"now","items","pokemon","preview","reload","status","pause","resume"}) {
                root.then(Commands.literal(name).executes(context -> {
                    var source=context.getSource();
                    try {
                        if(name.equals("reload")) {
                            var config=ClearConfig.load(CONFIG);
                            if(service==null) throw new IllegalStateException("Reinicie o servidor após corrigir a configuração inicial para indexar as entidades com segurança");
                            service.reload(config); configurationHealthy=true; source.sendSuccess(() -> Component.literal("Configuração recarregada."),false); return 1;
                        }
                        if(service==null || !configurationHealthy) throw new IllegalStateException("Limpeza indisponível: corrija a configuração e use reload");
                        String message=switch(name) {
                            case "status" -> "Automática: "+(service.paused()?"pausada":service.config().enabled?"ativa":"desativada")
                                +"; próxima: "+service.remainingSeconds()+"s; em execução: "+service.busy()
                                +"; última: "+service.last().items()+" itens, "+service.last().pokemon()+" Pokémon.";
                            case "pause" -> {service.pause(); yield "Limpeza automática pausada.";}
                            case "resume" -> {service.resume(); yield "Limpeza automática retomada.";}
                            default -> {
                                var mode=name.equals("items")?ClearService.Mode.ITEMS:name.equals("pokemon")?ClearService.Mode.POKEMON:ClearService.Mode.ALL;
                                if(!service.start(mode,name.equals("preview"), result -> {
                                    if(result.preview()) source.sendSuccess(() -> Component.literal("Prévia: "+result.items()+" itens, "+result.pokemon()+" Pokémon; nenhuma remoção."),false);
                                })) throw new IllegalStateException("Já existe uma limpeza/prévia em andamento");
                                yield "Processamento em lotes iniciado.";
                            }
                        };
                        source.sendSuccess(() -> Component.literal(message),false); return 1;
                    } catch(Exception e) { source.sendFailure(Component.literal("Shirokai Clear: "+e.getMessage())); return 0; }
                }));
            }
            dispatcher.register(root);
        });
    }
}
