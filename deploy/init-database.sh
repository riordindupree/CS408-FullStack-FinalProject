#!/usr/bin/env bash
#
# Initialize the application database from src/main/resources/sql_queries.
# The schema and seed SQL run once by default. Pass --refresh-seeds to reload seed data.

set -euo pipefail

REFRESH_SEEDS=false
case "${1:-}" in
    "")
        ;;
    --refresh-seeds)
        REFRESH_SEEDS=true
        shift
        ;;
    --help|-h)
        printf 'Usage: %s [--refresh-seeds]\n' "$0"
        printf 'By default, apply schema and seed files only once. --refresh-seeds reapplies seed files.\n'
        exit 0
        ;;
    *)
        printf 'ERROR: unknown argument: %s\n' "$1" >&2
        printf 'Usage: %s [--refresh-seeds]\n' "$0" >&2
        exit 1
        ;;
esac
[ "$#" -eq 0 ] || {
    printf 'ERROR: unexpected extra argument: %s\n' "$1" >&2
    exit 1
}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
SQL_DIR="${APP_DIR}/src/main/resources/sql_queries"
DB_NAME="${PGDATABASE:-game_db}"
INIT_MARKER="deploy_sql_initialized"
FILE_MARKER="deploy_sql_files"

: "${SPRING_DATASOURCE_USERNAME:?database username is required}"
: "${SPRING_DATASOURCE_PASSWORD:?database password is required}"

export PGHOST="${PGHOST:-127.0.0.1}"
export PGPORT="${PGPORT:-5432}"
export PGDATABASE="$DB_NAME"
export PGUSER="$SPRING_DATASOURCE_USERNAME"
export PGPASSWORD="$SPRING_DATASOURCE_PASSWORD"
export PGCLIENTENCODING="UTF8"

if [ ! -s "${SQL_DIR}/schema.sql" ]; then
    printf 'ERROR: required schema file is missing or empty: %s/schema.sql\n' "$SQL_DIR" >&2
    exit 1
fi

psql --no-psqlrc --set=ON_ERROR_STOP=1 \
    --command "CREATE TABLE IF NOT EXISTS public.${FILE_MARKER} (file_name TEXT PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())"

printf 'Initializing PostgreSQL database from %s\n' "$SQL_DIR"

previously_initialized="$(psql --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1 \
    --command "SELECT to_regclass('public.${INIT_MARKER}') IS NOT NULL")"

mark_file_applied() {
    psql --no-psqlrc --set=ON_ERROR_STOP=1 --set=file_name="$1" <<'SQL'
INSERT INTO public.deploy_sql_files (file_name)
VALUES (:'file_name')
ON CONFLICT (file_name) DO NOTHING;
SQL
}

schema_applied="$(psql --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1 <<'SQL'
SELECT EXISTS (
    SELECT 1 FROM public.deploy_sql_files WHERE file_name = 'schema.sql'
);
SQL
)"
if [ "$schema_applied" != "t" ]; then
    schema_table_count="$(psql --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1 \
        --command "SELECT count(*) FROM unnest(ARRAY['player', 'run', 'item', 'monster', 'gained_item', 'gained_monster']) AS expected(name) WHERE to_regclass('public.' || expected.name) IS NOT NULL")"
    if [ "$schema_table_count" -eq 6 ]; then
        printf 'Schema tables already exist; recording schema.sql as applied and resuming.\n'
        mark_file_applied "schema.sql"
    elif [ "$schema_table_count" -ne 0 ]; then
        printf 'ERROR: database has a partial schema (%s of 6 required tables exist); refusing to rerun schema.sql over existing data.\n' "$schema_table_count" >&2
        exit 1
    else
        printf 'Applying schema.sql\n'
        psql --no-psqlrc --set=ON_ERROR_STOP=1 --single-transaction \
            --file="${SQL_DIR}/schema.sql"
        mark_file_applied "schema.sql"
    fi
else
    printf 'schema.sql already applied; skipping.\n'
fi

for sql_file in "${SQL_DIR}"/*.sql; do
    [ -f "$sql_file" ] || continue
    [ "$sql_file" = "${SQL_DIR}/schema.sql" ] && continue
    file_name="$(basename "$sql_file")"
    file_applied="$(psql --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1 \
        --set=file_name="$file_name" <<'SQL'
SELECT EXISTS (
    SELECT 1 FROM public.deploy_sql_files WHERE file_name = :'file_name'
);
SQL
)"
    if [ "$REFRESH_SEEDS" != "true" ] && [ "$file_applied" = "t" ]; then
        printf '%s already applied; skipping.\n' "$file_name"
        continue
    fi
    if [ "$REFRESH_SEEDS" != "true" ] && [ "$previously_initialized" = "t" ]; then
        printf 'Database was already initialized; recording %s as applied without replaying it.\n' "$file_name"
        mark_file_applied "$file_name"
        continue
    fi
    if [ "$REFRESH_SEEDS" = "true" ]; then
        printf 'Refreshing seed data from %s\n' "$file_name"
    else
        printf 'Applying %s\n' "$file_name"
    fi
    psql --no-psqlrc --set=ON_ERROR_STOP=1 --file="$sql_file"
    mark_file_applied "$file_name"
done

psql --no-psqlrc --set=ON_ERROR_STOP=1 \
    --command "CREATE TABLE IF NOT EXISTS public.${INIT_MARKER} (initialized_at TIMESTAMPTZ NOT NULL DEFAULT now())"

if [ "$REFRESH_SEEDS" = "true" ]; then
    printf 'Database schema verified and seed data refreshed successfully.\n'
else
    printf 'Database schema verified and unapplied SQL files processed successfully.\n'
fi
