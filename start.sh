#!/usr/bin/env bash
#
# Start the Spring Boot app on your own machine.
#
# Checks that the required tools are installed, resolves the project
# dependencies, and starts the app.
#
#   ./start.sh              start the app on http://localhost:3000
#   ./start.sh --port 4000  listen somewhere other than port 3000
#   ./start.sh --help       show this message
#
# Works on Linux and macOS.

set -euo pipefail

REQUIRED_JAVA_MAJOR=17
DEFAULT_PORT=3000
PORT_OVERRIDE=""

say() {
    printf '==> %s\n' "$1"
}

die() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

usage() {
    awk 'NR>1 { if (/^#/) { sub(/^# ?/, ""); print } else { exit } }' "$0"
    exit 0
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        -p|--port)
            [ "$#" -ge 2 ] || die "--port needs a number, for example: --port 4000"
            case "$2" in
                ''|*[!0-9]*) die "--port needs a number, got '$2'" ;;
            esac
            if [ "$2" -lt 1 ] || [ "$2" -gt 65535 ]; then
                die "--port must be between 1 and 65535"
            fi
            PORT_OVERRIDE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            die "unknown option '$1' (try --help)"
            ;;
    esac
done

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

command -v java >/dev/null 2>&1 \
    || die "Java is not installed. Install a Java ${REQUIRED_JAVA_MAJOR} or newer JDK and add it to PATH."
command -v javac >/dev/null 2>&1 \
    || die "javac is not installed. Install a Java ${REQUIRED_JAVA_MAJOR} or newer JDK and add it to PATH."

if [ -n "${JAVA_HOME:-}" ] &&
    { [ ! -x "$JAVA_HOME/bin/java" ] || [ ! -x "$JAVA_HOME/bin/javac" ]; }; then
    die "JAVA_HOME points to '$JAVA_HOME', which does not contain an executable Java ${REQUIRED_JAVA_MAJOR} JDK."
fi

if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
    die "curl or wget is required to download Maven on the first run. Install either tool and try again."
fi

if ! command -v unzip >/dev/null 2>&1 && ! command -v tar >/dev/null 2>&1; then
    die "unzip or tar is required to unpack Maven on the first run. Install either tool and try again."
fi

java_version="$(java -version 2>&1 | sed -n '1s/.*version "\([^"]*\)".*/\1/p')"
javac_version="$(javac -version 2>&1 | sed -n '1s/.* //p')"
java_major="${java_version%%.*}"
javac_major="${javac_version%%.*}"
if [ "$java_major" = "1" ]; then
    java_major="${java_version#*.}"
    java_major="${java_major%%.*}"
fi
if [ "$javac_major" = "1" ]; then
    javac_major="${javac_version#*.}"
    javac_major="${javac_major%%.*}"
fi

case "$java_major:$javac_major" in
    *[!0-9:]*|:*) die "Could not determine the installed Java versions. Check that java and javac are on PATH." ;;
esac
if [ "$java_major" -lt "$REQUIRED_JAVA_MAJOR" ] || [ "$javac_major" -lt "$REQUIRED_JAVA_MAJOR" ]; then
    die "Java ${REQUIRED_JAVA_MAJOR} or newer is required. Found java ${java_version:-unknown} and javac ${javac_version:-unknown}."
fi
if [ "$java_major" -ne "$javac_major" ]; then
    die "java and javac must use the same Java major version. Found java ${java_version} and javac ${javac_version}."
fi

[ -f pom.xml ] || die "no pom.xml here. Run this script from inside the project folder."
[ -f mvnw ] || die "the Maven Wrapper (mvnw) is missing from the project root."
[ -f .mvn/wrapper/maven-wrapper.properties ] \
    || die "the Maven Wrapper configuration is missing from .mvn/wrapper/maven-wrapper.properties."

PORT="${PORT_OVERRIDE:-$DEFAULT_PORT}"
say "Building the application and resolving Maven dependencies"
bash ./mvnw --batch-mode package

APP_JAR="target/spring-boot-0.0.1-SNAPSHOT.jar"
[ -f "$APP_JAR" ] || die "the Maven build completed without producing $APP_JAR."

say "Starting on http://localhost:${PORT} -- press Ctrl+C to stop"
exec java -jar "$APP_JAR" "--server.port=${PORT}"
