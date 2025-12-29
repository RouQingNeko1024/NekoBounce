package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.util.ResourceLocation
import java.util.*

object Cape : Module("Cape", Category.RENDER) {

    private val capeMode by choices(
        "Cape",
        arrayOf("cape1", "cape2", "cape3", "astolfo", "ravenanime", "ravenxd", "Augustus", "Astolfotrap"),
        "cape1"
    )

    private val capes = mapOf(
        "cape1" to ResourceLocation("liquidbounce/cape/cape1.png"),
        "cape2" to ResourceLocation("liquidbounce/cape/cape2.png"),
        "cape3" to ResourceLocation("liquidbounce/cape/cape3.png"),
        "astolfo" to ResourceLocation("liquidbounce/cape/astolfo.png"),
        "ravenanime" to ResourceLocation("liquidbounce/cape/ravenanime.png"),
        "ravenxd" to ResourceLocation("liquidbounce/cape/ravenxd.png"),
        "Augustus" to ResourceLocation("liquidbounce/cape/Augustus.png"),
        "Astolfotrap" to ResourceLocation("liquidbounce/cape/Astolfotrap.png")
    )

    fun getCapeForPlayer(uuid: UUID): ResourceLocation? {
        // 仅对当前玩家显示披风
        if (!state) return null
        
        val currentPlayer = mc.thePlayer ?: return null
        
        // 检查传入的UUID是否是当前玩家的UUID
        if (uuid != currentPlayer.uniqueID) return null
        
        return capes[capeMode]
    }

    override val tag
        get() = capeMode
}