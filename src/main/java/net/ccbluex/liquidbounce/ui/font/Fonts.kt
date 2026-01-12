/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.file.FileManager.fontsDir
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.io.*
import net.ccbluex.liquidbounce.utils.io.Downloader
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.opengl.GL40
import java.awt.Font
import java.io.File
import java.io.IOException
import kotlin.system.measureTimeMillis

data class FontInfo(val name: String, val size: Int = -1, val isCustom: Boolean = false)

data class CustomFontInfo(val name: String, val fontFile: String, val fontSize: Int)

private val FONT_REGISTRY = LinkedHashMap<FontInfo, FontRenderer>()

object Fonts : MinecraftInstance {

    /**
     * Custom Fonts
     */
    private val configFile = File(fontsDir, "fonts.json")
    private var customFontInfoList: List<CustomFontInfo>
        get() = with(configFile) {
            if (exists()) {
                try {
                    // For old versions
                    readJson().asJsonArray.map {
                        it as JsonObject
                        val fontFile = it["fontFile"].asString
                        val fontSize = it["fontSize"].asInt
                        val name = if (it.has("name")) it["name"].asString else fontFile
                        CustomFontInfo(name, fontFile, fontSize)
                    }
                } catch (e: Exception) {
                    LOGGER.error("Failed to load fonts", e)
                    emptyList()
                }
            } else {
                createNewFile()
                writeText("[]") // empty list
                emptyList()
            }
        }
        set(value) = configFile.writeJson(value)

    val minecraftFontInfo = FontInfo(name = "Minecraft Font")
    val minecraftFont: FontRenderer by lazy {
        mc.fontRendererObj
    }

    lateinit var fontExtraBold35: GameFontRenderer
    lateinit var fontExtraBold40: GameFontRenderer
    lateinit var fontSemibold35: GameFontRenderer
    lateinit var fontSemibold40: GameFontRenderer
    lateinit var fontRegular40: GameFontRenderer
    lateinit var fontRegular45: GameFontRenderer
    lateinit var fontRegular35: GameFontRenderer
    lateinit var fontRegular30: GameFontRenderer
    lateinit var fontRegular180: GameFontRenderer
    lateinit var fontBold180: GameFontRenderer

    lateinit var fontGoogleSans18: GameFontRenderer
    lateinit var fontGoogleSans30: GameFontRenderer
    lateinit var fontGoogleSans35: GameFontRenderer
    lateinit var fontGoogleSans40: GameFontRenderer
    lateinit var fontGoogleSans45: GameFontRenderer

    lateinit var fontBorel45:  GameFontRenderer
    lateinit var fontBorel50:  GameFontRenderer
    lateinit var fontBorel55:  GameFontRenderer

    lateinit var fontAppleUI45:  GameFontRenderer
    lateinit var fontAppleUI50:  GameFontRenderer
    lateinit var fontAppleUI55:  GameFontRenderer

    // 新增字体 - 直接从资源路径加载
    lateinit var fontsiyuanback5: GameFontRenderer
    lateinit var fontsiyuanback10: GameFontRenderer
    lateinit var fontsiyuanback15: GameFontRenderer
    lateinit var fontsiyuanback16: GameFontRenderer
    lateinit var fontsiyuanback18: GameFontRenderer
    lateinit var fontsiyuanback20: GameFontRenderer
    lateinit var fontsiyuanback25: GameFontRenderer
    lateinit var fontsiyuanback26: GameFontRenderer
    lateinit var fontsiyuanback28: GameFontRenderer
    lateinit var fontsiyuanback30: GameFontRenderer
    lateinit var fontsiyuanback35: GameFontRenderer
    lateinit var fontsiyuanback40: GameFontRenderer
    lateinit var fontsiyuanback45: GameFontRenderer
    lateinit var fontsiyuanback50: GameFontRenderer
    lateinit var fontsiyuanback55: GameFontRenderer
    lateinit var fontsiyuanback65: GameFontRenderer
    lateinit var fontsiyuanback70: GameFontRenderer
    lateinit var fontsiyuanback85: GameFontRenderer
    lateinit var fontsiyuanback95: GameFontRenderer

    lateinit var fontAugs35: GameFontRenderer
    lateinit var fontAugs40: GameFontRenderer
    lateinit var fontAugs45: GameFontRenderer

    lateinit var fontFortalesia35: GameFontRenderer
    lateinit var fontFortalesia40: GameFontRenderer
    lateinit var fontFortalesia45: GameFontRenderer

    lateinit var fontProductSans35: GameFontRenderer
    lateinit var fontProductSans40: GameFontRenderer
    lateinit var fontProductSans45: GameFontRenderer

    private fun <T : FontRenderer> register(fontInfo: FontInfo, fontRenderer: T): T {
        FONT_REGISTRY[fontInfo] = fontRenderer
        return fontRenderer
    }

    fun registerCustomAWTFont(customFontInfo: CustomFontInfo, save: Boolean = true): GameFontRenderer? {
        val font = getFontFromFileOrNull(customFontInfo.fontFile, customFontInfo.fontSize) ?: return null

        val result = register(
            FontInfo(customFontInfo.name, customFontInfo.fontSize, isCustom = true),
            font.asGameFontRenderer()
        )

        if (save) {
            customFontInfoList += customFontInfo
        }

        return result
    }

    fun loadFonts() {
        LOGGER.info("Start to load fonts.")
        val time = measureTimeMillis {
            downloadFonts()
            LOGGER.info("Start to load fonts from file.")

            register(minecraftFontInfo, minecraftFont)

            fontRegular30 = register(
                FontInfo(name = "Outfit Regular", size = 30),
                getFontFromFile("Outfit-Regular.ttf", 30).asGameFontRenderer()
            )

            fontSemibold35 = register(
                FontInfo(name = "Outfit Semibold", size = 35),
                getFontFromFile("Outfit-Semibold.ttf", 35).asGameFontRenderer()
            )

            fontRegular35 = register(
                FontInfo(name = "Outfit Regular", size = 35),
                getFontFromFile("Outfit-Regular.ttf", 35).asGameFontRenderer()
            )

            fontRegular40 = register(
                FontInfo(name = "Outfit Regular", size = 40),
                getFontFromFile("Outfit-Regular.ttf", 40).asGameFontRenderer()
            )

            fontSemibold40 = register(
                FontInfo(name = "Outfit Semibold", size = 40),
                getFontFromFile("Outfit-Semibold.ttf", 40).asGameFontRenderer()
            )

            fontRegular45 = register(
                FontInfo(name = "Outfit Regular", size = 45),
                getFontFromFile("Outfit-Regular.ttf", 45).asGameFontRenderer()
            )

            fontSemibold40 = register(
                FontInfo(name = "Outfit Semibold", size = 40),
                getFontFromFile("Outfit-Semibold.ttf", 40).asGameFontRenderer()
            )

            fontExtraBold35 = register(
                FontInfo(name = "Outfit Extrabold", size = 35),
                getFontFromFile("Outfit-Extrabold.ttf", 35).asGameFontRenderer()
            )

            fontExtraBold40 = register(
                FontInfo(name = "Outfit Extrabold", size = 40),
                getFontFromFile("Outfit-Extrabold.ttf", 40).asGameFontRenderer()
            )

            fontBold180 = register(
                FontInfo(name = "Outfit Bold", size = 180),
                getFontFromFile("Outfit-Bold.ttf", 180).asGameFontRenderer()
            )
            fontRegular180 = register(
                FontInfo(name = "Outfit Regular", size = 180),
                getFontFromFile("Outfit-Regular.ttf", 180).asGameFontRenderer()
            )


            fontGoogleSans18 = register(
                FontInfo(name = "Google Sans", size = 18),
                getFontFromResource("ProductSans-Bold.ttf", 18).asGameFontRenderer()
            )

            fontGoogleSans30 = register(
                FontInfo(name = "Google Sans", size = 30),
                getFontFromResource("ProductSans-Bold.ttf", 30).asGameFontRenderer()
            )

            fontGoogleSans35 = register(
                FontInfo(name = "Google Sans", size = 35),
                getFontFromResource("ProductSans-Bold.ttf", 35).asGameFontRenderer()
            )

            fontGoogleSans40 = register(
                FontInfo(name = "Google Sans", size = 40),
                getFontFromResource("ProductSans-Bold.ttf", 40).asGameFontRenderer()
            )

            fontGoogleSans45 = register(
                FontInfo(name = "Google Sans", size = 45),
                getFontFromResource("ProductSans-Bold.ttf", 45).asGameFontRenderer()
            )


            fontBorel55 = register(
                FontInfo(name = "Borel", size = 55),
                getFontFromResource("Borel-Regular.ttf", 55).asGameFontRenderer()
            )

            fontBorel50 = register(
                FontInfo(name = "Borel", size = 50),
                getFontFromResource("Borel-Regular.ttf", 50).asGameFontRenderer()
            )

            fontBorel45 = register(
                FontInfo(name = "Borel", size = 45),
                getFontFromResource("Borel-Regular.ttf", 45).asGameFontRenderer()
            )

            fontAppleUI45 = register(
                FontInfo(name = "AppleUI", size = 45),
                getFontFromResource("Apple-UI.ttf", 45).asGameFontRenderer()
            )

            fontAppleUI50 = register(
                FontInfo(name = "AppleUI", size = 50),
                getFontFromResource("Apple-UI.ttf", 50).asGameFontRenderer()
            )

            fontAppleUI55 = register(
                FontInfo(name = "AppleUI", size = 55),
                getFontFromResource("Apple-UI.ttf", 55).asGameFontRenderer()
            )

            // 新增字体注册 - 直接从资源路径加载
fontsiyuanback5 = register(
    FontInfo(name = "siyuan", size = 5),
    getFontFromResource("siyuanback.ttf", 5).asGameFontRenderer()
)
fontsiyuanback10 = register(
    FontInfo(name = "siyuan", size = 10),
    getFontFromResource("siyuanback.ttf", 10).asGameFontRenderer()
)
fontsiyuanback15 = register(
    FontInfo(name = "siyuan", size = 15),
    getFontFromResource("siyuanback.ttf", 15).asGameFontRenderer()
)
fontsiyuanback16 = register(
    FontInfo(name = "siyuan", size = 16),
    getFontFromResource("siyuanback.ttf", 16).asGameFontRenderer()
)
fontsiyuanback18 = register(
    FontInfo(name = "siyuan", size = 18),
    getFontFromResource("siyuanback.ttf", 18).asGameFontRenderer()
)
fontsiyuanback20 = register(
    FontInfo(name = "siyuan", size = 20),
    getFontFromResource("siyuanback.ttf", 20).asGameFontRenderer()
)
fontsiyuanback25 = register(
    FontInfo(name = "siyuan", size = 25),
    getFontFromResource("siyuanback.ttf", 25).asGameFontRenderer()
)
fontsiyuanback26 = register(
    FontInfo(name = "siyuan", size = 26),
    getFontFromResource("siyuanback.ttf", 26).asGameFontRenderer()
)
fontsiyuanback28 = register(
    FontInfo(name = "siyuan", size = 28),
    getFontFromResource("siyuanback.ttf", 28).asGameFontRenderer()
)
fontsiyuanback30 = register(
    FontInfo(name = "siyuan", size = 30),
    getFontFromResource("siyuanback.ttf", 30).asGameFontRenderer()
)
fontsiyuanback35 = register(
    FontInfo(name = "siyuan", size = 35),
    getFontFromResource("siyuanback.ttf", 35).asGameFontRenderer()
)
fontsiyuanback40 = register(
    FontInfo(name = "siyuan", size = 40),
    getFontFromResource("siyuanback.ttf", 40).asGameFontRenderer()
)
fontsiyuanback45 = register(
    FontInfo(name = "siyuan", size = 45),
    getFontFromResource("siyuanback.ttf", 45).asGameFontRenderer()
)
fontsiyuanback50 = register(
    FontInfo(name = "siyuan", size = 50),
    getFontFromResource("siyuanback.ttf", 50).asGameFontRenderer()
)
fontsiyuanback55 = register(
    FontInfo(name = "siyuan", size = 55),
    getFontFromResource("siyuanback.ttf", 55).asGameFontRenderer()
)
fontsiyuanback65 = register(
    FontInfo(name = "siyuan", size = 65),
    getFontFromResource("siyuanback.ttf", 65).asGameFontRenderer()
)
fontsiyuanback70 = register(
    FontInfo(name = "siyuan", size = 70),
    getFontFromResource("siyuanback.ttf", 70).asGameFontRenderer()
)
fontsiyuanback85 = register(
    FontInfo(name = "siyuan", size = 85),
    getFontFromResource("siyuanback.ttf", 85).asGameFontRenderer()
)
fontsiyuanback95 = register(
    FontInfo(name = "siyuan", size = 95),
    getFontFromResource("siyuanback.ttf", 95).asGameFontRenderer()
)
//siyuan

            fontAugs35 = register(
                FontInfo(name = "Augs", size = 35),
                getFontFromResource("augs.ttf", 35).asGameFontRenderer()
            )

            fontAugs40 = register(
                FontInfo(name = "Augs", size = 40),
                getFontFromResource("augs.ttf", 40).asGameFontRenderer()
            )

            fontAugs45 = register(
                FontInfo(name = "Augs", size = 45),
                getFontFromResource("augs.ttf", 45).asGameFontRenderer()
            )

            fontFortalesia35 = register(
                FontInfo(name = "Fortalesia", size = 35),
                getFontFromResource("Fortalesia.ttf", 35).asGameFontRenderer()
            )

            fontFortalesia40 = register(
                FontInfo(name = "Fortalesia", size = 40),
                getFontFromResource("Fortalesia.ttf", 40).asGameFontRenderer()
            )

            fontFortalesia45 = register(
                FontInfo(name = "Fortalesia", size = 45),
                getFontFromResource("Fortalesia.ttf", 45).asGameFontRenderer()
            )

            fontProductSans35 = register(
                FontInfo(name = "ProductSans", size = 35),
                getFontFromResource("ProductSans-Bold.ttf", 35).asGameFontRenderer()
            )

            fontProductSans40 = register(
                FontInfo(name = "ProductSans", size = 40),
                getFontFromResource("ProductSans-Bold.ttf", 40).asGameFontRenderer()
            )

            fontProductSans45 = register(
                FontInfo(name = "ProductSans", size = 45),
                getFontFromResource("ProductSans-Bold.ttf", 45).asGameFontRenderer()
            )

            loadCustomFonts()
        }
        LOGGER.info("Loaded ${FONT_REGISTRY.size} fonts in ${time}ms")
    }

    private fun loadCustomFonts() {
        FONT_REGISTRY.keys.removeIf { it.isCustom }

        customFontInfoList.forEach {
            registerCustomAWTFont(it, save = false)
        }
    }

    fun downloadFonts() {
        fontsDir.mkdirs()
        val outputFile = File(fontsDir, "outfit.zip")
        if (!outputFile.exists()) {
            LOGGER.info("Downloading fonts...")
            val localResource = "/assets/minecraft/liquidbounce/fonts/outfit.zip"
            //Downloader.downloadWholeFile("$CLIENT_CLOUD/fonts/Outfit.zip", outputFile)
            runCatching {
                javaClass.getResourceAsStream(localResource)?.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                        LOGGER.info("Successfully copied local fonts")
                    }
                } ?: throw IOException("Local font resource not found")
            }
            LOGGER.info("Extracting fonts...")
            outputFile.extractZipTo(fontsDir)
        }
    }

    fun getFontRenderer(name: String, size: Int): FontRenderer {
        return FONT_REGISTRY.entries.firstOrNull { (fontInfo, _) ->
            fontInfo.size == size && fontInfo.name.equals(name, true)
        }?.value ?: minecraftFont
    }

    fun getFontDetails(fontRenderer: FontRenderer): FontInfo? {
        return FONT_REGISTRY.keys.firstOrNull { FONT_REGISTRY[it] == fontRenderer }
    }

    val fonts: List<FontRenderer>
        get() = FONT_REGISTRY.values.toList()

    val customFonts: Map<FontInfo, FontRenderer>
        get() = FONT_REGISTRY.filterKeys { it.isCustom }

    fun removeCustomFont(fontInfo: FontInfo): CustomFontInfo? {
        if (!fontInfo.isCustom) {
            return null
        }

        FONT_REGISTRY.remove(fontInfo)
        return customFontInfoList.firstOrNull {
            it.name == fontInfo.name && it.fontSize == fontInfo.size
        }?.also {
            customFontInfoList -= it
        }
    }

    private fun getFontFromFileOrNull(file: String, size: Int): Font? = try {
        File(fontsDir, file).inputStream().use { inputStream ->
            Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(Font.PLAIN, size.toFloat())
        }
    } catch (e: Exception) {
        LOGGER.warn("Exception during loading font[name=${file}, size=${size}]", e)
        null
    }
    
    private fun getFontFromResourceOrNull(file: String, size: Int): Font? = try {
        val resourcePath = "/assets/minecraft/liquidbounce/fonts/$file"
        val inputStream = Fonts::class.java.getResourceAsStream(resourcePath)
            ?: throw IOException("Font resource not found: $resourcePath")

        inputStream.use {
            Font.createFont(Font.TRUETYPE_FONT, it).deriveFont(Font.PLAIN, size.toFloat())
        }
    } catch (e: Exception) {
        LOGGER.warn("Exception during loading font from resource[name=${file}, size=${size}]", e)
        null
    }

    private fun getFontFromFile(file: String, size: Int): Font =
        getFontFromFileOrNull(file, size) ?: Font("default", Font.PLAIN, size)

    private fun getFontFromResource(file: String, size: Int): Font =
        getFontFromResourceOrNull(file, size) ?: Font("default", Font.PLAIN, size)

    private fun Font.asGameFontRenderer(): GameFontRenderer {
        return GameFontRenderer(this@asGameFontRenderer)
    }
    
    fun getFont(fontName: String, size: Int) =
        try {
            val inputStream = File(fontsDir, fontName).inputStream()
            var awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream)
            awtClientFont = awtClientFont.deriveFont(Font.PLAIN, size.toFloat())
            inputStream.close()
            awtClientFont
        } catch (e: Exception) {
            e.printStackTrace()
            Font("default", Font.PLAIN, size)
        }
}