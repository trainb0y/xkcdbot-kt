package io.github.trainb0y.xkcdbot.extensions


import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.applyComponents
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.components.publicButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.edit
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import kotlin.random.Random

class XKCDExtension : Extension() {
	override val name = "xkcd"
	private val comicNames = mutableMapOf<String, Int>()

	data class XKCD(
		val num: Int,
		val title: String,
		val alt: String,
		val img: String
	) {
		fun getEmbed(builder: EmbedBuilder) = builder.apply {
			this.title = this@XKCD.title
			this.description = this@XKCD.alt
			this.image = this@XKCD.img
			this.footer {
				text = "xkcd #${this@XKCD.num}"
			}
		}
	}

	private fun getXKCD(url: String): XKCD {
		val doc =
			try {
				Jsoup.connect(url).get()
			} catch (e: HttpStatusException) {
				null
			}

		val comic = doc?.select("#comic img")?.first()

		return XKCD(
			doc?.select("meta")?.get(3)?.attr("content")?.split("/")?.run { this[this.size - 2].toInt() } ?: -1,
			comic?.attr("alt") ?: "no comic title found",
			comic?.attr("title") ?: "no alt text found",
			comic?.let { "https:" + comic.attr("src") } ?: "https://imgs.xkcd.com/comics/not_available.png"
		)
	}
	private fun getXKCD(num: Int) = getXKCD("https://xkcd.com/$num")


	suspend fun xkcdInteractiveMessage(comic: XKCD, message: MessageBehavior) {
		var currentNum = comic.num
		suspend fun applyNew(message: MessageBehavior) {
			message.edit {
				this.embeds?.clear()
				this.content = null
				this.embed {
					getXKCD(currentNum).getEmbed(this)
				}
				this.components?.clear()
				this.applyComponents(components {
					publicButton {
						label = "Previous"
						action {
							currentNum--
							applyNew(message)
						}
					}
					publicButton {
						label = "\uD83C\uDFB2"
						action {
							currentNum = Random.nextInt(1, getXKCD("https://xkcd.com/").num)
							applyNew(message)
						}
					}
					publicButton {
						label = "Next"
						action {
							currentNum++
							applyNew(message)
						}
					}
					linkButton {
						label = "xkcd.com"
						url = "https://xkcd.com/${currentNum}"
					}
					linkButton {
						label = "explain"
						url = "https://www.explainxkcd.com/${currentNum}"
					}
				}
				)
			}
		}
		applyNew(message)
	}

	private fun updateNames() {
		comicNames.clear()
		val doc = Jsoup.connect("https://xkcd.com/archive/").get()
		val links = doc.select("a")
		for (link in links) {
			val name = link.text().lowercase()
			val num = try {
				link.attr("href").split("/")[1].toInt()
			} catch (e: NumberFormatException) {
				continue
			}
			comicNames[name] = num
		}
	}


	override suspend fun setup() {
		updateNames()

		publicSlashCommand {
			name = "xkcd"
			description = "XKCD related commands"

			publicSubCommand {
				name = "latest"
				description = "Gets the latest XKCD comic"
				action {
					val xkcd = getXKCD("https://xkcd.com")
					val message = respond{embed{xkcd.getEmbed()}}.message
					xkcdInteractiveMessage(xkcd, message)
				}
			}

			publicSubCommand {
				name = "random"
				description = "Get a random XKCD"
				action {
					val xkcd = getXKCD(Random.nextInt(1, getXKCD("https://xkcd.com/").num))
					val message = respond { embed{ xkcd.getEmbed(this)}}.message
					xkcdInteractiveMessage(xkcd, message)
				}
			}

			publicSubCommand(::RangeCommandArgs) {
				name = "range"
				description = "Gets a range of XKCD comics"
				action {
					if (kotlin.math.abs(arguments.last - arguments.first) > 10) {
						respond {
							content = "Cannot get more than 10 comics at once!"
						}
						return@action
					}
					for (num in arguments.first..arguments.last){
						val xkcd = getXKCD(num)
						val message = respond {embed{xkcd.getEmbed(this)} }.message
						if (arguments.buttons == true) xkcdInteractiveMessage(xkcd, message)
					}
				}
			}

			publicSubCommand(::SingleCommandArgs) {
				name = "get"
				description = "Get a specific XKCD comic"
				action {
					val xkcd = getXKCD(arguments.num)
					val message = respond { embed { xkcd.getEmbed(this) } }.message
					if (arguments.buttons == true) xkcdInteractiveMessage(xkcd, message)
				}
			}

			publicSubCommand(::LookupCommandArgs) {
				name = "lookup"
				description = "Get a comic by its name"
				action {
					val xkcd = getXKCD(comicNames[arguments.name.lowercase()] ?: -1)
					val message = respond { embed { xkcd.getEmbed(this) } }.message
					if (arguments.buttons == true) xkcdInteractiveMessage(xkcd, message)
				}
			}

			ephemeralSubCommand {
				name = "update"
				description = "Force update the comic name to id map"
				action {
					updateNames()
					respond { content = "Updated" }
				}
			}
		}
	}

	inner class RangeCommandArgs : Arguments() {
		val first by int {
			name = "first"
			description = "The first comic to get"
		}
		val last by int {
			name = "last"
			description = "The last comic to get"
		}
		val buttons by optionalBoolean {
			name = "buttons"
			description = "Whether to show navigation buttons"
		}
	}

	inner class SingleCommandArgs : Arguments() {
		val num by int {
			name = "num"
			description = "The comic to get"
		}
		val buttons by optionalBoolean {
			name = "buttons"
			description = "Whether to show navigation buttons"
		}
	}

	inner class LookupCommandArgs : Arguments() {
		val name by string {
			name = "name"
			description = "The name of the comic to get"
		}
		val buttons by optionalBoolean {
			name = "buttons"
			description = "Whether to show navigation buttons"
		}
	}
}
