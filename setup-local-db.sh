#!/usr/bin/env bash
#
# Configure a local PostgreSQL installation for this application.
#
# PostgreSQL must already be installed and running, and the admin role must
# have permission to create roles and databases.
# This creates the database, applies the SQL files (schema.sql first), and
# saves local credentials. After this completes, start the app with ./start.sh.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

DB_HOST="${PGHOST:-127.0.0.1}"
DB_PORT="${PGPORT:-5432}"
DB_NAME="${PGDATABASE:-game_db}"
DB_USER="${SPRING_DATASOURCE_USERNAME:-game_app}"
ADMIN_USER="${PGADMINUSER:-postgres}"
ENV_FILE="${PROJECT_DIR}/.env.local"

die() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    sed -n '2,8p' "$0" | sed 's/^# *//'
    exit 0
fi
[ "$#" -eq 0 ] || die "unknown argument: $1 (try --help)"

[ -n "$DB_NAME" ] && [ -n "$DB_USER" ] || die "database name and application role cannot be empty."
case "$DB_NAME" in
    *[!A-Za-z0-9_]*) die "database name may only contain letters, numbers, and underscores." ;;
esac
case "$DB_USER" in
    *[!A-Za-z0-9_]*) die "application role may only contain letters, numbers, and underscores." ;;
esac
case "$DB_PORT" in
    ''|*[!0-9]*) die "PGPORT must be a number." ;;
esac
[ "$DB_PORT" -ge 1 ] && [ "$DB_PORT" -le 65535 ] || die "PGPORT must be between 1 and 65535."

command -v psql >/dev/null 2>&1 \
    || die "PostgreSQL's psql command is required. Install PostgreSQL and add psql to PATH."
command -v openssl >/dev/null 2>&1 \
    || die "openssl is required to generate an application database password."

if [ -n "${SPRING_DATASOURCE_PASSWORD:-}" ]; then
    APP_PASSWORD="$SPRING_DATASOURCE_PASSWORD"
elif [ -f "$ENV_FILE" ]; then
    APP_PASSWORD="$(bash -c 'set -a; . "$1"; printf "%s" "${SPRING_DATASOURCE_PASSWORD:-}"' _ "$ENV_FILE")"
else
    APP_PASSWORD=""
fi
if [ -z "$APP_PASSWORD" ]; then
    APP_PASSWORD="$(openssl rand -hex 32)"
fi

if [ -n "${PGPASSWORD:-}" ]; then
    ADMIN_PASSWORD="$PGPASSWORD"
else
    [ -t 0 ] || die "PGPASSWORD is required when setup-local-db.sh runs non-interactively."
    read -r -s -p "PostgreSQL admin password for ${ADMIN_USER}: " ADMIN_PASSWORD
    printf '\n'
fi
[ -n "$ADMIN_PASSWORD" ] || die "PostgreSQL admin password cannot be empty."

export PGHOST="$DB_HOST"
export PGPORT="$DB_PORT"
export PGUSER="$ADMIN_USER"
export PGPASSWORD="$ADMIN_PASSWORD"
export PGDATABASE="postgres"

printf 'Checking PostgreSQL at %s:%s\n' "$DB_HOST" "$DB_PORT"
psql --no-psqlrc --set=ON_ERROR_STOP=1 --command "SELECT 1" >/dev/null \
    || die "could not connect as '$ADMIN_USER'. Check that PostgreSQL is running and the admin credentials are correct."

psql --no-psqlrc --set=ON_ERROR_STOP=1 \
    --set=app_user="$DB_USER" \
    --set=app_password="$APP_PASSWORD" \
    --set=db_name="$DB_NAME" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'app_user', :'app_password')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'db_name', :'app_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db_name')
\gexec

SELECT format('ALTER DATABASE %I OWNER TO %I', :'db_name', :'app_user')
\gexec
SQL

umask 077
{
    printf 'export PGHOST=%q\n' "$DB_HOST"
    printf 'export PGPORT=%q\n' "$DB_PORT"
    printf 'export PGDATABASE=%q\n' "$DB_NAME"
    printf 'export SPRING_DATASOURCE_URL=%q\n' "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
    printf 'export SPRING_DATASOURCE_USERNAME=%q\n' "$DB_USER"
    printf 'export SPRING_DATASOURCE_PASSWORD=%q\n' "$APP_PASSWORD"
} > "$ENV_FILE"
chmod 600 "$ENV_FILE"

set -a
. ./.env.local
set +a

printf 'Configured database %s and role %s.\n' "$DB_NAME" "$DB_USER"
printf 'Verifying the schema and refreshing local seed data (schema.sql first).\n'
bash ./deploy/init-database.sh --refresh-seeds

printf 'Local connection settings saved to .env.local (ignored by Git).\n'
printf 'Database setup is complete. Run ./start.sh to launch the app.\n'
