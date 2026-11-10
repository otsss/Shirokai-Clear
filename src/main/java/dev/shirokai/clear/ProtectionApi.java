package dev.shirokai.clear;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/** Optional additive vetoes for breeding/event integrations. Throwing also vetoes removal. */
public final class ProtectionApi {
    private static final List<Predicate<PokemonEntity>> VETOES = new CopyOnWriteArrayList<>();
    private ProtectionApi() {}
    public static void register(Predicate<PokemonEntity> veto) { VETOES.add(java.util.Objects.requireNonNull(veto)); }
    public static boolean protectedByIntegration(PokemonEntity entity) {
        for (var veto : VETOES) {
            try { if (veto.test(entity)) return true; }
            catch (RuntimeException | LinkageError e) { return true; }
        }
        return false;
    }
}
