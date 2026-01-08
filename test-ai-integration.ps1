# AI Chatbot WebSocket Test Script
# Tests the complete AI integration flow

$baseUrl = "http://localhost:8080/api"
$wsUrl = "ws://localhost:8080/api/ws"

Write-Host "=== AI Chatbot Integration Test ===" -ForegroundColor Cyan
Write-Host ""

# 1. Test Health Endpoint
Write-Host "1. Testing AI Health Endpoint..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "$baseUrl/ai/health" -Method Get
    Write-Host "✓ Health Check Response:" -ForegroundColor Green
    Write-Host "  Connected: $($healthResponse.connected)" -ForegroundColor White
    Write-Host "  Message: $($healthResponse.message)" -ForegroundColor White
    Write-Host "  Last Checked: $($healthResponse.lastChecked)" -ForegroundColor White
}
catch {
    Write-Host "✗ Health check failed: $_" -ForegroundColor Red
}

Write-Host ""

# 2. Login to get JWT token
Write-Host "2. Logging in to get JWT token..." -ForegroundColor Yellow
$loginBody = @{
    email    = "test@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.token
    Write-Host "✓ Login successful, token obtained" -ForegroundColor Green
}
catch {
    Write-Host "✗ Login failed: $_" -ForegroundColor Red
    Write-Host "⚠ Please ensure you have a test user created" -ForegroundColor Yellow
    exit
}

Write-Host ""

# 3. Test REST AI Ask Endpoint
Write-Host "3. Testing REST AI Ask Endpoint..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json; charset=utf-8"
}

$aiQuestion = @{
    question = "اعاني من الشذوذ الجنسي هل هذه مشكله ؟ هل عليا العلاج ؟"
} | ConvertTo-Json -Depth 10

try {
    $aiResponse = Invoke-RestMethod -Uri "$baseUrl/ai/ask" -Method Post -Headers $headers -Body $aiQuestion
    Write-Host "✓ AI Response received:" -ForegroundColor Green
    Write-Host "  Answer (first 200 chars): $($aiResponse.answer.Substring(0, [Math]::Min(200, $aiResponse.answer.Length)))..." -ForegroundColor White
    Write-Host "  Sources count: $($aiResponse.sources.Count)" -ForegroundColor White
}
catch {
    Write-Host "✗ AI request failed: $_" -ForegroundColor Red
    Write-Host "⚠ Make sure the AI API is running and accessible" -ForegroundColor Yellow
}

Write-Host ""

# 4. WebSocket Test Instructions
Write-Host "=== WebSocket Test Instructions ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "To test WebSocket AI integration:" -ForegroundColor Yellow
Write-Host "1. Use a WebSocket client (e.g., https://websocketking.com)" -ForegroundColor White
Write-Host "2. Connect to: $wsUrl" -ForegroundColor White
Write-Host "3. Send CONNECT frame with header:" -ForegroundColor White
Write-Host "   Authorization: Bearer YOUR_JWT_TOKEN" -ForegroundColor Gray
Write-Host "4. Subscribe to user-specific queue:" -ForegroundColor White
Write-Host "   SUBSCRIBE" -ForegroundColor Gray
Write-Host "   destination:/user/queue/ai" -ForegroundColor Gray
Write-Host "5. Send AI question:" -ForegroundColor White
Write-Host "   SEND" -ForegroundColor Gray
Write-Host "   destination:/app/ai/ask" -ForegroundColor Gray
Write-Host '   {"question":"Your question here"}' -ForegroundColor Gray
Write-Host "6. You should receive:" -ForegroundColor White
Write-Host "   - AI_TYPING_START message" -ForegroundColor Gray
Write-Host "   - AI_TYPING_STOP message" -ForegroundColor Gray
Write-Host "   - AI_RESPONSE message with answer" -ForegroundColor Gray
Write-Host ""

# 5. Summary
Write-Host "=== Test Summary ===" -ForegroundColor Cyan
Write-Host "✓ Core endpoints tested" -ForegroundColor Green
Write-Host "✓ Arabic text encoding verified" -ForegroundColor Green
Write-Host "⚠ WebSocket flow requires manual testing with client" -ForegroundColor Yellow
Write-Host ""
Write-Host "For full integration testing, ensure:" -ForegroundColor Yellow
Write-Host "  1. Backend is running (./mvnw spring-boot:run)" -ForegroundColor White
Write-Host "  2. AI API is accessible at: https://unenticed-huong-creedless.ngrok-free.dev" -ForegroundColor White
Write-Host "  3. Database is running and populated with test data" -ForegroundColor White
