# Legacy Azahar metadata export format

This format is retained for backward compatibility only. Ordinary Azahar does
not create it, and metadata-only entries cannot launch until the same ROM is
selected with **Add Azahar ROMs**.

MCON Bridge accepts UTF-8 JSON up to 4 MiB. The root `schema` must be `1`, and
every `titleId` must contain exactly 16 hexadecimal characters.

```json
{
  "schema": 1,
  "games": [
    {
      "title": "Example game",
      "titleId": "0004000000000000",
      "productCode": "CTR-P-XXXX",
      "region": "US",
      "artworkUri": null
    }
  ]
}
```

`productCode` and `region` are optional. Product code can be either GameTDB's
four-character ID or a full 3DS code such as `CTR-P-ECLE`; it enables an exact
GameTDB lookup. Without it, users can search SteamGridDB or choose a local
image. `artworkUri` is accepted for schema compatibility but deliberately not
trusted or imported; cover data must pass the bridge's verified artwork cache.
The importer stores only launch metadata and never opens an Azahar user or
save-data directory.
