import { Client, IMessage } from "@stomp/stompjs";
import { getAccessToken } from "./auth";

let stompClient: Client | null = null;
const subscriptions = new Map<string, { unsubscribe: () => void }>();

function getWsUrl(): string {
  const loc = window.location;
  const proto = loc.protocol === "https:" ? "wss:" : "ws:";
  const base = import.meta.env.VITE_API_BASE_URL;
  if (base) {
    const url = new URL(base);
    const wsProto = url.protocol === "https:" ? "wss:" : "ws:";
    return `${wsProto}//${url.host}/ws`;
  }
  return `${proto}//${loc.host}/ws`;
}

export function connectStomp(onConnect?: () => void): Client {
  if (stompClient?.connected) {
    onConnect?.();
    return stompClient;
  }

  const client = new Client({
    brokerURL: getWsUrl(),
    connectHeaders: {
      Authorization: `Bearer ${getAccessToken() || ""}`
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      onConnect?.();
    },
    onStompError: (frame) => {
      console.error("STOMP error", frame.headers["message"]);
    }
  });

  client.activate();
  stompClient = client;
  return client;
}

export function disconnectStomp(): void {
  subscriptions.forEach((sub) => sub.unsubscribe());
  subscriptions.clear();
  stompClient?.deactivate();
  stompClient = null;
}

export function subscribeTo(
  destination: string,
  callback: (msg: IMessage) => void
): () => void {
  const key = destination;

  if (subscriptions.has(key)) {
    subscriptions.get(key)!.unsubscribe();
    subscriptions.delete(key);
  }

  if (!stompClient?.connected) {
    console.warn("STOMP not connected, queueing subscription for", destination);
    const origOnConnect = stompClient?.onConnect;
    if (stompClient) {
      stompClient.onConnect = (frame) => {
        origOnConnect?.(frame);
        const sub = stompClient!.subscribe(destination, callback);
        subscriptions.set(key, sub);
      };
    }
    return () => {
      subscriptions.get(key)?.unsubscribe();
      subscriptions.delete(key);
    };
  }

  const sub = stompClient.subscribe(destination, callback);
  subscriptions.set(key, sub);

  return () => {
    sub.unsubscribe();
    subscriptions.delete(key);
  };
}

export function sendStompMessage(destination: string, body: object): void {
  if (!stompClient?.connected) {
    console.warn("STOMP not connected, cannot send to", destination);
    return;
  }
  stompClient.publish({
    destination,
    body: JSON.stringify(body)
  });
}

export function isStompConnected(): boolean {
  return stompClient?.connected ?? false;
}
