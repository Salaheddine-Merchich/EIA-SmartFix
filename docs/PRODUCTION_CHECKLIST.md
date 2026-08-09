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

1. `docker compose up -d postgres backend frontend` — login `admin@ocp.ma` / compte seed local
2. Liste pannes → ouvrir une panne → interventions visibles
3. Créer / soumettre / valider une intervention (rôle adapté) → badge/feed **live** réactif
4. Sans AI : pas d’erreurs Ollama en boucle (`KNOWLEDGE_ENABLED=false`)
5. Avec AI (`--profile ai` + `KNOWLEDGE_ENABLED=true`) : assist sync ou stream répond
6. Search + dashboard recurring defects
7. Upload document sur brouillon OK ; sur intervention **validée** → refus
8. Export PDF intervention (token encore valide)
