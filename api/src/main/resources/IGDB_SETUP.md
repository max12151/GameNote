# IGDB setup

## Environment variables

```bash
export IGDB_CLIENT_ID="your_twitch_client_id"
export IGDB_CLIENT_SECRET="your_twitch_client_secret"
export IGDB_BASE_URL="https://api.igdb.com/v4"
```

Never commit the secret or the access token. Create the application at https://dev.twitch.tv/console/apps, enable Twitch 2FA, register a confidential application, and generate a client secret.

The API obtains the application access token automatically from `https://id.twitch.tv/oauth2/token` and caches it in memory until one minute before expiration.

## Test

Start the backend, then call:

```bash
curl "http://localhost:8080/api/igdb/games/search?title=Street%20Fighter%206&limit=5"
```

The endpoint returns normalized game data and public IGDB image URLs. The Twitch Client Secret never leaves the backend.
