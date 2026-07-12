#!/usr/bin/env bash
#
# build-all.sh — build every shippable MC Waterslides jar in one shot, into ./dist/.
#
# Produces one jar per NeoForge Stonecutter node (see NEOFORGE_NODES below):
#   dist/mcwaterslides-<ver>-neoforge-<node>.jar   (Java 21 launcher; 26.x nodes compile
#                                                   on a Java 25 toolchain that
#                                                   Gradle/foojay provisions itself)
#
# Usage:
#   ./scripts/build-all.sh                       # version from gradle.properties
#   ./scripts/build-all.sh --version 0.2.0       # override version (e.g. a release tag)
#   ./scripts/build-all.sh --nodes 1.21.1,1.21.8 # override the node list (default: shippable)
#
# JDK: needs a Java 21 launcher (Stonecutter requires it). Auto-detected, or set
# MCWS_JDK21 to override. In CI, GitHub's setup-java exports JAVA_HOME_21_X64, which
# is picked up automatically.
set -euo pipefail

# --- locate repo root (script lives in scripts/) ---
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# --- the SHIPPABLE NeoForge nodes. Only nodes that actually compile belong here —
# release.yml runs this script, so a broken node here breaks the release. The full
# ladder (1.21.8 1.21.9 1.21.10 1.21.11 26.1 26.2) rejoins as the forward-port
# ticket lands each one. Override ad hoc with --nodes.
NEOFORGE_NODES=("1.21.1")
CANONICAL_NODE="1.21.1"   # the vcsVersion; the tree is left on this on exit

# --- args ---
VERSION=""
while [ $# -gt 0 ]; do
    case "$1" in
        --version) VERSION="$2"; shift 2 ;;
        --version=*) VERSION="${1#*=}"; shift ;;
        --nodes) IFS=',' read -r -a NEOFORGE_NODES <<< "$2"; shift 2 ;;
        --nodes=*) IFS=',' read -r -a NEOFORGE_NODES <<< "${1#*=}"; shift ;;
        -h|--help) sed -n '2,16p' "$0"; exit 0 ;;
        *) echo "unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$VERSION" ]; then
    VERSION="$(grep -E '^mod_version=' gradle.properties | head -1 | cut -d= -f2 | tr -d '[:space:]')"
fi
[ -n "$VERSION" ] || { echo "could not determine version" >&2; exit 1; }

# --- resolve a JDK 21 home ---
# order: explicit override -> GitHub setup-java env -> macOS java_home -> foojay (~/.gradle/jdks) -> empty
resolve_jdk() {
    local major="$1" override_var="$2" candidate
    candidate="${!override_var:-}"
    [ -n "$candidate" ] && { echo "$candidate"; return; }
    candidate="$(printenv "JAVA_HOME_${major}_X64" 2>/dev/null || true)"
    [ -n "$candidate" ] && { echo "$candidate"; return; }
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        candidate="$(/usr/libexec/java_home -v "$major" 2>/dev/null || true)"
        [ -n "$candidate" ] && { echo "$candidate"; return; }
    fi
    candidate="$(ls -d "$HOME"/.gradle/jdks/*"${major}"*/ 2>/dev/null | head -1 || true)"
    if [ -n "$candidate" ]; then
        # foojay may nest jdk-XX/Contents/Home (macOS) or jdk-XX (linux)
        [ -d "${candidate}Contents/Home" ] && candidate="${candidate}Contents/Home"
        local inner; inner="$(ls -d "${candidate}"jdk-* 2>/dev/null | head -1 || true)"
        [ -n "$inner" ] && { [ -d "${inner}/Contents/Home" ] && echo "${inner}/Contents/Home" || echo "$inner"; return; }
        echo "${candidate%/}"; return
    fi
    echo ""
}

JDK21="$(resolve_jdk 21 MCWS_JDK21)"
[ -n "$JDK21" ] && [ -x "$JDK21/bin/java" ] || { echo "ERROR: no Java 21 JDK found (set MCWS_JDK21)" >&2; exit 1; }
echo "Java 21 (NeoForge): $JDK21"

DIST="$ROOT/dist"
rm -rf "$DIST"; mkdir -p "$DIST"
echo "==> Building MC Waterslides $VERSION → $DIST"

# leave the tree on the canonical node no matter how we exit
restore_node() { JAVA_HOME="$JDK21" ./gradlew "Set active project to $CANONICAL_NODE" -q >/dev/null 2>&1 || true; }
trap restore_node EXIT

# pick the production jar in a build/libs dir (exclude sources/dev/javadoc)
pick_jar() {
    ls "$1"/mcwaterslides-*.jar 2>/dev/null | grep -vE -- '-(sources|dev|javadoc|slim)\.jar$' | head -1
}

chmod +x ./gradlew
for node in "${NEOFORGE_NODES[@]}"; do
    echo "==> NeoForge $node"
    JAVA_HOME="$JDK21" ./gradlew "Set active project to $node" -q
    JAVA_HOME="$JDK21" ./gradlew ":$node:assemble" -Pmod_version="$VERSION" --no-daemon --stacktrace
    jar="$(pick_jar "versions/$node/build/libs")"
    [ -n "$jar" ] || { echo "ERROR: no jar produced for node $node" >&2; exit 1; }
    cp "$jar" "$DIST/mcwaterslides-$VERSION-neoforge-$node.jar"
done

echo
echo "==> Done. Artifacts in $DIST:"
ls -la "$DIST"/*.jar | awk '{print "    " $NF, "(" $5 " bytes)"}'
