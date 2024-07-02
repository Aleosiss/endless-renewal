package com.aleosiss.endlessrenewal.util

import com.aleosiss.endlessrenewal.EndlessRenewal
import com.aleosiss.endlessrenewal.EndlessRenewal.Companion.DIMENSION_IDENTIFIER
import com.aleosiss.endlessrenewal.EndlessRenewal.Companion.ENDLESS_END
import com.aleosiss.endlessrenewal.access.ServerTimerAccess
import net.minecraft.advancement.AdvancementEntry
import net.minecraft.entity.boss.dragon.EnderDragonFight
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.world.World
import net.minecraft.world.dimension.DimensionTypes
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import xyz.nucleoid.fantasy.Fantasy
import xyz.nucleoid.fantasy.RuntimeWorldConfig
import xyz.nucleoid.fantasy.RuntimeWorldHandle
import java.util.*

object EndlessRenewalUtils {
    private val LOG: Logger = LogManager.getLogger()
    private const val NUM_TICKS_BEFORE_DELETE: Long = 10

    @JvmStatic
    fun renew(server: MinecraftServer) {
        try {
            val worldHandle = ENDLESS_END
            val world = worldHandle!!.asWorld()
            val fantasyApi = Fantasy.get(server)

            // teleport all players as if they had entered the end portal
            val overworld: ServerWorld = server.overworld
            // create copy of the list
            world.players.toList().forEach { player ->
                val spawnPos = player.spawnPointPosition ?: overworld.spawnPos
                val spawnWorld = server.getWorld(player.spawnPointDimension)
                player.teleport(spawnWorld, spawnPos.x + 0.5, spawnPos.y.toDouble(), spawnPos.z + 0.5, player.spawnAngle, 0.0F);
            }

            // schedule world deletion in 5 ticks
            val access = (server as ServerTimerAccess)
            access.`endlessrenewal$setTimer`(NUM_TICKS_BEFORE_DELETE, renew(fantasyApi, world, server))

        } catch (e: Exception) {
            LOG.error("renew hit an exception!", e)
        }
    }

    // TODO: some method to not try infinitely if someone built some chunkloading stuff
    // however to my knowledge it is currently not possible via Vanilla mechanics to chunkload the end
    private fun renew(fantasyApi: Fantasy, world: ServerWorld?, server: MinecraftServer): () -> Boolean = {
        if (fantasyApi.tickDeleteWorld(world)) {
            LOG.info("Renewing renewable End")
            createEndlessEnd(server)
            true
        } else {
            LOG.info("Could not delete renewable End, wasn't unloaded, trying again in $NUM_TICKS_BEFORE_DELETE ticks")
            false
        }
    }

    @JvmStatic
    fun doesServerWorldNeedDragonFightReset(world: ServerWorld): Boolean {
        if(world.dimensionEntry != ENDLESS_END!!.registryKey) return false

        val previousEndDragonFight = world.enderDragonFight
        var requiresReset = world.dimension == null
        requiresReset = requiresReset || previousEndDragonFight == null
        requiresReset = requiresReset || previousEndDragonFight!!.hasPreviouslyKilled()

        return requiresReset
    }

    @JvmStatic
    fun hasPlayerKilledDragon(server: MinecraftServer, player: ServerPlayerEntity): Boolean {
        val killDragonAchievement: AdvancementEntry = server.advancementLoader.advancements
            .first { it.id == Identifier.ofVanilla("end/kill_dragon") }
            ?: throw RuntimeException("There is no kill dragon advancement?")
        return player.advancementTracker.getProgress(killDragonAchievement).isDone
    }

    fun renewIfNecessary(server: MinecraftServer) {
        val requiresReset: Boolean = doesServerWorldNeedDragonFightReset(server.getWorld(ENDLESS_END!!.registryKey)!!)
        if (requiresReset) {
            renew(server)
        }
    }

    fun createEndlessEnd(server: MinecraftServer): RuntimeWorldHandle {
        val fantasyApi: Fantasy = Fantasy.get(server)
        val end = server.getWorld(World.END)!!

        val seed = Random().nextLong()
        val worldConfig: RuntimeWorldConfig = RuntimeWorldConfig()
            .setDimensionType(DimensionTypes.THE_END)
            .setDifficulty(server.saveProperties.difficulty)
            .setGenerator(end.chunkManager.chunkGenerator)
            .setSeed(seed)

        val handle = fantasyApi.getOrOpenPersistentWorld(DIMENSION_IDENTIFIER, worldConfig)
        val world = handle.asWorld()
        if(world.enderDragonFight == null) {
            if(world.aliveEnderDragons.size > 0) {
                LOG.info("There was an ender dragon already active, but no ender dragon fight?")
            }
            world.enderDragonFight = EnderDragonFight(world, seed, EnderDragonFight.Data.DEFAULT)
            world.enderDragonFight!!.tick()
        }

        return handle
    }
}
