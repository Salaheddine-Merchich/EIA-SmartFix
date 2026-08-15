# Compare dashboard API stats with direct PostgreSQL counts.
param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$Password = "Password123!",
    [string]$PgContainer = "eia-postgres",
    [string]$PgUser = "eia_user",
    [string]$PgDb = "eia_smartfix"
)

$ErrorActionPreference = "Stop"

function Invoke-PgQuery([string]$Sql) {
    $oneLine = ($Sql -replace "`r`n", " " -replace "\s+", " ").Trim()
    docker exec $PgContainer psql -U $PgUser -d $PgDb -t -A -F "|" -c $oneLine
}

function Compare-Metric([string]$Label, $ApiValue, $DbValue) {
    $match = "$ApiValue" -eq "$DbValue"
    $status = if ($match) { "OK" } else { "ECART" }
    $color = if ($match) { "Green" } else { "Red" }
    Write-Host ("  {0,-32} API={1,-8} DB={2,-8} [{3}]" -f $Label, $ApiValue, $DbValue, $status) -ForegroundColor $color
    return $match
}

Write-Host "=== Audit tableau de bord ===" -ForegroundColor Cyan

$loginBody = @{ email = "admin@ocp.ma"; password = $Password } | ConvertTo-Json
$token = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method POST -ContentType "application/json" -Body $loginBody).accessToken
$api = Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard" -Method GET -Headers @{ Authorization = "Bearer $token" }

Write-Host "`n--- Compteurs globaux ---" -ForegroundColor Yellow
$allOk = $true
$allOk = (Compare-Metric "totalFailures" $api.totalFailures (Invoke-PgQuery "SELECT COUNT(*) FROM failures;")) -and $allOk
$allOk = (Compare-Metric "openFailures" $api.openFailures (Invoke-PgQuery "SELECT COUNT(*) FROM failures WHERE statut IN ('OUVERTE','EN_COURS');")) -and $allOk
$allOk = (Compare-Metric "criticalOpenFailures" $api.criticalOpenFailures (Invoke-PgQuery "SELECT COUNT(*) FROM failures WHERE criticite IN ('HAUTE','CRITIQUE') AND statut IN ('OUVERTE','EN_COURS');")) -and $allOk
$allOk = (Compare-Metric "equipmentCount" $api.equipmentCount (Invoke-PgQuery "SELECT COUNT(*) FROM equipment;")) -and $allOk
$allOk = (Compare-Metric "validatedInterventions" $api.validatedInterventions (Invoke-PgQuery "SELECT COUNT(*) FROM interventions WHERE statut_validation = 'VALIDEE';")) -and $allOk
$allOk = (Compare-Metric "pendingValidations" $api.pendingValidations (Invoke-PgQuery "SELECT COUNT(*) FROM interventions WHERE statut_validation = 'SOUMISE';")) -and $allOk
$allOk = (Compare-Metric "draftInterventions" $api.draftInterventions (Invoke-PgQuery "SELECT COUNT(*) FROM interventions WHERE statut_validation = 'BROUILLON';")) -and $allOk
$allOk = (Compare-Metric "rejectedInterventions" $api.rejectedInterventions (Invoke-PgQuery "SELECT COUNT(*) FROM interventions WHERE statut_validation = 'REJETEE';")) -and $allOk
$allOk = (Compare-Metric "knowledgeDocuments" $api.knowledgeDocuments (Invoke-PgQuery "SELECT COUNT(*) FROM knowledge_documents;")) -and $allOk
$allOk = (Compare-Metric "activeKnowledgeDocuments" $api.activeKnowledgeDocuments (Invoke-PgQuery "SELECT COUNT(*) FROM knowledge_documents WHERE active = true;")) -and $allOk
$allOk = (Compare-Metric "activeEquipmentSchemas" $api.activeEquipmentSchemas (Invoke-PgQuery "SELECT COUNT(*) FROM equipment_schemas WHERE active = true;")) -and $allOk
$allOk = (Compare-Metric "indexedInterventions" $api.indexedInterventions (Invoke-PgQuery "SELECT COUNT(*) FROM intervention_embeddings e JOIN interventions i ON i.id = e.intervention_id WHERE i.statut_validation = 'VALIDEE';")) -and $allOk

Write-Host "`n--- Tendance mensuelle (API) ---" -ForegroundColor Yellow
$apiMonthSum = 0
foreach ($item in $api.failuresByMonth) {
    Write-Host ("  {0} : {1}" -f $item.month, $item.count)
    $apiMonthSum += [long]$item.count
}
Write-Host ("  Somme mensuelle API : {0}" -f $apiMonthSum)

Write-Host "`n--- Tendance mensuelle (DB) ---" -ForegroundColor Yellow
$dbMonthLines = Invoke-PgQuery @"
WITH bounds AS (
  SELECT date_trunc('month', MIN(date_heure)) AS start_month,
         date_trunc('month', MAX(date_heure)) AS end_month
  FROM failures
),
months AS (
  SELECT generate_series(start_month, end_month, interval '1 month') AS month
  FROM bounds
  WHERE start_month IS NOT NULL
)
SELECT TO_CHAR(m.month, 'YYYY-MM') AS month,
       COALESCE(COUNT(f.id), 0) AS cnt
FROM months m
LEFT JOIN failures f ON date_trunc('month', f.date_heure) = m.month
GROUP BY m.month
ORDER BY m.month
"@
$dbMonthSum = 0
foreach ($line in ($dbMonthLines -split "`n" | Where-Object { $_.Trim() -ne "" })) {
    $parts = $line -split "\|"
    if ($parts.Length -ge 2) {
        Write-Host ("  {0} : {1}" -f $parts[0], $parts[1])
        $dbMonthSum += [long]$parts[1]
    }
}
Write-Host ("  Somme mensuelle DB  : {0}" -f $dbMonthSum)

$allOk = (Compare-Metric "monthlySum=totalFailures" $apiMonthSum $api.totalFailures) -and $allOk
$allOk = (Compare-Metric "monthlySum API vs DB" $apiMonthSum $dbMonthSum) -and $allOk

Write-Host "`n--- Encodage UTF-8 ---" -ForegroundColor Yellow
$encodingIssues = [int](Invoke-PgQuery @"
SELECT COUNT(*) FROM (
  SELECT 1 FROM equipment WHERE famille LIKE '%?%' OR designation LIKE '%?%'
  UNION ALL
  SELECT 1 FROM interventions WHERE cause_racine LIKE '%?%' OR description LIKE '%?%'
) t;
"@)
Write-Host ("  Lignes avec '?' en base : {0}" -f $encodingIssues) -ForegroundColor $(if ($encodingIssues -eq 0) { "Green" } else { "Red" })
$allOk = (Compare-Metric "encodingIssues" 0 $encodingIssues) -and $allOk

$apiFamilleLabels = ($api.failuresByFamille | ForEach-Object { $_.famille }) -join ", "
if ($apiFamilleLabels -match '\?') {
    Write-Host "  API failuresByFamille contient '?' : $apiFamilleLabels" -ForegroundColor Red
    $allOk = $false
} else {
    Write-Host "  API failuresByFamille sans '?' : OK" -ForegroundColor Green
}

if ($allOk) {
    Write-Host "`nAudit OK : API alignee avec PostgreSQL." -ForegroundColor Green
    exit 0
}

Write-Host "`nAudit ECHEC : ecarts detectes entre API et base." -ForegroundColor Red
exit 1
