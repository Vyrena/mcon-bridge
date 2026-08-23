# MCON Bridge library handoff

MCON Bridge always supports copying individual `mconbridge://launch/<uuid>`
links. For bulk transfer it sends an Android `ACTION_SEND` intent with MIME
type:

```text
application/vnd.vyrena.mconbridge.library+json
```

The attached JSON has schema `com.vyrena.mconbridge.library/1` and contains
only titles, stable bridge links, shareable cover URIs, and cover attribution.
It deliberately excludes emulator payloads, Artemis host details, and Kirin
filesystem paths.

If `com.ohsnap.mconutilities` declares a matching import activity, the intent
is sent directly. Otherwise Android's share sheet is used and the individual
copy-link workflow remains available. This avoids assuming an undocumented
private MCON URI contract.

Example:

```json
{
  "schema": "com.vyrena.mconbridge.library/1",
  "generatedAt": 0,
  "sourceApp": "MCON Bridge",
  "games": [
    {
      "id": "00000000-0000-0000-0000-000000000000",
      "title": "Example",
      "launchUrl": "mconbridge://launch/00000000-0000-0000-0000-000000000000",
      "artworkUri": null,
      "artworkAttribution": null
    }
  ]
}
```
