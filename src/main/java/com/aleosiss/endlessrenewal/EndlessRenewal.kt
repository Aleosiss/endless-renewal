package com.aleosiss.endlessrenewal

import com.aleosiss.endlessrenewal.util.Commands
import com.aleosiss.endlessrenewal.util.EndlessRenewalUtils.createEndlessEnd
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.boss.dragon.EnderDragonEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.world.World
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import xyz.nucleoid.fantasy.RuntimeWorldHandle

class EndlessRenewal : ModInitializer {
    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, registry, environment ->
            run {
                dispatcher.register(
                    literal("endless").executes(Commands::printHelp)
                        .then(literal("init").executes(Commands::renewableEndInit))
                        .then(literal("swap").executes(Commands::swapTargeted))
                        .then(literal("currentDimension").executes(Commands::getCurrentDimension))
                        .then(literal("toggleActive").executes(Commands::toggleModActive))
                        .then(literal("renew").executes(Commands::rebuildEnd))
                )
            }
        }

        ServerLifecycleEvents.SERVER_STARTED.register(EndlessRenewal::onServerStarted)
        ServerLivingEntityEvents.AFTER_DEATH.register(EndlessRenewal::onEntityDeath)
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger()
        private const val MOD_ID: String = "endless_renewal"

        val DIMENSION_IDENTIFIER: Identifier = Identifier.of(MOD_ID, "renewed_end")
        @JvmField
        var ENDLESS_END: RuntimeWorldHandle? = null

        // did we kill the real ender dragon?
        @JvmField
        var MOD_ACTIVE: Boolean = false

        private fun onEntityDeath(livingEntity: LivingEntity, damageSource: DamageSource?) {
            val isDragon = livingEntity is EnderDragonEntity
            if(!isDragon) return
            LOG.info("A dragon died, where was it?")
            val isCorrectDragon = livingEntity.world.registryKey == World.END
            LOG.info("${livingEntity.world.registryKey}, so $isCorrectDragon")
            if(isCorrectDragon) {
                MOD_ACTIVE = true
            }
        }

        private fun onServerStarted(server: MinecraftServer) {
            LOG.info("Endless Renewal caught the server start!")
            val end: ServerWorld = server.getWorld(World.END)!!
            val originDragonFight = end.enderDragonFight
            if (originDragonFight == null) {
                LOG.warn("The original ender dragon fight was not detected, not activating Endless Renewal!")
            }

            // creates if it does not exist
            ENDLESS_END = createEndlessEnd(server)
            MOD_ACTIVE = originDragonFight?.hasPreviouslyKilled() ?: false;
            LOG.info("Endless Renewal finished startup!")
        }
    }
}
