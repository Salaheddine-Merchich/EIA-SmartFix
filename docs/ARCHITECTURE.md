# Architecture EIA SmartFix

## Style retenu

**Modular Monolith + Hexagonal Architecture + DDD tactique**

- Un seul déploiement (JAR + PostgreSQL + Ollama)
- Modules métier indépendants sous `com.ocp.eia.modules.*`
- Ports/adapters pour l'IA et le stockage documentaire
- Événements Spring pour découpler Maintenance ↔ Knowledge

## Modules backend

| Module | Package | Responsabilité |
|--------|---------|----------------|
| **IAM** | `modules/iam/application/*UseCase` | JWT, utilisateurs |
| **Asset** | `modules/asset/application/*UseCase` | Référentiel équipements |
| **Maintenance** | `modules/maintenance/*` | Workflow intervention, documents, événements |
| **Knowledge** | `modules/knowledge/*` | RAG, indexation, recherche full-text + vectorielle |
| **Analytics** | `modules/analytics/application/DashboardUseCase` | MTBF, MTTR, KPIs |

## Règles de dépendance

```
presentation → application → domain
infrastructure → implémente les ports
application → shared.exception (pas presentation.exception)
maintenance.domain ↛ knowledge (découplage par événements)
knowledge.domain ↛ spring.ai
```

Vérifié par `ArchitectureTest` (ArchUnit).

Indexes : `idx_interventions_fts` (V3) retiré en V19 — FTS interventions via `search_vector` (V16). Listes panne/équipement utilisent `ILIKE` pour les trigram V18 ; FTS knowledge aligne `to_tsvector(title) || to_tsvector(content)` sur les GIN V12.

## Module Knowledge (ports)

| Port | Implémentation actuelle | Futures |
|------|-------------------------|---------|
| `EmbeddingProviderPort` | `OllamaEmbeddingAdapter` | OpenAI, Gemini |
| `LlmProviderPort` | `OllamaLlmAdapter` | OpenAI, Gemini |
| `VectorStorePort` | `PgVectorStoreAdapter` | — |

Activation : `app.knowledge.enabled=true` (profil `dev` et/ou config `ai`).  
Provider : `app.knowledge.provider=ollama|openai|gemini` (défaut `ollama`).

En profil `dev` avec knowledge activé, les interventions `VALIDEE` existantes sont réindexées au démarrage (`KnowledgeDevReindexConfiguration`).

## Workflow intervention

```
BROUILLON → SOUMISE → VALIDEE | REJETEE
```

Règles dans `InterventionWorkflow` (domaine pur, testé unitairement).

À la validation → `InterventionValidatedEvent` → indexation RAG async **après commit** (`@TransactionalEventListener(AFTER_COMMIT)`).

Mise à jour d’une panne / d’un équipement dont des champs entrent dans le payload indexé → `InterventionKnowledgeChangedEvent` pour chaque intervention encore `VALIDEE` liée → ré-embed async après commit (les interventions `VALIDEE` restent immuables côté corps d’intervention).

Rejet ou suppression → `InterventionKnowledgeRemovedEvent` → désindexation pgvector.

Contenu indexé : symptômes, cause racine, analyse, actions correctives, pièces, description, plus métadonnées panne/équipement (pas les fichiers documents).

Documents knowledge techniques : pas d’API CRUD de mise à jour — indexation via admin (`POST /api/v1/admin/knowledge/reindex-documents`) ou reindex démarrage `dev`. Le trigger Flyway V15 (`RAISE NOTICE`) reste un no-op applicatif.

## Ports & commandes (rappel)

| Service | Port / URL |
|---------|------------|
| Frontend Vite (local) | http://localhost:3000 |
| Frontend Docker | http://localhost:80 |
| Backend | http://localhost:8080 |
| PostgreSQL (hôte) | `localhost:15432` |
| Ollama | http://localhost:11434 |

```bash
# Local
docker compose up -d postgres
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm run dev

# Docker
docker compose up -d postgres backend frontend
docker compose --profile ai up -d ollama
```

Détail frontend : [FRONTEND.md](FRONTEND.md). Démarrage complet : [README.md](../README.md).

## Frontend

Structure et conventions : [FRONTEND.md](FRONTEND.md).

```
src/
├── app/             # Routes, bootstrap
├── features/        # Un dossier par domaine métier
├── shared/          # API, composants, types
├── design-system/   # Tokens + composants Enterprise*
└── layouts/
```

Alias TypeScript : `@/` → `src/`
