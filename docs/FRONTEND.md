# Frontend — EIA SmartFix

React 19 + Vite + TypeScript. Dev server : **http://localhost:3000** (`vite.config.ts`), avec proxy `/api` → `http://localhost:8080`.

## Structure

```
frontend/src/
├── app/              # Router, providers shell, error boundary
├── features/         # Un dossier par domaine métier
│   ├── auth/
│   ├── dashboard/
│   ├── equipment/
│   ├── failures/
│   ├── ai-assistant/
│   ├── search/
│   ├── users/
│   ├── live/
│   ├── profile/
│   └── settings/
├── shared/
│   ├── api/          # Client Axios + façades REST (authApi, failuresApi, …)
│   ├── components/
│   ├── hooks/
│   └── types/
├── design-system/    # Tokens, thème, composants Enterprise*
├── layouts/          # AppLayout (shell navigué)
└── test/             # Setup Vitest
```

Alias TypeScript : `@/` → `src/`.

## `shared/api`

- `client.ts` — instance Axios, injection JWT, refresh sur 401/403, clear session.
- `baseUrl.ts` — en `import.meta.env.DEV`, base URL vide (proxy Vite) pour éviter CORS ; sinon `VITE_API_URL`.
- `index.ts` — API typées par domaine (`authApi`, `equipmentApi`, `aiApi`, etc.).

## TanStack Query

`QueryClientProvider` est monté dans `app/App.tsx`. Les hooks métier (`useFailuresList`, `useEquipmentList`, `useUsers`, …) utilisent `useQuery` / `useMutation` avec invalidation ciblée après écriture.

## Design system

Sous `design-system/` : tokens, `ThemeProvider`, animations, et composants réutilisables (`EnterpriseButton`, `EnterpriseTable`, `EnterpriseModal`, …), exposés via `design-system/index.ts`. Enveloppé par `DesignSystemProviders` à la racine de l'app.

## Auth

- `features/auth/context/AuthContext.tsx` — login / logout, bootstrap session (refresh), `hasRole`.
- `ProtectedRoute` — garde les routes authentifiées (et optionnellement par rôle, ex. `/users` → `ADMIN`).
- Tokens et utilisateur en `localStorage` (`accessToken`, `refreshToken`, `user`).
- Login : `POST /api/v1/auth/login` ; refresh : `POST /api/v1/auth/refresh`.

## Variables d'environnement

Voir `frontend/.env.example` :

- `VITE_API_URL` — laisser vide en local (proxy) ; renseigner seulement si l'API n'est pas proxifiée.
