/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.utils.login

import io.qzz.nekobounce.utils.io.HttpClient
import io.qzz.nekobounce.utils.io.get
import io.qzz.nekobounce.utils.io.jsonBody

object UserUtils {

    private val uuidCache = hashMapOf<String, String>()
    private val usernameCache = hashMapOf<String, String>()

    /**
     * Check if token is valid
     *
     * Exam
     * 7a7c4193280a4060971f1e73be3d9bdb
     * 89371141db4f4ec485d68d1f63d01eec
     */
    fun isValidTokenOffline(token: String) = token.length >= 32

    fun getUsername(uuid: String): String? {
        uuidCache[uuid]?.let { return it }

        return HttpClient.get(
            "https://api.minecraftservices.com/minecraft/profile/lookup/$uuid"
        ).jsonBody<Profile>()?.name?.also {
            usernameCache[uuid] = it
        }
    }

    /**
     * Get UUID of username
     */
    fun getUUID(username: String): String? {
        usernameCache[username]?.let { return it }

        return HttpClient.get(
            "https://api.minecraftservices.com/minecraft/profile/lookup/name/$username"
        ).jsonBody<Profile>()?.id?.also {
            usernameCache[username] = it
        }
    }
}

private class Profile(val id: String, val name: String)