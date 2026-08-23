# MCON Bridge

MCON Bridge is an Android companion app that builds a controller-friendly game
catalog for MCON and launches individual games in:

- Azahar MCON
- Artemis
- Kirin

It stores only launch metadata and cached artwork. It never writes emulator
saves or modifies game folders.

## Status

Initial implementation is in progress on `feat/initial-implementation`.

## Launch contract

MCON entries point to stable bridge URLs:

```text
mconbridge://launch/<game-uuid>
```

The UUID resolves to a locally stored, allow-listed emulator adapter. Raw Kirin
paths and Artemis host identifiers are never exposed in the MCON URL.

## Save safety

- Azahar save data remains owned by Azahar.
- Artemis save data remains on the streaming host.
- Kirin remains the official signed application. MCON Bridge only reads the
  folder selected by the user and sends Kirin its supported shortcut intent.
- Back up `/storage/emulated/0/Kirin` before first-device compatibility tests.

## Building

Install Android SDK 36 and JDK 17, then run:

```bash
./gradlew test assembleDebug
```

## License

Apache-2.0. Box art remains subject to its original copyright and provider
terms; the app records the provider and attribution for every downloaded image.
