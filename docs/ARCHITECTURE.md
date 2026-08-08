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
maintenance.domain ↛ knowledge (découplage par événements)
knowledge.domain ↛ spring.ai
```

Vérifié par `ArchitectureTest` (ArchUnit).

## Module Knowledge (ports)

| Port | Implémentation actuelle | Futures |
|------|-------------------------|---------|
| `EmbeddingProviderPort` | `OllamaEmbeddingAdapter` | OpenAI, Gemini |
| `LlmProviderPort` | `OllamaLlmAdapter` | OpenAI, Gemini |
| `VectorStorePort` | `PgVectorStoreAdapter` | — |

Activation : `app.knowledge.enabled=true` (profil `ai`).  
Provider : `app.knowledge.provider=ollama|openai|gemini` (défaut `ollama`).

En dev avec profil `ai`, les interventions `VALIDEE` existantes sont réindexées au démarrage (`KnowledgeDevReindexConfiguration`).

## Workflow intervention

```
BROUILLON → SOUMISE → VALIDEE | REJETEE
```

Règles dans `InterventionWorkflow` (domaine pur, testé unitairement).

À la validation → `InterventionValidatedEvent` → indexation RAG async **après commit** (`@TransactionalEventListener(AFTER_COMMIT)`).

Rejet ou suppression → `InterventionKnowledgeRemovedEvent` → désindexation pgvector.

Contenu indexé : symptômes, cause racine, analyse, actions correctives, pièces, description (pas les fichiers documents).

## Frontend

```
src/
├── app/           # Routes, bootstrap
├── features/      # Un dossier par domaine métier
├── shared/        # API, composants, types
└── layouts/
```

Alias TypeScript : `@/` → `src/`
