/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.ItemStack
import java.awt.Color

object CSGOHUD : Module("CSGOHUD", Category.RENDER) {

    private val showArmor by boolean("ShowArmor", true)
    private val showHealth by boolean("ShowHealth", true)
    private val showHunger by boolean("ShowHunger", true)
    private val showInventory by boolean("ShowInventory", true)
    private val showHotbar by boolean("ShowHotbar", true)
    
    private val hudScale by float("HUDScale", 1.0f, 0.5f..2.0f)
    private val inventoryScale by float("InventoryScale", 1.0f, 0.5f..2.0f)
    private val inventoryOffsetY by int("InventoryOffsetY", 0, -200..200)
    
    private val healthColorRed by int("HealthRed", 255, 0..255)
    private val healthColorGreen by int("HealthGreen", 0, 0..255)
    private val healthColorBlue by int("HealthBlue", 0, 0..255)
    
    private val armorColorRed by int("ArmorRed", 0, 0..255)
    private val armorColorGreen by int("ArmorGreen", 100, 0..255)
    private val armorColorBlue by int("ArmorBlue", 255, 0..255)
    
    private val hungerColorRed by int("HungerRed", 255, 0..255)
    private val hungerColorGreen by int("HungerGreen", 255, 0..255)
    private val hungerColorBlue by int("HungerBlue", 0, 0..255)
    
    private val textColorRed by int("TextRed", 255, 0..255)
    private val textColorGreen by int("TextGreen", 255, 0..255)
    private val textColorBlue by int("TextBlue", 255, 0..255)
    
    private val highlightColorRed by int("HighlightRed", 255, 0..255)
    private val highlightColorGreen by int("HighlightGreen", 255, 0..255)
    private val highlightColorBlue by int("HighlightBlue", 255, 0..255)
    private val highlightAlpha by int("HighlightAlpha", 100, 0..255)

    private val separatorText by boolean("SeparatorText", true)
    private val separatorColorRed by int("SeparatorRed", 255, 0..255)
    private val separatorColorGreen by int("SeparatorGreen", 255, 0..255)
    private val separatorColorBlue by int("SeparatorBlue", 255, 0..255)

    val onRender2D = handler<Render2DEvent> { event ->
        val sr = ScaledResolution(mc)
        val width = sr.scaledWidth
        val height = sr.scaledHeight
        
        GlStateManager.pushMatrix()
        GlStateManager.scale(hudScale, hudScale, 1.0f)
        
        val scaledWidth = (width / hudScale).toInt()
        val scaledHeight = (height / hudScale).toInt()
        
        // Draw health and armor/hunger info
        if (mc.thePlayer != null) {
            val health = mc.thePlayer.health.toInt()
            val armor = mc.thePlayer.totalArmorValue
            val hunger = mc.thePlayer.foodStats.foodLevel
            
            val healthColor = Color(healthColorRed, healthColorGreen, healthColorBlue)
            val armorColor = Color(armorColorRed, armorColorGreen, armorColorBlue)
            val hungerColor = Color(hungerColorRed, hungerColorGreen, hungerColorBlue)
            val textColor = Color(textColorRed, textColorGreen, textColorBlue)
            val separatorColor = Color(separatorColorRed, separatorColorGreen, separatorColorBlue)
            
            val yPos = scaledHeight - 25f
            
            // Health text
            if (showHealth) {
                val healthText = health.toString()
                mc.fontRendererObj.drawString(
                    healthText,
                    scaledWidth / 2f - 70f - mc.fontRendererObj.getStringWidth(healthText) / 2f,
                    yPos,
                    healthColor.rgb,
                    false
                )
            }
            
            // Separator (-----------)
            if (separatorText) {
                val separator = "-----------"
                mc.fontRendererObj.drawString(
                    separator,
                    scaledWidth / 2f - mc.fontRendererObj.getStringWidth(separator) / 2f,
                    yPos,
                    separatorColor.rgb,
                    false
                )
            }
            
            // Armor/Hunger text
            if (showArmor || showHunger) {
                val armorHungerText = if (showArmor && showHunger) {
                    "$armor/$hunger"
                } else if (showArmor) {
                    armor.toString()
                } else {
                    hunger.toString()
                }
                
                val armorHungerColor = if (showArmor && showHunger) {
                    // Use armor color for armor part and hunger color for hunger part
                    // For simplicity, we'll use text color for now
                    textColor
                } else if (showArmor) {
                    armorColor
                } else {
                    hungerColor
                }
                
                mc.fontRendererObj.drawString(
                    armorHungerText,
                    scaledWidth / 2f + 70f - mc.fontRendererObj.getStringWidth(armorHungerText) / 2f,
                    yPos,
                    armorHungerColor.rgb,
                    false
                )
            }
        }
        
        GlStateManager.popMatrix()
        
        // Draw inventory on right side
        if (showInventory && mc.thePlayer != null && mc.thePlayer.inventory != null) {
            GlStateManager.pushMatrix()
            GlStateManager.scale(inventoryScale, inventoryScale, 1.0f)
            
            val invScaledWidth = (width / inventoryScale).toInt()
            val invScaledHeight = (height / inventoryScale).toInt()
            
            // Apply vertical offset
            val startX = invScaledWidth - 24
            val baseY = invScaledHeight / 2 - 90
            val startY = baseY + inventoryOffsetY
            
            // Draw hotbar selection highlight
            if (showHotbar) {
                val currentItemIndex = mc.thePlayer.inventory.currentItem
                val highlightColor = Color(
                    highlightColorRed, 
                    highlightColorGreen, 
                    highlightColorBlue, 
                    highlightAlpha
                )
                
                RenderUtils.drawRect(
                    startX.toFloat(),
                    startY + currentItemIndex * 20.toFloat(),
                    startX + 20f,
                    startY + currentItemIndex * 20f + 20f,
                    highlightColor.rgb
                )
            }
            
            for (i in 0 until 9) {
                val stack = mc.thePlayer.inventory.mainInventory[i]
                val x = startX
                val y = startY + i * 20
                
                // Draw background (semi-transparent black)
                RenderUtils.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    x + 20f,
                    y + 20f,
                    Color(0, 0, 0, 150).rgb
                )
                
                // Draw item
                if (stack != null && stack.item != null) {
                    RenderHelper.enableGUIStandardItemLighting()
                    GlStateManager.pushMatrix()
                    GlStateManager.translate(x + 2f, y + 2f, 0f)
                    GlStateManager.scale(1.0f, 1.0f, 1.0f)
                    mc.renderItem.renderItemIntoGUI(stack, 0, 0)
                    GlStateManager.popMatrix()
                    RenderHelper.disableStandardItemLighting()
                    
                    // Draw count
                    if (stack.stackSize > 1) {
                        val countText = stack.stackSize.toString()
                        mc.fontRendererObj.drawString(
                            countText,
                            x + 20f - mc.fontRendererObj.getStringWidth(countText) - 1f,
                            y + 20f - 8f,
                            Color(textColorRed, textColorGreen, textColorBlue).rgb,
                            false
                        )
                    }
                }
                
                // Draw slot number (small text in top-left)
                val slotText = "${i + 1}"
                mc.fontRendererObj.drawString(
                    slotText,
                    x + 2f,
                    y + 2f,
                    Color(textColorRed, textColorGreen, textColorBlue).rgb,
                    false
                )
            }
            
            GlStateManager.popMatrix()
        }
    }

    // Mixin will check this variable to hide vanilla HUD
    var hideVanillaHUD = false
    // Mixin will check this to hide crosshair
    var hideCrosshair = true

    override fun onEnable() {
        hideVanillaHUD = true
        hideCrosshair = true
    }

    override fun onDisable() {
        hideVanillaHUD = false
        hideCrosshair = false
    }

    override val tag: String
        get() = "CSGO Style"

    // Java-friendly static accessors for mixins - use different method names to avoid conflict
    @JvmStatic
    fun isCSGOHUDEnabled(): Boolean = state
    
    @JvmStatic
    fun shouldHideVanillaHUD(): Boolean = hideVanillaHUD
    
    @JvmStatic
    fun shouldHideCrosshair(): Boolean = hideCrosshair
}