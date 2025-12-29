/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module

import net.minecraft.util.ResourceLocation

enum class Category(val displayName: String) {

    COMBAT("Combat"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    WORLD("World"),
    MISC("Misc"),
    EXPLOIT("Exploit"),
    NEKO("Neko"),
    SKID("Skid"),
    CUSTOMTAG("Customtag"),
    FUN("Fun");

    val iconResourceLocation = ResourceLocation("liquidbounce/tabgui/${name.lowercase()}.png")

}
