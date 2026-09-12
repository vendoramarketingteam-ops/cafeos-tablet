# Phase 1 Data Model (unchanged — presentation-only feature)

This feature is a **UI re-skin**. It does **not** add, remove, or modify any data
entity, DAO, or business rule. All persistence logic remains owned solely by Room
DAOs (CaféOS Tablet Constitution III).

## Source of truth (read-only references)
- `app/src/main/java/com/cafeos/tablet/data/Entities.kt` — all tables: `Product`, `Category`, `Order`, `OrderItem`, `OrderOption`, `Payment`, `Ingredient`, `IngredientTransaction`, `Customer`, `LoyaltyVoucher`, `LoyaltySetting`, `Staff`, `Table`, `Station`, `Expense`, `OptionGroup`, `Option`, `SyncMeta`, plus internal join/recipe rows.
- `app/src/main/java/com/cafeos/tablet/data/CafeDao.kt` — all DAO queries.
- `app/src/main/java/com/cafeos/tablet/data/CafeDatabase.kt` — Room database definition.

## What the re-skin adds to data
- A **single** new UI-mode setting: `ui_mode` (FAST | GAMIFIED | CLASSIC), stored as a key in the existing `pebot_sync` SharedPreferences. This is presentation preference only — it does not affect any financial record and does not alter any entity schema. Per Constitution II ("Receipts are sequential and immutable"), no UI setting touches receipt numbering or finalised orders.
