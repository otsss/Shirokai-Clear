package dev.shirokai.clear.mixin;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import dev.shirokai.clear.BattleGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Invalidates a batch snapshot even if an integration starts a battle reentrantly. */
@Mixin(value=BattleRegistry.class, remap=false)
public abstract class BattleEpochMixin {
    @Inject(method="startShowdown",at=@At("HEAD"),remap=false)
    private void shirokaiBattleStarting(PokemonBattle battle, CallbackInfo ci) { BattleGuard.battleStarting(); }
}
