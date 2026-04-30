# WebSocket Integration Guide

This guide explains how to connect your frontend application to the Replime backend to receive real-time WebSocket updates when the status of an ingested Video or an entire Training Source changes.

## 1. Connection Details

To establish a WebSocket connection, you will connect to the exposed Spring Boot STOMP endpoint:

- **Protocol**: `ws://` (or `wss://` if using HTTPS)
- **Endpoint**: `ws://{backend-url}/api/v1/ws` 
  *(e.g., `ws://localhost:8080/api/v1/ws`)*

## 2. Topic Structure

Once connected, your frontend needs to subscribe to a topic that is dynamically tied to the specific Chatbot's ID. You'll receive real-time JSON payloads whenever processing finishes.

- **Topic Subscription Path**: `/topic/chatbot/{chatbot_id}/sync-status`

*(Replace `{chatbot_id}` with the UUID of the influencer's chatbot).*

## 3. Message Payload Definitions

When the backend fires a status update, you will receive a JSON message. We send two types of messages, identified by the `type` field.

### A. Single Video Update (`VIDEO_UPDATE`)
Fired the second a single video finishes processing in the background.

```json
{
  "type": "VIDEO_UPDATE",
  "sourceId": 15,
  "videoId": 42,
  "status": "COMPLETED"   // or "FAILED"
}
```

### B. Source Completed (`SOURCE_COMPLETE`)
Fired when the final video in a Playlist or Training Source finishes processing, indicating the entire job is done.

```json
{
  "type": "SOURCE_COMPLETE",
  "sourceId": 15,
  "videoId": null,
  "status": "COMPLETED"
}
```

## 4. Example JavaScript (React/Next.js) Implementation

Here is how you can use the `@stomp/stompjs` and `sockjs-client` packages to connect and listen to the updates.

**Prerequisites:**
```bash
npm install @stomp/stompjs sockjs-client
```

**Implementation:**

```javascript
import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const IngestionTracker = ({ chatbotId }) => {
  const [messages, setMessages] = useState([]);

  useEffect(() => {
    // 1. Initialize the STOMP client
    const stompClient = new Client({
      // We fall back to SockJS if pure WebSockets aren't available
      webSocketFactory: () => new SockJS('http://localhost:8080/api/v1/ws'),
      
      onConnect: () => {
        console.log('Connected to WebSocket!');
        
        // 2. Subscribe to the chatbot's specific topic
        stompClient.subscribe(`/topic/chatbot/${chatbotId}/sync-status`, (message) => {
           // Parse the incoming JSON message
           const body = JSON.parse(message.body);
           console.log('Received WebSocket Message:', body);

           setMessages((prev) => [...prev, body]);

           if (body.type === 'VIDEO_UPDATE') {
             // Handle UI update for single video (e.g. change a spinner to a checkmark)
           } else if (body.type === 'SOURCE_COMPLETE') {
             // Show Toast notification: "Playlist processing complete!"
           }
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
    });

    // 3. Activate connection
    stompClient.activate();

    // 4. Clean up on unmount
    return () => {
      stompClient.deactivate();
    };
  }, [chatbotId]);

  return (
    <div>
       {/* Map over messages for debugging */}
       {messages.map((msg, idx) => (
          <div key={idx}>
             {msg.type} - Source: {msg.sourceId} - Video: {msg.videoId} - Status: {msg.status}
          </div>
       ))}
    </div>
  );
};

export default IngestionTracker;
```

## 5. Testing with Postman

Postman supports plain WebSockets, but Spring Boot STOMP operates over SockJS fallback bindings by default, which Postman's basic raw WebSocket request type does **not** natively understand.

To easily test this locally without building a full frontend, you can use tools explicitly designed for STOMP or create a tiny raw HTML client.

### Method A: Use Apic (Recommended)
Download the free **Apic** Chrome Extension or Desktop App (which natively supports STOMP over SockJS).
1. Click **+ New WS Request** and select **SockJS** + **STOMP** as protocols.
2. Url: `http://localhost:8080/api/v1/ws`
3. Click Connect.
4. Under Subscriptions add `/topic/chatbot/{your_chatbot_id_here}/sync-status`.
5. Trigger your backend APIs normally through Postman (e.g. update the sync status), and watch the payload arrive seamlessly on the Apic listener pane!

### Method B: Using Postman (Raw Websocket without SockJS fallback)
If you specifically want to use Postman, you must hit the raw WebSocket endpoint directly, bypassing SockJS fallback wrappers.

1. Open Postman -> New **WebSocket Request**.
2. URL: `ws://localhost:8080/api/v1/ws/websocket` *(Notice the `/websocket` append at the end)*
3. Hit Connect.
4. STOMP requires a handshake string to be sent first. In Postman's "Message" text box, paste exactly this block (be sure to add a trailing empty line AND hold control/command and press `[Ctrl+@]` representing the standard STOMP null byte terminator `^@` if your client supports it):
   ```
   CONNECT
   accept-version:1.1,1.0
   heart-beat:10000,10000
   
   ^@
   ```
5. You should receive a `CONNECTED` response frame.
6. Now Subscribe by sending:
   ```
   SUBSCRIBE
   id:sub-0
   destination:/topic/chatbot/{chatbot_id}/sync-status
   
   ^@
   ```
*(Note: Method B can be finicky depending on Postman's unescaped termination characters support, so Method A or utilizing the JS snippet is heavily recommended for testing).*
