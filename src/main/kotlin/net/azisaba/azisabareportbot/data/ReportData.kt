package net.azisaba.azisabareportbot.data

import net.azisaba.azisabareportbot.util.Util
import java.sql.ResultSet
import java.util.UUID

data class ReportData(
    val id: Long,
    val reporterId: UUID,
    val reportedId: UUID,
    val reason: String,
    val flags: Int,
    val publicComment: String?,
    val comment: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        fun fromResultSet(rs: ResultSet) =
            ReportData(
                rs.getLong("id"),
                UUID.fromString(rs.getString("reporter_id")),
                UUID.fromString(rs.getString("reported_id")),
                rs.getString("reason"),
                rs.getInt("flags"),
                rs.getString("public_comment"),
                rs.getString("comment"),
                rs.getTimestamp("created_at").time,
                rs.getTimestamp("updated_at").time,
            )

        fun getById(id: Long) =
            Util.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM `reports` WHERE `id` = ?").use { ps ->
                    ps.setLong(1, id)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            fromResultSet(rs)
                        } else {
                            null
                        }
                    }
                }
            }
    }
}
