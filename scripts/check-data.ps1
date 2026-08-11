# Quick sanity check: compare PostgreSQL row counts with what the UI lists depend on.
# Usage: .\scripts\check-data.ps1

$ErrorActionPreference = 'Stop'
$container = 'eia-postgres'
$dbUser = 'eia_user'
$dbName = 'eia_smartfix'

$running = docker ps --format '{{.Names}}' | Select-String -Pattern "^$container$"
if (-not $running) {
    Write-Host "Container '$container' is not running. Start with: docker compose up -d postgres" -ForegroundColor Red
    exit 1
}

$sql = @"
SELECT 'users' AS entity, COUNT(*)::text AS count FROM users
UNION ALL SELECT 'failures', COUNT(*)::text FROM failures
UNION ALL SELECT 'interventions', COUNT(*)::text FROM interventions
UNION ALL SELECT 'interventions_validees', COUNT(*)::text FROM interventions WHERE statut_validation = 'VALIDEE'
UNION ALL SELECT 'knowledge_documents', COUNT(*)::text FROM knowledge_documents
UNION ALL SELECT 'equipment', COUNT(*)::text FROM equipment;
"@

Write-Host ""
Write-Host "=== EIA SmartFix - data counts ($dbName) ===" -ForegroundColor Cyan
docker exec $container psql -U $dbUser -d $dbName -c $sql

Write-Host ""
Write-Host "Notes:" -ForegroundColor Yellow
Write-Host "- Flyway seeds: ~3 demo users, 0 failures (V14 cleanup), ~13 knowledge docs"
Write-Host "- User data persists in volume eiasmartfix_postgres_data unless you run: docker compose down -v"
Write-Host "- Backup: docker exec $container pg_dump -U $dbUser $dbName > backup.sql"
Write-Host "- After full restore (restore-rag-data.ps1): ~7 users, ~14 equipment, ~26 failures, ~26 validated interventions"
Write-Host "- Restore demo RAG: run scripts/restore-rag-data.ps1 (users + API seed + FullRag SQL + reindex)"
Write-Host ""
