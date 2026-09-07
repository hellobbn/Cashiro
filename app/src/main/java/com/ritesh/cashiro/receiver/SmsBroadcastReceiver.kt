package com.ritesh.cashiro.receiver

/** Constants only. SMS ingest was removed; notification code still uses these IDs. */
object SmsBroadcastReceiver {
    const val ACTION_EDIT_TRANSACTION = "com.pennywiseai.tracker.ACTION_EDIT_TRANSACTION"
    const val EXTRA_TRANSACTION_ID = "transaction_id"
    const val CHANNEL_ID = "transaction_notifications"
    const val CHANNEL_NAME = "Transaction Notifications"
}
