package io.github.trainb0y.xkcdbot.extensions


import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import kotlin.random.Random

class XKCDExtension: Extension() {
	override val name = "xkcd"
	val names = mutableMapOf<String, Int>()

	data class XKCD(
		val num: Int,
		val title: String,
		val alt: String,
		val img: String
	) {
		fun getEmbed(builder: FollowupMessageCreateBuilder) =
			builder.embed {
				this.title = this@XKCD.title
				this.description = this@XKCD.alt
				this.image = img
				this.footer {
					text = "xkcd #$num"
				}
			}
	}

	private fun getXKCD(url: String): XKCD {
		val doc =
			try {
				Jsoup.connect(url).get()
			}
			catch (e: HttpStatusException) {null}

		val comic = doc?.select("#comic img")?.first()

		val comicPermLink = doc?.select("meta")?.get(3)?.attr("content")
		val comicNum = comicPermLink?.split("/")?.run {
			this[this.size - 2].toInt()
		}

		return XKCD(
			comicNum ?: -1,
			comic?.attr("alt") ?: "no comic title found",
			comic?.attr("title") ?: "no alt text found",
			comic?.let {"https:" + comic.attr("src") } ?: "https://imgs.xkcd.com/comics/not_available.png"
		)
	}

	private fun getXKCD(num: Int): XKCD {
		return getXKCD("https://xkcd.com/$num")
	}

	private fun updateNames() {
		println("running")
		names.clear()
		val doc = Jsoup.connect("https://xkcd.com/archive/").get()
		val links = doc.select("a")
		for (link in links) {
			val name = link.text().lowercase()
			val num = try {
				link.attr("href").split("/")[1].toInt()
			}
			catch (e: NumberFormatException) {
				continue
			}
			names[name] = num
		}
	}


	override suspend fun setup() {
		updateNames()

		publicSlashCommand() {
			name = "xkcd"
			description = "XKCD related commands"

			publicSubCommand {
				name = "latest"
				description = "Gets the latest XKCD comic"
				action {respond { getXKCD("https://xkcd.com/").getEmbed(this) } }
			}

			publicSubCommand {
				name = "random"
				description = "Get a random XKCD"
				action {respond {
					getXKCD(Random.nextInt(1, getXKCD("https://xkcd.com/").num)).getEmbed(this)
				}}
			}

			publicSubCommand(::RangeCommandArgs) {
				name = "range"
				description = "Gets a range of XKCD comics"
				action {
					for (num in arguments.first..arguments.last) {
						respond { getXKCD(num).getEmbed(this) }
					}
				}
			}

			publicSubCommand(::SingleCommandArgs) {
				name = "get"
				description = "Get a specific XKCD comic"
				action {respond { getXKCD(arguments.num).getEmbed(this) } }
			}

			publicSubCommand(::LookupCommandArgs) {
				name = "lookup"
				description = "Get a comic by its name"
				action {respond { getXKCD(names[arguments.name.lowercase()] ?: -1).getEmbed(this) } }
			}

			ephemeralSubCommand() {
				name = "update"
				description = "Force update the comic name to id map"
				action {
					updateNames()
					respond {
						content = "Updated"
					}
				}
			}
		}
	}

	inner class RangeCommandArgs: Arguments() {
		val first by int {
			name = "first"
			description = "The first comic to get"
		}
		val last by int {
			name = "last"
			description = "The last comic to get"
		}
	}

	inner class SingleCommandArgs: Arguments() {
		val num by int {
			name = "num"
			description = "The comic to get"
		}
	}

	inner class LookupCommandArgs: Arguments() {
		val name by string {
			name = "name"
			description = "The name of the comic to get"
		}
	}
}
