# Sécurité — EIA SmartFix

## JWT et Server-Sent Events

Le navigateur `EventSource` ne permet pas d’envoyer un header `Authorization`.
Le frontend utilise donc `fetch` + `ReadableStream` (`shared/api/sseFetch.ts`) avec :

```
Authorization: Bearer <jwt>
```

pour le live monitoring (`GET /api/v1/live/events`) et le stream assist
(`GET /api/v1/ai/assist/stream`). Le paramètre query `access_token` **n’est plus accepté**
par [`JwtAuthenticationFilter`](../backend/src/main/java/com/ocp/eia/infrastructure/security/JwtAuthenticationFilter.java).

### Risques résiduels

- En cas de XSS, le token en `localStorage` reste exposé (inchangé)
- CORS doit autoriser les origines frontend en production

## Secrets

- Profil `prod` : `JWT_SECRET`, datasource et CORS obligatoires (`application-prod.yml`)
- Hors `dev` exclusif : secret placeholder / trop court → fail-fast au démarrage
- Docker Compose : ne pas démarrer sans `.env` pour `JWT_SECRET` et `POSTGRES_PASSWORD`

## Refresh tokens (limitation connue)

Les refresh tokens sont des JWT signés, **sans store de révocation côté serveur**.
Un `logout` ne fait qu’effacer les tokens côté client (`localStorage`) ; un refresh encore valide
reste accepté jusqu’à expiration. Une blacklist / rotation persistante n’est prévue que si
une exigence sécurité produit l’impose.

## Swagger

- Désactivé en `prod`
- En `dev` uniquement (sécurité Spring + springdoc)
