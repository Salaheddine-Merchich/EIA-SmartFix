# EIA SmartFix — Plateforme de gestion intelligente des pannes EIA (OCP)

Plateforme web collaborative pour la gestion, l'historisation et l'assistance IA des pannes du service **Électricité, Instrumentation et Automatisme (EIA)**.

## Stack technique

| Couche | Technologies |
|--------|-------------|
| Frontend | React 18, Vite, Tailwind CSS, Axios, React Router |
| Backend | Java 21, Spring Boot 3.3, Spring Security JWT, JPA, Flyway, MapStruct, Spring AI |
| Base de données | PostgreSQL 16 + pgvector |
| IA | Ollama (nomic-embed-text + llama3.2) — RAG on-premise |
| Déploiement | Docker Compose |

## Architecture

Voir [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) pour le découpage modulaire, les ports RAG et les règles de dépendance.

## Démarrage rapide

### Prérequis

- Docker & Docker Compose
- Java 21 + Maven (dev backend local)
- Node.js 20+ (dev frontend local)

### Docker (recommandé)

```bash
cp .env.example .env
docker compose up -d postgres
# Attendre PostgreSQL, puis :
docker compose up -d backend frontend
# Phase IA (optionnel) :
docker compose --profile ai up -d ollama
docker exec eia-ollama ollama pull nomic-embed-text
docker exec eia-ollama ollama pull llama3.2
docker compose up -d backend  # avec SPRING_PROFILES_ACTIVE=dev,ai
```

- Frontend : http://localhost:80
- Backend API : http://localhost:8080
- Swagger : http://localhost:8080/swagger-ui.html

### Développement local

```bash
# PostgreSQL
docker compose up -d postgres

# Backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd frontend
npm install
npm run dev
```

## Comptes de démonstration

Ces comptes sont créés par les migrations de seed et destinés **exclusivement au développement local**. Ils ne doivent jamais être déployés : en production, les utilisateurs sont créés via l'API d'administration et aucun mot de passe n'est versionné.

| Email | Mot de passe | Rôle |
|-------|-------------|------|
| admin@ocp.ma | Password123! | Administrateur |
| responsable@ocp.ma | Password123! | Responsable EIA |
| technicien@ocp.ma | Password123! | Technicien |

## Architecture

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

## API principale

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/auth/login` | Authentification JWT |
| `/api/v1/equipment` | CRUD équipements + historique |
| `/api/v1/failures` | Gestion des pannes |
| `/api/v1/interventions` | Interventions + validation + documents |
| `/api/v1/search` | Recherche full-text |
| `/api/v1/dashboard` | MTBF, MTTR, statistiques |
| `/api/v1/ai/assist` | Assistance RAG (profil ai) |

## Licence

Projet de stage OCP — usage interne.
