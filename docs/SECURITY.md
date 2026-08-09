# Sécurité — EIA SmartFix

## JWT et Server-Sent Events (dette connue)

Le navigateur `EventSource` ne permet pas d’envoyer un header `Authorization`.
Pour le live monitoring et le stream assist, le frontend passe donc temporairement :

```
?access_token=<jwt>
```

Le filtre [`JwtAuthenticationFilter`](../backend/src/main/java/com/ocp/eia/infrastructure/security/JwtAuthenticationFilter.java)
accepte ce paramètre en secours après le header Bearer.

### Risques

- Fuite possible via logs proxy, historique navigateur, referer
- En cas de XSS, le token en `localStorage` est déjà exposé

### Plan de sortie (progressif — ne pas casser SSE)

1. Conserver query token pour EventSource (état actuel)
2. Migrer les flux SSE vers `fetch` + `ReadableStream` avec header Bearer, ou cookie `httpOnly` + CSRF
3. Retirer le support `access_token` query une fois le frontend migré
4. Vérifier après chaque étape : feed live + assist stream

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
