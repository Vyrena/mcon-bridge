# MCON Bridge

MCON Bridge is an Android companion app that builds a controller-friendly game
catalog for MCON and launches individual games in:

- ordinary Azahar
- Artemis
- Kirin

It stores only launch metadata and cached artwork. It never writes emulator
saves or modifies game folders.

## What works

- Select one or more ROM files already used by ordinary Azahar. The bridge keeps
  Android read permission, reads the ROM header for GameTDB cover matching, and
  forwards the selected ROM to Azahar at launch.
- Import one or more Artemis `.art` launcher files.
- Read-only scan of any Kirin game-library folder in internal shared storage,
  or of a single game folder selected with Android's system folder picker.
- Stable per-game launch links, backup/restore, local cover selection, and
  online cover search through GameTDB, Libretro Thumbnails, and SteamGridDB.
- Save any selected cover to a user-chosen device folder for manual selection
  in launchers that do not consume the bridge's shared artwork URI.
- Artwork results show their real width:height ratio and use uncropped previews.
- MCON-targeted bulk sharing when MCON advertises the public bridge MIME type,
  with a share-sheet and copy-link fallback when it does not.

## Launch contract

MCON entries point to stable bridge URLs:

```text
mconbridge://launch/<game-uuid>
```

The UUID resolves to a locally stored, allow-listed emulator adapter. Raw Kirin
paths and Artemis host identifiers are never exposed in the MCON URL.

## Save safety

- Azahar save data remains owned by ordinary Azahar. The bridge never opens its
  user-data or save directories.
- Artemis save data remains on the streaming host.
- Kirin remains the official signed application. MCON Bridge only reads the
  folder selected by the user and sends Kirin its supported shortcut intent.
- Back up Kirin's existing data before first-device compatibility tests.

Removing a game from MCON Bridge deletes only its catalog row. It never deletes
a ROM, streamed app, emulator configuration, or save file.

Bridge backups contain catalog metadata, not cached cover image bytes. A cover
whose original URI is unavailable after restore is cleared safely and can be
downloaded or selected again.

## Setup

1. Install MCON Bridge and the matching emulator app.
2. Use **Add Azahar ROMs**, **Artemis .art**, or **Scan Kirin**. For Kirin,
   choose either a folder containing game folders or one game folder itself;
   `/Kirin/games` is not required.
3. Optionally choose local art or search online. GameTDB works from an exact 3DS
   product code; SteamGridDB requires your own API key, which is encrypted with
   Android Keystore.
4. Tap **Export to MCON**. If your MCON build does not advertise the bulk-import
   MIME type, use the per-game copy button and paste the stable link into MCON.

See [compatibility details](docs/COMPATIBILITY.md), the
[legacy Azahar metadata schema](docs/AZAHAR_EXPORT_FORMAT.md), and the
[MCON handoff schema](docs/MCON_IMPORT_FORMAT.md).

## Building

Install Android SDK 36 and JDK 17, then run:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

For an update-compatible release build, provide `MCON_BRIDGE_KEYSTORE`,
`MCON_BRIDGE_STORE_PASSWORD`, `MCON_BRIDGE_KEY_ALIAS`, and
`MCON_BRIDGE_KEY_PASSWORD`, then run `./gradlew assembleRelease`. Never commit
the signing key: every future APK installed as an update must use the same key.

## License

Apache-2.0. Box art remains subject to its original copyright and provider
terms; the app records the provider and attribution for every downloaded image.
