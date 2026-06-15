# Android Assistant

Native Android wrapper for a private web app family. The repository is generic:
production origin, route paths, and widget targets are injected at build time by
the deployment repository.

## Build

```bash
ANDROID_SITE_ORIGIN=https://example.invalid \
ANDROID_AUTH_BASE_PATH=/auth \
ANDROID_AGENT_BASE_PATH=/agent \
ANDROID_DEFAULT_START_PATH=/auth \
ANDROID_APP_SHORTCUTS_JSON='[{"label":"Directory","path":"/auth"}]' \
scripts/build-debug-apk.sh
```

Set `ANDROID_BUILD_WITH_DOCKER=1` to build with the Android SDK container when
a local SDK is unavailable.

The output is a debug APK under `build/outputs/`. Do not commit generated APKs,
keystores, or production route values. This project intentionally uses a
debug/manual-install build flow; production signing is out of scope for now.
