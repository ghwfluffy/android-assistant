#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}"

output_dir="${ANDROID_OUTPUT_DIR:-${repo_root}/build/outputs}"
mkdir -p "${output_dir}"

required=(
  ANDROID_SITE_ORIGIN
  ANDROID_AUTH_BASE_PATH
  ANDROID_AGENT_BASE_PATH
  ANDROID_DEFAULT_START_PATH
  ANDROID_APP_SHORTCUTS_JSON
)

for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "missing required Android build config: ${name}" >&2
    exit 1
  fi
done

run_gradle() {
  ./gradlew --no-daemon assembleDebug \
    -PANDROID_SITE_ORIGIN="${ANDROID_SITE_ORIGIN}" \
    -PANDROID_AUTH_BASE_PATH="${ANDROID_AUTH_BASE_PATH}" \
    -PANDROID_AGENT_BASE_PATH="${ANDROID_AGENT_BASE_PATH}" \
    -PANDROID_DEFAULT_START_PATH="${ANDROID_DEFAULT_START_PATH}" \
    -PANDROID_APP_SHORTCUTS_JSON="${ANDROID_APP_SHORTCUTS_JSON}" \
    -PANDROID_VERSION_CODE="${ANDROID_VERSION_CODE:-1}" \
    -PANDROID_VERSION_NAME="${ANDROID_VERSION_NAME:-0.1.0}"
}

use_docker="${ANDROID_BUILD_WITH_DOCKER:-}"
if [[ -z "${use_docker}" ]]; then
  sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -n "${sdk_root}" && -d "${sdk_root}/platforms/android-35" && -d "${sdk_root}/build-tools/35.0.0" ]]; then
    use_docker=0
  elif command -v docker >/dev/null 2>&1; then
    use_docker=1
  else
    use_docker=0
  fi
fi

if [[ "${use_docker}" == "1" ]]; then
  docker run --rm \
    -v "${repo_root}:/workspace" \
    -w /workspace \
    -e ANDROID_SITE_ORIGIN \
    -e ANDROID_AUTH_BASE_PATH \
    -e ANDROID_AGENT_BASE_PATH \
    -e ANDROID_DEFAULT_START_PATH \
    -e ANDROID_APP_SHORTCUTS_JSON \
    -e ANDROID_VERSION_CODE="${ANDROID_VERSION_CODE:-1}" \
    -e ANDROID_VERSION_NAME="${ANDROID_VERSION_NAME:-0.1.0}" \
    ghcr.io/cirruslabs/android-sdk:35 \
    bash -lc 'set -euo pipefail; ./gradlew --no-daemon assembleDebug -PANDROID_SITE_ORIGIN="$ANDROID_SITE_ORIGIN" -PANDROID_AUTH_BASE_PATH="$ANDROID_AUTH_BASE_PATH" -PANDROID_AGENT_BASE_PATH="$ANDROID_AGENT_BASE_PATH" -PANDROID_DEFAULT_START_PATH="$ANDROID_DEFAULT_START_PATH" -PANDROID_APP_SHORTCUTS_JSON="$ANDROID_APP_SHORTCUTS_JSON" -PANDROID_VERSION_CODE="$ANDROID_VERSION_CODE" -PANDROID_VERSION_NAME="$ANDROID_VERSION_NAME"; for path in /workspace/.gradle /workspace/build /workspace/app/build; do [ -e "$path" ] && chown -R '"$(id -u):$(id -g)"' "$path"; done'
else
  run_gradle
fi

apk_source="${repo_root}/app/build/outputs/apk/debug/app-debug.apk"
apk_target="${output_dir}/assistant-debug.apk"
metadata_target="${output_dir}/assistant-debug.json"

cp "${apk_source}" "${apk_target}"
cat > "${metadata_target}" <<JSON
{
  "artifact": "assistant-debug.apk",
  "versionName": "${ANDROID_VERSION_NAME:-0.1.0}",
  "versionCode": ${ANDROID_VERSION_CODE:-1},
  "builtAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "gitSha": "$(git rev-parse HEAD)"
}
JSON

echo "${apk_target}"
