package com.gokova.myanimelist.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_user_anime_mutations",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["anime_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["anime_id"], unique = true),
        Index(value = ["sync_state"]),
        Index(value = ["created_at"]),
    ],
)
data class PendingUserAnimeMutationEntity(
    @PrimaryKey
    @ColumnInfo(name = "anime_id")
    val animeId: Long,
    val status: String? = null,
    @ColumnInfo(name = "num_episodes_watched")
    val numEpisodesWatched: Int? = null,
    val score: Int? = null,
    @ColumnInfo(name = "is_rewatching")
    val isRewatching: Boolean? = null,
    @ColumnInfo(name = "baseline_status")
    val baselineStatus: String? = null,
    @ColumnInfo(name = "baseline_episodes")
    val baselineEpisodes: Int? = null,
    @ColumnInfo(name = "baseline_score")
    val baselineScore: Int? = null,
    @ColumnInfo(name = "baseline_is_rewatching")
    val baselineIsRewatching: Boolean? = null,
    @ColumnInfo(name = "remote_status")
    val remoteStatus: String? = null,
    @ColumnInfo(name = "remote_episodes")
    val remoteEpisodes: Int? = null,
    @ColumnInfo(name = "remote_score")
    val remoteScore: Int? = null,
    @ColumnInfo(name = "remote_is_rewatching")
    val remoteIsRewatching: Boolean? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_state")
    val syncState: String = SYNC_STATE_PENDING,
    @ColumnInfo(name = "sync_error_message")
    val syncErrorMessage: String? = null,
) {
    companion object {
        const val SYNC_STATE_PENDING = "PENDING"
        const val SYNC_STATE_SYNCING = "SYNCING"
        const val SYNC_STATE_FAILED = "FAILED"
        const val SYNC_STATE_CONFLICT = "CONFLICT"
        const val SYNC_STATE_AUTH_REQUIRED = "AUTH_REQUIRED"
    }
}
