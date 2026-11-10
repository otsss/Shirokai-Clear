package dev.shirokai.clear;

import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.shirokai.clear.mixin.BattleRegistryAccess;
import java.util.*;

/** Fresh per batch, on the server thread. Includes originals, clones, reserves and actor anchors. */
public final class BattleGuard {
    private static volatile long epoch;
    private final long capturedEpoch = epoch;
    public static void battleStarting() { epoch++; }
    private final Set<UUID> pokemon = new HashSet<>(), entities = new HashSet<>();
    private boolean reliable = true;
    public BattleGuard() {
        try {
            for (var battle : BattleRegistryAccess.shirokaiBattles().values()) {
                // Protect even ending battles until the registry actually removes them.
                for (var actor : battle.getActors()) {
                    if (actor instanceof EntityBackedBattleActor<?> backed && backed.getEntity() != null)
                        entities.add(backed.getEntity().getUUID());
                    for (var member : actor.getPokemonList()) {
                        add(member.getOriginalPokemon()); add(member.getEffectedPokemon());
                        if (member.getEntity() != null) entities.add(member.getEntity().getUUID());
                    }
                }
            }
        } catch (RuntimeException | LinkageError failure) { reliable = false; }
    }
    private void add(Pokemon p) {
        if (p == null) { reliable = false; return; }
        pokemon.add(p.getUuid());
        if (p.getEntity() != null) entities.add(p.getEntity().getUUID());
    }
    public boolean protects(UUID entity, UUID id) { return !reliable || capturedEpoch != epoch || entities.contains(entity) || pokemon.contains(id); }
    public boolean reliable() { return reliable; }
}
