package dev.shirokai.clear;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.api.spawning.SpawnCause;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class Eligibility {
    private Eligibility() {}
    /** null means eligible. Every non-null result is a veto reason. No world/chunk loads. */
    public static String reason(Entity entity, ClearConfig c, BattleGuard guard) {
        if (!(entity.level() instanceof ServerLevel level) || entity.isRemoved() || !entity.isAlive()) return "inativa";
        if (!level.getChunkSource().hasChunk(entity.chunkPosition().x, entity.chunkPosition().z)) return "chunk descarregado";
        if (!c.worlds.accepts(level.dimension().location().toString())) return "dimensão excluída";
        if (entity instanceof ItemEntity item && entity.getClass() == ItemEntity.class) {
            if (!c.clearItems) return "itens desativados";
            if ((long)entity.tickCount < (long)c.items.minimumAgeSeconds * 20) return "item recente";
            if (item.getItem().isEmpty()) return "item vazio";
            if (c.items.protectNamedItems && (item.hasCustomName() || item.getItem().has(DataComponents.CUSTOM_NAME))) return "item nomeado";
            if (c.items.excludedItemIds.contains(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString())) return "item excluído";
            // Explicit protection tags also protect items used by event systems.
            if (entity.getTags().stream().anyMatch(Eligibility::specialTag)) return "item protegido";
            return null;
        }
        // Exact type AND class: no modded NPC subclass or EasyNPC proxy is a removal target.
        if (!(entity instanceof PokemonEntity p) || entity.getClass() != PokemonEntity.class
            || !BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("cobblemon:pokemon")) return "tipo protegido";
        if (!c.clearWildPokemon) return "Pokémon desativados";
        try {
            var pokemon = p.getPokemon();
            if (guard == null || guard.protects(p.getUUID(), pokemon.getUuid()) || p.getBattleId() != null || p.isBattling() || p.isBusy()) return "batalha ou estado incerto";
            if (!pokemon.isWild() || pokemon.isPlayerOwned() || pokemon.isNPCOwned() || p.getOwnerUUID() != null) return "dono ou treinador";
            if (p.getTethering() != null || p.isBattleClone() || p.isUncatchable() || p.isPersistenceRequired()
                || p.isInvulnerable() || p.isNoAi() || p.isPassenger() || p.isVehicle() || p.isLeashed()) return "vínculo especial";
            if (!pokemon.getPersistentData().isEmpty()) return "metadados de integração";
            if (ProtectionApi.protectedByIntegration(p)) return "integração protegida";
            if (p.getTags().stream().anyMatch(Eligibility::specialTag)) return "raid/evento/proteção";
            if (c.pokemon.protectTaggedPokemon && !p.getTags().isEmpty()) return "tag";
            // Only the stock natural player spawner proves provenance. Unknown and restored entities stay safe.
            var cause = p.getSpawnCause();
            if (cause == null || cause.getClass() != SpawnCause.class || cause.getSpawner().getClass() != PlayerSpawner.class) return "origem natural não confirmada";
            if ((long)p.tickCount < (long)c.pokemon.minimumAgeSeconds * 20) return "Pokémon recente";
            if (c.pokemon.protectLegendary && pokemon.isLegendary()) return "lendário";
            if (c.pokemon.protectMythical && pokemon.isMythical()) return "mítico";
            if (c.pokemon.protectShiny && pokemon.getShiny()) return "shiny";
            if (c.pokemon.protectNamedPokemon && (p.hasCustomName() || pokemon.getNickname() != null)) return "nomeado";
            var species = pokemon.getSpecies().getResourceIdentifier();
            if (c.pokemon.excludedSpecies.contains(species.toString()) || c.pokemon.excludedSpecies.contains(species.getPath())) return "espécie excluída";
            return null;
        } catch (RuntimeException | LinkageError e) { return "falha ao confirmar proteção"; }
    }
    private static boolean specialTag(String tag) {
        String s = tag.toLowerCase(java.util.Locale.ROOT);
        return s.contains("raid") || s.contains("boss") || s.contains("npc") || s.contains("trainer")
            || s.contains("gym") || s.contains("breed") || s.contains("pasture") || s.contains("event")
            || s.contains("protect") || s.contains("quest");
    }
}
