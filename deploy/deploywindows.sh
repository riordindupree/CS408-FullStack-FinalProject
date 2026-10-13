#!/usr/bin/env bash
# filepath: c:\Users\ohcad\Documents\CS-408-FullStackWebDev\CS408-FullStack-FinalProject\deploy\deploywindows.sh
#
# Deploy from Windows using Git Bash, or from WSL/Linux/macOS.
#
#   ./deploy/deploywindows.sh -h 54.12.34.56 -i keys/login.pem
#
# What it does:
#   1. rsync the project to /opt/99-steps-and-7-falls on the server
#   2. rebuild the Spring Boot artifact on the server
#   3. restart the systemd service
#
# Options:
#   -h HOST   public IP or DNS name of the instance   (required)
#   -i KEY    path to your .pem private key           (optional if ssh-agent has it)
#   -u USER   SSH login user (default: ubuntu; use 'ec2-user' for Amazon Linux)
#   -d DIR    remote directory (default: /opt/99-steps-and-7-falls)

set -euo pipefail

REMOTE_HOST=""
SSH_KEY=""
REMOTE_USER="ubuntu"
REMOTE_DIR="/opt/99-steps-and-7-falls"
SERVICE="99-steps-and-7-falls"

usage() {
    awk 'NR>1 { if (/^#/) { sub(/^# ?/, ""); print } else { exit } }' "$0"
    exit 1
}

while getopts ":h:i:u:d:" option; do
    case "$option" in
        h) REMOTE_HOST="$OPTARG" ;;
        i) SSH_KEY="$OPTARG" ;;
        u) REMOTE_USER="$OPTARG" ;;
        d) REMOTE_DIR="$OPTARG" ;;
        :) echo "ERROR: -$OPTARG needs a value" >&2; usage ;;
        \?) echo "ERROR: unknown option -$OPTARG" >&2; usage ;;
    esac
done

[ -n "$REMOTE_HOST" ] || { echo "ERROR: -h HOST is required" >&2; usage; }

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

[ -f pom.xml ] || { echo "ERROR: no pom.xml in $PROJECT_DIR" >&2; exit 1; }

for tool in bash java tar scp ssh curl; do
    command -v "$tool" >/dev/null 2>&1 \
        || { echo "ERROR: required command not found: $tool" >&2; exit 1; }
done

SSH_OPTS=(-o StrictHostKeyChecking=accept-new)
if [ -n "$SSH_KEY" ]; then
    [ -f "$SSH_KEY" ] || { echo "ERROR: key file not found: $SSH_KEY" >&2; exit 1; }
    SSH_OPTS+=(-i "$SSH_KEY")
fi

TARGET="${REMOTE_USER}@${REMOTE_HOST}"
ARCHIVE="$(mktemp)"
REMOTE_ARCHIVE="/tmp/99-steps-and-7-falls-$$.tar.gz"
trap 'rm -f "$ARCHIVE"' EXIT

echo "==> Running tests"
bash ./mvnw test

echo "==> Building the Spring Boot jar"
bash ./mvnw clean package -DskipTests

echo "==> Creating deployment archive"
tar -czf "$ARCHIVE" \
    --exclude='./.git' \
    --exclude='./data' \
    --exclude='./.DS_Store' \
    --exclude='*.log' \
    -C "$PROJECT_DIR" .

echo "==> Copying files to ${TARGET}:${REMOTE_DIR}"
scp "${SSH_OPTS[@]}" "$ARCHIVE" "${TARGET}:${REMOTE_ARCHIVE}"

echo "==> Extracting files and restarting the service"
ssh "${SSH_OPTS[@]}" "$TARGET" "
    set -e
    mkdir -p '${REMOTE_DIR}'
    tar -xzf '${REMOTE_ARCHIVE}' -C '${REMOTE_DIR}'
    rm -f '${REMOTE_ARCHIVE}'
    sudo systemctl restart '${SERVICE}'
    sleep 2
    sudo systemctl is-active --quiet '${SERVICE}' && echo 'service is running'
"

echo "==> Checking the health endpoint"
if curl -fsS --max-time 10 "http://${REMOTE_HOST}/actuator/health"; then
    printf '\n\n==> Deployed. Open http://%s in your browser.\n' "$REMOTE_HOST"
else
    printf '\n'
    echo "WARNING: the health check did not answer." >&2
    echo "Check the EC2 security group (port 80 inbound) and the logs:" >&2
    echo "  ssh ${SSH_OPTS[*]} $TARGET 'journalctl -u ${SERVICE} -n 50'" >&2
    exit 1
fi