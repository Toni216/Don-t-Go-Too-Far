# 🧟 Don't Go Too Far
### A difficulty scaling mod for CipolloLand (a Minecraft server with my friends) — Forge 1.20.1

> *The further you go, the worse it gets.*

**Don't Go Too Far** is a server-side Forge mod that divides the Overworld into 5 danger zones based on distance from spawn (0, 0). The further players venture, the stronger, faster, and more numerous the zombies become.

Designed for post-apocalyptic zombie survival servers.

---

## ⚠️ Zones

| Zone | Distance from spawn | Difficulty |
|------|---------------------|------------|
| 1 — Safe Lands | 0 – 500 | Normal |
| 2 — Frontier | 500 – 1500 | Moderate |
| 3 — Wildlands | 1500 – 3000 | Hard |
| 4 — The Abyss | 3000 – 5000 | Extreme |
| 5 — Beyond | 5000+ | Maximum |

All zone boundaries are fully configurable.

---

## ⚙️ Features

- **Damage scaling** — zombies hit harder the further they are from spawn
- **Health scaling** — zombies have more HP in outer zones
- **Speed scaling** — zombies move faster in outer zones
- **Spawn rate scaling** — more zombies spawn in outer zones, fewer in inner ones
- **Night hordes** — waves of zombies assault players each night, scaling with zone
- **Fire-immune zombies** — zombies don't burn in daylight and won't flee the sun
- **Per-player zone cache** — zones are recalculated every 16 blocks (configurable), not every tick
- **Admin commands** — `/dgtf info` and `/dgtf zone <player>`

---

## 🛠️ Configuration

On first launch, a `dontgotoofar-server.toml` file is generated in your `serverconfig/` folder.

```toml
[zones]
  zone1_max = 500
  zone2_max = 1500
  zone3_max = 3000
  zone4_max = 5000

[zone5]
  damage = 3.0
  health = 3.0
  speed = 2.0
  spawn_rate = 2.5

[extras]
  zombie_fire_immune = true
  hordes_enabled = true
  zombies_per_wave = 5
  wave_interval_ticks = 40
  affected_mobs = ["minecraft:zombie", "minecraft:husk", "minecraft:drowned", ...]
  horde_mobs = ["minecraft:zombie:70", "minecraft:husk:20", "minecraft:drowned:10"]
```

---

## 💻 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/dgtf info` | All players | Shows your current zone and active multipliers |
| `/dgtf zone <player>` | OP (level 2) | Shows another player's current zone |

---

## 📦 Installation

1. Download the `.jar` file
2. Drop it in your server's `mods/` folder
3. No client-side installation needed — this is a **server-only** mod

---

## 🔧 Compatibility

- **Minecraft:** 1.20.1
- **Forge:** 47.x
- **Side:** Server-only

---

## 👤 Author

Made by me for the CipolloLand server.