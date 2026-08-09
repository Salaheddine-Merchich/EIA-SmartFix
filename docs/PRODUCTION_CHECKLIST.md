# Checklist Production Ready — EIA SmartFix

- [ ] `JWT_SECRET` fort obligatoire (≥ 32 chars), pas de placeholder
- [ ] `POSTGRES_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` non défaut
- [ ] Profil `prod` : swagger off, SQL log off, `ddl-auto: validate`
- [ ] `CORS_ALLOWED_ORIGINS` explicites (pas de wildcards)
- [ ] Ollama/knowledge cohérents : `KNOWLEDGE_ENABLED=true` seulement si Ollama joignable
- [ ] Events live vérifiés (validation intervention → feed ; IA down → badge)
- [ ] Liste pannes sans N+1 (batch stats)
- [ ] Stream assist stable (>30s si LLM lent — timeout = llm-timeout)
- [ ] Reindex admin sans saturer le pool Hikari
- [ ] Backup DB + procédure restore documentées
- [ ] Seeds démo absents ou désactivés en prod (`DevDataInitializer` hors prod)
- [ ] CI : `mvn verify` + `npm test` + `npm run build`
- [ ] Monitoring health + métriques RAG
- [x] JWT SSE via Bearer (`fetch` stream) — query `access_token` retiré ([SECURITY.md](SECURITY.md))
- [ ] Runbook incident IA (circuit breaker, fallback, Ollama down)

## Démarrage Docker minimal (sans IA)

```bash
cp .env.example .env   # renseigner POSTGRES_PASSWORD, JWT_SECRET, SPRING_DATASOURCE_PASSWORD
# Ne pas définir SPRING_DATASOURCE_URL / OLLAMA_BASE_URL (défauts Compose)
docker compose up -d postgres backend frontend
```

## Démarrage avec RAG

```bash
# .env : SPRING_PROFILES_ACTIVE=dev,ai  KNOWLEDGE_ENABLED=true
# optionnel : OLLAMA_BASE_URL=http://ollama:11434
docker compose --profile ai up -d
docker exec eia-ollama ollama pull nomic-embed-text
docker exec eia-ollama ollama pull llama3.2
```

## Smoke démo OCP (10 min)

**Prérequis machine présentation**

```bash
cp .env.example .env
# Renseigner POSTGRES_PASSWORD, JWT_SECRET, SPRING_DATASOURCE_PASSWORD (mêmes valeurs DB)
# Ne PAS définir SPRING_DATASOURCE_URL ni OLLAMA_BASE_URL pour Compose
```

**Garde-fous script présentation (environnement démo = profil `dev`, pas prod)**

- Ne **pas** redémarrer / `docker compose restart` le backend pendant que le feed live est projeté (la reconnexion SSE est automatique, mais évite un trou visible).
- Privilégier **assist sync** (POST) ; stream OK si Ollama réactif (nginx `proxy_read_timeout` ≥ 200s).
- **Ne pas** lancer `POST /api/v1/admin/knowledge/reindex` pendant la démo.
- Si badge IA / RAG dégradé : ne pas ouvrir « Analyse IA » défauts récurrents ; montrer la liste agrégée seule.
- Démo desktop (sidebar complète) ; narratif honnête : stack locale / démo, pas « production ».

1. `docker compose up -d postgres backend frontend` — login compte seed local (`admin@ocp.ma`)
2. Liste pannes → ouvrir une panne → interventions visibles
3. Créer / soumettre / valider une intervention (rôle adapté) → feed **live** : StatusBar « Temps réel actif (SSE) » + événements
4. Couper le backend brièvement : StatusBar passe en erreur / déconnecté puis **reconnexion** (badge SSE aligné sur l’état client)
5. Sans AI : `KNOWLEDGE_ENABLED=false` — pas d’erreurs Ollama en boucle
6. Avec AI (`--profile ai` + `KNOWLEDGE_ENABLED=true` + modèles pull) : **assist sync** répond
7. Search + dashboard recurring defects (liste) ; analyse IA seulement si Ollama sain
8. Upload document sur brouillon OK ; sur intervention **validée** → refus
9. Export PDF intervention (token encore valide)
