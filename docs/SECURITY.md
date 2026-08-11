# Sécurité — EIA SmartFix

## Authentification

- Access JWT + refresh JWT avec `jti` persisté (`refresh_tokens`) ; logout révoque le refresh.
- Cookies HttpOnly `eia_access` / `eia_refresh` (SameSite=Lax) ; le filtre accepte aussi `Authorization: Bearer`.
- Le frontend n’enregistre **pas** les JWT dans `localStorage` (profil non secret en `sessionStorage` uniquement).
- SSE / upload utilisent `credentials: 'include'`.

## JWT et Server-Sent Events

Le navigateur `EventSource` ne permet pas d’envoyer un header `Authorization`.
Le frontend utilise `fetch` + `ReadableStream` (`shared/api/sseFetch.ts`) avec cookies HttpOnly.

Le paramètre query `access_token` **n’est plus accepté**.

## Secrets

- Profil `prod` : `JWT_SECRET`, datasource et CORS obligatoires
- Compose : défaut `SPRING_PROFILES_ACTIVE=prod` ; démo locale via `docker-compose.override.yml`
- Comptes seed V4 désactivés par V20 ; profil `dev` les réactive (`DevDataInitializer`)
- Ports Postgres / API / frontend bindés sur `127.0.0.1` ; Ollama non publié

## Documents

- Lecture plant-wide pour rôles TECH/RESP/ADMIN (documenté produit CMMS mono-site)
- Écriture / suppression : `ensureEditable` (ownership technicien)
- Stockage : jail sous `FILE_STORAGE_PATH`, MIME sniff (magic bytes), noms sanitizés

## Swagger

- Désactivé hors profil `dev`
- En `dev` uniquement (sécurité Spring + springdoc)

## Logs

- Pas de contenu de prompts / réponses LLM dans les logs (métriques de taille uniquement)
