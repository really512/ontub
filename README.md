# ONTUB

Video platform project by GingerXYT.

## Android app

Native Android app source is in `android/`. It provides an ONTUB home screen, video file picker, and multipart upload/list integration with the Node server.

To build an APK without a local PC, open **Actions → Build ONTUB Android APK → Run workflow** in GitHub. When the workflow finishes, open its run and download the `ontub-debug-apk` artifact. The artifact is a debug APK for testing, not a Play Store release.

Before uploading videos, set `API_BASE` in `android/app/src/main/java/com/gingerxyt/ontub/MainActivity.java` to the HTTPS public URL of your deployed ONTUB server, then rebuild the APK.

## Server

Requires Node.js 20+:
```sh
npm install
npm start
```
The server exposes `GET /api/health`, `GET /api/videos`, and `POST /api/videos`. Default upload limit: 250 MB.

## Limitations

This is an early prototype. The Android app does not yet have real account authentication. The server upload API currently does not authenticate uploaders or enforce ownership, so do not expose it publicly as-is. It also lacks content scanning, video transcoding, thumbnails, and production object storage.
