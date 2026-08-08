# Script complet d'enrichissement RAG - EIA SmartFix
# Exécute les scripts SQL et réindexe le RAG
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

Write-Host "=== Enrichissement RAG EIA SmartFix ===" -ForegroundColor Green

$env:PGPASSWORD = $DbPassword

try {
    # Vérifier connexion PostgreSQL
    Write-Host "Vérification connexion PostgreSQL..." -ForegroundColor Yellow
    try {
        $testConnection = psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT COUNT(*) FROM equipment;" -t
        Write-Host "✓ Connexion PostgreSQL OK - $($testConnection.Trim()) équipements existants" -ForegroundColor Green
    } catch {
        Write-Host "✗ Erreur connexion PostgreSQL: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }

    # 1. Créer les nouveaux équipements
    Write-Host "`n1. Création des nouveaux équipements..." -ForegroundColor Yellow
    try {
        psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -f (Join-Path $ScriptDir "enrichment-equipment.sql")
        $equipmentCount = psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT COUNT(*) FROM equipment;" -t
        Write-Host "✓ Équipements créés - Total: $($equipmentCount.Trim())" -ForegroundColor Green
    } catch {
        Write-Host "✗ Erreur création équipements: $($_.Exception.Message)" -ForegroundColor Red
    }

    # 2. Créer les pannes
    Write-Host "`n2. Création des pannes..." -ForegroundColor Yellow
    try {
        psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -f (Join-Path $ScriptDir "enrichment-failures.sql")
        $failuresCount = psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT COUNT(*) FROM failures;" -t
        Write-Host "✓ Pannes créées - Total: $($failuresCount.Trim())" -ForegroundColor Green
    } catch {
        Write-Host "✗ Erreur création pannes: $($_.Exception.Message)" -ForegroundColor Red
    }

    # 3. Créer les interventions (UUIDs valides — fichier consolidé)
    Write-Host "`n3. Création des interventions..." -ForegroundColor Yellow
    try {
        psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -f (Join-Path $ScriptDir "enrichment-interventions.sql")
        $interventionsCount = psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT COUNT(*) FROM interventions WHERE statut_validation = 'VALIDEE';" -t
        Write-Host "✓ Interventions créées - Validées: $($interventionsCount.Trim())" -ForegroundColor Green
    } catch {
        Write-Host "✗ Erreur création interventions: $($_.Exception.Message)" -ForegroundColor Red
    }

    # 4. Attendre que le serveur backend soit prêt pour le reindex
    Write-Host "`n4. Attente serveur backend prêt..." -ForegroundColor Yellow
    $maxRetries = 10
    $retryCount = 0

    do {
        try {
            $response = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                Write-Host "✓ Serveur backend prêt" -ForegroundColor Green
                break
            }
        } catch {
            $retryCount++
            Write-Host "Tentative $retryCount/$maxRetries - Serveur pas encore prêt..." -ForegroundColor Gray
            Start-Sleep -Seconds 3
        }
    } while ($retryCount -lt $maxRetries)

    if ($retryCount -eq $maxRetries) {
        Write-Host "✗ Serveur backend non disponible après $maxRetries tentatives" -ForegroundColor Red
        Write-Host "Le reindex devra être fait manuellement via: POST /api/v1/admin/knowledge/reindex" -ForegroundColor Yellow
        exit 1
    }

    # 5. Authentification et reindex RAG
    Write-Host "`n5. Réindexation RAG..." -ForegroundColor Yellow
    try {
        $loginBody = @{
            email = "admin@ocp.ma"
            password = $AdminPassword
        } | ConvertTo-Json

        $loginResponse = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method POST -ContentType "application/json" -Body $loginBody -TimeoutSec 10
        $headers = @{
            "Authorization" = "Bearer $($loginResponse.accessToken)"
            "Content-Type" = "application/json"
        }

        $reindexResponse = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/knowledge/reindex" -Method POST -Headers $headers -TimeoutSec 30
        Write-Host "✓ Réindexation terminée:" -ForegroundColor Green
        Write-Host "  - Traitées: $($reindexResponse.processed)" -ForegroundColor Cyan
        Write-Host "  - Indexées: $($reindexResponse.indexed)" -ForegroundColor Cyan
        Write-Host "  - Ignorées: $($reindexResponse.skipped)" -ForegroundColor Cyan
        Write-Host "  - Erreurs: $($reindexResponse.errors)" -ForegroundColor Cyan

    } catch {
        Write-Host "✗ Erreur réindexation: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Réindexation manuelle nécessaire via API admin" -ForegroundColor Yellow
    }

    # 6. Vérification finale
    Write-Host "`n6. Vérification finale..." -ForegroundColor Yellow
    try {
        $finalStats = psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "
            SELECT 
                'Équipements: ' || COUNT(*) FROM equipment
            UNION ALL SELECT 
                'Pannes: ' || COUNT(*) FROM failures  
            UNION ALL SELECT
                'Interventions validées: ' || COUNT(*) FROM interventions WHERE statut_validation = 'VALIDEE'
            UNION ALL SELECT
                'Embeddings: ' || COUNT(*) FROM intervention_embeddings;" -t

        Write-Host "✓ Statistiques finales:" -ForegroundColor Green
        $finalStats | ForEach-Object { Write-Host "  $($_)" -ForegroundColor Cyan }

    } catch {
        Write-Host "✗ Erreur vérification finale: $($_.Exception.Message)" -ForegroundColor Red
    }

    Write-Host "`n=== Enrichissement RAG terminé ===" -ForegroundColor Green
    Write-Host "Le RAG contient maintenant des données diversifiées pour tester l'assistant IA." -ForegroundColor White
    Write-Host "Testez avec des requêtes comme:" -ForegroundColor White
    Write-Host "- 'Capteur pression signal instable'" -ForegroundColor Gray
    Write-Host "- 'Débitmètre aucun signal électromagnétique'" -ForegroundColor Gray
    Write-Host "- 'Automate PLC communication perdue'" -ForegroundColor Gray
    Write-Host "- 'Contacteur ne ferme plus moteur'" -ForegroundColor Gray
    Write-Host "- 'Réducteur vibrations huile chaude'" -ForegroundColor Gray
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
