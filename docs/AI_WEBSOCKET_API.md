# AI Chat (STOMP WebSocket API)

---
**Last Updated:** 2025-01-15
**Version:** 0.6
**Changes:** 
- Migrated AI Chat from Raw WebSocket to STOMP
- Updated endpoints: `/ai-ws` → `/ws` with STOMP protocol
- Added subscription path: `/user/queue/ai`
- Implemented STOMP heartbeats (10s) for session robustness
---

## Overview
The AI Chat leverages the standard **STOMP Broker** for structured communication, enabling session management and direct destination routing.

*   **Protocol**: STOMP over WebSocket
*   **Endpoint**: `ws://localhost:8080/ws`
*   **Authentication**: Bearer Token in `Authorization` header OR `neuralhealer_token` cookie.

---

## Message Flow

### 1. Subscription
Subscribe to the user-specific queue to receive AI responses.
**Topic**: `/user/queue/ai`

### 2. Sending Questions
Send questions to the STOMP destination.
**Destination**: `/app/ai/ask`

**Payload:**
```json
{
  "question": "What are the common symptoms of stress?"
}
```

### 3. Receiving Events
The server broadcasts events to your subscribed queue.

#### AI Typing Start
```json
{
  "type": "AI_TYPING_START",
  "senderName": "AI Assistant",
  "content": "AI is typing..."
}
```

#### AI Response
```json
{
  "type": "AI_RESPONSE",
  "senderName": "AI Assistant",
  "content": "Common symptoms of stress include..."
}
```

---

## Client Example (JavaScript / StompJS)

```javascript
const client = new StompJs.Client({
    brokerURL: "ws://localhost:8080/ws",
    connectHeaders: {
        Authorization: "Bearer <token>"
    },
    onConnect: () => {
        // Subscribe to AI responses
        client.subscribe("/user/queue/ai", (message) => {
            const data = JSON.parse(message.body);
            console.log("AI Event:", data);
        });

        // Send a question
        client.publish({
            destination: "/app/ai/ask",
            body: JSON.stringify({ question: "Hello AI" })
        });
    }
});

client.activate();
```
