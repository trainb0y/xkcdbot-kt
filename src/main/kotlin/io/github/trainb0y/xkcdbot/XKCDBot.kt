package io.github.trainb0y.xkcdbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import io.github.trainb0y.xkcdbot.extensions.XKCDExtension


suspend fun main() {
    val bot = ExtensibleBot(env("TOKEN")) {
        applicationCommands {
            enabled = true
            defaultGuild = Snowflake(env("TEST_SERVER").toLong())
        }
        presence {
            status = PresenceStatus.Online
            // Can be changed later
            watching(env("STATUS"))
        }
        extensions {
            add(::XKCDExtension)
        }
    }
    bot.start()
}
