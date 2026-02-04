/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.setup

import com.formdev.flatlaf.themes.FlatMacLightLaf
import java.awt.BorderLayout
import java.awt.Desktop
import javax.swing.JEditorPane
import javax.swing.JFrame
import javax.swing.WindowConstants
import javax.swing.event.HyperlinkEvent

fun main() {
    FlatMacLightLaf.setup()

    // Setup instruction frame
    val frame = JFrame("NekoBounce | Installation")
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.layout = BorderLayout()
    frame.isResizable = false
    frame.isAlwaysOnTop = true

    // Add instruction as editor pane (uneditable)
    val editorPane = JEditorPane().apply {
        contentType = "text/html"
        text = """
            <html>
            <body style="font-family: Arial, sans-serif; padding: 20px;">
                <h1>NekoBounce Installation Instructions</h1>
                
                <h2>Requirements:</h2>
                <ul>
                    <li>Minecraft 1.8.9</li>
                    <li>Forge for Minecraft 1.8.9</li>
                    <li>Java 8 or higher</li>
                </ul>
                
                <h2>Installation Steps:</h2>
                <ol>
                    <li>Make sure you have Forge installed for Minecraft 1.8.9</li>
                    <li>Download the latest LiquidBounce b100 jar file</li>
                    <li>Place the jar file in your Minecraft mods folder</li>
                    <li>Launch Minecraft with Forge profile</li>
                    <li>Press RIGHT SHIFT to open the ClickGUI</li>
                </ol>
                
                <h2>Important Notes:</h2>
                <ul>
                    <li>This is a hacked client - use at your own risk</li>
                    <li>Some servers may ban for using this client</li>
                    <li>Always make backups of your worlds</li>
                </ul>
                
                <p>For more information, visit the 
                <a href="https://github.com/CCBlueX/LiquidBounce">GitHub repository</a></p>
                
                <p style="color: red; font-weight: bold;">
                    WARNING: This client is for educational purposes only!
                </p>
            </body>
            </html>
        """.trimIndent()
        isEditable = false
        addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                Desktop.getDesktop().browse(event.url.toURI())
            }
        }
    }

    frame.add(editorPane, BorderLayout.CENTER)

    // Pack frame
    frame.pack()

    // Set location to center of screen
    frame.setLocationRelativeTo(null)

    // Display instruction frame
    frame.isVisible = true
}