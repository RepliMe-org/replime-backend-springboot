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
