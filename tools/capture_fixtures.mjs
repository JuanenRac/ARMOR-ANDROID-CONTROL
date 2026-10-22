#!/usr/bin/env node
/**
 * Captures the answers of a running ARMOR-SERVER that the app reads, to be the fixtures of the contract tests (app/src/test/resources/fixtures).
 * Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
 *
 * Start a server on a computer (never the house's), feed it with ARMOR-SIMULATOR, then:
 *
 *   ARMOR_URL=http://127.0.0.1:8080 ARMOR_USER=admin ARMOR_PASSWORD=... node tools/capture_fixtures.mjs
 *
 * The password is read from the environment and never written. Every answer is saved pretty-printed, with the moment of the capture replaced by a fixed one;
 * the fixtures only hold what the simulator invented.
 */
import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const origin = (process.env.ARMOR_URL ?? "").replace(/\/$/, "");
const user = process.env.ARMOR_USER ?? "", password = process.env.ARMOR_PASSWORD ?? "";
if (!origin || !user || !password) { console.error("Set ARMOR_URL, ARMOR_USER and ARMOR_PASSWORD."); process.exit(2); }
const out = path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "app", "src", "test", "resources", "fixtures");
const ROUTES = { status: "/api/v1/status", alarms: "/api/v1/alarms", history: "/api/v1/history?limit=50", camera_views: "/api/v1/camera-views", media: "/api/v1/media", solar: "/api/v1/solar", node_health: "/api/v1/node-health" };

const login = await fetch(`${origin}/api/v1/studio/session`, { method: "POST", headers: { "Content-Type": "application/json", Accept: "application/json" }, body: JSON.stringify({ username: user, password }) });
if (!login.ok) { console.error(`Sign-in refused: ${login.status}`); process.exit(1); }
const cookie = (login.headers.getSetCookie?.() ?? []).map(item => item.split(";")[0]).join("; ");
mkdirSync(out, { recursive: true });
for (const [name, route] of Object.entries(ROUTES)) {
  const response = await fetch(origin + route, { headers: { Accept: "application/json", Cookie: cookie } });
  if (!response.ok) { console.error(`${route}: ${response.status}`); process.exit(1); }
  const text = JSON.stringify(await response.json(), null, 2).replace(/"\d{4}-\d{2}-\d{2}T[\d:.]+Z"/g, '"2030-01-01T00:00:00.000Z"');
  writeFileSync(path.join(out, `${name}.json`), text + "\n");
  console.log("saved", name);
}
