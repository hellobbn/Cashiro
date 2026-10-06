package com.ritesh.cashiro.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A synced record that changed here and has not been sent yet: one row per record, the latest
 * change wins. Written only by the triggers of SyncTriggers; see docs/sync.md.
 */
@Entity(tableName = "sync_outbox", primaryKeys = ["table_name", "sync_id"])
data class SyncOutboxEntity(
    @ColumnInfo(name = "table_name") val tableName: String,
    @ColumnInfo(name = "sync_id") val syncId: String,
    /** SyncTriggers.OP_UPSERT or SyncTriggers.OP_DELETE */
    @ColumnInfo(name = "op") val op: String,
    /** Epoch millis */
    @ColumnInfo(name = "queued_at") val queuedAt: Long
)

/**
 * The single row (id 1) that switches change capture. While [applyingRemote] is set, the sync
 * triggers neither stamp rows nor queue them: it is set, inside one transaction, while changes
 * from another device are written. [capturing] is the triggers' own guard against capturing the
 * row update they make.
 */
@Entity(tableName = "sync_control")
data class SyncControlEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = 1,
    @ColumnInfo(name = "applying_remote", defaultValue = "0") val applyingRemote: Boolean = false,
    @ColumnInfo(name = "capturing", defaultValue = "0") val capturing: Boolean = false
)
