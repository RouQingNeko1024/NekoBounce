/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement

import io.qzz.nekobounce.event.MovementInputEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.simulation.SimulatedPlayer

object Parkour : Module("Parkour", Category.MOVEMENT, subjective = true, gameDetecting = false) {

    val onMovementInput = handler<MovementInputEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        val simPlayer = SimulatedPlayer.fromClientPlayer(event.originalInput)

        simPlayer.tick()

        if (thePlayer.isMoving && thePlayer.onGround && !thePlayer.isSneaking && !mc.gameSettings.keyBindSneak.isKeyDown && !simPlayer.onGround) {
            event.originalInput.jump = true
        }

    }
}
