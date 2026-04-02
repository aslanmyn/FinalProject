import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ApiError,
  cancelFoodOrder,
  createFoodOrder,
  fetchFoodCategories,
  fetchFoodItems,
  fetchFoodOrders,
  fetchPopularFoodItems
} from "../../lib/api";
import type { FoodCategory, FoodItem, FoodOrder } from "../../types/food";

function formatPrice(price: number): string {
  return `${price.toLocaleString("en-US")} KZT`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

type Tab = "menu" | "orders";

export default function StudentFoodPage() {
  const [tab, setTab] = useState<Tab>("menu");
  const [categories, setCategories] = useState<FoodCategory[]>([]);
  const [items, setItems] = useState<FoodItem[]>([]);
  const [popularItems, setPopularItems] = useState<FoodItem[]>([]);
  const [orders, setOrders] = useState<FoodOrder[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [cart, setCart] = useState<Record<number, number>>({});
  const [note, setNote] = useState("");
  const [pickupAt, setPickupAt] = useState("");
  const [loading, setLoading] = useState(true);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadMenu = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [loadedCategories, loadedPopular, loadedItems] = await Promise.all([
        fetchFoodCategories(),
        fetchPopularFoodItems(),
        fetchFoodItems()
      ]);
      setCategories(loadedCategories);
      setPopularItems(loadedPopular);
      setItems(loadedItems);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load canteen data");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadOrders = useCallback(async () => {
    setOrdersLoading(true);
    setError(null);
    try {
      setOrders(await fetchFoodOrders());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load food orders");
    } finally {
      setOrdersLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadMenu();
  }, [loadMenu]);

  useEffect(() => {
    if (tab === "orders") {
      void loadOrders();
    }
  }, [loadOrders, tab]);

  const visibleItems = useMemo(
    () => (selectedCategory == null ? items : items.filter((item) => item.categoryId === selectedCategory)),
    [items, selectedCategory]
  );

  const cartCount = Object.values(cart).reduce((sum, qty) => sum + qty, 0);
  const cartTotal = Object.entries(cart).reduce((sum, [itemId, qty]) => {
    const item = items.find((entry) => entry.id === Number(itemId));
    return sum + (item ? item.price * qty : 0);
  }, 0);

  function addToCart(itemId: number) {
    setCart((current) => ({ ...current, [itemId]: (current[itemId] || 0) + 1 }));
  }

  function removeFromCart(itemId: number) {
    setCart((current) => {
      const next = { ...current };
      if (!next[itemId]) return current;
      if (next[itemId] === 1) delete next[itemId];
      else next[itemId] -= 1;
      return next;
    });
  }

  async function handlePlaceOrder() {
    if (cartCount === 0) return;
    setSubmitting(true);
    setError(null);
    try {
      await createFoodOrder(
        cart,
        note.trim() || undefined,
        pickupAt ? new Date(pickupAt).toISOString() : undefined
      );
      setCart({});
      setNote("");
      setPickupAt("");
      setTab("orders");
      await loadOrders();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to place order");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel(orderId: number) {
    try {
      await cancelFoodOrder(orderId);
      await loadOrders();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to cancel order");
    }
  }

  return (
    <div className="screen app-screen">
      <section className="card service-hero">
        <div className="service-hero-copy">
          <span className="auth-kicker">Campus Life</span>
          <h2>Canteen ordering</h2>
          <p className="muted">
            Browse menu categories, collect items in the cart, and place a pickup order directly from the student portal.
          </p>
        </div>
        <div className="service-hero-metrics">
          <div className="service-metric-card">
            <span>Categories</span>
            <strong>{categories.length}</strong>
          </div>
          <div className="service-metric-card">
            <span>Popular items</span>
            <strong>{popularItems.length}</strong>
          </div>
          <div className="service-metric-card">
            <span>Cart total</span>
            <strong>{cartCount > 0 ? formatPrice(cartTotal) : "Empty"}</strong>
          </div>
        </div>
      </section>

      {error ? <div className="banner banner-danger">{error}</div> : null}

      <section className="card">
        <div className="service-toolbar">
          <div className="service-pill-row">
            <button className={`btn ${tab === "menu" ? "btn-primary" : ""}`} onClick={() => setTab("menu")}>
              Menu
            </button>
            <button className={`btn ${tab === "orders" ? "btn-primary" : ""}`} onClick={() => setTab("orders")}>
              My Orders
            </button>
          </div>
          <div className="service-summary-inline">
            <span className="badge badge-info">{cartCount} item{cartCount === 1 ? "" : "s"}</span>
            <strong>{formatPrice(cartTotal)}</strong>
          </div>
        </div>
      </section>

      {tab === "menu" ? (
        <>
          <section className="card">
            <div className="service-section-header">
              <div>
                <h3>Categories</h3>
                <p className="muted">Filter the menu and add items to your current cart.</p>
              </div>
            </div>
            <div className="service-pill-row">
              <button
                className={`btn btn-sm ${selectedCategory == null ? "btn-primary" : ""}`}
                onClick={() => setSelectedCategory(null)}
              >
                All
              </button>
              {categories.map((category) => (
                <button
                  key={category.id}
                  className={`btn btn-sm ${selectedCategory === category.id ? "btn-primary" : ""}`}
                  onClick={() => setSelectedCategory(category.id)}
                >
                  {category.icon ? `${category.icon} ` : ""}
                  {category.name}
                </button>
              ))}
            </div>
          </section>

          {!loading && selectedCategory == null && popularItems.length > 0 ? (
            <section className="card">
              <div className="service-section-header">
                <div>
                  <h3>Popular today</h3>
                  <p className="muted">Quick picks that other students are ordering right now.</p>
                </div>
              </div>
              <div className="service-card-grid">
                {popularItems.map((item) => (
                  <FoodCard
                    key={item.id}
                    item={item}
                    quantity={cart[item.id] || 0}
                    onAdd={() => addToCart(item.id)}
                    onRemove={() => removeFromCart(item.id)}
                  />
                ))}
              </div>
            </section>
          ) : null}

          <div className="service-split service-split-wide">
            <section className="card">
              <div className="service-section-header">
                <div>
                  <h3>{selectedCategory == null ? "All items" : categories.find((item) => item.id === selectedCategory)?.name || "Items"}</h3>
                  <p className="muted">Choose items and adjust quantities in place.</p>
                </div>
              </div>
              {loading ? <p>Loading menu...</p> : null}
              {!loading && visibleItems.length === 0 ? <div className="service-empty">No menu items available right now.</div> : null}
              <div className="service-card-grid">
                {visibleItems.map((item) => (
                  <FoodCard
                    key={item.id}
                    item={item}
                    quantity={cart[item.id] || 0}
                    onAdd={() => addToCart(item.id)}
                    onRemove={() => removeFromCart(item.id)}
                  />
                ))}
              </div>
            </section>

            <section className="card">
              <div className="service-section-header">
                <div>
                  <h3>Your cart</h3>
                  <p className="muted">Review the order before sending it to the canteen.</p>
                </div>
              </div>

              {cartCount === 0 ? (
                <div className="service-empty">Your cart is empty. Add a few items from the menu first.</div>
              ) : (
                <div className="service-stack">
                  {Object.entries(cart).map(([itemId, quantity]) => {
                    const item = items.find((entry) => entry.id === Number(itemId));
                    if (!item) return null;
                    return (
                      <div key={itemId} className="service-inline-card">
                        <div>
                          <strong>{item.name}</strong>
                          <div className="muted">{formatPrice(item.price)} each</div>
                        </div>
                        <div className="inline-actions">
                          <button className="btn btn-sm" onClick={() => removeFromCart(item.id)}>
                            -
                          </button>
                          <span>{quantity}</span>
                          <button className="btn btn-sm" onClick={() => addToCart(item.id)}>
                            +
                          </button>
                        </div>
                      </div>
                    );
                  })}

                  <div className="form-group">
                    <label>Pickup time (optional)</label>
                    <input
                      type="datetime-local"
                      className="input"
                      value={pickupAt}
                      onChange={(e) => setPickupAt(e.target.value)}
                    />
                  </div>
                  <div className="form-group">
                    <label>Note (optional)</label>
                    <textarea
                      className="input"
                      rows={3}
                      value={note}
                      onChange={(e) => setNote(e.target.value)}
                      placeholder="Special instructions for the canteen"
                    />
                  </div>
                  <div className="service-checkout-row">
                    <div>
                      <span className="muted">Total</span>
                      <strong>{formatPrice(cartTotal)}</strong>
                    </div>
                    <button className="btn btn-primary" onClick={() => void handlePlaceOrder()} disabled={submitting}>
                      {submitting ? "Placing order..." : "Place order"}
                    </button>
                  </div>
                  <p className="muted">Orders are picked up and paid for at the canteen counter.</p>
                </div>
              )}
            </section>
          </div>
        </>
      ) : (
        <section className="card">
          <div className="service-section-header">
            <div>
              <h3>My orders</h3>
              <p className="muted">Track current and previous canteen orders.</p>
            </div>
          </div>
          {ordersLoading ? <p>Loading orders...</p> : null}
          {!ordersLoading && orders.length === 0 ? (
            <div className="service-empty">You have not placed any food orders yet.</div>
          ) : null}

          <div className="service-stack">
            {orders.map((order) => (
              <div key={order.id} className="service-inline-card service-inline-card-block">
                <div className="service-order-head">
                  <div>
                    <strong>Order #{order.id}</strong>
                    <div className="muted">{formatDate(order.createdAt)}</div>
                  </div>
                  <span className={`badge badge-${order.status === "READY" || order.status === "PICKED_UP" ? "success" : order.status === "CANCELLED" ? "danger" : "info"}`}>
                    {order.status}
                  </span>
                </div>
                <div className="service-stack">
                  {order.items.map((item) => (
                    <div key={item.id} className="service-order-line">
                      <span>{item.foodItemName || "Unknown item"} x{item.quantity}</span>
                      <strong>{formatPrice(item.unitPrice * item.quantity)}</strong>
                    </div>
                  ))}
                </div>
                <div className="service-order-footer">
                  <div>
                    <div className="muted">Total</div>
                    <strong>{formatPrice(order.totalAmount)}</strong>
                  </div>
                  {order.status === "PENDING" ? (
                    <button className="btn btn-sm btn-danger" onClick={() => void handleCancel(order.id)}>
                      Cancel
                    </button>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function FoodCard({
  item,
  quantity,
  onAdd,
  onRemove
}: {
  item: FoodItem;
  quantity: number;
  onAdd: () => void;
  onRemove: () => void;
}) {
  return (
    <article className="service-product-card">
      <div className="service-product-copy">
        <div className="service-product-top">
          <strong>{item.name}</strong>
          {item.popular ? <span className="badge badge-info">Popular</span> : null}
        </div>
        {item.description ? <p className="muted">{item.description}</p> : null}
      </div>
      <div className="service-product-footer">
        <strong>{formatPrice(item.price)}</strong>
        {quantity > 0 ? (
          <div className="inline-actions">
            <button className="btn btn-sm" onClick={onRemove}>
              -
            </button>
            <span>{quantity}</span>
            <button className="btn btn-sm" onClick={onAdd}>
              +
            </button>
          </div>
        ) : (
          <button className="btn btn-sm btn-primary" onClick={onAdd}>
            Add
          </button>
        )}
      </div>
    </article>
  );
}
