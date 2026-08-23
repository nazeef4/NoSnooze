#!/usr/bin/env sh
set -eu
VERSION=8.9
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-$VERSION-bin"
GRADLE="$BASE/gradle-$VERSION/bin/gradle"
if [ ! -x "$GRADLE" ]; then
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-$VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  echo "Downloading Gradle $VERSION..." >&2
  if command -v curl >/dev/null 2>&1; then curl -fL "$URL" -o "$ZIP"; else wget -O "$ZIP" "$URL"; fi
  (cd "$BASE" && unzip -q -o "$ZIP")
  rm -f "$ZIP"
fi
# Capture the complete build output. On GitHub Actions, also publish the useful
# tail as a check annotation so failures can be diagnosed without downloading logs.
LOG="${TMPDIR:-/tmp}/nosnooze-gradle-$$.log"
set +e
"$GRADLE" "$@" >"$LOG" 2>&1
STATUS=$?
set -e
cat "$LOG"
if [ "$STATUS" -ne 0 ] && [ "${GITHUB_ACTIONS:-false}" = "true" ]; then
  MESSAGE=$(tail -n 120 "$LOG" | sed 's/%/%25/g; s/\r/%0D/g' | awk '{printf "%s%%0A", $0}')
  printf '::error title=NoSnooze Gradle build failure::%s\n' "$MESSAGE"
fi
rm -f "$LOG"
exit "$STATUS"
