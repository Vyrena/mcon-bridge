# Launcher compatibility contracts

MCON Bridge exposes only `mconbridge://launch/<uuid>` to launchers. The UUID is
resolved locally to one of these allow-listed adapters.

## Azahar MCON

- Package: `org.azahar_emu.azahar.mcon`
- Intent: `ACTION_VIEW`
- URI: `azahar-mcon://game/<16-hex-title-id>`

The adapter deliberately targets the side-by-side Azahar MCON fork, so it
cannot accidentally route into an incompatible official Azahar installation.

## Artemis

- Package: `com.limelight.noir`
- Activity: `com.limelight.ShortcutTrampoline`
- Intent: `ACTION_VIEW` with a temporary FileProvider-backed `.art` file

The generated file uses Artemis' own exported launcher keys. The contract was
checked against Artemis source revision `404d70d` (2025-09-14). No host secret,
credential, or save data is copied into MCON.

## Kirin

- Package: `com.gmax.kirin`
- Activity: `com.gmax.kirin.MainActivity`
- Action: `com.gmax.kirin.action.LAUNCH_GAME_SHORTCUT`
- Extra: `shortcut_game_path`

The contract was checked against Kirin 0.3.5 (`versionCode 23`). Paths must be
canonical direct children of `/storage/emulated/0/Kirin/games`. The scanner
uses Android's Storage Access Framework with read permission only. It never
writes inside Kirin's tree and never accesses saves.

These are compatibility contracts, not vendor APIs. Revalidate them after an
emulator update before depending on a new release.
