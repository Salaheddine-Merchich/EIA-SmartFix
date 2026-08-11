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

En local Maven : profil `dev` (ou `dev,ai`). En Docker Compose : défaut **`prod`** (sécurisé). Pour une démo locale avec seeds/Swagger : `cp docker-compose.override.example.yml docker-compose.override.yml` (profil `dev`). RAG : `--profile ai` + `KNOWLEDGE_ENABLED=true` (+ `SPRING_PROFILES_ACTIVE=dev,ai` dans l’override).

Secrets et config sensible (JWT, mots de passe DB, CORS prod, etc.) viennent de l'environnement — voir `.env.example` et [docs/SECURITY.md](docs/SECURITY.md). Ne pas committer de `.env` réel. Compose **exige** `POSTGRES_PASSWORD` et `JWT_SECRET` dans `.env`.

Checklist livraison / démo : [docs/PRODUCTION_CHECKLIST.md](docs/PRODUCTION_CHECKLIST.md).

## Démarrage rapide

### Prérequis

- Docker & Docker Compose
- Java 21 + Maven (dev backend local)
- Node.js 20+ (dev frontend local)

### Docker (recommandé)

```bash
cp .env.example .env
# Renseigner POSTGRES_PASSWORD, JWT_SECRET, SPRING_DATASOURCE_PASSWORD
# Démo locale (Swagger + comptes seed) :
cp docker-compose.override.example.yml docker-compose.override.yml
# Ne PAS définir SPRING_DATASOURCE_URL ni OLLAMA_BASE_URL pour Compose

docker compose up -d postgres
docker compose up -d backend frontend

# Phase IA (optionnel — Ollama interne au réseau Docker, non publié sur l'hôte) :
docker compose --profile ai up -d ollama
docker exec eia-ollama ollama pull nomic-embed-text
docker exec eia-ollama ollama pull llama3.2
# Override / .env : SPRING_PROFILES_ACTIVE=dev,ai  KNOWLEDGE_ENABLED=true
docker compose up -d backend
```

Checklist smoke démo : [docs/PRODUCTION_CHECKLIST.md](docs/PRODUCTION_CHECKLIST.md#smoke-démo-ocp-10-min).


| Service | URL |
|---------|-----|
| Frontend (nginx) | http://127.0.0.1:80 |
| Backend API | http://127.0.0.1:8080 |
| Swagger (profil `dev` / override) | http://127.0.0.1:8080/swagger-ui.html |
| Ollama | réseau Docker uniquement (`http://ollama:11434`) |
| PostgreSQL (hôte) | `127.0.0.1:15432` |

### Développement local

```bash
# PostgreSQL (port hôte 15432)
docker compose up -d postgres

# Backend — définir SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15432/eia_smartfix
# (et password aligné sur Postgres). Profils : dev ; RAG : KNOWLEDGE_ENABLED=true + Ollama local.
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

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

Créés par les seeds Flyway, **désactivés hors profil `dev`** (migration V20). Le profil `dev` (override Compose) les réactive avec le mot de passe ci-dessous. En production : créer les utilisateurs via l’API admin — aucun mot de passe démo.

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

## Persistance des données (Docker)

Les pannes, interventions, utilisateurs et documents sont stockés dans le volume Docker **`postgres_data`** (fichiers uploadés : volume **`document_data`**).

| Action | Données conservées ? |
|--------|----------------------|
| `docker compose restart` / `up` / rebuild image | Oui |
| `docker compose down` (sans `-v`) | Oui |
| `docker compose down -v` ou suppression du volume | **Non** — retour aux seeds Flyway (~3 users démo, 0 pannes) |

**Sauvegarde rapide :**

```bash
docker exec eia-postgres pg_dump -U eia_user eia_smartfix > backup.sql
```

**Restauration :**

```bash
docker exec -i eia-postgres psql -U eia_user -d eia_smartfix < backup.sql
```

**Vérifier le contenu actuel :**

```powershell
.\scripts\check-data.ps1
```

**Restaurer les données RAG après perte de volume (plan B — sans backup) :**

```powershell
.\scripts\restore-rag-data.ps1
```

Crée les 4 utilisateurs manquants, injecte 8 pannes/interventions validées (API), **+10 équipements et +18 pannes/interventions SQL** (FullRag, activé par défaut), puis réindexe le RAG. Mot de passe par défaut : `Password123!`.

Counts attendus après restauration complète : **14 équipements**, **26 pannes**, **26 interventions validées**.

**Vérifier la base RAG (guides + embeddings)** — le KPI « Documents techniques » affiche les guides actifs ; le hint dashboard indique aussi les interventions indexées pour le RAG :

```bash
docker compose -p eiasmartfix --profile ai up -d ollama backend frontend
docker exec eia-ollama ollama list   # llama3.2 + nomic-embed-text
docker exec eia-postgres psql -U eia_user -d eia_smartfix -c "SELECT COUNT(*) FROM knowledge_documents; SELECT COUNT(*) FROM intervention_embeddings;"
```

Attendu après seeds Flyway + restauration : **13** guides (`knowledge_documents`) et **26** embeddings d'interventions (`intervention_embeddings`). Si les embeddings sont incomplets :

```powershell
# JWT admin requis — ou relancer restore-rag-data.ps1 (réindexe en fin de script)
curl -X POST http://127.0.0.1:8080/api/v1/admin/knowledge/reindex -H "Authorization: Bearer <token>"
```

Pour restaurer sans l'enrichissement SQL : `.\scripts\restore-rag-data.ps1 -NoFullRag`

**Backups datés (recommandé après restauration ou avant `docker compose down -v`) :**

```powershell
mkdir backups -Force
docker exec eia-postgres pg_dump -U eia_user eia_smartfix > backups/backup-$(Get-Date -Format yyyyMMdd).sql
```

Les fichiers `backups/*.sql` sont ignorés par Git (données locales).

Ne pas définir `SPRING_DATASOURCE_URL=localhost:15432` dans le conteneur backend Docker — utiliser le service Compose `postgres:5432`.

## Scripts d'enrichissement (dev)

Sous `scripts/` : SQL d'enrichissement RAG + `enrich-rag-complete.ps1` (mots de passe **paramétrés**, jamais en dur).

```powershell
cd scripts
.\enrich-rag-complete.ps1 -DbPassword '<db>' -AdminPassword '<admin>'
```

## Licence

Projet de stage OCP — usage interne.
