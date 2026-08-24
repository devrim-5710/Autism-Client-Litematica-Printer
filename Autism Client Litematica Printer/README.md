# Litematica Printer Addon

AUTISM Client addon that prints whatever schematic is loaded in **Litematica** - no file picking,
no paths. Made by theflex5710. It watches Litematica's schematic hologram and places every block it can reach.

## Module

**Litematica Printer** (in the addon's own category in the module menu):

| Setting | Default | Description |
| --- | --- | --- |
| Reach | `4.5` | Max distance to place blocks from your eyes. |
| Air Place | `off` | Also place blocks with no support block yet (targets the block itself, face toward you). |
| Blocks Per Tick | `1` | How many blocks may be placed per client tick. |
| Delay Ticks | `0` | Extra cooldown between placement ticks. |
| Restore Selected Slot | `off` | Put your original hotbar slot back after placing. |

Usage:

1. Load your `.litematic` and position its placement like you normally would in Litematica.
2. Toggle the module on.
3. Walk near the build area - reachable missing blocks get placed automatically (nearest first,
   support-block aware). Toggle off to stop.

Requires the Litematica mod (`26.2-0.28.x`) to be installed alongside AUTISM Client.

## Build

The addon needs the AUTISM Client API in your local Maven repo:

1. From the AUTISM Client project:
   ```powershell
   .\gradlew.bat publishToMavenLocal --no-daemon
   ```
2. From this folder:
   ```powershell
   .\gradlew.bat build --no-daemon
   ```

The jar lands in `build/libs/`. Load it like any other AUTISM addon.

## Source layout

- `PrinterAddon.java` / `PrinterInit.java` - entrypoints (`fabric.mod.json`)
- `PrinterModule.java` - module settings + placement tick loop
- `LitematicaBridge.java` - reflective hook into Litematica's schematic world (no compile dep)
