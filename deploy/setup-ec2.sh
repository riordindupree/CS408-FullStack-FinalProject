#!/usr/bin/env bash
#
# One-time setup for a fresh EC2 instance.
#
# Copy this whole deploy/ folder to the server and run it once as root:
#
#   scp -i mykey.pem -r deploy ubuntu@<PUBLIC-IP>:~
#   ssh -i mykey.pem ubuntu@<PUBLIC-IP>
#
# (On Amazon Linux the login user is ec2-user instead of ubuntu.)
#   sudo bash deploy/setup-ec2.sh
#
# It installs Java, Maven, and nginx, creates the service account and directories,
# and registers the app with systemd so it starts on boot. On the app's first
# start, its service loads SQL files from src/main/resources/sql_queries, with
# schema.sql applied first. After setup, use deploy/deploy.sh from your laptop
# to push code.
#
# Works on Amazon Linux 2023 and on Ubuntu 22.04/24.04.

# Bash "strict mode": stop on the first error (-e), stop on an undefined
# variable (-u), and make a failing command inside a pipeline fail the whole
# pipeline (-o pipefail). Without these a script happily keeps going after a
# step fails and leaves you with a half configured server.
set -euo pipefail

APP_NAME="99-steps-and-7-falls"
APP_DIR="/opt/99-steps-and-7-falls"
APP_USER="game_app"
DB_NAME="game_db"
DB_USER="game_app"
DB_CONFIG_DIR="/etc/${APP_NAME}"
DB_PASSWORD_FILE="${DB_CONFIG_DIR}/db-password"
DB_ENV_FILE="${DB_CONFIG_DIR}/database.env"

# The directory this script lives in, so it can find the files next to it no
# matter where it was called from.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# The human who ran `sudo`. They get write access to APP_DIR so that rsync from
# a laptop works without sudo on the far end.
DEPLOY_USER="${SUDO_USER:-root}"

# --- helpers ----------------------------------------------------------------

# Print a step heading so the output is easy to follow.
say() {
    printf '\n==> %s\n' "$1"
}

die() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

# --- checks -----------------------------------------------------------------

[ "$(id -u)" -eq 0 ] || die "run this with sudo: sudo bash $0"

# /etc/os-release exists on every modern Linux and tells us which distro we are
# on. We need to know because Amazon Linux uses dnf and Ubuntu uses apt.
[ -r /etc/os-release ] || die "cannot read /etc/os-release; unsupported system"
# shellcheck disable=SC1091
. /etc/os-release

case "${ID:-}" in
    amzn|rhel|centos|fedora)
        PKG="dnf"
        ;;
    ubuntu|debian)
        PKG="apt"
        export DEBIAN_FRONTEND=noninteractive
        ;;
    *)
        die "unsupported distribution '${ID:-unknown}'; expected Amazon Linux or Ubuntu"
        ;;
esac

say "Detected ${PRETTY_NAME:-$ID} (package manager: $PKG)"

# --- packages ---------------------------------------------------------------

say "Installing Java 17, Maven, nginx, and PostgreSQL"

if [ "$PKG" = "dnf" ]; then
    dnf -y update
    dnf -y install java-17-openjdk maven nginx rsync openssl postgresql15 postgresql15-server
else
    apt-get update -y
    apt-get install -y ca-certificates curl gnupg rsync openssl openjdk-17-jdk maven nginx postgresql
fi

say "Java $(java -version 2>&1 | head -n 1), Maven $(mvn -version 2>&1 | head -n 1)"

# --- PostgreSQL -------------------------------------------------------------

say "Initializing and starting PostgreSQL"

# Amazon Linux packages need an explicit initial database setup. Ubuntu's
# PostgreSQL packages create their default cluster during installation.
if [ "$PKG" = "dnf" ] && [ ! -s /var/lib/pgsql/data/PG_VERSION ]; then
    postgresql-setup --initdb
fi

systemctl enable --now postgresql

say "Creating the local application database and credentials"

install -d -o root -g root -m 700 "$DB_CONFIG_DIR"

# Keep a generated password across reruns so setup remains idempotent.
if [ ! -s "$DB_PASSWORD_FILE" ]; then
    openssl rand -hex 32 > "$DB_PASSWORD_FILE"
    chmod 600 "$DB_PASSWORD_FILE"
fi
DB_PASSWORD="$(cat "$DB_PASSWORD_FILE")"
[ -n "$DB_PASSWORD" ] || die "database password file is empty: $DB_PASSWORD_FILE"

# The password is generated as hex so it can be safely quoted in this SQL.
runuser -u postgres -- psql --set=ON_ERROR_STOP=1 <<SQL
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${DB_USER}') THEN
        CREATE ROLE ${DB_USER} LOGIN;
    END IF;
END
\$\$;
ALTER ROLE ${DB_USER} WITH LOGIN PASSWORD '${DB_PASSWORD}';
SQL

if ! runuser -u postgres -- psql --tuples-only --no-align \
    --command "SELECT 1 FROM pg_database WHERE datname = '${DB_NAME}'" | grep -q 1; then
    runuser -u postgres -- createdb --owner="$DB_USER" "$DB_NAME"
fi

umask 077
cat > "$DB_ENV_FILE" <<EOF
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/${DB_NAME}
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
EOF
chmod 600 "$DB_ENV_FILE"

# --- service account and directories ----------------------------------------

say "Creating the '$APP_USER' service account"

# id fails when the user does not exist, which is how we test for it. The app
# never needs to log in, so it gets no shell and no home directory.
if ! id "$APP_USER" >/dev/null 2>&1; then
    useradd --system --no-create-home --shell /usr/sbin/nologin "$APP_USER"
else
    echo "user '$APP_USER' already exists, leaving it alone"
fi

say "Creating $APP_DIR"

mkdir -p "$APP_DIR/data"

# The person deploying owns the code directory so they can rsync into it.
# The service account owns the data directory because that is the only place
# the running app has to write.
chown -R "$DEPLOY_USER:$APP_USER" "$APP_DIR"
chown -R "$APP_USER:$APP_USER" "$APP_DIR/data"
chmod 750 "$APP_DIR"
chmod 770 "$APP_DIR/data"

# --- systemd ----------------------------------------------------------------

say "Installing the $APP_NAME systemd service"

install -m 644 "$SCRIPT_DIR/game-app.service" "/etc/systemd/system/${APP_NAME}.service"
systemctl daemon-reload

systemctl enable "$APP_NAME"

# --- nginx ------------------------------------------------------------------

say "Configuring nginx as a reverse proxy on port 80"

install -m 644 "$SCRIPT_DIR/nginx-game.conf" "/etc/nginx/conf.d/${APP_NAME}.conf"

# Ubuntu ships a default site that also claims port 80. Two default servers on
# one port is an error, so remove theirs.
if [ -L /etc/nginx/sites-enabled/default ]; then
    rm -f /etc/nginx/sites-enabled/default
fi

# On SELinux systems nginx is not allowed to open network connections to the
# Spring Boot app until this switch is flipped.
if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce)" != "Disabled" ]; then
    setsebool -P httpd_can_network_connect 1 || true
fi

# Always check the config before restarting: a typo here takes the site down.
nginx -t
systemctl enable nginx
systemctl restart nginx

# --- firewall ---------------------------------------------------------------

# EC2 security groups are the firewall that matters, but Ubuntu images sometimes
# have ufw switched on as well.
if command -v ufw >/dev/null 2>&1 && ufw status | grep -q "Status: active"; then
    say "Opening ports 80 and 22 in ufw"
    ufw allow 80/tcp
    ufw allow 22/tcp
fi

# --- done -------------------------------------------------------------------

cat <<EOF

==> Server setup is complete.

PostgreSQL is running locally on this EC2 instance. The app database is
'$DB_NAME'; its generated credentials are stored in the root-only file
'$DB_ENV_FILE' and loaded by systemd. PostgreSQL is not exposed publicly.
The first application start initializes it from sql_queries/schema.sql followed
by the remaining SQL files. The initialization marker prevents replay on
subsequent restarts.

The app is registered but not running yet, because no code has been deployed.
From your laptop, in the project folder, run:

    ./deploy/deploy.sh -h <PUBLIC-IP-OR-DNS> -i ~/path/to/key.pem

Then open http://<PUBLIC-IP> in a browser.

Make sure your EC2 security group allows inbound TCP port 80 from 0.0.0.0/0
and port 22 from your own IP address.

Handy commands on this server:
    sudo systemctl status $APP_NAME
    journalctl -u $APP_NAME -f
    sudo systemctl restart $APP_NAME

EOF
