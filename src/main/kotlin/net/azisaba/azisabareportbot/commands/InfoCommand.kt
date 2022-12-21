package net.azisaba.azisabareportbot.commands

import dev.kord.common.Color
import dev.kord.common.Locale
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder
import dev.kord.rest.builder.interaction.number
import dev.kord.rest.builder.message.modify.embed
import net.azisaba.azisabareportbot.data.ReportData
import net.azisaba.azisabareportbot.data.ReportFlags
import net.azisaba.azisabareportbot.util.Util
import net.azisaba.azisabareportbot.util.Util.optLong
import java.util.Date

object InfoCommand : CommandHandler {
    override suspend fun canProcess(interaction: ApplicationCommandInteraction): Boolean = true

    override suspend fun handle0(interaction: ApplicationCommandInteraction) {
        val defer = interaction.deferPublicResponse()
        val reportId = interaction.optLong("id")!!
        val report = ReportData.getById(reportId)
        if (report == null) {
            defer.respond { content = "レポート#${reportId}が存在しません。" }
            return
        }
        val reporter = Util.getPlayerName(report.reporterId).toString()
        val reported = Util.getPlayerName(report.reportedId).toString()
        val namedFlags = mutableListOf<String>()
        for (field in ReportFlags::class.java.fields) {
            if (field.type != Int::class.java) continue
            if (report.flags.and(field.get(null) as Int) != 0) {
                namedFlags.add(field.name)
            }
        }
        defer.respond {
            embed {
                title = "レポート #${reportId}"
                color = Color(0x00FF00)

                field("通報したプレイヤー") { reporter }
                field("通報されたプレイヤー") { reported }
                field("理由") { report.reason }
                field("フラグ") { namedFlags.joinToString(", ") }
                field("コメント(公開)") { report.publicComment.toString() }
                field("コメント(非公開)") { report.comment.toString() }
                field("通報日時") { Date(report.createdAt).toString() }
                field("更新日時") { Date(report.updatedAt).toString() }
            }
        }
    }

    override fun register(builder: GlobalMultiApplicationCommandBuilder) {
        builder.input("info", "Show the report info") {
            description(Locale.JAPANESE, "レポートの情報を表示します。")

            number("id", "Report #ID") {
                required = true
                description(Locale.JAPANESE, "レポート#ID")
            }
        }
    }
}
