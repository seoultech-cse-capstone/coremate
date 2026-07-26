---
name: start-backend
description: Start the CoreMate local backend stack (MySQL + Redis + Backend via Docker Compose). Use when the user asks to start/run/boot up the backend server locally, e.g. "백엔드 서버 켜줘", "start the backend", "run backend locally".
---

Runs `scripts/local/start-backend.sh` from the repo root (`coremate/`) to bring up the local backend stack.

## Steps

1. Start the containers (detached, no log follow) with the Bash tool from the `coremate` repo root (not `apps/mobile`):
   ```
   cd infra && docker compose -f docker-compose.local.yml up -d
   ```
   - If the user asks to rebuild the image (e.g. backend code changed), append `--build`.
   - If Docker Desktop isn't running yet, run the full `scripts/local/start-backend.sh` script instead (it auto-starts Docker Desktop and waits for the engine) — pass `run_in_background: true` since it tails logs forever. Docker Desktop can take 1-3 minutes to boot.

2. Always also run `node scripts/local/sync-ip.js` from the repo root right after the containers are up. It rewrites `apps/mobile/.env`'s `EXPO_PUBLIC_API_BASE_URL` to the machine's current LAN IP (the value drifts whenever the machine reconnects to WiFi / gets a new DHCP lease, and a stale IP silently breaks every API call from a phone/emulator with no obvious error — it just looks like empty data everywhere). `scripts/local/start-backend.sh` already runs this itself, so skip this step only if you ran the full script instead of the raw `docker compose` command. Tell the user that if Metro/Expo is already running, they must fully stop and restart it (not just reload the app) for the refreshed `.env` to take effect — a running Metro process keeps the IP it started with.

3. Once containers are up, tell the user:
   - Backend: http://localhost:8080
   - Swagger: http://localhost:8080/swagger-ui.html
   - To stop: `cd infra && docker compose -f docker-compose.local.yml down`

4. For live logs, there's a **VS Code Task** at `apps/mobile/.vscode/tasks.json` (label `Backend Logs`) that opens a dedicated integrated-terminal panel running `docker compose logs -f backend`. There is no CLI/API to trigger a VS Code task's UI panel from outside the editor, so tell the user to run it themselves via `Ctrl+Shift+P` → `Tasks: Run Task` → `Backend Logs` (or bind a keyboard shortcut to `workbench.action.tasks.runTask` with that label). Do not try to launch it programmatically.
