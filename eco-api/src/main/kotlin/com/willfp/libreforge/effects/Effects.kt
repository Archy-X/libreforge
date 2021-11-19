package com.willfp.libreforge.effects

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.JSONConfig
import com.willfp.libreforge.LibReforge
import com.willfp.libreforge.effects.effects.EffectAttackSpeedMultiplier
import com.willfp.libreforge.effects.effects.EffectBonusHealth
import com.willfp.libreforge.effects.effects.EffectCritMultiplier
import com.willfp.libreforge.effects.effects.EffectDamageMultiplier
import com.willfp.libreforge.effects.effects.EffectFallDamageMultiplier
import com.willfp.libreforge.effects.effects.EffectGiveMoney
import com.willfp.libreforge.effects.effects.EffectIncomingDamageMultiplier
import com.willfp.libreforge.effects.effects.EffectKnockbackMultiplier
import com.willfp.libreforge.effects.effects.EffectMovementSpeedMultiplier
import com.willfp.libreforge.filters.Filter
import com.willfp.libreforge.filters.Filters
import com.willfp.libreforge.filters.filters.CompoundFilter
import com.willfp.libreforge.filters.filters.FilterEmpty
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.Triggers

object Effects {
    private val BY_ID = HashBiMap.create<String, Effect>()

    val DAMAGE_MULTIPLIER: Effect = EffectDamageMultiplier()
    val CRIT_MULTIPLIER: Effect = EffectCritMultiplier()
    val KNOCKBACK_MULTIPLIER: Effect = EffectKnockbackMultiplier()
    val GIVE_MONEY: Effect = EffectGiveMoney()
    val INCOMING_DAMAGE_MULTIPLIER: Effect = EffectIncomingDamageMultiplier()
    val ATTACK_SPEED_MULTIPLIER: Effect = EffectAttackSpeedMultiplier()
    val MOVEMENT_SPEED_MULTIPLIER: Effect = EffectMovementSpeedMultiplier()
    val BONUS_HEALTH: Effect = EffectBonusHealth()
    val FALL_DAMAGE_MULTIPLIER: Effect = EffectFallDamageMultiplier()

    /**
     * Get effect matching id.
     *
     * @param id The id to query.
     * @return The matching effect, or null if not found.
     */
    fun getByID(id: String): Effect? {
        return BY_ID[id]
    }

    /**
     * List of all registered effects.
     *
     * @return The effects.
     */
    fun values(): List<Effect> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    /**
     * Add new effect.
     *
     * @param effect The effect to add.
     */
    fun addNewEffect(effect: Effect) {
        BY_ID.remove(effect.id)
        BY_ID[effect.id] = effect
    }

    /**
     * Compile an effect.
     *
     * @param config The config for the effect.
     * @param context The context to log violations for.
     *
     * @return The configured effect, or null if invalid.
     */
    fun compile(config: JSONConfig, context: String): ConfiguredEffect? {
        val effect = config.getString("id").let {
            val found = Effects.getByID(it)
            if (found == null) {
                LibReforge.logViolation(
                    it,
                    context,
                    com.willfp.libreforge.ConfigViolation("id", "Invalid effect ID specified!")
                )
            }

            found
        } ?: return null

        val args = config.getSubsection("args")
        if (effect.checkConfig(args, context)) {
            return null
        }

        val filters = config.getSubsectionsOrNull("filters").let {
            if (!effect.supportsFilters && it != null) {
                LibReforge.logViolation(
                    effect.id,
                    context,
                    com.willfp.libreforge.ConfigViolation("filters", "Specified effect does not support filters")
                )

                return@let null
            }

            val builder = mutableListOf<Filter>()

            for (filterConfig in it ?: emptyList()) {
                val id = filterConfig.getString("id")
                val filter = Filters.createById(id, filterConfig)
                if (filter is FilterEmpty) {
                    LibReforge.logViolation(
                        effect.id,
                        context,
                        com.willfp.libreforge.ConfigViolation(
                            "filters", "Invalid filter specified: $id"
                        )
                    )
                } else {
                    builder.add(filter)
                }
            }

            return@let CompoundFilter(*builder.toTypedArray())
        } ?: return null

        val triggers = config.getStrings("triggers").let {
            val triggers = mutableListOf<Trigger>()

            if (it.isNotEmpty() && effect.applicableTriggers.isEmpty()) {
                LibReforge.logViolation(
                    effect.id,
                    context,
                    com.willfp.libreforge.ConfigViolation(
                        "triggers", "Specified effect does not support triggers"
                    )
                )

                return@let null
            }

            for (id in it) {
                val trigger = Triggers.getById(id)

                if (trigger == null) {
                    LibReforge.logViolation(
                        effect.id,
                        context,
                        com.willfp.libreforge.ConfigViolation(
                            "triggers", "Invalid trigger specified: $id"
                        )
                    )
                } else {
                    triggers.add(trigger)
                }
            }

            triggers
        } ?: return null

        return ConfiguredEffect(effect, args, filters, triggers)
    }
}