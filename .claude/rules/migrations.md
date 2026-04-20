---
paths:
  - "src/main/resources/db/migration/**/*.sql"
---

# Rules for Flyway migrations

Loaded only when working on files under `src/main/resources/db/migration/`.

## Naming

`V<N>__<description_in_snake_case>.sql` — two underscores between the version and description.

Good: `V2__add_loyalty_points_column.sql`
Bad: `V2_loyalty.sql`, `V2-add-loyalty.sql`, `migration_2.sql`

## Append-only

Migrations are immutable once committed. **Never** edit `V<N>__*.sql` after it's been run against any environment. If you need to change a past migration's effect, add a new migration that alters the schema forward.

## Portable SQL

The test suite runs against H2; production runs against Postgres. Write SQL that works on both:

- `UUID` — both support natively.
- `SMALLINT` (not `TINYINT` — Postgres doesn't have TINYINT).
- `VARCHAR(n)` — both.
- `TIMESTAMP(6) WITH TIME ZONE` — both.
- `NUMERIC(p, s)` — both.

Avoid:
- H2's `ENUM(...)` type — use `VARCHAR` with a `CHECK` constraint if you need enum semantics.
- Vendor-specific functions.

## Hibernate's role

Hibernate is configured `ddl-auto=none`. It does not create, validate, or modify the schema.
Everything goes through Flyway. Don't try to make Hibernate pick up a change by annotating —
write the migration.

## What a new schema change looks like

For a new column on an existing table:

```sql
-- V2__add_loyalty_points.sql
ALTER TABLE customer ADD COLUMN loyalty_points INTEGER NOT NULL DEFAULT 0;
```

For a new table:

```sql
-- V3__add_notifications_table.sql
CREATE TABLE notifications_sent (
    id UUID NOT NULL PRIMARY KEY,
    customer_id UUID NOT NULL,
    sent_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
```

## Don't

- Don't modify an existing `V<N>__*.sql`. Add a new one.
- Don't use vendor-specific SQL that breaks H2 or Postgres.
- Don't drop tables / columns without considering in-flight deployments.
- Don't rely on `ddl-auto` to catch up — it's disabled on purpose.
