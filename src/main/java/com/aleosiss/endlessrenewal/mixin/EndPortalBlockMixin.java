package com.aleosiss.endlessrenewal.mixin;

import com.aleosiss.endlessrenewal.EndlessRenewal;
import com.aleosiss.endlessrenewal.util.EndlessRenewalUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static com.aleosiss.endlessrenewal.EndlessRenewal.ENDLESS_END;
import static com.aleosiss.endlessrenewal.EndlessRenewal.MOD_ACTIVE;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
    Logger LOG = LoggerFactory.getLogger(EndPortalBlockMixin.class);

    @ModifyVariable(method = "createTeleportTarget", at = @At("STORE"), ordinal = 0)
    public RegistryKey<World> modifyWorldKey(RegistryKey<World> targetWorldKey, ServerWorld originWorld, Entity travelingEntity, BlockPos pos) {
        if (!(travelingEntity instanceof ServerPlayerEntity player)) return targetWorldKey;

        var ret = targetWorldKey;
        var hasKilledDragon = EndlessRenewalUtils.hasPlayerKilledDragon(originWorld.getServer(), player);
        // if we're going to the end, the mod is active, and we haven't killed the dragon, go to the renewing end
        LOG.info("Mod active: {}", MOD_ACTIVE);
        LOG.info("has the player killed the dragon: {}", hasKilledDragon);
        LOG.info("trying to go to: {}", targetWorldKey);

        if(EndlessRenewal.MOD_ACTIVE
                && !hasKilledDragon
                && targetWorldKey == World.END
        ) {
            ret = ENDLESS_END.getRegistryKey();
        }

        if (originWorld.getRegistryKey() == ENDLESS_END.getRegistryKey()) {
            ret = World.OVERWORLD;
        }

        LOG.info("Sending {} to {}", player.getName().getString(), ret);
        return ret;
    }

    @ModifyVariable(method = "createTeleportTarget", at = @At("STORE"), ordinal = 0)
    public boolean modifyBoolean(boolean original, ServerWorld originWorld, Entity travelingEntity, BlockPos pos, @Local(ordinal = 0) RegistryKey<World> worldRegistryKey) {
        if(originWorld.getRegistryKey() == ENDLESS_END.getRegistryKey()) return false;

        return worldRegistryKey == World.END || worldRegistryKey == ENDLESS_END.getRegistryKey();
    }
}
