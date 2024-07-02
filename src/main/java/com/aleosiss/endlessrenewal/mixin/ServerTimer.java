package com.aleosiss.endlessrenewal.mixin;

import com.aleosiss.endlessrenewal.access.ServerTimerAccess;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Callable;

/**
 * Runs the callable after ticksUntil ticks
 */
@Mixin(MinecraftServer.class) // ServerWorld, MinecraftServer, etc
public abstract class ServerTimer implements ServerTimerAccess {
    @Unique
    private long originalTicksUntil;

    @Unique
    private long ticksUntil;

    /**
     * If the callable returns true, then we're done; if false, try again
     */
    @Unique
    private Callable<Boolean> runnable;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) throws Exception { // Fix parameters as needed
        if (--this.ticksUntil == 0L) {
            var done = runnable.call();
            // If you want to repeat this, reset ticksUntil here.
            if (!done) { ticksUntil = originalTicksUntil; }
        }
    }

    @Override
    public void endlessrenewal$setTimer(long ticksUntil, Callable<Boolean> runnable) {
        this.ticksUntil = ticksUntil;
        this.runnable = runnable;
        this.originalTicksUntil = ticksUntil;
    }
}