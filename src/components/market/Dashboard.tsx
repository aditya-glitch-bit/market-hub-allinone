import { useEffect, useState, type FormEvent } from "react";
import { getRouteApi, Link } from "@tanstack/react-router";
import {
  LayoutDashboard,
  Users,
  Package,
  ShoppingBag,
  Activity,
  BarChart3,
  AlertTriangle,
  Heart,
  Clock,
  UserRound,
  MapPin,
  CreditCard,
  Truck,
  DollarSign,
  ArrowUpRight,
  Plus,
  Pencil,
  Trash2,
  X,
  Check,
  Download,
  Store,
} from "lucide-react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { Button } from "@/components/ui/button";
import { categories, demoOrders, demoUsers, imageByFile, money, type Product } from "@/data/demo";
import { api, messageOf } from "@/lib/api";
import { useStore } from "./store";
import { useMarket, fromApi, workspaceOf, type ApiProduct } from "./market";
import { ProductCard } from "./ProductCard";
const routeApi = getRouteApi("/dashboard/$role");
/** One row of the orders table (sample data or an order from the Java API). */
type Row = {
  id: string;
  rawId?: number;
  customer: string;
  date: string;
  product: string;
  total: number;
  status: string;
};
type UserRow = { id?: number; name: string; email: string; role: string; active: boolean };
interface ApiOrder {
  id: number;
  code: string;
  customer: string;
  date: string | null;
  status: string;
  total: number;
  product?: string;
  items: { product: string }[];
}
interface ApiUser {
  id: number;
  name: string;
  email: string;
  role: string;
  active: boolean;
}
interface Summary {
  revenue?: number;
  totalOrders?: number;
  totalUsers?: number;
  totalProducts?: number;
  totalSpent?: number;
  ordersInProgress?: number;
  lowStockThreshold?: number;
  salesByCategory?: Record<string, number>;
  recentOrders?: ApiOrder[];
  recentActivity?: { at: string; actor: string; message: string }[];
}
const toRow = (o: ApiOrder): Row => ({
  id: o.code,
  rawId: o.id,
  customer: o.customer,
  date: o.date
    ? new Date(o.date).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
      })
    : "",
  product: o.product ?? `${o.items.length} items`,
  total: Number(o.total),
  status: o.status,
});
const capitalize = (text: string) => text.charAt(0) + text.slice(1).toLowerCase();
const imageName = (p: Product) =>
  Object.entries(imageByFile).find(([, url]) => url === p.image)?.[0] ?? "product-1.jpg";
const menus = {
  admin: [
    ["overview", "Overview", LayoutDashboard],
    ["users", "User management", Users],
    ["products", "Product management", Package],
    ["orders", "Order management", ShoppingBag],
    ["activity", "Activity monitor", Activity],
  ],
  seller: [
    ["overview", "Overview", LayoutDashboard],
    ["products", "Product listings", Package],
    ["inventory", "Inventory", Store],
    ["orders", "Orders", ShoppingBag],
    ["reports", "Sales reports", BarChart3],
    ["alerts", "Low-stock alerts", AlertTriangle],
  ],
  buyer: [
    ["overview", "Overview", LayoutDashboard],
    ["orders", "My orders", ShoppingBag],
    ["tracking", "Order tracking", Truck],
    ["wishlist", "Wishlist", Heart],
    ["recent", "Recently viewed", Clock],
    ["profile", "My profile", UserRound],
    ["addresses", "Addresses", MapPin],
    ["payments", "Payment methods", CreditCard],
  ],
} as const;
const chartData = [
  { day: "Mon", sales: 820 },
  { day: "Tue", sales: 1250 },
  { day: "Wed", sales: 1050 },
  { day: "Thu", sales: 1680 },
  { day: "Fri", sales: 1450 },
  { day: "Sat", sales: 2150 },
  { day: "Sun", sales: 1840 },
];
function SalesChart({ data }: { data: { day: string; sales: number }[] }) {
  return (
    <div className="chart-container">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 15, right: 10, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="sales-fill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--primary)" stopOpacity={0.22} />
              <stop offset="100%" stopColor="var(--primary)" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke="var(--border)" vertical={false} />
          <XAxis
            dataKey="day"
            axisLine={false}
            tickLine={false}
            tick={{ fill: "var(--muted-foreground)", fontSize: 10 }}
            dy={10}
          />
          <YAxis
            axisLine={false}
            tickLine={false}
            tick={{ fill: "var(--muted-foreground)", fontSize: 10 }}
            tickFormatter={(n) => (n >= 1000 ? `$${n / 1000}k` : `$${n}`)}
          />
          <Tooltip
            content={({ active, payload, label }) =>
              active && payload?.length ? (
                <div className="chart-tooltip">
                  {label}
                  <br />
                  {money(Number(payload[0]?.value ?? 0))}
                </div>
              ) : null
            }
          />
          <Area
            type="monotone"
            dataKey="sales"
            stroke="var(--primary)"
            strokeWidth={2.5}
            fill="url(#sales-fill)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
export function Dashboard() {
  const { role: requested } = routeApi.useParams();
  const role = requested === "admin" ? "admin" : requested === "seller" ? "seller" : "buyer";
  const { section } = routeApi.useSearch();
  const navigate = routeApi.useNavigate();
  const store = useStore();
  const market = useMarket();
  const products = market.products;
  const live = Boolean(market.live && market.user && workspaceOf(market.user) === role);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [orders, setOrders] = useState<Row[]>(demoOrders);
  const [items, setItems] = useState<Product[]>(products);
  const [users, setUsers] = useState<UserRow[]>(demoUsers.map((u) => ({ ...u, active: true })));
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState<Product | null>(null);
  const [adding, setAdding] = useState(false);
  const [profile, setProfile] = useState({
    name: "Alex Morgan",
    email: "alex@example.com",
    phone: "+1 555 010 2048",
  });
  const [addresses, setAddresses] = useState([
    { id: 1, label: "Home", line: "124 Maple Avenue, Portland, OR 97205", name: "Alex Morgan" },
  ]);
  const [addressForm, setAddressForm] = useState(false);
  const [tracking, setTracking] = useState("MH-1048");
  const accountId = market.user?.id;
  const accountName = market.user?.name;
  const accountEmail = market.user?.email;
  useEffect(() => {
    if (!live) {
      setSummary(null);
      setOrders(demoOrders);
      setItems(products);
      setUsers(demoUsers.map((u) => ({ ...u, active: true })));
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const sum = await api<Summary>("/dashboard");
        const list = await api<ApiOrder[]>("/orders");
        if (cancelled) return;
        setSummary(sum);
        setOrders(list.map(toRow));
        if (role === "buyer") setTracking(list[0]?.code ?? "");
        if (accountName && accountEmail)
          setProfile((prev) => ({ ...prev, name: accountName, email: accountEmail }));
        if (role === "seller") {
          const mine = await api<ApiProduct[]>("/products/mine");
          if (!cancelled) setItems(mine.map(fromApi));
        }
        if (role === "admin") {
          const [all, people] = await Promise.all([
            api<{ items: ApiProduct[] }>("/products?size=100"),
            api<ApiUser[]>("/users"),
          ]);
          if (!cancelled) {
            setItems(all.items.map(fromApi));
            setUsers(
              people.map((u) => ({
                id: u.id,
                name: u.name,
                email: u.email,
                role: capitalize(u.role),
                active: u.active,
              })),
            );
          }
        }
      } catch (error) {
        if (!cancelled) store.notify(messageOf(error));
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [live, role, accountId]);
  const isLow = (p: Product) =>
    live ? p.stock > 0 && p.stock <= (summary?.lowStockThreshold ?? 5) : p.stock < 5;
  const currentMenu = menus[role];
  const current = currentMenu.find((m) => m[0] === section);
  const actual = current ? section : "overview";
  const title = current?.[1] ?? "Overview";
  const heading =
    actual === "overview"
      ? role === "admin"
        ? "Your marketplace at a glance"
        : role === "seller"
          ? `Good morning, ${live ? accountName : "Sound Collective"}`
          : `Welcome back, ${live ? accountName : "Alex"}`
      : title;
  function go(section: string) {
    navigate({ to: ".", params: { role }, search: { section } });
  }
  function exportReport() {
    if (live && role === "seller") {
      const link = document.createElement("a");
      link.href = "/api/dashboard/sales.csv";
      link.download = "sales.csv";
      link.click();
      store.notify("Sales report downloaded");
      return;
    }
    const rows = ["Day,Sales", ...chartData.map((r) => `${r.day},${r.sales}`)].join("\n");
    const url = URL.createObjectURL(new Blob([rows], { type: "text/csv" }));
    const a = document.createElement("a");
    a.href = url;
    a.download = "markethub-demo-sales.csv";
    a.click();
    URL.revokeObjectURL(url);
    store.notify("Sample sales report downloaded");
  }
  async function changeStatus(o: Row, status: string) {
    if (live && o.rawId !== undefined) {
      try {
        const updated = await api<ApiOrder>(`/orders/${o.rawId}/status`, {
          method: "PUT",
          body: { status: status.toLowerCase() },
        });
        setOrders((prev) => prev.map((x) => (x.rawId === o.rawId ? toRow(updated) : x)));
        store.notify(`Order ${o.id} is now ${updated.status}`);
      } catch (error) {
        store.notify(messageOf(error));
      }
      return;
    }
    setOrders((prev) => prev.map((item) => (item.id === o.id ? { ...item, status } : item)));
  }
  async function toggleUser(u: UserRow) {
    if (live && u.id !== undefined) {
      try {
        const saved = await api<ApiUser>(`/users/${u.id}/active`, {
          method: "PUT",
          body: { active: !u.active },
        });
        setUsers((prev) => prev.map((x) => (x.id === u.id ? { ...x, active: saved.active } : x)));
      } catch (error) {
        store.notify(messageOf(error));
      }
      return;
    }
    setUsers((prev) => prev.map((x) => (x.email === u.email ? { ...x, active: !x.active } : x)));
  }
  async function commitStock(p: Product, stock: number) {
    if (!live) return;
    try {
      const saved = await api<ApiProduct>(`/products/${p.id}/stock`, {
        method: "PUT",
        body: { stock },
      });
      setItems((prev) => prev.map((x) => (x.id === p.id ? fromApi(saved) : x)));
      store.notify("Stock updated");
      void market.refreshProducts();
    } catch (error) {
      store.notify(messageOf(error));
    }
  }
  async function removeItem(p: Product) {
    if (live) {
      try {
        await api(`/products/${p.id}`, { method: "DELETE" });
        setItems((prev) => prev.filter((x) => x.id !== p.id));
        store.notify("Listing removed");
        void market.refreshProducts();
      } catch (error) {
        store.notify(messageOf(error));
      }
      return;
    }
    setItems((prev) => prev.filter((x) => x.id !== p.id));
    store.notify("Sample listing removed");
  }
  function restock(p: Product) {
    if (live) {
      void commitStock(p, 20);
      return;
    }
    setItems((prev) => prev.map((item) => (item.id === p.id ? { ...item, stock: 20 } : item)));
    store.notify("Sample inventory restocked");
  }
  async function saveProduct(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    const category = String(data.get("category") || editing?.category || "Electronics");
    if (live) {
      const draft = {
        name: String(data.get("name")),
        category,
        price: Number(data.get("price")),
        oldPrice: editing?.oldPrice ?? 0,
        stock: Number(data.get("stock")),
        image: editing ? imageName(editing) : "product-1.jpg",
        badge: editing?.badge ?? "",
        sellerId: role === "admin" ? Number(data.get("sellerId")) : undefined,
      };
      try {
        const saved = editing
          ? await api<ApiProduct>(`/products/${editing.id}`, { method: "PUT", body: draft })
          : await api<ApiProduct>("/products", { method: "POST", body: draft });
        const next = fromApi(saved);
        setItems((prev) =>
          editing ? prev.map((x) => (x.id === editing.id ? next : x)) : [...prev, next],
        );
        setEditing(null);
        setAdding(false);
        store.notify("Listing saved");
        void market.refreshProducts();
      } catch (error) {
        store.notify(messageOf(error));
      }
      return;
    }
    const base = editing ?? products[0];
    if (!base) return;
    const updated = {
      ...base,
      id: editing?.id ?? `sample-${Date.now()}`,
      name: String(data.get("name")),
      category,
      seller: role === "seller" ? "Sound Collective" : String(data.get("seller")),
      price: Number(data.get("price")),
      stock: Number(data.get("stock")),
    };
    setItems((prev) =>
      editing ? prev.map((x) => (x.id === editing.id ? updated : x)) : [...prev, updated],
    );
    setEditing(null);
    setAdding(false);
    store.notify("Sample listing saved");
  }
  function orderTable() {
    return (
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Order</th>
              <th>{role === "buyer" ? "Product" : "Customer"}</th>
              <th>Date</th>
              <th>Total</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {orders
              .filter((o) =>
                `${o.id} ${o.customer} ${o.product}`.toLowerCase().includes(query.toLowerCase()),
              )
              .map((o) => (
                <tr key={o.id}>
                  <td>
                    <strong>#{o.id}</strong>
                  </td>
                  <td>{role === "buyer" ? o.product : o.customer}</td>
                  <td>{o.date}</td>
                  <td>{money(o.total)}</td>
                  <td>
                    <span className={`status ${o.status.toLowerCase()}`}>{o.status}</span>
                  </td>
                  <td>
                    {role === "buyer" ? (
                      <Button
                        variant="link"
                        size="sm"
                        onClick={() => {
                          setTracking(o.id);
                          go("tracking");
                        }}
                      >
                        Track <ArrowUpRight />
                      </Button>
                    ) : (
                      <select
                        className="select-input"
                        aria-label={`Status for ${o.id}`}
                        value={o.status}
                        onChange={(e) => void changeStatus(o, e.target.value)}
                      >
                        <option>Processing</option>
                        <option>Shipped</option>
                        <option>Delivered</option>
                        <option>Cancelled</option>
                      </select>
                    )}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
        {orders.filter((o) =>
          `${o.id} ${o.customer} ${o.product}`.toLowerCase().includes(query.toLowerCase()),
        ).length === 0 && (
          <div className="empty">
            <p>No orders match your search.</p>
          </div>
        )}
      </div>
    );
  }
  function productTable(inventory = false) {
    const shown = items.filter(
      (p) =>
        (live || role === "admin" || p.seller === "Sound Collective") &&
        `${p.name} ${p.seller}`.toLowerCase().includes(query.toLowerCase()),
    );
    return (
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Product</th>
              {role === "admin" && <th>Seller</th>}
              <th>Price</th>
              <th>Stock</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {shown.map((p) => (
              <tr key={p.id}>
                <td className="product-cell">
                  <img src={p.image} alt="" />
                  {p.name}
                </td>
                {role === "admin" && <td>{p.seller}</td>}
                <td>{money(p.price)}</td>
                <td>
                  {inventory ? (
                    <input
                      className="field w-20"
                      aria-label={`Stock for ${p.name}`}
                      type="number"
                      min={0}
                      value={p.stock}
                      onChange={(e) =>
                        setItems((prev) =>
                          prev.map((x) =>
                            x.id === p.id
                              ? { ...x, stock: Math.max(0, Number(e.target.value)) }
                              : x,
                          ),
                        )
                      }
                      onBlur={(e) => void commitStock(p, Math.max(0, Number(e.target.value)))}
                    />
                  ) : (
                    p.stock
                  )}
                </td>
                <td>
                  <span className={`status ${isLow(p) ? "low" : ""}`}>
                    {isLow(p) ? "Low stock" : "Active"}
                  </span>
                </td>
                <td>
                  <Button
                    variant="ghost"
                    size="icon"
                    title="Edit product"
                    aria-label={`Edit ${p.name}`}
                    onClick={() => setEditing(p)}
                  >
                    <Pencil />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    title="Remove listing"
                    aria-label={`Remove ${p.name}`}
                    onClick={() => void removeItem(p)}
                  >
                    <Trash2 />
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!shown.length && (
          <div className="empty">
            <Package size={30} />
            <h3>No listings found</h3>
            <Button onClick={() => setAdding(true)}>Add a sample listing</Button>
          </div>
        )}
      </div>
    );
  }
  function activity() {
    if (live && summary) {
      const events = summary.recentActivity ?? [];
      return events.length ? (
        <>
          {events.map((a, i) => (
            <div className="activity-item" key={i}>
              <span className="activity-icon">
                <Activity size={16} />
              </span>
              <div>
                <strong>{a.message}</strong>
                <p>{a.actor}</p>
                <p>{new Date(a.at).toLocaleString()}</p>
              </div>
            </div>
          ))}
        </>
      ) : (
        <div className="empty">
          <p>No activity yet.</p>
        </div>
      );
    }
    return (
      <>
        {[
          {
            icon: ShoppingBag,
            title: "New order received",
            text: "#MH-1048 · Olivia Martin",
            time: "12 minutes ago",
          },
          {
            icon: Users,
            title: "A new seller joined",
            text: "Botanical House",
            time: "35 minutes ago",
          },
          {
            icon: Package,
            title: "Product listing updated",
            text: "Studio Wireless Headphones",
            time: "1 hour ago",
          },
          {
            icon: Check,
            title: "Order successfully delivered",
            text: "#MH-1046 · Emma Thompson",
            time: "2 hours ago",
          },
        ].map((a) => (
          <div className="activity-item" key={a.title}>
            <span className="activity-icon">
              <a.icon size={16} />
            </span>
            <div>
              <strong>{a.title}</strong>
              <p>{a.text}</p>
              <p>{a.time}</p>
            </div>
          </div>
        ))}
      </>
    );
  }
  function alerts() {
    const low = items.filter(isLow);
    return low.length ? (
      <>
        {low.map((p) => (
          <div key={p.id} className="inventory-alert">
            <div>
              <h4>{p.name}</h4>
              <p>Only {p.stock} left in stock</p>
            </div>
            <Button variant="outline" size="sm" onClick={() => restock(p)}>
              Restock
            </Button>
          </div>
        ))}
      </>
    ) : (
      <div className="empty">
        <Check size={30} />
        <p>All sample products are well stocked.</p>
      </div>
    );
  }
  const demoStats =
    role === "admin"
      ? [
          ["Marketplace revenue", "$24,580", DollarSign, "↑ 12.8% this month"],
          ["Total orders", "184", ShoppingBag, "↑ 8.2% this month"],
          ["Active sellers", "32", Store, "4 new this month"],
          ["Total customers", "2,148", Users, "↑ 15.3% this month"],
        ]
      : role === "seller"
        ? [
            ["Total sales", "$8,420", DollarSign, "↑ 18.6% this month"],
            ["Orders", "64", ShoppingBag, "↑ 12.4% this month"],
            [
              "Active products",
              String(items.filter((p) => p.seller === "Sound Collective").length),
              Package,
              "Your published listings",
            ],
            [
              "Low stock",
              String(items.filter((p) => p.stock < 5).length),
              AlertTriangle,
              "Items needing attention",
            ],
          ]
        : [
            ["Total orders", "12", ShoppingBag, "2 orders on the way"],
            ["Wishlist", String(store.wishlist.length), Heart, "Your saved favorites"],
            ["Total spent", "$842", DollarSign, "A collection of good finds"],
            ["On the way", "2", Truck, "Your next favorites are coming"],
          ];
  const series =
    live && summary
      ? role === "seller"
        ? Object.entries(summary.salesByCategory ?? {}).map(([day, sales]) => ({
            day,
            sales: Number(sales),
          }))
        : (summary.recentOrders ?? [])
            .slice()
            .reverse()
            .map((o) => ({ day: o.code, sales: Number(o.total) }))
      : chartData;
  const stats =
    live && summary
      ? role === "admin"
        ? [
            [
              "Marketplace revenue",
              money(Number(summary.revenue ?? 0)),
              DollarSign,
              "Cancelled orders excluded",
            ],
            ["Total orders", String(summary.totalOrders ?? 0), ShoppingBag, "All orders so far"],
            ["Products", String(summary.totalProducts ?? 0), Package, "Across all sellers"],
            ["Users", String(summary.totalUsers ?? 0), Users, "Buyers, sellers and admins"],
          ]
        : role === "seller"
          ? [
              ["Total sales", money(Number(summary.revenue ?? 0)), DollarSign, "From your orders"],
              ["Orders", String(summary.totalOrders ?? 0), ShoppingBag, "Containing your products"],
              ["Active products", String(items.length), Package, "Your published listings"],
              [
                "Low stock",
                String(items.filter(isLow).length),
                AlertTriangle,
                "Items needing attention",
              ],
            ]
          : [
              [
                "Total orders",
                String(summary.totalOrders ?? 0),
                ShoppingBag,
                `${summary.ordersInProgress ?? 0} on the way`,
              ],
              ["Wishlist", String(store.wishlist.length), Heart, "Your saved favorites"],
              [
                "Total spent",
                money(Number(summary.totalSpent ?? 0)),
                DollarSign,
                "Cancelled orders excluded",
              ],
              ["On the way", String(summary.ordersInProgress ?? 0), Truck, "Processing or shipped"],
            ]
      : demoStats;
  return (
    <main className="dashboard">
      <aside className="dash-sidebar">
        <div className="workspace-label">{role} workspace</div>
        {currentMenu.map(([key, name, Icon]) => (
          <Link
            key={key}
            to="/dashboard/$role"
            params={{ role }}
            search={{ section: key }}
            className={actual === key ? "active" : ""}
          >
            <Icon />
            {name}
          </Link>
        ))}
        <div className="role-switch">
          <label htmlFor="role-switch">Preview workspace</label>
          <select
            id="role-switch"
            className="select-input"
            value={role}
            onChange={(e) =>
              navigate({
                to: ".",
                params: { role: e.target.value },
                search: { section: "overview" },
              })
            }
          >
            <option value="buyer">Buyer</option>
            <option value="seller">Seller</option>
            <option value="admin">Admin</option>
          </select>
        </div>
      </aside>
      <div className="dash-main">
        <div className="dash-heading">
          <div>
            <h1>{heading}</h1>
            <p>
              {actual === "overview"
                ? "Here’s what’s happening in your corner of MarketHub."
                : "Everything you need, all in one place."}
            </p>
          </div>
          <span className="sample-label">
            {live
              ? "Live data"
              : market.live
                ? `Sample data · Sign in as ${role} for live data`
                : "Sample data · Preview only"}
          </span>
        </div>
        {actual === "overview" && (
          <>
            <div className="stats-grid">
              {stats.map(([label, value, Icon, caption]) => (
                <div className="stat-card" key={String(label)}>
                  <div>
                    <span>{String(label)}</span>
                    {Icon && typeof Icon !== "string" && <Icon />}
                  </div>
                  <strong>{String(value)}</strong>
                  <p>{String(caption)}</p>
                </div>
              ))}
            </div>
            {role === "buyer" ? (
              <>
                <section className="dash-section">
                  <div className="section-heading">
                    <h2>Your recent orders</h2>
                    <Button variant="link" size="sm" onClick={() => go("orders")}>
                      View all orders <ArrowUpRight />
                    </Button>
                  </div>
                  {orderTable()}
                </section>
                <section className="dash-section">
                  <div className="section-heading">
                    <h2>Picked for you</h2>
                    <Button asChild variant="link" size="sm">
                      <Link to="/products" search={{ q: "", category: "All", sort: "featured" }}>
                        Keep exploring <ArrowUpRight />
                      </Link>
                    </Button>
                  </div>
                  <div className="product-grid">
                    {products.slice(0, 3).map((p) => (
                      <ProductCard key={p.id} product={p} />
                    ))}
                  </div>
                </section>
              </>
            ) : (
              <>
                <div className="dashboard-columns">
                  <section className="dash-section">
                    <div className="section-heading">
                      <div>
                        <h2>Sales overview</h2>
                        <p>
                          {live
                            ? role === "seller"
                              ? "Revenue by category"
                              : "Totals of the latest orders"
                            : "Sample revenue for the last seven days"}
                        </p>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        title="Download sample report"
                        aria-label="Download sample report"
                        onClick={exportReport}
                      >
                        <Download />
                      </Button>
                    </div>
                    <SalesChart data={series} />
                  </section>
                  <section className="dash-section">
                    <div className="section-heading">
                      <h2>{role === "admin" ? "Recent activity" : "Low-stock alerts"}</h2>
                    </div>
                    {role === "admin" ? activity() : alerts()}
                  </section>
                </div>
                <section className="dash-section">
                  <div className="section-heading">
                    <h2>Recent orders</h2>
                    <Button variant="link" size="sm" onClick={() => go("orders")}>
                      View all orders <ArrowUpRight />
                    </Button>
                  </div>
                  {orderTable()}
                </section>
              </>
            )}
          </>
        )}
        {(actual === "products" || actual === "inventory") && (
          <>
            <div className="dash-toolbar">
              <input
                className="field"
                placeholder="Search listings..."
                aria-label="Search listings"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
              <Button onClick={() => setAdding(true)}>
                <Plus />
                Add product
              </Button>
            </div>
            {productTable(actual === "inventory")}
            {!live && (
              <p className="demo-note">
                Changes affect only this sample workspace and reset on refresh.
              </p>
            )}
          </>
        )}
        {actual === "orders" && (
          <>
            <div className="dash-toolbar">
              <input
                className="field"
                placeholder="Search orders..."
                aria-label="Search orders"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
              <span className="muted">
                {orders.length} {live ? "" : "sample "}orders
              </span>
            </div>
            {orderTable()}
          </>
        )}
        {actual === "users" && (
          <>
            <div className="dash-toolbar">
              <input
                className="field"
                placeholder="Search users..."
                aria-label="Search users"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
              <span className="muted">
                {users.length} {live ? "" : "sample "}users
              </span>
            </div>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {users
                    .filter((u) =>
                      `${u.name} ${u.email}`.toLowerCase().includes(query.toLowerCase()),
                    )
                    .map((u) => (
                      <tr key={u.email}>
                        <td>{u.name}</td>
                        <td>{u.email}</td>
                        <td>{u.role}</td>
                        <td>
                          <span className={`status ${u.active ? "" : "low"}`}>
                            {u.active ? "Active" : "Suspended"}
                          </span>
                        </td>
                        <td>
                          <Button variant="outline" size="sm" onClick={() => void toggleUser(u)}>
                            {u.active ? "Suspend" : "Activate"}
                          </Button>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </>
        )}
        {actual === "activity" && <section className="dash-section">{activity()}</section>}
        {actual === "reports" && (
          <>
            <div className="dash-toolbar">
              <h2 className="text-lg">Sales performance</h2>
              <Button variant="outline" onClick={exportReport}>
                <Download />
                Export sample CSV
              </Button>
            </div>
            <div className="stats-grid">
              {stats.slice(0, 2).map(([label, value]) => (
                <div className="stat-card" key={String(label)}>
                  <div>{String(label)}</div>
                  <strong>{String(value)}</strong>
                  <p>This month · Sample data</p>
                </div>
              ))}
            </div>
            <section className="dash-section">
              <SalesChart data={series} />
            </section>
            {productTable()}
          </>
        )}
        {actual === "alerts" && (
          <section className="dash-section">
            {alerts()}
            <p className="demo-note">Sample alert threshold: fewer than 5 items.</p>
          </section>
        )}
        {(actual === "wishlist" || actual === "recent") &&
          (() => {
            const ids = actual === "wishlist" ? store.wishlist : store.recent;
            const list = ids
              .map((id) => products.find((p) => p.id === id))
              .filter((p): p is Product => Boolean(p));
            return list.length ? (
              <div className="product-grid">
                {list.map((p) => (
                  <ProductCard key={p.id} product={p} />
                ))}
              </div>
            ) : (
              <div className="empty">
                {actual === "wishlist" ? <Heart size={38} /> : <Clock size={38} />}
                <h3>
                  {actual === "wishlist"
                    ? "A place for your favorites"
                    : "Your discoveries start here"}
                </h3>
                <p>
                  {actual === "wishlist"
                    ? "Save products you love and find them here."
                    : "Products you explore will appear here."}
                </p>
                <Button asChild>
                  <Link to="/products" search={{ q: "", category: "All", sort: "featured" }}>
                    Explore the marketplace <ArrowUpRight />
                  </Link>
                </Button>
              </div>
            );
          })()}
        {actual === "tracking" && (
          <>
            <select
              className="select-input"
              aria-label="Select order to track"
              value={tracking}
              onChange={(e) => setTracking(e.target.value)}
            >
              {orders.map((o) => (
                <option key={o.id} value={o.id}>
                  #{o.id} · {o.product}
                </option>
              ))}
            </select>
            <section className="dash-section">
              <h2>On its way to something good</h2>
              {["Order confirmed", "Packed with care", "Shipped", "Delivered"].map((step, i) => {
                const status = orders.find((o) => o.id === tracking)?.status;
                const complete =
                  i < 2 ||
                  (i === 2 && (status === "Shipped" || status === "Delivered")) ||
                  (i === 3 && status === "Delivered");
                return (
                  <div className="track-step" key={step}>
                    <span>{complete ? <Check size={17} /> : <Clock size={17} />}</span>
                    <div>
                      <h4>{step}</h4>
                      <p>{complete ? "Complete" : "Pending"} · Sample tracking event</p>
                    </div>
                  </div>
                );
              })}
            </section>
          </>
        )}
        {actual === "profile" && (
          <form
            className="edit-form"
            onSubmit={(e) => {
              e.preventDefault();
              store.notify("Sample profile updated for this preview");
            }}
          >
            <label>
              Full name
              <input
                required
                value={profile.name}
                onChange={(e) => setProfile({ ...profile, name: e.target.value })}
              />
            </label>
            <label>
              Email address
              <input
                required
                type="email"
                value={profile.email}
                onChange={(e) => setProfile({ ...profile, email: e.target.value })}
              />
            </label>
            <label>
              Phone number
              <input
                type="tel"
                value={profile.phone}
                onChange={(e) => setProfile({ ...profile, phone: e.target.value })}
              />
            </label>
            <Button type="submit">
              Save changes <Check />
            </Button>
            <p className="demo-note">
              No personal information is stored beyond this preview session.
            </p>
          </form>
        )}
        {actual === "addresses" && (
          <>
            <Button variant="outline" onClick={() => setAddressForm(true)}>
              <Plus />
              Add address
            </Button>
            {addresses.length === 0 && (
              <div className="empty">
                <MapPin />
                <p>No sample addresses saved.</p>
              </div>
            )}
            {addresses.map((a) => (
              <div className="address-item" key={a.id}>
                <div className="section-heading">
                  <h3>{a.label}</h3>
                  <Button
                    variant="ghost"
                    size="icon"
                    title="Remove address"
                    aria-label={`Remove ${a.label} address`}
                    onClick={() => setAddresses((prev) => prev.filter((x) => x.id !== a.id))}
                  >
                    <Trash2 />
                  </Button>
                </div>
                <strong>{a.name}</strong>
                <p>{a.line}</p>
                <span className="sample-label">Sample address</span>
              </div>
            ))}
          </>
        )}
        {actual === "payments" && (
          <div className="empty">
            <CreditCard size={40} />
            <h3>No payment methods connected</h3>
            <p>
              Payments are not implemented in this frontend preview.
              <br />
              No card details are collected or stored.
            </p>
            <Button
              variant="outline"
              onClick={() =>
                store.notify(
                  "Payment method setup requires a payment service, which is not connected yet.",
                )
              }
            >
              Set up a payment method
            </Button>
          </div>
        )}
        {(editing || adding) && (
          <div
            className="modal-backdrop"
            onClick={() => {
              setEditing(null);
              setAdding(false);
            }}
          >
            <section
              className="modal"
              role="dialog"
              aria-modal="true"
              aria-label={editing ? "Edit sample product" : "Add sample product"}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="section-heading">
                <h2>{editing ? "Edit product" : "Add product"}</h2>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label="Close product form"
                  onClick={() => {
                    setEditing(null);
                    setAdding(false);
                  }}
                >
                  <X />
                </Button>
              </div>
              <form className="edit-form" onSubmit={(e) => void saveProduct(e)}>
                <label>
                  Product name
                  <input name="name" required defaultValue={editing?.name} />
                </label>
                <label>
                  Category
                  <select
                    name="category"
                    className="select-input"
                    defaultValue={editing?.category ?? "Electronics"}
                  >
                    {categories.map((c) => (
                      <option key={c}>{c}</option>
                    ))}
                  </select>
                </label>
                {role === "admin" &&
                  (live ? (
                    <label>
                      Seller
                      <select
                        name="sellerId"
                        className="select-input"
                        defaultValue={
                          editing?.sellerId ?? users.find((u) => u.role === "Seller")?.id
                        }
                      >
                        {users
                          .filter((u) => u.role === "Seller")
                          .map((u) => (
                            <option key={u.id} value={u.id}>
                              {u.name}
                            </option>
                          ))}
                      </select>
                    </label>
                  ) : (
                    <label>
                      Seller
                      <input
                        name="seller"
                        required
                        defaultValue={editing?.seller ?? "Sound Collective"}
                      />
                    </label>
                  ))}
                <div className="form-grid">
                  <label>
                    Price ($)
                    <input
                      name="price"
                      type="number"
                      step="0.01"
                      min="0.01"
                      required
                      defaultValue={editing?.price ?? 49}
                    />
                  </label>
                  <label>
                    Stock
                    <input
                      name="stock"
                      type="number"
                      min="0"
                      required
                      defaultValue={editing?.stock ?? 20}
                    />
                  </label>
                </div>
                {!live && (
                  <p className="demo-note">
                    New sample listings use a placeholder from the demo product collection. Changes
                    are not saved to production.
                  </p>
                )}
                <Button type="submit">
                  Save sample listing <Check />
                </Button>
              </form>
            </section>
          </div>
        )}
        {addressForm && (
          <div className="modal-backdrop">
            <section
              className="modal"
              role="dialog"
              aria-modal="true"
              aria-label="Add sample address"
            >
              <div className="section-heading">
                <h2>Add address</h2>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label="Close address form"
                  onClick={() => setAddressForm(false)}
                >
                  <X />
                </Button>
              </div>
              <form
                className="edit-form"
                onSubmit={(e) => {
                  e.preventDefault();
                  const d = new FormData(e.currentTarget);
                  setAddresses((prev) => [
                    ...prev,
                    {
                      id: Date.now(),
                      label: String(d.get("label")),
                      name: String(d.get("name")),
                      line: String(d.get("line")),
                    },
                  ]);
                  setAddressForm(false);
                  store.notify("Sample address added");
                }}
              >
                <label>
                  Label
                  <input name="label" placeholder="Home or work" required />
                </label>
                <label>
                  Full name
                  <input name="name" required />
                </label>
                <label>
                  Address
                  <input name="line" required />
                </label>
                <Button type="submit">Save sample address</Button>
                <p className="demo-note">For preview only. Please use fictional details.</p>
              </form>
            </section>
          </div>
        )}
      </div>
    </main>
  );
}
