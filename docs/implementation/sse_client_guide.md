# SSE Client Implementation Guide: Reconnection & Last-Event-ID

This document explains how to implement the client-side logic for the Server-Sent Events (SSE) notification system, specifically focusing on handling disconnections and resuming the stream without missing data.

## Overview

The NeuralHealer SSE stream supports the standard `Last-Event-ID` protocol. Each notification event sent by the server includes a unique ID in the format `uuid_epoch`.

When the browser's `EventSource` automatically reconnects after a network interruption, it sends the ID of the last successfully received event in the `Last-Event-ID` HTTP header. The server then replays any notifications that occurred after that specific timestamp.

## Basic Implementation (Standard EventSource)

The standard `EventSource` API handles reconnection and `Last-Event-ID` automatically.

```javascript
// 1. Initialize the connection
const eventSource = new EventSource('/api/notifications/stream', {
    withCredentials: true // Required for cookie-based JWT auth
});

// 2. Listen for standard message events
eventSource.onmessage = (event) => {
    const notification = JSON.parse(event.data);
    console.log("Received notification:", notification);
    // The browser automatically stores 'event.lastEventId'
};

// 3. Listen for specific event types
eventSource.addEventListener('connected', (event) => {
    console.log("SSE Stream Connected:", JSON.parse(event.data));
});

// 4. Handle errors
eventSource.onerror = (error) => {
    console.error("SSE Error:", error);
};
```

## Advanced Implementation (Handling Refresh/Explicit Resubmission)

If the user refreshes the page, the browser's native `EventSource` state is lost. To ensure no notifications are missed across page reloads, you should persist the `lastEventId` in `localStorage`.

> [!IMPORTANT]
> Since standard `EventSource` doesn't allow setting custom headers (like `Last-Event-ID`) on the *initial* request, you must pass the ID as a query parameter if you want to resume after a page refresh.

### Recommended Pattern:

```javascript
const STORAGE_KEY = 'last_sse_id';

function connectToSse() {
    const lastId = localStorage.getItem(STORAGE_KEY);
    
    // Fallback: Use query param for manual 'Last-Event-ID' support on initial connect
    const url = new URL('/api/notifications/stream', window.location.origin);
    if (lastId) {
        url.searchParams.append('lastEventId', lastId);
    }

    const es = new EventSource(url.toString(), { withCredentials: true });

    es.onmessage = (event) => {
        // Update local storage so we can resume after refresh
        if (event.lastEventId) {
            localStorage.setItem(STORAGE_KEY, event.lastEventId);
        }
        
        const data = JSON.parse(event.data);
        displayNotification(data);
    };

    return es;
}
```

## Event Format Reference

### Heartbeat (sent every 30s)
```json
{
  "status": "ping",
  "timestamp": "2024-01-25T12:00:00.123"
}
```

### Notification
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "type": "MESSAGE_RECEIVED",
  "title": "New Message",
  "message": "Dr. Smith: Hello there",
  "priority": "normal",
  "source": "message",
  "payload": { "senderId": "..." },
  "read": false,
  "sentAt": "2024-01-25T12:00:05"
}
```

## Best Practices

1.  **Always use `withCredentials: true`** if your API is protected by session cookies or HTTP-only JWTs.
2.  **Filter low priority notifications**: The server does not push `low` priority notifications via SSE to save bandwidth.
3.  **Handle JSON Parsing**: All NeuralHealer SSE data is JSON-formatted. Wrap `JSON.parse` in try-catch.
4.  **Connection Management**: Close the `EventSource` when the user logs out or the component unmounts to prevent memory leaks.
