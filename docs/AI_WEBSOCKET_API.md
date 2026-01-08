# AI WebSocket API Documentation
**Version:** 0.4

This document describes the interface for the NeuralHealer AI Chatbot WebSocket.

## Overview
The AI Chat uses a **Raw WebSocket** connection (Text/JSON).
*   **Protocol**: Standard WebSocket (`ws://` or `wss://`)
*   **No STOMP involved**. No subscription headers required.
*   **Data Format**: JSON stringified text.

## Connection Details

*   **Endpoint**: `ws://<host>:<port>/ai-ws`
    *   *Localhost Example*: `ws://localhost:8080/ai-ws`
    *   *Production Example*: `wss://api.neuralhealer.com/ai-ws`
*   **Authentication**: Public / Anonymous. No headers required.

---

## Message Workflow

### 1. Client Sends Question
Send a JSON object with the `question` field.

**Payload:**
```json
{
  "question": "Hello, how are you?"
}
```

### 2. Server Sends "Typing" Event
Indicates the AI is processing the request.

**Payload:**
```json
{
  "type": "AI_TYPING_START",
  "senderName": "AI Assistant",
  "content": "AI is typing...",
  "timestamp": "2026-01-08T05:30:00"
}
```

### 3. Server Sends "Typing Stop" Event (Optional for UI)
Indicates the AI has finished processing and is about to send the response.

**Payload:**
```json
{
  "type": "AI_TYPING_STOP",
  "senderName": "AI Assistant"
}
```

### 4. Server Sends Response
Contains the final answer and source citations.
*   **Note**: The server automatically cleans prefixes like "Answer:" or "الإجابة:" from the content.

**Payload:**
```json
{
  "type": "AI_RESPONSE",
  "senderName": "AI Assistant",
  "content": "I am fine, thank you! How can I help you with your health today?",
  "sources": [
    "path/to/source1.md",
    "path/to/source2.md"
  ]
}
```

### 4. Error Handling
If an error occurs (e.g., AI service timeout), the server sends an error event.

**Payload:**
```json
{
  "type": "AI_ERROR",
  "senderName": "System",
  "content": "Sorry, the AI service is currently unavailable."
}
```

---

## Client Implementation Example (JavaScript)

```javascript
const ws = new WebSocket("ws://localhost:8080/api/ai-ws");

ws.onopen = () => {
    console.log("Connected to AI Chat");
    
    // Send a message
    ws.send(JSON.stringify({ 
        question: "What are the symptoms of anxiety?" 
    }));
};

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    
    switch(data.type) {
        case "AI_TYPING_START":
            showTypingIndicator();
            break;
            
        case "AI_RESPONSE":
            hideTypingIndicator();
            displayMessage(data.content);
            console.log("Sources:", data.sources);
            break;
            
        case "AI_ERROR":
            hideTypingIndicator();
            showError(data.content);
            break;
    }
};

ws.onclose = () => {
    console.log("Disconnected");
};
```
