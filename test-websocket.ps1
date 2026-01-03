# 1. Register and login doctor
$docData = @{
    email = "doctor_ws@test.com"
    password = "Test1234!"
    firstName = "John"
    lastName = "Smith"
    role = "DOCTOR"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
        -Method Post -ContentType "application/json" -Body $docData -SessionVariable docSession
} catch {
    # Login if already exists
     $loginData = @{
        email = "doctor_ws@test.com"
        password = "Test1234!"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method Post -ContentType "application/json" -Body $loginData -SessionVariable docSession
}

# 2. Register and login patient
$patData = @{
    email = "patient_ws@test.com"
    password = "Test1234!"
    firstName = "Jane"
    lastName = "Doe"
    role = "PATIENT"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
        -Method Post -ContentType "application/json" -Body $patData -SessionVariable patSession
} catch {
     $loginData = @{
        email = "patient_ws@test.com"
        password = "Test1234!"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method Post -ContentType "application/json" -Body $loginData -SessionVariable patSession
}

# 3. Get patient ID
$patId = (Invoke-RestMethod -Uri "http://localhost:8080/api/users/me" -WebSession $patSession).userId

# 4. Initiate engagement
$initRequest = @{
    patientId = $patId
    accessRuleName = "FULL_ACCESS"
} | ConvertTo-Json

$initRes = Invoke-RestMethod -Uri "http://localhost:8080/api/engagements/initiate" `
    -Method Post -ContentType "application/json" -Body $initRequest -WebSession $docSession

$engagementId = $initRes.id
$startToken = $initRes.verificationInfo.token

Write-Host "Engagement ID: $engagementId"
Write-Host "Token: $startToken"

# 5. Verify engagement (Patient) - This should trigger WebSocket 'active' notice
$verifyRequest = @{ token = $startToken } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/engagements/verify-start" `
    -Method Post -ContentType "application/json" -Body $verifyRequest -WebSession $patSession

Write-Host "✅ Engagement activated! WebSocket notification should have fired."

# 6. Cancel Request (Simulate Cancel for cleanup/test)
# Note: In real flow, we'd end it. Here we just test the endpoints don't crash.
$endRequest = Invoke-RestMethod -Uri "http://localhost:8080/api/engagements/end" `
    -Method Post -ContentType "application/json" -Body (@{ engagementId = $engagementId; reason = "Test End" } | ConvertTo-Json) -WebSession $docSession

Write-Host "✅ End requested. WebSocket notification should have fired."
