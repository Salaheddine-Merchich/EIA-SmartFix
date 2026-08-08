# Seed validated interventions for RAG knowledge base
param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)][string]$Password
)

$ErrorActionPreference = "Stop"

$Equipment = @{
    MOT001 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    VAR012 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
    CAP045 = "cccccccc-cccc-cccc-cccc-cccccccccccc"
    POM008 = "dddddddd-dddd-dddd-dddd-dddddddddddd"
}

$Credentials = @{
    "mehdi@ocp.ma"       = $Password
    "mohamad@ocp.ma"     = $Password
    "technicien@ocp.ma"  = $Password
    "ahmed@ocp.ma"       = $Password
    "kamal@ocp.ma"       = $Password
    "responsable@ocp.ma" = $Password
    "admin@ocp.ma"       = $Password
}

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

Write-Host "=== Test connexions ===" -ForegroundColor Cyan
$Tokens = @{}
foreach ($entry in $Credentials.GetEnumerator()) {
    $Tokens[$entry.Key] = Login $entry.Key $entry.Value
    Write-Host "  OK  $($entry.Key)" -ForegroundColor Green
}

$adminToken = $Tokens["admin@ocp.ma"]
$allUsers = Invoke-Api -Token $adminToken -Method GET -Path "/api/v1/users"
$UserIds = @{}
foreach ($u in $allUsers) { $UserIds[$u.email] = $u.id }

$Scenarios = @(
    @{
        Num = 1; Equipment = $Equipment.MOT001; Code = "F001"; Criticite = "HAUTE"; Zone = "Zone A"
        Description = "Moteur convoyeur A en surchauffe, arret automatique"
        Tech = "mehdi@ocp.ma"; Declarant = "kamal@ocp.ma"; Validator = "kamal@ocp.ma"
        Intervention = @{
            description = "Intervention surchauffe moteur convoyeur A"
            symptomes = "Temperature carter > 85C, ventilateur bruyant, odeur isolation"
            causeRacine = "Roulement arriere grippe par manque de graissage"
            analyseTechnique = "Mesure vibration ISO 10816 depassement zone C. Inspection palier arriere : jeu axial excessif."
            actionsCorrectives = "Remplacement roulement SKF 6312, graissage paliers, controle alignement"
            piecesRemplacees = "Roulement SKF 6312, graisse LGMT2"
            dureeArretMinutes = 240; tempsInterventionMinutes = 180
        }
        Comment = "Intervention conforme"
    },
    @{
        Num = 2; Equipment = $Equipment.VAR012; Code = "E001"; Criticite = "MOYENNE"; Zone = "Zone B"
        Description = "Variateur ABB affiche defaut surintensite ligne 2"
        Tech = "mohamad@ocp.ma"; Declarant = "responsable@ocp.ma"; Validator = "responsable@ocp.ma"
        Intervention = @{
            description = "Correction defaut surintensite variateur ABB ACS880"
            symptomes = "Code E001 au demarrage, courant phase R eleve"
            causeRacine = "Parametrage limite courant moteur incorrect apres remplacement moteur"
            analyseTechnique = "Verification cablage et parametres 99.06/99.09. Courant nominal moteur 42A vs param 32A."
            actionsCorrectives = "Ajustement parametres moteur, reset defauts, test charge nominale"
            dureeArretMinutes = 90; tempsInterventionMinutes = 45
        }
        Comment = "Parametrage corrige et teste"
    },
    @{
        Num = 3; Equipment = $Equipment.CAP045; Code = "S004"; Criticite = "CRITIQUE"; Zone = "Zone C"
        Description = "Capteur niveau silo 3 signal errone, risque debordement"
        Tech = "technicien@ocp.ma"; Declarant = "kamal@ocp.ma"; Validator = "kamal@ocp.ma"
        Intervention = @{
            description = "Diagnostic capteur niveau silo 3 Endress+Hauser"
            symptomes = "Mesure figee a 100 pourcent, alarme niveau haut malgre vidange"
            causeRacine = "Encrassement sonde radar par poussiere phosphate"
            analyseTechnique = "Inspection visuelle : depot calcaire sur antenne. Test loop 4-20mA OK cote automate."
            actionsCorrectives = "Nettoyage sonde, recalibrage distance vide/plein, remise en service"
            piecesRemplacees = "Kit nettoyage sonde radar"
            dureeArretMinutes = 360; tempsInterventionMinutes = 120
        }
        Comment = "Priorite securite traitee"
    },
    @{
        Num = 4; Equipment = $Equipment.POM008; Code = "M003"; Criticite = "HAUTE"; Zone = "Zone D"
        Description = "Pompe mecanique fuite joint arbre, perte de debit"
        Tech = "ahmed@ocp.ma"; Declarant = "responsable@ocp.ma"; Validator = "responsable@ocp.ma"
        Intervention = @{
            description = "Reparation fuite joint pompe KSB"
            symptomes = "Flaque huile sous palier arriere, debit reduit de 30 pourcent"
            causeRacine = "Usure joint mecanique arbre par cavitation repetee"
            analyseTechnique = "Inspection palier : traces de cavitation sur roue. Joint mecanique HS."
            actionsCorrectives = "Remplacement joint mecanique, controle alignement, purge circuit"
            piecesRemplacees = "Joint mecanique KSB type MG1-45"
            dureeArretMinutes = 120; tempsInterventionMinutes = 90
        }
        Comment = "Pompe remise en service"
    },
    @{
        Num = 5; Equipment = $Equipment.MOT001; Code = "F002"; Criticite = "MOYENNE"; Zone = "Zone A"
        Description = "Vibrations anormales moteur convoyeur A"
        Tech = "mehdi@ocp.ma"; Declarant = "kamal@ocp.ma"; Validator = "kamal@ocp.ma"
        Intervention = @{
            description = "Analyse vibrations moteur convoyeur A"
            symptomes = "Vibrations 7 mm/s axe vertical, bruit metallique intermittent"
            causeRacine = "Desalignement courroie apres changement poulie"
            analyseTechnique = "Mesures avant/apres tension courroie. Alignement laser confirme ecart 0.8mm."
            actionsCorrectives = "Reglage tension courroie, alignement laser, controle paliers"
            piecesRemplacees = "Courroie trapezoidale SPB 2000"
            dureeArretMinutes = 60; tempsInterventionMinutes = 90
        }
        Comment = "Vibrations reduites sous seuil"
    },
    @{
        Num = 6; Equipment = $Equipment.VAR012; Code = "E012"; Criticite = "FAIBLE"; Zone = "Zone B"
        Description = "Communication Modbus intermittente variateur ligne 2"
        Tech = "mohamad@ocp.ma"; Declarant = "responsable@ocp.ma"; Validator = "responsable@ocp.ma"
        Intervention = @{
            description = "Correction communication Modbus variateur ABB"
            symptomes = "Timeouts Modbus aleatoires toutes les 2h, perte supervision HMI"
            causeRacine = "Connecteur RJ45 oxyde sur carte option Modbus, resistance contact elevee"
            analyseTechnique = "Mesure continuite et inspection connecteur. Erreurs CRC Modbus confirmees sur bus RS485."
            actionsCorrectives = "Remplacement connecteur, ressertissage blindage, test polling 24h"
            piecesRemplacees = "Connecteur RJ45 industriel M12"
            dureeArretMinutes = 30; tempsInterventionMinutes = 60
        }
        Comment = "Communication stable apres test"
    },
    @{
        Num = 7; Equipment = $Equipment.CAP045; Code = "S011"; Criticite = "MOYENNE"; Zone = "Zone C"
        Description = "Derive signal 4-20mA capteur niveau silo 3"
        Tech = "technicien@ocp.ma"; Declarant = "kamal@ocp.ma"; Validator = "kamal@ocp.ma"
        Intervention = @{
            description = "Correction derive signal capteur niveau 4-20mA"
            symptomes = "Signal derive de 2mA en 24h, mesure instable sur HMI"
            causeRacine = "Connecteur field oxyde, resistance contact variable sur boucle 4-20mA"
            analyseTechnique = "Mesure boucle avec simulateur. Resistance connecteur 15 ohms vs 0.5 ohm nominal."
            actionsCorrectives = "Remplacement connecteur M12, verification blindage cable, recalibrage"
            piecesRemplacees = "Connecteur M12 4 pins etanche"
            dureeArretMinutes = 45; tempsInterventionMinutes = 75
        }
        Comment = "Signal stable apres recalibrage"
    },
    @{
        Num = 8; Equipment = $Equipment.POM008; Code = "M007"; Criticite = "HAUTE"; Zone = "Zone D"
        Description = "Cavitation pompe alimentation circuit mecanique"
        Tech = "ahmed@ocp.ma"; Declarant = "responsable@ocp.ma"; Validator = "responsable@ocp.ma"
        Intervention = @{
            description = "Traitement cavitation pompe KSB circuit mecanique"
            symptomes = "Bruit gravellement a l aspiration, debit instable, vibration palier"
            causeRacine = "NPSH disponible insuffisant, clapet aspiration partiellement obstrue"
            analyseTechnique = "Mesure pression aspiration -0.3 bar. Inspection clapet : debris phosphate."
            actionsCorrectives = "Nettoyage clapet et crepine, ajustement vitesse pompe, controle NPSH"
            piecesRemplacees = "Creine inox et joint clapet"
            dureeArretMinutes = 180; tempsInterventionMinutes = 150
        }
        Comment = "Cavitation eliminee"
    }
)

$baseDate = [DateTime]::Parse("2026-03-01T08:00:00Z").ToUniversalTime()

Write-Host "`n=== Creation des 8 scenarios ===" -ForegroundColor Cyan

foreach ($s in $Scenarios) {
    $dateHeure = $baseDate.AddDays($s.Num - 1).ToString("yyyy-MM-ddTHH:mm:ss.000Z")
    $declarantId = $UserIds[$s.Declarant]

    Write-Host "`n--- Scenario #$($s.Num) ---" -ForegroundColor Yellow

    $failure = Invoke-Api -Token $Tokens[$s.Declarant] -Method POST -Path "/api/v1/failures" -Body @{
        equipmentId         = $s.Equipment
        dateHeure           = $dateHeure
        criticite           = $s.Criticite
        zoneService         = $s.Zone
        descriptionInitiale = $s.Description
        codeDefaut          = $s.Code
        statut              = "RESOLUE"
        responsableId       = $declarantId
    }
    Write-Host "  Panne: $($failure.id) (responsable: $($failure.responsableNom))"

    $interBody = @{ failureId = $failure.id } + $s.Intervention
    $intervention = Invoke-Api -Token $Tokens[$s.Tech] -Method POST -Path "/api/v1/interventions" -Body $interBody
    Write-Host "  Intervention: $($intervention.id) par $($s.Tech)"

    Invoke-Api -Token $Tokens[$s.Tech] -Method POST -Path "/api/v1/interventions/$($intervention.id)/submit" | Out-Null
    Write-Host "  Soumise"

    $validated = Invoke-Api -Token $Tokens[$s.Validator] -Method POST -Path "/api/v1/interventions/$($intervention.id)/validate" -Body @{
        approved    = $true
        commentaire = $s.Comment
    }
    Write-Host "  VALIDEE par $($s.Validator)" -ForegroundColor Green
}

Write-Host "`nSeed termine." -ForegroundColor Green
