# Launcher compatibility contracts

MCON Bridge exposes only `mconbridge://launch/<uuid>` to launchers. The UUID is
resolved locally to one of these allow-listed adapters.

## Ordinary Azahar

- Package: `org.azahar_emu.azahar`
- Activity: `org.citra.citra_emu.activities.EmulationActivity`
- Intent: explicit `ACTION_VIEW` with a user-selected `content://` ROM URI
- Extra: Azahar-compatible `Game` Parcelable under the `game` key

Android's picker grants MCON Bridge persistent read access to the selected ROM.
The bridge grants that URI to ordinary Azahar only for launch and never opens
Azahar's private user-data or save directories. Installed CIA titles cannot be
imported through this route because ordinary Azahar exposes no public API for
its private installed-title library.

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

The contract was checked against Kirin 0.3.5 (`versionCode 23`). Kirin records
the absolute path of each registered game, so the bridge accepts a game folder
or direct child of any explicitly selected folder in primary shared storage;
`/storage/emulated/0/Kirin/games` remains supported but is not required. The
scanner uses Android's Storage Access Framework with read permission only. It
never writes inside the selected tree and never accesses saves.

These are compatibility contracts, not vendor APIs. Revalidate them after an
emulator update before depending on a new release.
