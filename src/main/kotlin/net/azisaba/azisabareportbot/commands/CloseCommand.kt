package net.azisaba.azisabareportbot.commands

import dev.kord.common.Locale
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder
import dev.kord.rest.builder.interaction.number
import dev.kord.rest.builder.interaction.string
import net.azisaba.azisabareportbot.data.ReportData
import net.azisaba.azisabareportbot.data.ReportFlags
import net.azisaba.azisabareportbot.util.Util
import net.azisaba.azisabareportbot.util.Util.optLong
import net.azisaba.azisabareportbot.util.Util.optString

object CloseCommand : CommandHandler {
    override suspend fun canProcess(interaction: ApplicationCommandInteraction): Boolean = true

    override suspend fun handle0(interaction: ApplicationCommandInteraction) {
        val defer = interaction.deferPublicResponse()
        val reportId = interaction.optLong("report-id")!!
        val reason = interaction.optString("reason")!!
        val publicComment = interaction.optString("public-comment")
        val privateComment = interaction.optString("private-comment")
        val report = ReportData.getById(reportId)
        if (report == null) {
            defer.respond { content = "レポート#${reportId}が存在しません。" }
            return
        }
        if (report.flags.and(ReportFlags.CLOSED) != 0) {
            defer.respond { content = "レポート#${reportId}はすでに閉じられています。" }
            return
        }
        val additionalFlag = when (reason) {
            "resolved" -> ReportFlags.RESOLVED
            "invalid" -> ReportFlags.INVALID
            "not-enough-proof" -> ReportFlags.NEED_MORE_PROOF
            else -> 0
        }
        val newFlags = report.flags.and(ReportFlags.OPEN.inv()).or(ReportFlags.CLOSED).or(additionalFlag)
        if (privateComment == null) {
            Util.getConnection().use { conn ->
                conn.prepareStatement("UPDATE `reports` SET `flags` = ?, `public_comment` = ? WHERE `id` = ?")
                    .use { ps ->
                        ps.setInt(1, newFlags)
                        ps.setString(2, publicComment)
                        ps.setLong(3, reportId)
                        ps.executeUpdate()
                    }
            }
        } else {
            Util.getConnection().use { conn ->
                conn.prepareStatement("UPDATE `reports` SET `flags` = ?, `public_comment` = ?, `comment` = ? WHERE `id` = ?")
                    .use { ps ->
                        ps.setInt(1, newFlags)
                        ps.setString(2, publicComment)
                        ps.setString(3, privateComment)
                        ps.setLong(4, reportId)
                        ps.executeUpdate()
                    }
            }
        }
        defer.respond { content = "レポート#${reportId}を閉じました。" }
    }

    override fun register(builder: GlobalMultiApplicationCommandBuilder) {
        builder.input("close", "Close the report") {
            description(Locale.JAPANESE, "報告を閉じる")

            number("report-id", "Report #ID") {
                required = true
                description(Locale.JAPANESE, "レポート#ID")
            }

            string("reason", "Reason for closing the report") {
                required = true
                description(Locale.JAPANESE, "報告を閉じる理由 (必須)")

                choice("Invalid", "invalid") {
                    name(Locale.JAPANESE, "無効")
                }
                choice("Resolved", "resolved") {
                    name(Locale.JAPANESE, "解決")
                }
                choice("Not enough proof", "not-enough-proof") {
                    name(Locale.JAPANESE, "証拠不十分")
                }
            }

            string("public-comment", "Comment shown to the reporter") {
                description(Locale.JAPANESE, "報告者に表示されるコメント")
            }

            string("private-comment", "Comment shown to the staff") {
                description(Locale.JAPANESE, "スタッフに表示されるコメント")
            }
        }
    }
}
