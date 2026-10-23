# ONTUB

Video platform project by GingerXYT.

**© 2026 GingerXYT. All rights reserved.**

## Run locally

Requires Node.js 20 or newer.

```sh
npm install
npm start
```

Open `http://localhost:3000`.

## Video upload API

- `GET /api/health` — health check and upload limit.
- `GET /api/videos` — list uploaded video metadata.
- `POST /api/videos` — multipart form containing `title`, optional `description`, optional `creator`, and one `video` file.

Supported formats: MP4, WebM, OGG, MOV, MKV. Default upload limit is 250 MB; configure with `MAX_VIDEO_BYTES`. Files and metadata are stored in `server/data/` unless `ONTUB_DATA_DIR` is set.

## Important limitations

This is an early prototype, not production-ready. Uploads currently have no real account authentication or ownership enforcement, content scanning, video transcoding, thumbnails, or object storage. Do not expose the server publicly as-is: anyone who can reach it can upload files within the configured limit. The registration on the static page is demo-only and is not connected to server accounts.
