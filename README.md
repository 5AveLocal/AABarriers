# AABarriers
Automatic Arm Barriers Plugin for Minecraft
(Note: This is a vibe-coded plugin.)

## 🔗 Requirement

Must be installed in Bukkit-based servers (e.g. Bukkit / Spigot / Paper) with Minecraft 1.21.4 or above.<br/>

## 📁 Setup

### `gates.yml`

This file is purely used to store gate data. There is no need to edit it.

### `warn_presets.yml`

Please set up new presets if needed.<br/>
You can adjust the flashing interval of the lights in `flash_interval_ticks`,<br/>
and adjust sounds in `sounds`, where the syntax is `<sound_name>,<volume>,<pitch>,<delay_ticks_until_next>`.

## ⚙️ Commands

The commands can be executed by players who have permission, and by command blocks.

### 🚨 Warning signals

`/aab warn <on/off> <warn_preset> <x1> <y1> <z1> [x2] [y2] [z2]` to start or stop flashing lights and warning sounds.<br/>
The `<warn_preset>` can be selected from `warn_presets.yml`.<br/>
The two coordinates are the positions of the flashing lights. One of them is optional if you only have one light.

### 🚧 Barrier arms

`/aab <open/close> <startX> <startY> <startZ> <endX> <endY> <endZ> <rotSpeed>` to open / close the barrier arm,<br/>
`/aab tempopen <startX> <startY> <startZ> <endX> <endY> <endZ> <rotSpeed> <duration>` to temporarily open the barrier arm, and<br/>
`/aab reset <startX> <startY> <startZ> <endX> <endY> <endZ>` to reset the barrier arm back to its block status, where<br/>

- `<startX/Y/Z>` is the start position (pivot)
- `<endX/Y/Z>` is the end position
- `<rotSpeed>` is the time taken (in ticks) to rotate the barrier arm from 0 degrees to 90 degrees
- `<duration>` is the total time taken to open, hold, and close the barrier arm

Note that the blocks between the start position and end position will become block displays during use, and stained glass (panes) will be replaced by their respective colored concrete while being block displays.
