# Multi-device sync

Goal: the same ledger on several devices through Firebase Firestore. Each record is stored as
`users/{uid}/records/{table}_{syncId}`, with its payload encrypted on the device.

| Phase | What | State |
|---|---|---|
| 1 | Local groundwork: record identity, change capture, outbox | done (database version 71) |
| 2 | Push the outbox to Firestore; apply remote changes | not started |

Phase 1 adds no Firebase dependency, no network access and no UI.

## Synced tables

`accounts`, `account_currencies`, `account_balances`, `transactions`, `categories`,
`subcategories`, `cards`, `budgets`, `budget_category_limits`, `subscriptions`,
`lend_borrow_persons`, `lend_borrow_transactions`, `quick_templates`
(`SyncTriggers.TABLES`).

`exchange_rates` is not synced, because the rates are fetched, not entered.

## Columns

Each synced table has two extra columns:

| Column | Entity field | Meaning |
|---|---|---|
| `sync_id TEXT NOT NULL DEFAULT ''` | `syncId` | The record's identity on every device: 32 lowercase hex digits |
| `sync_updated_at INTEGER NOT NULL DEFAULT 0` | `syncUpdatedAt` | Epoch millis of the last local change |

Each table has a plain index `index_<table>_sync_id` on `sync_id`.

Two choices differ from the first sketch:

- **`sync_updated_at`, not `updated_at`.** Most of these tables already have an `updated_at`
  column, a `LocalDateTime` stored as text.
- **The index is not unique.** Uniqueness is kept by the triggers. App code makes new rows by
  copying saved entities (for example `latest.copy(id = 0, …)` in a balance calibration), and
  the copy carries the original's `sync_id`. With a unique index, such an insert would fail
  under ABORT and be dropped under IGNORE. Under REPLACE, the most dangerous case, it would
  delete the original row.

Migration 70→71 adds the columns and gives every existing row a new id and the current time. It
also creates the tables below and the triggers. Existing rows are not queued: the first sync must
upload everything anyway.

## Tables

- **`sync_outbox(table_name, sync_id, op, queued_at)`**: primary key `(table_name, sync_id)`.
  There is one row per changed record, and the latest change replaces earlier ones. `op` is
  `UPSERT` or `DELETE`. Entity: `SyncOutboxEntity`; DAO: `SyncDao`.
  - `pending()` reads the queue.
  - `markSent(entry)` removes an entry only while it is unchanged (same op and `queued_at`). A
    record that changed again while its upload was in flight stays queued.
- **`sync_control(id = 1, applying_remote, capturing)`**: a single row (`SyncControlEntity`).
  - `applying_remote = 1` pauses capture: see below.
  - `capturing` is set by the triggers while they update their own row, so that update does not
    capture itself.

## Triggers

`SyncTriggers` defines three triggers per table, named `sync_v1_<table>_insert|update|delete`.

| Trigger | What it does |
|---|---|
| After insert | Gives the row a new `sync_id` if it has none, or if another row already holds that id (a copy). Sets `sync_updated_at = max(now, incoming value)` and queues `UPSERT`. |
| After update | Keeps the old `sync_id`: an update that blanks it (an entity built afresh) or changes it gets the old id back, so a record's identity never changes. Sets `sync_updated_at = max(now, old + 1)` and queues `UPSERT`. |
| After delete | Queues `DELETE` with the old `sync_id`. Foreign-key cascades fire it too, for example a budget's limits or an account's currencies. |

Notes on how the triggers are written:

- **No `OR REPLACE` / `OR IGNORE` in trigger bodies.** In SQLite, the conflict policy of the
  statement that fired a trigger overrides the policy of statements inside it. An outbox entry is
  therefore replaced by a delete followed by an insert.
- **`recursive_triggers`.** `SyncTriggers.Callback.onOpen` turns this pragma on for the
  connection that writes. Without it, the rows that an `INSERT OR REPLACE` deletes would not
  queue their `DELETE`.
- **Where the triggers are created.** They are not part of Room's schema. `SyncTriggers.install`
  creates them with `CREATE TRIGGER IF NOT EXISTS`, and is called from:
  - migration 70→71;
  - `SyncTriggers.Callback.onCreate` (new installs, in-memory test databases);
  - `SyncTriggers.Callback.onOpen` (every open).
- **What `onOpen` also does.** It drops sync triggers of other versions and clears flags that a
  crash may have left set. It also gives an id to any row written while the triggers were
  missing.
- **Every builder needs the callback.** Every database builder, tests included, adds
  `SyncTriggers.Callback` before any callback that writes. `DatabaseModule` adds it before
  `DatabaseCallback`, so the seeded categories are captured.
- **Changing a trigger.** To change a trigger body, bump `SyncTriggers.VERSION`.

## Applying remote changes (phase 2)

Phase 2 writes changes from other devices in one Room transaction:

1. `setApplyingRemote(true)`.
2. Write each record verbatim, matched by `sync_id`, with its remote `sync_id` and
   `sync_updated_at`.
3. `setApplyingRemote(false)`.

While the flag is set, nothing is stamped or queued. That holds for any write in between, so the
flag must never stay set outside that transaction (`onOpen` clears it after a crash).

### Balances caveat

`account_balances` rows are written alongside transactions: `AddTransactionUseCase`,
`TransactionEditor`, the repository's delete and balance calibrations each write their own
balance rows (`balanceMoves`). They are synced as records like any other.

When phase 2 applies a remote transaction, it must copy the transaction and its balance rows
verbatim, with `applying_remote = 1`. It must never go through the use cases or re-run the balance
effects, or every balance would move twice.

Local row ids differ between devices. References between records therefore have to be carried by
`sync_id` and mapped back to local ids on apply. This covers `account_id`, `to_account_id`,
`transaction_id`, `budget_id`, `person_id` and `category_id`.

## Backups

Gson carries `syncId` / `syncUpdatedAt` in `backup.json` without extra code.

- **Old backups** have no such fields. `SyncIdDefaultsFactory` (Gson) and the `sanitize()`
  functions read a missing id as empty, and the insert trigger then gives the record a new one.
- **REPLACE_ALL** keeps the backup's ids.
  - Clearing the tables queues a `DELETE` for every old record.
  - Re-inserting queues an `UPSERT` for every restored one; a restored record with the same id
    replaces its `DELETE`.
  - The result is the restored state, ready to upload. Every restored record is stamped with the
    current time, so it wins against older remote copies.
- **MERGE** keeps the existing merge rules.
  - A transaction is also recognized by its `sync_id`, so an edit on another device (a new hash)
    updates the same record instead of adding a copy.
  - An updated record keeps its local `sync_id`.
  - A record the rules add whose `sync_id` is already taken locally gets a new id from the
    trigger, so a merge never fails on an id.
  - Accounts are still matched by name and last 4, because balance rows name their account.

## Phase 2

1. **Sign-in.** Firebase Auth gives the `uid`.
2. **Push.** Read `pending()` and encrypt each record's row. Write it to
   `users/{uid}/records/{table}_{syncId}` with its `sync_updated_at` (and a tombstone for
   `DELETE`), then `markSent`. The first sync after the migration, or after sign-in, uploads
   every row, not only the outbox.
3. **Pull.** Listen to remote changes newer than the last pull. Apply each record with
   `applying_remote = 1`, newest `sync_updated_at` winning, and map references by `sync_id`.
4. **Conflicts.** Last writer wins per record, by `sync_updated_at`. A local edit always moves
   the stamp past the record's previous one (`max(now, old + 1)`), even when the clock is behind.
