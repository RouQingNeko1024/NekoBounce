/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.ui.client.clickgui.style.styles

import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.ui.client.clickgui.Panel
import io.qzz.nekobounce.ui.client.clickgui.style.Style
import io.qzz.nekobounce.ui.font.Fonts
import io.qzz.nekobounce.utils.render.RenderUtils
import io.qzz.nekobounce.utils.render.RenderUtils.drawRect
import io.qzz.nekobounce.utils.render.RenderUtils.drawBorderedRect
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.MathHelper
import java.awt.Color

object NekoStyle : Style() {

    private val panelWidth = 400
    private val panelHeight = 350
    private var nekoPanelX = 50
    private var nekoPanelY = 50
    private var dragging = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    private val categoryHeight = 30
    private val moduleWidth = 150
    private val settingsWidth = panelWidth - moduleWidth

    private var selectedCategory: Category = Category.COMBAT
    private var selectedModule: Module? = null

    private var moduleScrollOffset = 0
    private var settingsScrollOffset = 0
    private var isModuleScrolling = false
    private var isSettingsScrolling = false

    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        // 处理面板拖动
        handleDragging(mouseX, mouseY)

        // 绘制主面板背景
        drawBorderedRect(
            nekoPanelX.toFloat(), 
            nekoPanelY.toFloat(), 
            (nekoPanelX + panelWidth).toFloat(), 
            (nekoPanelY + panelHeight).toFloat(), 
            2f, 
            Color(80, 80, 80).rgb, 
            Color(40, 40, 40).rgb
        )

        // 绘制标题栏
        drawRect(nekoPanelX, nekoPanelY, nekoPanelX + panelWidth, nekoPanelY + 30, Color(60, 60, 60).rgb)
        
        // 绘制"NekoBounce"标题
        Fonts.fontSemibold40.drawString("NekoBounce", nekoPanelX + 10, nekoPanelY + 8, Color(255, 150, 200).rgb)

        // 绘制分类区域（顶部）
        drawCategorySection(mouseX, mouseY)
        
        // 绘制模块区域（左侧）
        drawModuleSection(mouseX, mouseY)
        
        // 绘制设置区域（右侧）
        drawSettingsSection(mouseX, mouseY)
    }

    private fun handleDragging(mouseX: Int, mouseY: Int) {
        val scaledRes = ScaledResolution(mc)
        val maxX = scaledRes.scaledWidth - panelWidth
        val maxY = scaledRes.scaledHeight - panelHeight

        if (dragging) {
            nekoPanelX = (mouseX - dragOffsetX).coerceIn(0, maxX)
            nekoPanelY = (mouseY - dragOffsetY).coerceIn(0, maxY)
        }

        // 检查是否点击了标题栏
        val inTitleBar = mouseX in nekoPanelX..nekoPanelX + panelWidth && mouseY in nekoPanelY..nekoPanelY + 30
        if (mc.gameSettings.keyBindAttack.isKeyDown && inTitleBar && !dragging) {
            dragging = true
            dragOffsetX = mouseX - nekoPanelX
            dragOffsetY = mouseY - nekoPanelY
        }

        if (!mc.gameSettings.keyBindAttack.isKeyDown) {
            dragging = false
        }
    }

    private fun drawCategorySection(mouseX: Int, mouseY: Int) {
        val categoryY = nekoPanelY + 30
        val categories = Category.values()
        val itemWidth = panelWidth / categories.size
        
        var xOffset = 0
        
        for (category in categories) {
            val categoryX = nekoPanelX + xOffset
            val isSelected = category == selectedCategory
            val isHovered = mouseX in categoryX..categoryX + itemWidth && mouseY in categoryY..categoryY + categoryHeight

            // 绘制分类项背景
            if (isSelected) {
                drawRect(categoryX, categoryY, categoryX + itemWidth, categoryY + categoryHeight, Color(70, 70, 70).rgb)
            } else if (isHovered) {
                drawRect(categoryX, categoryY, categoryX + itemWidth, categoryY + categoryHeight, Color(60, 60, 60).rgb)
            } else {
                drawRect(categoryX, categoryY, categoryX + itemWidth, categoryY + categoryHeight, Color(50, 50, 50).rgb)
            }

            // 绘制分类分隔线
            if (xOffset > 0) {
                drawRect(categoryX, categoryY, categoryX + 1, categoryY + categoryHeight, Color(40, 40, 40).rgb)
            }

            // 绘制分类文本
            val text = category.displayName
            val textWidth = Fonts.fontRegular35.getStringWidth(text)
            val textX = categoryX + (itemWidth - textWidth) / 2
            val textY = categoryY + (categoryHeight - Fonts.fontRegular35.FONT_HEIGHT) / 2
            Fonts.fontRegular35.drawString(text, textX, textY, if (isSelected) Color(255, 150, 200).rgb else Color.WHITE.rgb)

            // 点击处理
            if (isHovered && mc.gameSettings.keyBindAttack.isKeyDown) {
                selectedCategory = category
                selectedModule = null
                moduleScrollOffset = 0
                settingsScrollOffset = 0
            }

            xOffset += itemWidth
        }
    }

    private fun drawModuleSection(mouseX: Int, mouseY: Int) {
        val moduleX = nekoPanelX
        val moduleY = nekoPanelY + 30 + categoryHeight
        val moduleHeight = panelHeight - 30 - categoryHeight

        // 模块区域背景
        drawRect(moduleX, moduleY, moduleX + moduleWidth, moduleY + moduleHeight, Color(45, 45, 45).rgb)

        // 获取当前分类下的模块
        val modules = getModulesByCategory(selectedCategory)
        val itemHeight = 25
        var yOffset = -moduleScrollOffset

        // 绘制模块标题
        Fonts.fontSemibold40.drawString("Modules", moduleX + 10, moduleY + 5, Color(255, 150, 200).rgb)
        yOffset += 25

        for (module in modules) {
            val itemY = moduleY + yOffset
            val isSelected = module == selectedModule
            val isHovered = mouseX in moduleX..moduleX + moduleWidth && mouseY in itemY..itemY + itemHeight
            val isVisible = itemY + itemHeight > moduleY && itemY < moduleY + moduleHeight

            if (!isVisible) {
                yOffset += itemHeight
                continue
            }

            // 绘制模块项
            if (isSelected) {
                drawRect(moduleX, itemY, moduleX + moduleWidth, itemY + itemHeight, Color(65, 65, 65).rgb)
            } else if (isHovered) {
                drawRect(moduleX, itemY, moduleX + moduleWidth, itemY + itemHeight, Color(55, 55, 55).rgb)
            }

            // 绘制模块状态指示器
            val stateColor = if (module.state) Color(0, 255, 100).rgb else Color(255, 50, 50).rgb
            drawRect(moduleX + 5, itemY + 5, moduleX + 7, itemY + itemHeight - 5, stateColor)

            // 绘制模块名称
            Fonts.fontRegular35.drawString(module.name, moduleX + 15, itemY + 8, 
                if (isSelected) Color(255, 150, 200).rgb else Color.WHITE.rgb)

            // 绘制模块标签（如果有）
            val tag = module.tag
            if (tag != null && tag.isNotEmpty()) {
                val tagWidth = Fonts.fontRegular35.getStringWidth(tag)
                Fonts.fontRegular35.drawString(tag, moduleX + moduleWidth - tagWidth - 5, itemY + 8, Color(150, 150, 150).rgb)
            }

            // 点击处理
            if (isHovered && mc.gameSettings.keyBindAttack.isKeyDown) {
                if (mouseX < moduleX + moduleWidth - 20) {
                    selectedModule = module
                    settingsScrollOffset = 0
                } else {
                    // 点击右侧区域切换模块状态
                    module.toggle()
                }
            }

            yOffset += itemHeight
        }

        // 绘制滚动条
        val totalHeight = modules.size * itemHeight + 25
        if (totalHeight > moduleHeight) {
            val scrollBarHeight = MathHelper.clamp_float((moduleHeight * moduleHeight / totalHeight.toFloat()), 15f, moduleHeight.toFloat())
            val scrollBarY = moduleY + (moduleScrollOffset.toFloat() / totalHeight * moduleHeight)
            drawRect(moduleX + moduleWidth - 4, scrollBarY.toInt(), moduleX + moduleWidth, (scrollBarY + scrollBarHeight).toInt(), Color(100, 100, 100).rgb)
            
            // 处理滚动条拖动
            if (mouseX in moduleX + moduleWidth - 4..moduleX + moduleWidth && mouseY in scrollBarY.toInt()..(scrollBarY + scrollBarHeight).toInt()) {
                if (mc.gameSettings.keyBindAttack.isKeyDown) {
                    isModuleScrolling = true
                }
            }
            
            if (isModuleScrolling) {
                val scrollPercent = ((mouseY - moduleY).toFloat() / moduleHeight)
                moduleScrollOffset = (scrollPercent * totalHeight).toInt().coerceIn(0, totalHeight - moduleHeight)
                
                if (!mc.gameSettings.keyBindAttack.isKeyDown) {
                    isModuleScrolling = false
                }
            }
        }

        // 处理鼠标滚轮滚动
        if (mouseX in moduleX..moduleX + moduleWidth && mouseY in moduleY..moduleY + moduleHeight) {
            val wheel = if (mc.gameSettings.keyBindForward.isKeyDown) 1 else if (mc.gameSettings.keyBindBack.isKeyDown) -1 else 0
            if (wheel != 0) {
                moduleScrollOffset = (moduleScrollOffset - wheel * 20).coerceIn(0, (totalHeight - moduleHeight).coerceAtLeast(0))
            }
        }
    }

    private fun drawSettingsSection(mouseX: Int, mouseY: Int) {
        val settingsX = nekoPanelX + moduleWidth
        val settingsY = nekoPanelY + 30 + categoryHeight
        val settingsHeight = panelHeight - 30 - categoryHeight

        // 设置区域背景
        drawRect(settingsX, settingsY, settingsX + settingsWidth, settingsY + settingsHeight, Color(40, 40, 40).rgb)

        // 如果没有选中模块，显示提示
        if (selectedModule == null) {
            val text = "Select a module"
            val textWidth = Fonts.fontSemibold40.getStringWidth(text)
            val textX = settingsX + (settingsWidth - textWidth) / 2
            val textY = settingsY + settingsHeight / 2 - 10
            Fonts.fontSemibold40.drawString(text, textX, textY, Color.GRAY.rgb)
            return
        }

        // 绘制模块详细设置
        val module = selectedModule!!
        var yOffset = -settingsScrollOffset + 10

        // 绘制模块标题
        val moduleName = module.name + (if (module.state) " [ON]" else " [OFF]")
        Fonts.fontSemibold40.drawString("Settings", settingsX + 10, settingsY + yOffset, Color(255, 150, 200).rgb)
        yOffset += 30
        
        Fonts.fontSemibold40.drawString(moduleName, settingsX + 10, settingsY + yOffset, 
            if (module.state) Color(0, 255, 100).rgb else Color(255, 150, 200).rgb)
        yOffset += 30

        // 使用反射获取模块的字段
        try {
            val fields = getAllFields(module)
            for (field in fields) {
                if (yOffset > settingsHeight - 50) break
                
                try {
                    field.isAccessible = true
                    val value = field.get(module)
                    val fieldName = field.name
                    
                    // 跳过一些内部字段
                    if (shouldSkipField(fieldName)) continue
                    
                    // 根据字段类型绘制对应的设置
                    when (value) {
                        is Boolean -> {
                            if (yOffset + 25 < settingsHeight) {
                                drawBooleanSetting(settingsX, settingsY + yOffset, settingsWidth, fieldName, value, module, field, mouseX, mouseY)
                                yOffset += 25
                            }
                        }
                        is Int -> {
                            if (yOffset + 30 < settingsHeight) {
                                drawIntSetting(settingsX, settingsY + yOffset, settingsWidth, fieldName, value, module, field, mouseX, mouseY)
                                yOffset += 30
                            }
                        }
                        is Float -> {
                            if (yOffset + 35 < settingsHeight) {
                                drawFloatSetting(settingsX, settingsY + yOffset, settingsWidth, fieldName, value, module, field, mouseX, mouseY)
                                yOffset += 35
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 忽略无法访问的字段
                    continue
                }
            }
        } catch (e: Exception) {
            // 如果反射失败，显示错误信息
            val text = "Error loading settings"
            Fonts.fontRegular35.drawString(text, settingsX + 10, settingsY + yOffset, Color(255, 50, 50).rgb)
            yOffset += 20
        }

        // 绘制滚动条（如果需要）
        val totalContentHeight = yOffset + settingsScrollOffset
        if (totalContentHeight > settingsHeight) {
            val scrollBarHeight = MathHelper.clamp_float((settingsHeight * settingsHeight / totalContentHeight.toFloat()), 15f, settingsHeight.toFloat())
            val scrollBarY = settingsY + (settingsScrollOffset.toFloat() / totalContentHeight * settingsHeight)
            drawRect(settingsX + settingsWidth - 4, scrollBarY.toInt(), settingsX + settingsWidth, (scrollBarY + scrollBarHeight).toInt(), Color(100, 100, 100).rgb)
            
            // 处理滚动条拖动
            if (mouseX in settingsX + settingsWidth - 4..settingsX + settingsWidth && mouseY in scrollBarY.toInt()..(scrollBarY + scrollBarHeight).toInt()) {
                if (mc.gameSettings.keyBindAttack.isKeyDown) {
                    isSettingsScrolling = true
                }
            }
            
            if (isSettingsScrolling) {
                val scrollPercent = ((mouseY - settingsY).toFloat() / settingsHeight)
                settingsScrollOffset = (scrollPercent * totalContentHeight).toInt().coerceIn(0, totalContentHeight - settingsHeight)
                
                if (!mc.gameSettings.keyBindAttack.isKeyDown) {
                    isSettingsScrolling = false
                }
            }
        }

        // 处理鼠标滚轮滚动
        if (mouseX in settingsX..settingsX + settingsWidth && mouseY in settingsY..settingsY + settingsHeight) {
            val wheel = if (mc.gameSettings.keyBindForward.isKeyDown) 1 else if (mc.gameSettings.keyBindBack.isKeyDown) -1 else 0
            if (wheel != 0) {
                settingsScrollOffset = (settingsScrollOffset - wheel * 20).coerceIn(0, (totalContentHeight - settingsHeight).coerceAtLeast(0))
            }
        }
    }

    private fun getAllFields(obj: Any): List<java.lang.reflect.Field> {
        val fields = mutableListOf<java.lang.reflect.Field>()
        var clazz: Class<*>? = obj.javaClass
        
        while (clazz != null && clazz != Any::class.java) {
            fields.addAll(clazz.declaredFields)
            clazz = clazz.superclass
        }
        
        return fields
    }

    private fun shouldSkipField(fieldName: String): Boolean {
        return fieldName.startsWith("$") || 
               fieldName.contains("INSTANCE") || 
               fieldName == "tag" || 
               fieldName == "name" || 
               fieldName == "category" ||
               fieldName == "state" || 
               fieldName == "keyBind" || 
               fieldName == "array" ||
               fieldName.contains("handler") ||
               fieldName.contains("on") ||
               fieldName.contains("event") ||
               fieldName.contains("timer")
    }

    private fun getModulesByCategory(category: Category): List<Module> {
        val modules = mutableListOf<Module>()
        
        try {
            // 尝试获取模块管理器
            val moduleManagerClass = Class.forName("io.qzz.nekobounce.features.module.ModuleManager")
            val instanceField = moduleManagerClass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            val moduleManager = instanceField.get(null)
            
            // 获取modules列表
            val modulesField = moduleManagerClass.getDeclaredField("modules")
            modulesField.isAccessible = true
            val allModules = modulesField.get(moduleManager) as? Collection<Module>
            
            if (allModules != null) {
                for (module in allModules) {
                    if (module.category == category) {
                        modules.add(module)
                    }
                }
            }
        } catch (e: Exception) {
            // 备用方法：尝试直接访问Module的伴生对象
            try {
                val moduleClass = Module::class.java
                val modulesField = moduleClass.getDeclaredField("modules")
                modulesField.isAccessible = true
                val allModules = modulesField.get(null) as? Collection<Module>
                
                if (allModules != null) {
                    for (module in allModules) {
                        if (module.category == category) {
                            modules.add(module)
                        }
                    }
                }
            } catch (e2: Exception) {
                // 如果都失败了，返回空列表
                return emptyList()
            }
        }
        
        // 按名称排序
        return modules.sortedBy { it.name }
    }

    private fun drawBooleanSetting(x: Int, y: Int, width: Int, name: String, value: Boolean, module: Module, field: java.lang.reflect.Field, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX in x..x + width && mouseY in y..y + 20
        
        // 绘制设置项背景
        if (isHovered) {
            drawRect(x, y, x + width, y + 20, Color(60, 60, 60).rgb)
        }

        // 绘制设置项名称
        Fonts.fontRegular35.drawString(name, x + 10, y + 5, Color.WHITE.rgb)

        // 绘制开关
        val toggleWidth = 40
        val toggleHeight = 15
        val toggleX = x + width - toggleWidth - 10
        val toggleY = y + 2
        
        // 开关背景
        drawRect(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, 
            if (value) Color(0, 100, 0).rgb else Color(100, 0, 0).rgb)
        
        // 开关滑块
        val sliderX = if (value) toggleX + toggleWidth - toggleHeight else toggleX
        drawRect(sliderX, toggleY, sliderX + toggleHeight, toggleY + toggleHeight, 
            if (value) Color(0, 255, 100).rgb else Color(255, 50, 50).rgb)

        // 点击处理
        if (isHovered && mc.gameSettings.keyBindAttack.isKeyDown) {
            try {
                field.set(module, !value)
            } catch (e: Exception) {
                // 忽略设置失败的情况
            }
        }
    }

    private fun drawIntSetting(x: Int, y: Int, width: Int, name: String, value: Int, module: Module, field: java.lang.reflect.Field, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX in x..x + width && mouseY in y..y + 30
        
        // 绘制设置项背景
        if (isHovered) {
            drawRect(x, y, x + width, y + 30, Color(60, 60, 60).rgb)
        }

        // 绘制设置项名称和值
        Fonts.fontRegular35.drawString(name, x + 10, y + 5, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString(value.toString(), x + width - Fonts.fontRegular35.getStringWidth(value.toString()) - 10, y + 5, Color(200, 200, 200).rgb)

        // 绘制滑块背景
        val sliderWidth = width - 20
        val sliderX = x + 10
        val sliderY = y + 25
        
        // 获取最小值和最大值（这里简化处理，实际应该从字段注解获取）
        val minValue = 0
        val maxValue = 100
        
        // 绘制滑块
        val normalizedValue = ((value - minValue).toFloat() / (maxValue - minValue)).coerceIn(0f, 1f)
        val sliderPos = normalizedValue * sliderWidth
        drawRect(sliderX, sliderY - 2, sliderX + sliderWidth, sliderY + 2, Color(100, 100, 100).rgb)
        drawRect(sliderX + sliderPos.toInt() - 3, sliderY - 5, sliderX + sliderPos.toInt() + 3, sliderY + 5, Color(255, 150, 200).rgb)

        // 点击处理（简化，实际应该支持拖动滑块）
        if (isHovered && mc.gameSettings.keyBindAttack.isKeyDown) {
            val clickPos = (mouseX - sliderX).coerceIn(0, sliderWidth)
            val newValue = minValue + (clickPos.toFloat() / sliderWidth * (maxValue - minValue)).toInt()
            try {
                field.set(module, newValue)
            } catch (e: Exception) {
                // 忽略设置失败的情况
            }
        }
    }

    private fun drawFloatSetting(x: Int, y: Int, width: Int, name: String, value: Float, module: Module, field: java.lang.reflect.Field, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX in x..x + width && mouseY in y..y + 35
        
        // 绘制设置项背景
        if (isHovered) {
            drawRect(x, y, x + width, y + 35, Color(60, 60, 60).rgb)
        }

        // 绘制设置项名称和值
        Fonts.fontRegular35.drawString(name, x + 10, y + 5, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString(String.format("%.2f", value), x + width - Fonts.fontRegular35.getStringWidth(String.format("%.2f", value)) - 10, y + 5, Color(200, 200, 200).rgb)

        // 绘制滑块背景
        val sliderWidth = width - 20
        val sliderX = x + 10
        val sliderY = y + 30
        
        // 获取最小值和最大值（这里简化处理，实际应该从字段注解获取）
        val minValue = 0f
        val maxValue = 1f
        
        // 绘制滑块
        val normalizedValue = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
        val sliderPos = normalizedValue * sliderWidth
        drawRect(sliderX, sliderY - 2, sliderX + sliderWidth, sliderY + 2, Color(100, 100, 100).rgb)
        drawRect(sliderX + sliderPos.toInt() - 3, sliderY - 5, sliderX + sliderPos.toInt() + 3, sliderY + 5, Color(255, 150, 200).rgb)

        // 点击处理（简化，实际应该支持拖动滑块）
        if (isHovered && mc.gameSettings.keyBindAttack.isKeyDown) {
            val clickPos = (mouseX - sliderX).coerceIn(0, sliderWidth)
            val newValue = minValue + (clickPos.toFloat() / sliderWidth * (maxValue - minValue))
            try {
                field.set(module, newValue)
            } catch (e: Exception) {
                // 忽略设置失败的情况
            }
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: io.qzz.nekobounce.ui.client.clickgui.elements.ButtonElement) {
        // NekoStyle不使用传统按钮元素
    }

    override fun drawModuleElementAndClick(
        mouseX: Int, mouseY: Int, moduleElement: io.qzz.nekobounce.ui.client.clickgui.elements.ModuleElement, mouseButton: Int?
    ): Boolean {
        // NekoStyle使用自定义绘制，不需要这个方法
        return false
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        // 使用默认悬停文本绘制
        super.drawHoverText(mouseX, mouseY, text)
    }
}