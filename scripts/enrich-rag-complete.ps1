# Script de reindexation RAG apres migrations Flyway V21-V24 (donnees constructeur PDF)
#
# DEPRECATED: enrichment-equipment.sql, enrichment-failures.sql, enrichment-interventions.sql
# Les donnees PDF sont desormais chargees via Flyway:
#   V21 cleanup demo, V22 equipment, V23 failures/interventions, V24 knowledge_documents
#
# Usage: appliquer Flyway (demarrage backend ou mvn flyway:migrate), puis:
#   .\scripts\enrich-rag-complete.ps1 -DbPassword <pwd> -AdminPassword Password123!
param(
    [Parameter(Mandatory = $true)][string]$DbPassword,
    [Parameter(Mandatory = $true)][string]$AdminPassword,
    [string]$DbHost = "localhost",
    [string]$DbPort = "15432",
    [string]$DbName = "eia_smartfix",
    [string]$DbUser = "eia_user",
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "=== Reindexation RAG EIA SmartFix (donnees PDF V21-V24) ===" -ForegroundColor Green
Write-Host "Note: enrichment-*.sql est deprecie — utiliser les migrations Flyway V21-V24." -ForegroundColor Yellow

$env:PGPASSWORD = $DbPassword

try {
    Write-Host "`n1. Verification connexion PostgreSQL et comptages..." -ForegroundColor Yellow
    $equipmentCount = (psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT COUNT(*) FROM equipment;" -t).Trim()
    $failuresCount = (psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT COUNT(*) FROM failures;" -t).Trim()
    $interventionsCount = (psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT COUNT(*) FROM interventions WHERE statut_validation = 'VALIDEE';" -t).Trim()
    $docsCount = (psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT COUNT(*) FROM knowledge_documents;" -t).Trim()

    Write-Host "  Equipements: $equipmentCount (attendu ~14)" -ForegroundColor Cyan
    Write-Host "  Pannes: $failuresCount (attendu ~119)" -ForegroundColor Cyan
    Write-Host "  Interventions validees: $interventionsCount (attendu ~119)" -ForegroundColor Cyan
    Write-Host "  Documents connaissance: $docsCount (attendu ~18)" -ForegroundColor Cyan

    if ([int]$equipmentCount -lt 14) {
        Write-Host "ATTENTION: peu d'equipements — verifier que Flyway V22+ est applique." -ForegroundColor Yellow
    }

    Write-Host "`n2. Attente serveur backend..." -ForegroundColor Yellow
    $maxRetries = 15
    $retryCount = 0
    $backendReady = $false

    do {
        try {
            $response = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                Write-Host "Serveur backend pret" -ForegroundColor Green
                $backendReady = $true
                break
            }
        } catch {
            $retryCount++
            Write-Host "Tentative $retryCount/$maxRetries..." -ForegroundColor Gray
            Start-Sleep -Seconds 3
        }
    } while ($retryCount -lt $maxRetries)

    if (-not $backendReady) {
        Write-Host "Backend non disponible. Reindex manuel:" -ForegroundColor Red
        Write-Host "  POST /api/v1/admin/knowledge/reindex" -ForegroundColor Yellow
        Write-Host "  POST /api/v1/admin/knowledge/reindex-documents" -ForegroundColor Yellow
        exit 1
    }

    Write-Host "`n3. Authentification admin..." -ForegroundColor Yellow
    $loginBody = @{ email = "admin@ocp.ma"; password = $AdminPassword } | ConvertTo-Json
    $loginResponse = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method POST -ContentType "application/json" -Body $loginBody -TimeoutSec 15
    $headers = @{
        "Authorization" = "Bearer $($loginResponse.accessToken)"
        "Content-Type"  = "application/json"
    }

    Write-Host "`n4. Reindexation interventions (intervention_embeddings)..." -ForegroundColor Yellow
    $reindexResponse = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/knowledge/reindex" -Method POST -Headers $headers -TimeoutSec 300
    Write-Host "  Traitees: $($reindexResponse.processed), Indexees: $($reindexResponse.indexed), Erreurs: $($reindexResponse.errors)" -ForegroundColor Cyan

    Write-Host "`n5. Reindexation documents connaissance (knowledge_document_embeddings)..." -ForegroundColor Yellow
    $docReindex = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/knowledge/reindex-documents" -Method POST -Headers $headers -TimeoutSec 600
    Write-Host "  Indexees: $($docReindex.indexed), Echecs: $($docReindex.failed), Total: $($docReindex.total)" -ForegroundColor Cyan

    Write-Host "`n6. Verification embeddings..." -ForegroundColor Yellow
    $embedStats = psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "
        SELECT 'intervention_embeddings: ' || COUNT(*) FROM intervention_embeddings
        UNION ALL SELECT 'knowledge_document_embeddings: ' || COUNT(*) FROM knowledge_document_embeddings;" -t
    $embedStats | ForEach-Object { Write-Host "  $($_.Trim())" -ForegroundColor Cyan }

    Write-Host "`n=== Reindexation terminee ===" -ForegroundColor Green
    Write-Host "Requetes test RAG:" -ForegroundColor White
    Write-Host "  - Code 2310 overcurrent variateur filature ABB" -ForegroundColor Gray
    Write-Host "  - E.oC1 surintensite acceleration VEICHI SI23" -ForegroundColor Gray
    Write-Host "  - A.LuT marche a sec pompe solaire" -ForegroundColor Gray
    Write-Host "  - OUt1 protection phase U Goodrive" -ForegroundColor Gray
    Write-Host "  - E21 surchauffe variateur Hitachi SJ200" -ForegroundColor Gray
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
