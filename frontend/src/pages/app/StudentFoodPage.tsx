import { useEffect, useState } from "react";
import {
  ApiError,
  fetchFoodCategories,
  fetchFoodItems,
  fetchPopularFoodItems,
  fetchFoodOrders,
  createFoodOrder,
  cancelFoodOrder
} from "../../lib/api";
import type { FoodCategory, FoodItem, FoodOrder } from "../../types/food";

function formatPrice(price: number): string {
  return price.toLocaleString("en-US") + " \u20B8";
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
  });
}

type Tab = "menu" | "orders";

export default function StudentFoodPage() {
  const [tab, setTab] = useState<Tab>("menu");
  const [categories, setCategories] = useState<FoodCategory[]>([]);
  const [items, setItems] = useState<FoodItem[]>([]);
  const [popular, setPopular] = useState<FoodItem[]>([]);
  const [orders, setOrders] = useState<FoodOrder[]>([]);
  const [selectedCat, setSelectedCat] = useState<number | null>(null);
  const [cart, setCart] = useState<Record<number, number>>({});
  const [note, setNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [showCart, setShowCart] = useState(false);

  async function loadMenu() {
    setLoading(true);
    setError(null);
    try {
      const [cats, pops] = await Promise.all([fetchFoodCategories(), fetchPopularFoodItems()]);
      setCategories(cats);
      setPopular(pops);
      const allItems = await fetchFoodItems(undefined);
      setItems(allItems);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load menu");
    } finally {
      setLoading(false);
    }
  }

  async function loadOrders() {
    setLoading(true);
    setError(null);
    try {
      setOrders(await fetchFoodOrders());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load orders");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadMenu();
  }, []);

  useEffect(() => {
    if (tab === "orders") void loadOrders();
  }, [tab]);

  function addToCart(itemId: number) {
    setCart((prev) => ({ ...prev, [itemId]: (prev[itemId] || 0) + 1 }));
  }

  function removeFromCart(itemId: number) {
    setCart((prev) => {
      const copy = { ...prev };
      if (copy[itemId] > 1) {
        copy[itemId]--;
      } else {
        delete copy[itemId];
      }
      return copy;
    });
  }

  const cartItemCount = Object.values(cart).reduce((a, b) => a + b, 0);
  const cartTotal = Object.entries(cart).reduce((sum, [id, qty]) => {
    const item = items.find((i) => i.id === Number(id));
    return sum + (item ? item.price * qty : 0);
  }, 0);

  async function handleOrder() {
    setSubmitting(true);
    setError(null);
    try {
      await createFoodOrder(cart, note || undefined);
      setCart({});
      setNote("");
      setShowCart(false);
      setTab("orders");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to place order");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancelOrder(id: number) {
    try {
      await cancelFoodOrder(id);
      await loadOrders();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to cancel order");
    }
  }

  const filteredItems = selectedCat ? items.filter((i) => i.categoryId === selectedCat) : items;

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Canteen</h2>
      </header>

      {error ? <div className="banner banner-danger">{error}</div> : null}

      {/* Tabs */}
      <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
        <button className={`btn ${tab === "menu" ? "btn-primary" : ""}`} onClick={() => setTab("menu")}>Menu</button>
        <button className={`btn ${tab === "orders" ? "btn-primary" : ""}`} onClick={() => setTab("orders")}>
          My Orders
        </button>
        {cartItemCount > 0 ? (
          <button className="btn btn-primary" onClick={() => setShowCart(!showCart)} style={{ marginLeft: "auto" }}>
            Cart ({cartItemCount}) — {formatPrice(cartTotal)}
          </button>
        ) : null}
      </div>

      {/* Cart drawer */}
      {showCart && cartItemCount > 0 ? (
        <section className="card" style={{ borderLeft: "3px solid var(--accent)" }}>
          <h3>Your Cart</h3>
          {Object.entries(cart).map(([id, qty]) => {
            const item = items.find((i) => i.id === Number(id));
            if (!item) return null;
            return (
              <div key={id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "0.5rem 0", borderBottom: "1px solid var(--border)" }}>
                <div>
                  <strong>{item.name}</strong>
                  <span className="muted"> x{qty}</span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                  <span>{formatPrice(item.price * qty)}</span>
                  <button className="btn btn-sm" onClick={() => removeFromCart(Number(id))}>-</button>
                  <button className="btn btn-sm" onClick={() => addToCart(Number(id))}>+</button>
                </div>
              </div>
            );
          })}
          <div style={{ marginTop: "1rem" }}>
            <div className="form-group">
              <label>Note (optional)</label>
              <input type="text" className="input" value={note} onChange={(e) => setNote(e.target.value)} placeholder="Special instructions..." />
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <strong>Total: {formatPrice(cartTotal)}</strong>
              <button className="btn btn-primary" onClick={handleOrder} disabled={submitting}>
                {submitting ? "Placing Order..." : "Place Order"}
              </button>
            </div>
            <p className="muted" style={{ fontSize: "0.8rem", marginTop: "0.5rem" }}>Pick up and pay at the canteen counter.</p>
          </div>
        </section>
      ) : null}

      {loading ? <p>Loading...</p> : null}

      {/* Menu tab */}
      {!loading && tab === "menu" ? (
        <>
          {/* Categories */}
          <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap", marginBottom: "1rem" }}>
            <button
              className={`btn btn-sm ${selectedCat === null ? "btn-primary" : ""}`}
              onClick={() => setSelectedCat(null)}
            >
              All
            </button>
            {categories.map((c) => (
              <button
                key={c.id}
                className={`btn btn-sm ${selectedCat === c.id ? "btn-primary" : ""}`}
                onClick={() => setSelectedCat(c.id)}
              >
                {c.icon ? `${c.icon} ` : ""}{c.name}
              </button>
            ))}
          </div>

          {/* Popular section */}
          {!selectedCat && popular.length > 0 ? (
            <section className="card">
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <h3>Popular Today</h3>
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "1rem", marginTop: "0.75rem" }}>
                {popular.map((item) => (
                  <FoodCard key={item.id} item={item} qty={cart[item.id] || 0} onAdd={() => addToCart(item.id)} onRemove={() => removeFromCart(item.id)} />
                ))}
              </div>
            </section>
          ) : null}

          {/* All items */}
          <section className="card">
            <h3>{selectedCat ? categories.find((c) => c.id === selectedCat)?.name || "Items" : "All Items"}</h3>
            {filteredItems.length === 0 ? <p className="muted">No items available.</p> : null}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "1rem", marginTop: "0.75rem" }}>
              {filteredItems.map((item) => (
                <FoodCard key={item.id} item={item} qty={cart[item.id] || 0} onAdd={() => addToCart(item.id)} onRemove={() => removeFromCart(item.id)} />
              ))}
            </div>
          </section>
        </>
      ) : null}

      {/* Orders tab */}
      {!loading && tab === "orders" ? (
        <section className="card">
          <h3>My Orders</h3>
          {orders.length === 0 ? <p className="muted">No orders yet.</p> : null}
          {orders.map((order) => (
            <div key={order.id} className="card" style={{ background: "var(--bg)", marginBottom: "0.75rem" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <strong>Order #{order.id}</strong>
                  <span className="muted"> — {formatDate(order.createdAt)}</span>
                </div>
                <span className={`badge badge-${order.status === "READY" || order.status === "PICKED_UP" ? "success" : order.status === "CANCELLED" ? "danger" : "info"}`}>
                  {order.status}
                </span>
              </div>
              <div style={{ marginTop: "0.5rem" }}>
                {order.items.map((oi) => (
                  <div key={oi.id} className="muted" style={{ fontSize: "0.85rem" }}>
                    {oi.foodItemName} x{oi.quantity} — {formatPrice(oi.unitPrice * oi.quantity)}
                  </div>
                ))}
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "0.5rem" }}>
                <strong>Total: {formatPrice(order.totalAmount)}</strong>
                {order.status === "PENDING" ? (
                  <button className="btn btn-sm btn-danger" onClick={() => handleCancelOrder(order.id)}>Cancel</button>
                ) : null}
              </div>
            </div>
          ))}
        </section>
      ) : null}
    </div>
  );
}

function FoodCard({ item, qty, onAdd, onRemove }: { item: FoodItem; qty: number; onAdd: () => void; onRemove: () => void }) {
  return (
    <div style={{ border: "1px solid var(--border)", borderRadius: 12, padding: "1rem", display: "flex", flexDirection: "column", gap: "0.5rem" }}>
      <div>
        <strong>{item.name}</strong>
        {item.popular ? <span className="badge badge-info" style={{ marginLeft: "0.5rem", fontSize: "0.7rem" }}>Popular</span> : null}
      </div>
      {item.description ? <p className="muted" style={{ fontSize: "0.85rem", margin: 0 }}>{item.description}</p> : null}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "auto" }}>
        <strong style={{ color: "var(--accent)" }}>{formatPrice(item.price)}</strong>
        {qty > 0 ? (
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <button className="btn btn-sm" onClick={onRemove}>-</button>
            <span>{qty}</span>
            <button className="btn btn-sm" onClick={onAdd}>+</button>
          </div>
        ) : (
          <button className="btn btn-sm btn-primary" onClick={onAdd}>+ Add</button>
        )}
      </div>
    </div>
  );
}
