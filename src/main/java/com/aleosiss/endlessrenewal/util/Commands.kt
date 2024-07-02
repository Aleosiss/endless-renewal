package com.aleosiss.endlessrenewal.util

import com.aleosiss.endlessrenewal.EndlessRenewal
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.TeleportTarget
import net.minecraft.world.World
import net.minecraft.world.gen.feature.EndPlatformFeature
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object Commands {
    private val LOG: Logger = LogManager.getLogger()

    fun printHelp(context: CommandContext<ServerCommandSource>): Int {
        context.source.player?.sendMessage(Text.literal("Hello World!"))
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun rebuildEnd(context: CommandContext<ServerCommandSource>): Int {
        val server: MinecraftServer = context.source.server
        EndlessRenewalUtils.renew(server)
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun swapTargeted(context: CommandContext<ServerCommandSource>): Int {
        val player: ServerPlayerEntity = context.source.player!!
        val serverWorld: ServerWorld = player.serverWorld
        val modWorld: ServerWorld = getEndlessEndWorld(context)
        val server = context.source.server

        if (serverWorld !== modWorld) {
            val blockPos = ServerWorld.END_SPAWN_POS
            val vec3d = blockPos.toBottomCenterPos()
            val targetPos = BlockPos.ofFloored(vec3d).down()
            EndPlatformFeature.generate(modWorld, targetPos, true)

            val target = TeleportTarget(
                getEndlessEndWorld(context),
                targetPos.toBottomCenterPos(),
                Vec3d.ZERO,
                Direction.WEST.asRotation(),
                player.pitch,
                TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET)
            )
            player.teleportTo(target)
        } else {
            val overworld = server.getWorld(World.OVERWORLD)!!
            val target = TeleportTarget(
                overworld,
                player.spawnPointPosition?.toBottomCenterPos() ?: overworld.spawnPos.toBottomCenterPos(),
                Vec3d.ZERO,
                Math.random().toFloat() * 360 - 180,
                Math.random().toFloat() * 360 - 180,
                TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET)
            )
            player.teleportTo(target)
        }

        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun toggleModActive(context: CommandContext<ServerCommandSource>): Int {
        EndlessRenewal.MOD_ACTIVE = !EndlessRenewal.MOD_ACTIVE

        val player: ServerPlayerEntity = context.source.player!!
        player.sendMessage(Text.literal("EndlessRenewal MOD_ACTIVE is " + EndlessRenewal.MOD_ACTIVE))
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun getCurrentDimension(context: CommandContext<ServerCommandSource>): Int {
        val player: ServerPlayerEntity = context.source.player!!
        val worldId: Identifier = player.serverWorld.registryKey.value

        player.sendMessage(Text.literal("Currently in dimension with type: $worldId"))
        return 1
    }

    fun renewableEndInit(context: CommandContext<ServerCommandSource>): Int {
        val server: MinecraftServer = context.source.server
        EndlessRenewalUtils.renewIfNecessary(server)
        return 1
    }

    private fun getEndlessEndWorld(context: CommandContext<ServerCommandSource>): ServerWorld {
        return getWorld(context, EndlessRenewal.ENDLESS_END!!.registryKey)!!
    }

    private fun getWorld(
        context: CommandContext<ServerCommandSource>,
        dimensionRegistryKey: RegistryKey<World>
    ): ServerWorld? {
        return context.source.server.getWorld(dimensionRegistryKey)
    }
}

