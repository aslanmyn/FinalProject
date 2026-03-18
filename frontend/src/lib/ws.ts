import { Client, IMessage } from "@stomp/stompjs";
import { getAccessToken } from "./auth";

let stompClient: Client | null = null;
const subscriptions = new Map<string, { unsubscribe: () => void }>();
const pendingSubscriptions = new Map<string, (msg: IMessage) => void>();
let pendingConnectCallbacks: Array<() => void> = [];

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
  if (onConnect) {
    pendingConnectCallbacks.push(onConnect);
  }

  if (stompClient?.connected) {
    const callbacks = [...pendingConnectCallbacks];
    pendingConnectCallbacks = [];
    callbacks.forEach((callback) => callback());
    return stompClient;
  }

  if (stompClient) {
    return stompClient;
  }

  const client = new Client({
    brokerURL: getWsUrl(),
    beforeConnect: (client) => {
      client.connectHeaders = {
        Authorization: `Bearer ${getAccessToken() || ""}`
      };
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      const callbacks = [...pendingConnectCallbacks];
      pendingConnectCallbacks = [];
      callbacks.forEach((callback) => callback());
      flushPendingSubscriptions();
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
  pendingSubscriptions.clear();
  pendingConnectCallbacks = [];
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
    connectStomp();
    pendingSubscriptions.set(key, callback);
    return () => {
      pendingSubscriptions.delete(key);
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

function flushPendingSubscriptions(): void {
  if (!stompClient?.connected) {
    return;
  }

  pendingSubscriptions.forEach((callback, destination) => {
    if (subscriptions.has(destination)) {
      subscriptions.get(destination)!.unsubscribe();
      subscriptions.delete(destination);
    }
    const sub = stompClient!.subscribe(destination, callback);
    subscriptions.set(destination, sub);
  });
  pendingSubscriptions.clear();
}
