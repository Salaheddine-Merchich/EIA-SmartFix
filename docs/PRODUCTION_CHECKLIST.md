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
- [ ] Plan sortie JWT query string suivi ([SECURITY.md](SECURITY.md))
- [ ] Runbook incident IA (circuit breaker, fallback, Ollama down)

## Démarrage Docker minimal (sans IA)

```bash
cp .env.example .env   # renseigner POSTGRES_PASSWORD, JWT_SECRET, SPRING_DATASOURCE_PASSWORD
docker compose up -d postgres backend frontend
```

## Démarrage avec RAG

```bash
# .env : SPRING_PROFILES_ACTIVE=dev,ai  KNOWLEDGE_ENABLED=true
docker compose --profile ai up -d
docker exec eia-ollama ollama pull nomic-embed-text
docker exec eia-ollama ollama pull llama3.2
```
