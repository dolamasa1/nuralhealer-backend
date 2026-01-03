# 1. Register and login doctor
$docData = @{
    email     = "doctor_noti@test.com"
    password  = "Test1234!"
    firstName = "DocNoti"
    lastName  = "Test"
    role      = "DOCTOR"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
        -Method Post -ContentType "application/json" -Body $docData -SessionVariable docSession
}
catch {
    $loginData = @{
        email    = "doctor_noti@test.com"
        password = "Test1234!"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method Post -ContentType "application/json" -Body $loginData -SessionVariable docSession
}

# 2. Register and login patient
$patData = @{
    email     = "patient_noti@test.com"
    password  = "Test1234!"
    firstName = "PatNoti"
    lastName  = "Test"
    role      = "PATIENT"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
        -Method Post -ContentType "application/json" -Body $patData -SessionVariable patSession
}
catch {
    $loginData = @{
        email    = "patient_noti@test.com"
        password = "Test1234!"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method Post -ContentType "application/json" -Body $loginData -SessionVariable patSession
}

# 3. Get user IDs
$docId = (Invoke-RestMethod -Uri "http://localhost:8080/api/users/me" -WebSession $docSession).userId
$patId = (Invoke-RestMethod -Uri "http://localhost:8080/api/users/me" -WebSession $patSession).userId

Write-Host "Doctor ID: $docId"
Write-Host "Patient ID: $patId"

# 4. Initiate engagement
$initRequest = @{
    patientId      = $patId
    accessRuleName = "FULL_ACCESS"
} | ConvertTo-Json

$initRes = Invoke-RestMethod -Uri "http://localhost:8080/api/engagements/initiate" `
    -Method Post -ContentType "application/json" -Body $initRequest -WebSession $docSession

$engagementId = $initRes.id
$startToken = $initRes.verificationInfo.token

Write-Host "Engagement ID: $engagementId"

# 5. Verify engagement (Triggers ENGAGEMENT_STARTED for Doctor)
$verifyRequest = @{ token = $startToken } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/engagements/verify-start" `
    -Method Post -ContentType "application/json" -Body $verifyRequest -WebSession $patSession

Start-Sleep -Seconds 1

# 6. Check Doctor's Notifications (Should see 1 unread)
Write-Host "`n--- Checking Doctor Notifications ---"
$docNotis = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications" -WebSession $docSession
Write-Host "Doctor Notifications Count: $($docNotis.Count)"
$docNotis | ForEach-Object { Write-Host " - Type: $($_.type), Msg: $($_.message), Read: $($_.isRead)" }

$unreadRes = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/unread" -WebSession $docSession
Write-Host "Unread Count: $($unreadRes.unreadCount)"

if ($unreadRes.unreadCount -ge 1) {
    Write-Host "✅ Notification Persistence Verified!"
}
else {
    Write-Error "❌ Notification failed to persist."
}

# 7. Mark as Read
if ($docNotis.Count -gt 0) {
    $notiId = $docNotis[0].id
    Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/$notiId/read" -Method Put -WebSession $docSession
    Write-Host "Marked notification $notiId as read."
    
    $unreadAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/unread" -WebSession $docSession
    Write-Host "Unread Count After Mark Read: $($unreadAfter.unreadCount)"
    
    if ($unreadAfter.unreadCount -eq ($unreadRes.unreadCount - 1)) {
        Write-Host "✅ Mark as Read Verified!"
    }
}

# 8. Send Message (Triggers NEW_MESSAGE for Patient)
Write-Host "`n--- Testing Message Notification ---"
$msgRequest = @{ content = "Hello Patient" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/engagements/$engagementId/messages" `
    -Method Post -ContentType "application/json" -Body $msgRequest -WebSession $docSession

Start-Sleep -Seconds 1

# 9. Check Patient Notifications
$patNotis = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications" -WebSession $patSession
Write-Host "Patient Notifications Count: $($patNotis.Count)"
$patNotis | ForEach-Object { Write-Host " - Type: $($_.type), Msg: $($_.message)" }

if ($patNotis | Where-Object { $_.type -eq "NEW_MESSAGE" }) {
    Write-Host "✅ Message Notification Verified!"
}
else {
    Write-Error "❌ Message Notification failed."
}

# 10. Delete Notification
if ($patNotis.Count -gt 0) {
    $delId = $patNotis[0].id
    Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/$delId" -Method Delete -WebSession $patSession
    Write-Host "Deleted notification $delId"
    
    $patNotisAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications" -WebSession $patSession
    if ($patNotisAfter.Count -eq ($patNotis.Count - 1)) {
        Write-Host "✅ Delete Notification Verified!"
    }
}
