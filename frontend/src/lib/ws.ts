import { Client, IMessage } from "@stomp/stompjs";
import { getAccessToken } from "./auth";

type Listener = (msg: IMessage) => void;

let stompClient: Client | null = null;
const activeSubscriptions = new Map<string, { unsubscribe: () => void }>();
const destinationListeners = new Map<string, Set<Listener>>();
const pendingDestinations = new Set<string>();
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
    beforeConnect: (nextClient) => {
      nextClient.connectHeaders = {
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
  activeSubscriptions.forEach((sub) => sub.unsubscribe());
  activeSubscriptions.clear();
  destinationListeners.clear();
  pendingDestinations.clear();
  pendingConnectCallbacks = [];
  stompClient?.deactivate();
  stompClient = null;
}

export function subscribeTo(destination: string, callback: Listener): () => void {
  const listeners = destinationListeners.get(destination) ?? new Set<Listener>();
  listeners.add(callback);
  destinationListeners.set(destination, listeners);

  if (!stompClient?.connected) {
    pendingDestinations.add(destination);
    connectStomp();
  } else {
    ensureSubscription(destination);
  }

  return () => {
    const currentListeners = destinationListeners.get(destination);
    if (!currentListeners) {
      return;
    }
    currentListeners.delete(callback);
    if (currentListeners.size > 0) {
      return;
    }

    destinationListeners.delete(destination);
    pendingDestinations.delete(destination);
    activeSubscriptions.get(destination)?.unsubscribe();
    activeSubscriptions.delete(destination);
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

  for (const destination of destinationListeners.keys()) {
    ensureSubscription(destination);
  }
  pendingDestinations.clear();
}

function ensureSubscription(destination: string): void {
  if (!stompClient?.connected || activeSubscriptions.has(destination)) {
    return;
  }

  const subscription = stompClient.subscribe(destination, (message) => {
    const listeners = destinationListeners.get(destination);
    if (!listeners) {
      return;
    }
    listeners.forEach((listener) => listener(message));
  });

  activeSubscriptions.set(destination, subscription);
}
