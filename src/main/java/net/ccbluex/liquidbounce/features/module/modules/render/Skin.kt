package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.util.ResourceLocation

object Skin : Module("Skin", Category.RENDER) {
    
    // 皮肤资源位置
    private val customSkin = ResourceLocation("liquidbounce/skin.png")
    
    // 是否启用皮肤替换
    fun shouldReplaceSkin(): Boolean = state
    
    // 获取自定义皮肤资源位置
    fun getCustomSkin(): ResourceLocation = customSkin
    
    override val tag: String
        get() = "Custom Skin"
}