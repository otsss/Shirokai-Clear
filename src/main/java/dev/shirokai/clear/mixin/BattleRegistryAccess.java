package dev.shirokai.clear.mixin;

import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
import java.util.UUID;

/** Cobblemon 1.8.3 has no public enumeration API. Exact-version, read-only accessor. */
@Mixin(value = BattleRegistry.class, remap = false)
public interface BattleRegistryAccess {
    @Accessor("battleMap") static java.util.concurrent.ConcurrentHashMap<UUID,PokemonBattle> shirokaiBattles() { throw new IllegalStateException("Accessor indisponível"); }
}
