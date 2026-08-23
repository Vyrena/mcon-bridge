# Azahar library export format

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

`productCode`, `region`, and `artworkUri` are optional. Product code enables an
exact GameTDB lookup; without it, users can search SteamGridDB or choose a local
image. The importer stores only launch metadata and never opens an Azahar user
or save-data directory.
