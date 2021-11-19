package com.willfp.libreforge.conditions.conditions

import com.willfp.eco.core.config.interfaces.JSONConfig
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.updateEffects
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerMoveEvent

class ConditionAboveY: Condition("above_y") {
    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun handle(event: PlayerMoveEvent) {
        val player = event.player

        if (event.from.y == event.to.y) {
            return
        }

        player.updateEffects()
    }

    override fun isConditionMet(player: Player, config: JSONConfig): Boolean {
        return player.location.y >= config.getDouble("y")
    }

    override fun validateConfig(config: JSONConfig): List<com.willfp.libreforge.ConfigViolation> {
        val violations = mutableListOf<com.willfp.libreforge.ConfigViolation>()

        config.getDoubleOrNull("y")
            ?: violations.add(
                com.willfp.libreforge.ConfigViolation(
                    "y",
                    "You must specify the y level!"
                )
            )

        return violations
    }
}