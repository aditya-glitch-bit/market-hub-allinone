import { createContext, useContext, useState, type ReactNode } from "react";
import { api, messageOf } from "@/lib/api";
import { useMarket } from "./market";
type CartItem = { id: string; quantity: number };
/** Result of a checkout attempt: ok = order placed, login = person must sign in first, failed = see the notice. */
type CheckoutResult = "ok" | "login" | "failed";
type Store = {
  cart: CartItem[];
  wishlist: string[];
  recent: string[];
  add: (id: string, quantity?: number) => void;
  change: (id: string, quantity: number) => void;
  toggleWish: (id: string) => void;
  view: (id: string) => void;
  checkout: () => Promise<CheckoutResult>;
  notice: string;
  notify: (text: string) => void;
};
interface PlacedOrder {
  code: string;
}
const Context = createContext<Store | null>(null);
/** Bag, wishlist and recently viewed live in the browser; checkout sends the bag to the Java API. */
export function DemoStore({ children }: { children: ReactNode }) {
  const market = useMarket();
  const products = market.products;
  const [cart, setCart] = useState<CartItem[]>([]);
  const [wishlist, setWishlist] = useState<string[]>([]);
  const [recent, setRecent] = useState<string[]>([]);
  const [notice, setNotice] = useState("");
  function notify(text: string) {
    setNotice(text);
    setTimeout(() => setNotice(""), 3500);
  }
  function add(id: string, quantity = 1) {
    if (!products.some((p) => p.id === id)) return;
    setCart((prev) =>
      prev.some((p) => p.id === id)
        ? prev.map((p) => (p.id === id ? { ...p, quantity: p.quantity + quantity } : p))
        : [...prev, { id, quantity }],
    );
    notify("Added to your bag");
  }
  function change(id: string, quantity: number) {
    setCart((prev) =>
      quantity <= 0
        ? prev.filter((p) => p.id !== id)
        : prev.map((p) => (p.id === id ? { ...p, quantity } : p)),
    );
  }
  function toggleWish(id: string) {
    setWishlist((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  }
  function view(id: string) {
    setRecent((prev) => [id, ...prev.filter((x) => x !== id)].slice(0, 8));
  }
  async function checkout(): Promise<CheckoutResult> {
    if (!market.live) {
      notify("Checkout needs the MarketHub server, which is not connected in this preview.");
      return "failed";
    }
    if (!market.user) {
      notify("Please sign in to check out.");
      return "login";
    }
    try {
      // The server keeps its own bag per session, so mirror the browser bag there and then place the order.
      await api("/cart", { method: "DELETE" });
      for (const line of cart) {
        await api("/cart", {
          method: "POST",
          body: { productId: Number(line.id), quantity: line.quantity },
        });
      }
      const order = await api<PlacedOrder>("/orders", { method: "POST" });
      setCart([]);
      await market.refreshProducts();
      notify(`Order ${order.code} placed. Thank you!`);
      return "ok";
    } catch (error) {
      notify(messageOf(error));
      return "failed";
    }
  }
  return (
    <Context.Provider
      value={{ cart, wishlist, recent, add, change, toggleWish, view, checkout, notice, notify }}
    >
      {children}
      {notice && (
        <div className="toast" role="status">
          {notice}
        </div>
      )}
    </Context.Provider>
  );
}
export function useStore() {
  const store = useContext(Context);
  if (!store) throw new Error("DemoStore is required");
  return store;
}
