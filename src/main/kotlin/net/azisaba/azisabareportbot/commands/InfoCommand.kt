package net.azisaba.azisabareportbot.commands

import dev.kord.common.Color
import dev.kord.common.Locale
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder
import dev.kord.rest.builder.interaction.number
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import net.azisaba.azisabareportbot.data.ReportData
import net.azisaba.azisabareportbot.data.ReportFlags
import net.azisaba.azisabareportbot.util.Util
import net.azisaba.azisabareportbot.util.Util.optLong
import net.azisaba.azisabareportbot.util.Util.optString
import net.azisaba.azisabareportbot.util.Util.toNamedFlags
import java.util.Date
import java.util.UUID

object InfoCommand : CommandHandler {
    private const val REPORTS_PER_PAGE = 10

    override suspend fun canProcess(interaction: ApplicationCommandInteraction): Boolean = true

    override suspend fun handle0(interaction: ApplicationCommandInteraction) {
        val reportId = interaction.optLong("id")
        val reporterName = interaction.optString("reporter_name")
        val reportedName = interaction.optString("reported_name")
        val page = interaction.optLong("page") ?: 0
        val defer = interaction.deferPublicResponse()
        if (reportId != null) {
            getReportInfo(defer, reportId)
        } else if (reporterName != null) {
            val uuid = Util.getPlayerId(reporterName)
            if (uuid == null) {
                defer.respond { content = "プレイヤー「$reporterName」は見つかりませんでした。" }
                return
            }
            getPlayerInfo(ReportData.getByReporterPlayer(uuid), page, "reporter:$uuid") { defer.respond(it) }
        } else if (reportedName != null) {
            val uuid = Util.getPlayerId(reportedName)
            if (uuid == null) {
                defer.respond { content = "プレイヤー「$reportedName」は見つかりませんでした。" }
                return
            }
            getPlayerInfo(ReportData.getByReportedPlayer(uuid), page, "reported:$uuid") { defer.respond(it) }
        }
    }

    private suspend fun getReportInfo(defer: DeferredPublicMessageInteractionResponseBehavior, reportId: Long) {
        val report = ReportData.getById(reportId)
        if (report == null) {
            defer.respond { content = "レポート#${reportId}が存在しません。" }
            return
        }
        val reporter = Util.getPlayerName(report.reporterId).toString()
        val reported = Util.getPlayerName(report.reportedId).toString()
        defer.respond {
            embed {
                title = "レポート #${reportId}"
                color = Color(0x00FF00)

                field("通報したプレイヤー") { reporter }
                field("通報されたプレイヤー") { reported }
                field("理由") { report.reason }
                field("フラグ") { report.flags.toNamedFlags<ReportFlags>().joinToString(", ") }
                field("コメント(公開)") { report.publicComment.toString() }
                field("コメント(非公開)") { report.comment.toString() }
                field("通報日時") { Date(report.createdAt).toString() }
                field("更新日時") { Date(report.updatedAt).toString() }
            }
        }
    }

    private suspend fun getPlayerInfo(
        reports: List<ReportData>,
        page: Long,
        customIdKey: String,
        create: suspend (builder: MessageCreateBuilder.() -> Unit) -> Unit = {},
        edit: suspend (builder: MessageModifyBuilder.() -> Unit) -> Unit = {},
    ) {
        if (reports.isEmpty()) {
            create { content = "レポートがありません。" }
            edit { content = "レポートがありません。" }
            return
        }
        val maxPage = ((reports.size - 1) / REPORTS_PER_PAGE).toLong()
        val currentPage = page.coerceIn(0, maxPage)
        val start = (currentPage * REPORTS_PER_PAGE).toInt()
        val end = ((currentPage + 1) * REPORTS_PER_PAGE).toInt()
        val reportList = reports.subList(start, end.coerceAtMost(reports.size))
        val hasNextPage = currentPage < maxPage
        val hasPrevPage = currentPage > 0
        val embed = EmbedBuilder().apply {
            title = "レポート一覧"

            val sb = StringBuilder()
            for (report in reportList) {
                val namedFlags = report.flags.toNamedFlags<ReportFlags>().joinToString(", ")
                val nineHours = 9 * 60 * 60 // timezone thing
                val createdAt = report.createdAt / 1000L + nineHours
                val updatedAt = report.updatedAt / 1000L + nineHours
                sb.append("#${report.id} [$namedFlags] (C: <t:$createdAt:d> <t:$createdAt:t>, U: <t:$updatedAt:d> <t:$updatedAt:t>)").append("\n")
                sb.append("> ${report.reason}").append("\n\n")
            }
            description = sb.toString().trimEnd('\n')
        }
        val actionRow = if (hasPrevPage || hasNextPage) {
            ActionRowBuilder().apply {
                if (hasPrevPage) {
                    interactionButton(ButtonStyle.Primary, "info:$customIdKey:${currentPage - 1}") {
                        emoji = DiscordPartialEmoji(name = "\u25c0\ufe0f")
                    }
                }
                if (hasNextPage) {
                    interactionButton(ButtonStyle.Primary, "info:$customIdKey:${currentPage + 1}") {
                        emoji = DiscordPartialEmoji(name = "\u25b6\ufe0f")
                    }
                }
            }
        } else null
        create {
            embeds.add(embed)
            actionRow?.let { components.add(it) }
        }
        edit {
            embeds = mutableListOf(embed)
            actionRow?.let { components = mutableListOf(it) }
        }
    }

    suspend fun handleButtonInteraction(interaction: ButtonInteraction) {
        val args = interaction.componentId.split(":")
        if (args[1] == "reporter") {
            val uuid = UUID.fromString(args[2])
            val page = args[3].toLong()
            val reports = ReportData.getByReporterPlayer(uuid)
            getPlayerInfo(reports, page, "reporter:$uuid", create = { interaction.updatePublicMessage(it) })
        } else if (args[1] == "reported") {
            val uuid = UUID.fromString(args[2])
            val page = args[3].toLong()
            val reports = ReportData.getByReportedPlayer(uuid)
            getPlayerInfo(reports, page, "reported:$uuid", create = { interaction.updatePublicMessage(it) })
        }
    }

    override fun register(builder: GlobalMultiApplicationCommandBuilder) {
        builder.input("info", "Show the report info") {
            description(Locale.JAPANESE, "レポートの情報を表示します。")

            number("id", "Report #ID") {
                description(Locale.JAPANESE, "レポート#ID")
            }

            string("reporter_name", "Reporter player") {
                description(Locale.JAPANESE, "レポートしたプレイヤー")
            }

            string("reported_name", "Reported player") {
                description(Locale.JAPANESE, "レポートされたプレイヤー")
            }

            number("page", "Page") {
                description(Locale.JAPANESE, "ページ")
                minValue = 0.0
            }
        }
    }
}
