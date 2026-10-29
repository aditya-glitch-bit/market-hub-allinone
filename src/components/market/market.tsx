import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api, ApiError, messageOf } from "@/lib/api";
import { products as demoProducts, imageByFile, type Product } from "@/data/demo";

/** A signed-in account as returned by the Java API. */
export interface AppUser {
  id: number;
  name: string;
  email: string;
  role: "ADMIN" | "SELLER" | "BUYER";
  active: boolean;
}

/** Product as the Java API sends it. */
export interface ApiProduct {
  id: number;
  name: string;
  seller: string;
  sellerId: number;
  category: string;
  price: number;
  oldPrice: number | null;
  rating: number;
  reviews: number;
  image: string;
  badge: string | null;
  stock: number;
}

interface PageOf<T> {
  items: T[];
}

export function fromApi(p: ApiProduct): Product {
  return {
    id: String(p.id),
    name: p.name,
    seller: p.seller,
    sellerId: p.sellerId,
    category: p.category,
    price: Number(p.price),
    oldPrice: Number(p.oldPrice ?? 0),
    rating: Number(p.rating),
    reviews: p.reviews,
    image: imageByFile[p.image] ?? imageByFile["product-1.jpg"] ?? "",
    badge: p.badge ?? "",
    stock: p.stock,
  };
}

interface Market {
  /** null while the first connection attempt is running, then true (API) or false (sample data). */
  live: boolean | null;
  /** false until the first connection attempt has finished. */
  ready: boolean;
  products: Product[];
  user: AppUser | null;
  refreshProducts: () => Promise<void>;
  login: (email: string, password: string) => Promise<AppUser>;
  register: (name: string, email: string, password: string, role: string) => Promise<AppUser>;
  logout: () => Promise<void>;
}

const Context = createContext<Market | null>(null);

/**
 * Decides once at start-up whether the Java backend answers. If it does the site shows live
 * products and real accounts; if it does not, the sample-data preview keeps working.
 */
export function MarketProvider({ children }: { children: ReactNode }) {
  const [live, setLive] = useState<boolean | null>(null);
  const [ready, setReady] = useState(false);
  const [products, setProducts] = useState<Product[]>(demoProducts);
  const [user, setUser] = useState<AppUser | null>(null);

  const refreshProducts = useCallback(async () => {
    const page = await api<PageOf<ApiProduct>>("/products?size=100");
    setProducts(page.items.map(fromApi));
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        // A sleeping free-tier server can need up to a minute to wake up.
        await api("/health", { timeoutMs: 60000 });
        await refreshProducts();
        const me = await api<AppUser>("/auth/me").catch((error: unknown) => {
          if (error instanceof ApiError && error.status === 401) return null;
          throw error;
        });
        if (cancelled) return;
        setUser(me);
        setLive(true);
      } catch {
        if (cancelled) return;
        setProducts(demoProducts);
        setLive(false);
      } finally {
        if (!cancelled) setReady(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [refreshProducts]);

  const login = useCallback(async (email: string, password: string) => {
    const account = await api<AppUser>("/auth/login", {
      method: "POST",
      body: { email, password },
    });
    setUser(account);
    return account;
  }, []);

  const register = useCallback(
    async (name: string, email: string, password: string, role: string) => {
      const account = await api<AppUser>("/auth/register", {
        method: "POST",
        body: { name, email, password, role },
      });
      setUser(account);
      return account;
    },
    [],
  );

  const logout = useCallback(async () => {
    try {
      await api("/auth/logout", { method: "POST" });
    } catch (error) {
      console.warn(messageOf(error));
    }
    setUser(null);
  }, []);

  return (
    <Context.Provider
      value={{ live, ready, products, user, refreshProducts, login, register, logout }}
    >
      {children}
    </Context.Provider>
  );
}

export function useMarket() {
  const market = useContext(Context);
  if (!market) throw new Error("MarketProvider is required");
  return market;
}

/** Workspace route of an account ("admin", "seller" or "buyer"). */
export function workspaceOf(user: AppUser): "admin" | "seller" | "buyer" {
  return user.role === "ADMIN" ? "admin" : user.role === "SELLER" ? "seller" : "buyer";
}
