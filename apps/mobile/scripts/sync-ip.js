const fs = require("fs");
const os = require("os");
const path = require("path");

const ENV_PATH = path.join(__dirname, "..", ".env");
const PREFERRED_INTERFACE_NAMES = ["Wi-Fi", "WLAN", "Ethernet", "이더넷"];
const IGNORED_NAME_PATTERN = /virtualbox|vmware|vethernet|loopback|tailscale|wsl/i;

function findLocalIPv4() {
  const interfaces = os.networkInterfaces();

  const candidates = [];
  for (const [name, addrs] of Object.entries(interfaces)) {
    if (IGNORED_NAME_PATTERN.test(name)) continue;
    for (const addr of addrs || []) {
      if (addr.family === "IPv4" && !addr.internal) {
        candidates.push({ name, address: addr.address });
      }
    }
  }

  if (candidates.length === 0) return null;

  for (const preferred of PREFERRED_INTERFACE_NAMES) {
    const match = candidates.find((c) => c.name === preferred);
    if (match) return match.address;
  }

  return candidates[0].address;
}

function main() {
  const ip = findLocalIPv4();
  if (!ip) {
    console.warn("[sync-ip] 로컬 IPv4 주소를 찾지 못했습니다. .env를 그대로 둡니다.");
    return;
  }

  if (!fs.existsSync(ENV_PATH)) {
    console.warn(`[sync-ip] ${ENV_PATH} 가 없습니다. 건너뜁니다.`);
    return;
  }

  const original = fs.readFileSync(ENV_PATH, "utf-8");
  const updated = original.replace(
    /(EXPO_PUBLIC_API_BASE_URL=https?:\/\/)([^:/\s]+)/,
    `$1${ip}`
  );

  if (updated === original) {
    console.log(`[sync-ip] IP 변경 없음 (${ip})`);
    return;
  }

  fs.writeFileSync(ENV_PATH, updated, "utf-8");
  console.log(`[sync-ip] EXPO_PUBLIC_API_BASE_URL 의 IP를 ${ip} 로 갱신했습니다.`);
}

main();
