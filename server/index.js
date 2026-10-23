import express from "express";
import multer from "multer";
import path from "node:path";
import fs from "node:fs";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const app = express();
const PORT = Number(process.env.PORT || 3000);
const MAX_VIDEO_BYTES = Number(process.env.MAX_VIDEO_BYTES || 250 * 1024 * 1024);
const DATA_DIR = path.resolve(process.env.ONTUB_DATA_DIR || path.join(__dirname, "data"));
const VIDEO_DIR = path.join(DATA_DIR, "videos");
const META_FILE = path.join(DATA_DIR, "videos.json");
fs.mkdirSync(VIDEO_DIR, { recursive: true });
if (!fs.existsSync(META_FILE)) fs.writeFileSync(META_FILE, "[]", "utf8");

function readVideos() {
  try {
    const parsed = JSON.parse(fs.readFileSync(META_FILE, "utf8"));
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
function saveVideos(videos) {
  const temp = META_FILE + ".tmp";
  fs.writeFileSync(temp, JSON.stringify(videos, null, 2), "utf8");
  fs.renameSync(temp, META_FILE);
}
const allowedMime = new Set(["video/mp4", "video/webm", "video/ogg", "video/quicktime", "video/x-matroska"]);
const extensions = new Set([".mp4", ".webm", ".ogv", ".ogg", ".mov", ".mkv"]);
const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, VIDEO_DIR),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    cb(null, crypto.randomUUID() + ext);
  }
});
const upload = multer({
  storage,
  limits: { fileSize: MAX_VIDEO_BYTES, files: 1, fields: 3, fieldSize: 2048 },
  fileFilter: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    if (allowedMime.has(file.mimetype) && extensions.has(ext)) return cb(null, true);
    cb(new Error("Поддерживаются MP4, WebM, OGG, MOV и MKV."));
  }
});

app.disable("x-powered-by");
app.use(express.json({ limit: "20kb" }));
app.use("/uploads", express.static(VIDEO_DIR, { fallthrough: false, setHeaders(res) { res.setHeader("X-Content-Type-Options", "nosniff"); res.setHeader("Content-Disposition", "inline"); } }));
app.use(express.static(path.resolve(__dirname, "../web")));

app.get("/api/health", (_req, res) => res.json({ ok: true, service: "ONTUB", uploadLimitBytes: MAX_VIDEO_BYTES }));
app.get("/api/videos", (_req, res) => {
  res.setHeader("Cache-Control", "no-store");
  res.json(readVideos().map(({ id, title, description, creator, filename, createdAt, size, mimeType }) => ({
    id, title, desc: description, creator, url: "/uploads/" + encodeURIComponent(filename), createdAt, size, mimeType, local: false
  })));
});
app.post("/api/videos", upload.single("video"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "Выбери видеофайл." });
  const title = String(req.body.title || "").trim();
  const description = String(req.body.description || "").trim();
  const creator = String(req.body.creator || "Онтубер").trim().slice(0, 40);
  if (title.length < 1 || title.length > 100 || description.length > 300) {
    fs.unlink(req.file.path, () => {});
    return res.status(400).json({ error: "Название должно быть до 100 символов, описание — до 300." });
  }
  const item = {
    id: crypto.randomUUID(), title, description, creator, filename: req.file.filename,
    createdAt: new Date().toISOString(), size: req.file.size, mimeType: req.file.mimetype
  };
  try {
    const videos = readVideos();
    videos.unshift(item);
    saveVideos(videos);
    return res.status(201).json({ id: item.id, title: item.title, desc: item.description, creator: item.creator, url: "/uploads/" + encodeURIComponent(item.filename), createdAt: item.createdAt, size: item.size });
  } catch {
    fs.unlink(req.file.path, () => {});
    return res.status(500).json({ error: "Не удалось сохранить видео. Попробуй ещё раз." });
  }
});
app.use((err, _req, res, _next) => {
  if (err instanceof multer.MulterError) {
    const msg = err.code === "LIMIT_FILE_SIZE" ? "Файл слишком большой. Лимит: " + Math.round(MAX_VIDEO_BYTES / 1024 / 1024) + " МБ." : "Не удалось принять файл: " + err.message;
    return res.status(413).json({ error: msg });
  }
  if (err) return res.status(400).json({ error: err.message || "Ошибка загрузки." });
  return res.status(500).json({ error: "Внутренняя ошибка сервера." });
});
app.listen(PORT, "0.0.0.0", () => console.log("ONTUB listening on port " + PORT));
