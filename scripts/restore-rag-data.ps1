# Restore RAG demo data when the DB volume was reset (plan B - no backup / old volume).
# Creates missing team users, seeds 8 validated interventions, optional FullRag SQL enrichment, then reindex.
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Password = "Password123!",
    [switch]$SkipReindex,
    [switch]$SkipSeed,
    [switch]$NoFullRag
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PgContainer = "eia-postgres"
$PgUser = "eia_user"
$PgDb = "eia_smartfix"

function Login([string]$Email, [string]$Pwd) {
    $body = @{ email = $Email; password = $Pwd } | ConvertTo-Json
    return (Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method POST -ContentType "application/json" -Body $body).accessToken
}

function Invoke-Api {
    param([string]$Token, [string]$Method, [string]$Path, $Body = $null)
    $params = @{
        Uri         = "$BaseUrl$Path"
        Method      = $Method
        Headers     = @{ Authorization = "Bearer $Token" }
        ContentType = "application/json"
    }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 6) }
    return Invoke-RestMethod @params
}

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

# Users required by seed-rag-interventions.ps1 (Flyway only seeds the first 3).
$RequiredUsers = @(
    @{ email = "mehdi@ocp.ma";       role = "TECHNICIEN";      nomPrenom = "Mehdi El Fassi" },
    @{ email = "mohamad@ocp.ma";     role = "TECHNICIEN";      nomPrenom = "Mohamad Berrada" },
    @{ email = "ahmed@ocp.ma";       role = "TECHNICIEN";      nomPrenom = "Ahmed Tazi" },
    @{ email = "kamal@ocp.ma";       role = "RESPONSABLE_EIA"; nomPrenom = "Kamal Idrissi" }
)

$FullRag = -not $NoFullRag

Write-Host "=== Restore RAG data (plan B) ===" -ForegroundColor Cyan
Write-Host "API: $BaseUrl | FullRag: $FullRag" -ForegroundColor Gray

Write-Host "`n1. Connexion admin..." -ForegroundColor Yellow
$adminToken = Login "admin@ocp.ma" $Password

Write-Host "2. Verification / creation des utilisateurs..." -ForegroundColor Yellow
$existing = Invoke-Api -Token $adminToken -Method GET -Path "/api/v1/users"
$existingEmails = @{}
foreach ($u in $existing) { $existingEmails[$u.email] = $true }

foreach ($user in $RequiredUsers) {
    if ($existingEmails.ContainsKey($user.email)) {
        Write-Host "  existe  $($user.email)" -ForegroundColor DarkGray
        continue
    }
    Invoke-Api -Token $adminToken -Method POST -Path "/api/v1/users" -Body @{
        email     = $user.email
        password  = $Password
        role      = $user.role
        nomPrenom = $user.nomPrenom
        actif     = $true
    } | Out-Null
    Write-Host "  cree    $($user.email) ($($user.role))" -ForegroundColor Green
}

if (-not $SkipSeed) {
    $failureCount = Get-DbCount "SELECT COUNT(*) FROM failures;"
    if ($failureCount -ge 8) {
        Write-Host "`n3. Seed API (8 scenarios) - deja present ($failureCount pannes), skip" -ForegroundColor DarkGray
    } else {
        Write-Host "`n3. Seed des 8 interventions VALIDEE (API)..." -ForegroundColor Yellow
        & (Join-Path $ScriptDir "seed-rag-interventions.ps1") -BaseUrl $BaseUrl -Password $Password
    }
} else {
    Write-Host "`n3. Seed API skip (-SkipSeed)" -ForegroundColor DarkGray
}

if ($FullRag) {
    $enriched = Get-DbCount "SELECT COUNT(*) FROM equipment WHERE code = 'PTI-056';"
    if ($enriched -gt 0) {
        Write-Host "`n4. Enrichissement FullRag - deja present (PTI-056), skip" -ForegroundColor DarkGray
    } else {
        Write-Host "`n4. Enrichissement FullRag (10 equipements + 20 pannes + 18 interventions)..." -ForegroundColor Yellow
        Invoke-DockerSql "enrichment-equipment.sql"
        Write-Host "  equipements OK" -ForegroundColor Green
        Invoke-DockerSql "enrichment-failures.sql"
        Write-Host "  pannes OK" -ForegroundColor Green
        Invoke-DockerSql "enrichment-interventions.sql"
        Write-Host "  interventions OK" -ForegroundColor Green
    }
} else {
    Write-Host "`n4. Enrichissement FullRag desactive (-FullRag:`$false)" -ForegroundColor DarkGray
}

if (-not $SkipReindex) {
    Write-Host "`n5. Reindexation RAG..." -ForegroundColor Yellow
    $adminToken = Login "admin@ocp.ma" $Password
    try {
        $result = Invoke-Api -Token $adminToken -Method POST -Path "/api/v1/admin/knowledge/reindex"
        Write-Host "  Reindex OK - processed: $($result.processed), indexed: $($result.indexed)" -ForegroundColor Green
    } catch {
        Write-Host "  Reindex via API echoue: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "  Relancez le backend (profil dev+ai) ou: POST /api/v1/admin/knowledge/reindex" -ForegroundColor Yellow
    }
}

Write-Host "`n6. Verification..." -ForegroundColor Yellow
& (Join-Path $ScriptDir "check-data.ps1")

Write-Host "Restore termine." -ForegroundColor Green
