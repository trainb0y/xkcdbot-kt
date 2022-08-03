package io.github.trainb0y.xkcdbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import io.github.trainb0y.xkcdbot.extensions.XKCDExtension
import mu.KotlinLogging

const val version = "1.0"
val logger = KotlinLogging.logger {}
suspend fun main() {
	logger.info { "Starting XKCD Bot v$version" }
	val bot = ExtensibleBot(env("TOKEN")) {
		applicationCommands {
			enabled = true
			defaultGuild = Snowflake(env("TEST_SERVER").toLong())
		}
		presence {
			status = PresenceStatus.Online
			watching(env("STATUS"))
		}
		extensions {
			add(::XKCDExtension)
		}
	}
	bot.start()
}
