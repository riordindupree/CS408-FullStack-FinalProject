#!/usr/bin/env bash
#
# Initialize a fresh application database from src/main/resources/sql_queries.
# The schema is always applied before the remaining SQL files.

set -euo pipefail

APP_DIR="/opt/99-steps-and-7-falls"
SQL_DIR="${APP_DIR}/src/main/resources/sql_queries"
DB_NAME="game_db"
INIT_MARKER="deploy_sql_initialized"

: "${SPRING_DATASOURCE_USERNAME:?database username is required}"
: "${SPRING_DATASOURCE_PASSWORD:?database password is required}"

export PGHOST="127.0.0.1"
export PGPORT="5432"
export PGDATABASE="$DB_NAME"
export PGUSER="$SPRING_DATASOURCE_USERNAME"
export PGPASSWORD="$SPRING_DATASOURCE_PASSWORD"

if [ ! -s "${SQL_DIR}/schema.sql" ]; then
    printf 'ERROR: required schema file is missing or empty: %s/schema.sql\n' "$SQL_DIR" >&2
    exit 1
fi

initialized="$(psql --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1 \
    --command "SELECT to_regclass('public.${INIT_MARKER}') IS NOT NULL")"
if [ "$initialized" = "t" ]; then
    printf 'Database bootstrap already completed; leaving existing data unchanged.\n'
    exit 0
fi

printf 'Initializing PostgreSQL database from %s\n' "$SQL_DIR"

# Run the schema before seed data, regardless of the names of other SQL files.
psql --no-psqlrc --set=ON_ERROR_STOP=1 --file="${SQL_DIR}/schema.sql"

for sql_file in "${SQL_DIR}"/*.sql; do
    [ -f "$sql_file" ] || continue
    [ "$sql_file" = "${SQL_DIR}/schema.sql" ] && continue
    printf 'Applying %s\n' "$(basename "$sql_file")"
    psql --no-psqlrc --set=ON_ERROR_STOP=1 --file="$sql_file"
done

psql --no-psqlrc --set=ON_ERROR_STOP=1 \
    --command "CREATE TABLE public.${INIT_MARKER} (initialized_at TIMESTAMPTZ NOT NULL DEFAULT now())"

printf 'Database bootstrap completed successfully.\n'
