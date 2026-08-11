# Repair UTF-8 corruption in FullRag enrichment data (M??canique, R??ducteur, etc.).
# Purges enrichment rows and re-imports SQL files via docker cp (UTF-8 safe on Windows).
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Password = "Password123!",
    [switch]$SkipReindex,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PgContainer = "eia-postgres"
$PgUser = "eia_user"
$PgDb = "eia_smartfix"

function Get-DbCount([string]$Sql) {
    $raw = docker exec $PgContainer psql -U $PgUser -d $PgDb -t -A -c $Sql 2>&1
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL query failed: $raw" }
    return [int]($raw.Trim())
}

function Invoke-DockerSql([string]$SqlFile) {
    $path = Join-Path $ScriptDir $SqlFile
    if (-not (Test-Path $path)) { throw "SQL file not found: $path" }
    $containerPath = "/tmp/_import_$([Guid]::NewGuid().ToString('N')).sql"
    docker cp $path "${PgContainer}:${containerPath}" 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "docker cp failed: $SqlFile" }
    docker exec $PgContainer psql -U $PgUser -d $PgDb -v ON_ERROR_STOP=1 -f $containerPath 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "SQL failed: $SqlFile" }
    docker exec $PgContainer rm -f $containerPath 2>&1 | Out-Null
}

function Login([string]$Email, [string]$Pwd) {
    $body = @{ email = $Email; password = $Pwd } | ConvertTo-Json
    return (Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method POST -ContentType "application/json" -Body $body).accessToken
}

Write-Host "=== Repair encodage UTF-8 (FullRag) ===" -ForegroundColor Cyan

$encodingIssues = Get-DbCount @"
SELECT COUNT(*) FROM (
  SELECT 1 FROM equipment WHERE famille LIKE '%?%' OR designation LIKE '%?%'
  UNION ALL
  SELECT 1 FROM interventions WHERE cause_racine LIKE '%?%' OR description LIKE '%?%'
) t;
"@

Write-Host "Lignes avec '?' (encodage suspect) : $encodingIssues" -ForegroundColor $(if ($encodingIssues -gt 0) { "Yellow" } else { "Green" })

if ($encodingIssues -eq 0) {
    $pti = Get-DbCount "SELECT COUNT(*) FROM equipment WHERE code = 'PTI-056';"
    if ($pti -gt 0) {
        Write-Host "Aucun probleme detecte et enrichissement present - rien a faire." -ForegroundColor Green
        exit 0
    }
    Write-Host "Pas de corruption detectee mais enrichissement absent - reimport prevu." -ForegroundColor Yellow
}

if ($DryRun) {
    Write-Host "DryRun : purge + reimport non executes." -ForegroundColor Gray
    exit 0
}

Write-Host "`n1. Purge donnees FullRag..." -ForegroundColor Yellow
Invoke-DockerSql "purge-enrichment-data.sql"
Write-Host "  purge OK" -ForegroundColor Green

Write-Host "`n2. Reimport enrichment (UTF-8 via docker cp)..." -ForegroundColor Yellow
Invoke-DockerSql "enrichment-equipment.sql"
Write-Host "  equipements OK" -ForegroundColor Green
Invoke-DockerSql "enrichment-failures.sql"
Write-Host "  pannes OK" -ForegroundColor Green
Invoke-DockerSql "enrichment-interventions.sql"
Write-Host "  interventions OK" -ForegroundColor Green

$remaining = Get-DbCount @"
SELECT COUNT(*) FROM (
  SELECT 1 FROM equipment WHERE famille LIKE '%?%' OR designation LIKE '%?%'
  UNION ALL
  SELECT 1 FROM interventions WHERE cause_racine LIKE '%?%' OR description LIKE '%?%'
) t;
"@

if ($remaining -gt 0) {
    Write-Host "`nECHEC : $remaining lignes avec '?' restantes." -ForegroundColor Red
    exit 1
}
Write-Host "`nEncodage OK : 0 ligne corrompue." -ForegroundColor Green

if (-not $SkipReindex) {
    Write-Host "`n3. Reindexation RAG..." -ForegroundColor Yellow
    try {
        $token = Login "admin@ocp.ma" $Password
        $result = Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/knowledge/reindex" -Method POST -Headers @{ Authorization = "Bearer $token" }
        Write-Host "  Reindex OK - processed: $($result.processed), indexed: $($result.indexed)" -ForegroundColor Green
    } catch {
        Write-Host "  Reindex via API echoue: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host "`nRepair termine." -ForegroundColor Green
