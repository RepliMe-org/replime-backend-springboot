# Seed test accounts for the Replime backend load test (PowerShell version).
# Idempotent: safe to re-run. Accounts that already exist are skipped.
#
# Usage:
#   .\seed-test-users.ps1
#   .\seed-test-users.ps1 -BaseUrl "http://localhost:8080/api/v1"
#
# If you get an execution-policy error, run this once in the same PowerShell session:
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

param(
    [string]$BaseUrl = "http://localhost:8080/api/v1"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Invoke-Signup {
    param($Uri, $Name, $Email, $Password, $Label)
    $body = @{ name = $Name; email = $Email; password = $Password } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri $Uri -Method Post -ContentType "application/json" -Body $body | Out-Null
        Write-Host "  created $($Label): $Email"
    } catch {
        Write-Host "  skipped (already exists or error) $($Label): $Email -- $($_.Exception.Message)"
    }
}

Write-Host "== Seeding regular USER accounts (load-users.csv) against $BaseUrl =="
Import-Csv (Join-Path $scriptDir "load-users.csv") | ForEach-Object {
    Invoke-Signup -Uri "$BaseUrl/auth/signup" -Name $_.name -Email $_.email -Password $_.password -Label "user"
}

Write-Host ""
Write-Host "== Seeding the ADMIN account (admin-users.csv) =="
Write-Host "   Note: /auth/signup/admin only succeeds ONCE per environment (first admin wins)."
Write-Host "   If an admin already exists, edit admin-users.csv to match its real credentials."
Import-Csv (Join-Path $scriptDir "admin-users.csv") | ForEach-Object {
    Invoke-Signup -Uri "$BaseUrl/auth/signup/admin" -Name $_.name -Email $_.email -Password $_.password -Label "admin"
}

Write-Host ""
Write-Host "== Influencer accounts (influencer-users.csv) =="
Write-Host "   NOT auto-seeded here: INFLUENCER role only comes from the real"
Write-Host "   /influencer/verify/request + /confirm flow (real YouTube channel)."
Write-Host "   You said you've already added a real verified influencer account --"
Write-Host "   just double check the email/password in influencer-users.csv exactly"
Write-Host "   match what you use to log in manually."

Write-Host ""
Write-Host "Done."
