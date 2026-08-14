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
| `docker compose down -v` ou suppression du volume | **Non** — retour aux seeds Flyway (users démo + données constructeur PDF V21–V24) |

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

Counts attendus après migrations Flyway V21–V24 (données constructeur PDF) : **14 équipements**, **~119 pannes**, **~119 interventions validées**, **~18 documents** de connaissance.

**Vérifier la base RAG (guides + embeddings)** — le KPI « Documents techniques » affiche les guides actifs ; le hint dashboard indique aussi les interventions indexées pour le RAG :

```bash
docker compose --profile ai up -d ollama backend frontend
docker exec eia-ollama ollama list   # llama3.2 + nomic-embed-text
docker exec eia-postgres psql -U eia_user -d eia_smartfix -c "
  SELECT COUNT(*) AS equipment FROM equipment;
  SELECT COUNT(*) AS failures FROM failures;
  SELECT COUNT(*) AS validated FROM interventions WHERE statut_validation = 'VALIDEE';
  SELECT COUNT(*) AS knowledge_docs FROM knowledge_documents;
  SELECT COUNT(*) AS intervention_emb FROM intervention_embeddings;"
```

Attendu après Flyway V24 + réindexation : **~18** guides (`knowledge_documents`) et **~119** embeddings d'interventions. Si les embeddings sont incomplets :

```powershell
# JWT admin requis
.\scripts\enrich-rag-complete.ps1 -DbPassword '<db>' -AdminPassword 'Password123!'
# ou manuellement :
# POST /api/v1/admin/knowledge/reindex
# POST /api/v1/admin/knowledge/reindex-documents
```

Pour restaurer sans l'enrichissement SQL : `.\scripts\restore-rag-data.ps1 -NoFullRag`

**Backups datés (recommandé après restauration ou avant `docker compose down -v`) :**

```powershell
mkdir backups -Force
docker exec eia-postgres pg_dump -U eia_user eia_smartfix > backups/backup-$(Get-Date -Format yyyyMMdd).sql
```

Les fichiers `backups/*.sql` sont ignorés par Git (données locales).

Ne pas définir `SPRING_DATASOURCE_URL=localhost:15432` dans le conteneur backend Docker — utiliser le service Compose `postgres:5432`.

## Données constructeur PDF (Flyway V21–V24)

Les manuels variateurs (ABB ACS880, Hitachi SJ200, VEICHI SI23, Goodrive 100-PV) alimentent la base via migrations Flyway :

| Migration | Contenu |
|-----------|---------|
| V21 | Suppression équipements démo (MOT-001, VAR-012…) |
| V22 | 14 équipements (filature, convoyage, pompage PV) |
| V23 | ~119 pannes + interventions **VALIDEE** (procédures constructeur) |
| V24 | 5 documents `knowledge_documents` (fault tracing / dépannage) |

Régénérer V23 après modification des données source :

```bash
python scripts/generate-pdf-seed.py
```

**Prérequis RAG :** `KNOWLEDGE_ENABLED=true`, profil `dev` ou `ai`, Ollama avec `nomic-embed-text` et `llama3.2`.

Reset base + import complet :

```powershell
docker compose down -v
docker compose --profile ai up -d --build
docker exec eia-ollama ollama pull nomic-embed-text
docker exec eia-ollama ollama pull llama3.2
# Attendre démarrage backend (Flyway V24) puis réindexer si profil prod :
.\scripts\enrich-rag-complete.ps1 -DbPassword '<db>' -AdminPassword 'Password123!'
```

Requêtes test assistant IA : `2310 overcurrent ACS880 filature`, `E.oC1 VEICHI`, `A.LuT marche à sec`, `OUt1 Goodrive`, `E21 Hitachi SJ200`, `Pompe PV ne démarre plus station solaire` (schémas VEICHI X1 + Goodrive PV).

## Schémas équipement (V25)

Schémas techniques PNG **read-only** (seed PDF), affichés dans l’assistant IA uniquement — **aucun upload utilisateur**.

| Élément | Détail |
|---------|--------|
| Table | `equipment_schemas` (FK `equipment_id`, `trigger_keywords`, `file_path`) |
| Stockage | `{FILE_STORAGE_PATH}/equipment/{equipmentId}/` |
| API | `GET /api/v1/equipment/{id}/schemas/{schemaId}/download` (lecture seule) |
| RAG | `EquipmentSchemaMatcher` — max 3 schémas par requête (keywords + zone/famille) |
| UI | Icône schéma dans la bulle assistant → choix équipement → liste PNG → lightbox |
| Seed | 20 pages extraites des 5 PDFs constructeur (classpath → copie au démarrage) |

Extraire ou régénérer les PNG depuis les PDFs locaux :

```powershell
python scripts/extract-equipment-schemas.py --pdf-dir "C:\Users\<vous>\Desktop\Data OCp"
```

Les fichiers seed sont versionnés sous `backend/src/main/resources/seed/equipment-schemas/`. Au démarrage, `EquipmentSchemaSeedConfiguration` les copie dans le volume documents si absents.

## Scripts RAG (dev)

`scripts/enrich-rag-complete.ps1` — vérifie les comptages post-Flyway et réindexe interventions + documents (mots de passe **paramétrés**).

Les fichiers `enrichment-*.sql` sont **dépréciés** (remplacés par V21–V24).

```powershell
.\scripts\enrich-rag-complete.ps1 -DbPassword '<db>' -AdminPassword '<admin>'
```

## Licence

Projet de stage OCP — usage interne.
