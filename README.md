# EIA SmartFix — Plateforme de gestion intelligente des pannes EIA (OCP)

Plateforme web collaborative pour la gestion, l'historisation et l'assistance IA des pannes du service **Électricité, Instrumentation et Automatisme (EIA)**.

## Stack technique

| Couche | Technologies |
|--------|-------------|
| Frontend | React 19, Vite (port 3000), Tailwind CSS, TanStack Query, Axios, React Router |
| Backend | Java 21, Spring Boot 3.3, Spring Security JWT, JPA, Flyway, MapStruct, Spring AI |
| Base de données | PostgreSQL 16 + pgvector (hôte : port **15432**) |
| IA | Ollama (nomic-embed-text + llama3.2) — RAG on-premise |
| Déploiement | Docker Compose |

## Architecture

Voir [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (backend modulaire) et [docs/FRONTEND.md](docs/FRONTEND.md) (structure React).

Monolithe Spring Boot en couches :

- `presentation` — Controllers REST + Swagger
- `application` — Services métier, DTOs, MapStruct
- `domain` — Entités JPA, repositories
- `infrastructure` — JWT, stockage fichiers, RAG/Ollama

### Workflow RAG

1. Le technicien décrit une panne via `/api/v1/ai/assist`
2. Embedding via Ollama → recherche vectorielle pgvector
3. Seules les interventions **VALIDEE** alimentent la base de connaissances
4. Le LLM génère causes probables, actions, résumé et conseils
5. **Le technicien décide** — pas de diagnostic automatique

## Profils Spring

| Profil | Fichier | Rôle |
|--------|---------|------|
| `dev` | `application-dev.yml` | Datasource locale, CORS Vite, logs détaillés ; active aussi `app.knowledge.enabled=true` et importe `application-ai.yml` |
| `ai` | `application-ai.yml` | Config Ollama / RAG (modèles, timeouts, top-k) |
| `prod` | `application-prod.yml` | Datasource, JWT, CORS et chemins **obligatoires via variables d'environnement** ; Swagger désactivé |

En local : `dev` (ou `dev,ai`). En Docker Compose : `SPRING_PROFILES_ACTIVE=dev,ai` par défaut.

Secrets et config sensible (JWT, mots de passe DB, CORS prod, etc.) viennent de l'environnement — voir `.env.example`. Ne pas committer de `.env` réel.

## Démarrage rapide

### Prérequis

- Docker & Docker Compose
- Java 21 + Maven (dev backend local)
- Node.js 20+ (dev frontend local)

### Docker (recommandé)

```bash
cp .env.example .env
# Ajuster POSTGRES_PASSWORD, JWT_SECRET, etc. dans .env

docker compose up -d postgres
# Attendre le healthcheck PostgreSQL, puis :
docker compose up -d backend frontend

# Phase IA (optionnel — profil Compose "ai") :
docker compose --profile ai up -d ollama
docker exec eia-ollama ollama pull nomic-embed-text
docker exec eia-ollama ollama pull llama3.2
# Redémarrer le backend si besoin (profils Spring : SPRING_PROFILES_ACTIVE=dev,ai)
docker compose up -d backend
```

| Service | URL |
|---------|-----|
| Frontend (nginx) | http://localhost:80 |
| Backend API | http://localhost:8080 |
| Swagger (profil `dev`) | http://localhost:8080/swagger-ui.html |
| Ollama | http://localhost:11434 |
| PostgreSQL (hôte) | `localhost:15432` |

### Développement local

```bash
# PostgreSQL (port hôte 15432)
docker compose up -d postgres

# Backend — profils Spring "dev" (importe la config AI) :
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Variante explicite : -Dspring-boot.run.profiles=dev,ai

# Frontend — Vite sur http://localhost:3000 (proxy /api → :8080)
cd frontend
npm install
npm run dev
```

### CORS

- En **dev Vite**, le proxy (`vite.config.ts`) envoie `/api` vers `http://localhost:8080` : pas de CORS côté navigateur.
- Si le frontend appelle l'API directement, les origines autorisées sont dans `application-dev.yml` (`http://localhost:3000`, `3001`, `80`, etc.) ou via `CORS_ALLOWED_ORIGINS` (voir `.env.example`).
- En **prod**, `CORS_ALLOWED_ORIGINS` est requis (`application-prod.yml`).

## Comptes de démonstration (local-dev uniquement)

Ces comptes sont créés par les migrations de seed et destinés **exclusivement au développement local**. Ils ne doivent jamais être déployés : en production, les utilisateurs sont créés via l'API d'administration et aucun mot de passe n'est versionné.

| Email | Mot de passe | Rôle |
|-------|-------------|------|
| admin@ocp.ma | Password123! | Administrateur |
| responsable@ocp.ma | Password123! | Responsable EIA |
| technicien@ocp.ma | Password123! | Technicien |

## API principale

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/auth/login` | Authentification JWT |
| `/api/v1/equipment` | CRUD équipements + historique |
| `/api/v1/failures` | Gestion des pannes |
| `/api/v1/interventions` | Interventions + validation + documents |
| `/api/v1/search` | Recherche full-text |
| `/api/v1/dashboard` | MTBF, MTTR, statistiques |
| `/api/v1/ai/assist` | Assistance RAG (`app.knowledge.enabled=true`) |

## Scripts d'enrichissement (dev)

Sous `scripts/` : SQL d'enrichissement RAG + `enrich-rag-complete.ps1` (mots de passe **paramétrés**, jamais en dur).

```powershell
cd scripts
.\enrich-rag-complete.ps1 -DbPassword '<db>' -AdminPassword '<admin>'
```

## Licence

Projet de stage OCP — usage interne.
