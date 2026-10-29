import { useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  Search,
  ShoppingBag,
  Heart,
  UserRound,
  ChevronDown,
  ArrowUpRight,
  Menu,
  X,
  ArrowRight,
  Minus,
  Plus,
  Trash2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { money } from "@/data/demo";
import { useStore } from "./store";
import { useMarket, workspaceOf } from "./market";
export function Logo() {
  return (
    <Link to="/" className="brand">
      <span className="brand-symbol">
        <ShoppingBag size={22} />
      </span>
      Market<span>Hub</span>
      <span className="brand-dot">.</span>
    </Link>
  );
}
export function Header() {
  const [search, setSearch] = useState("");
  const [menu, setMenu] = useState(false);
  const [bag, setBag] = useState(false);
  const navigate = useNavigate();
  const store = useStore();
  const market = useMarket();
  const products = market.products;
  const count = store.cart.reduce((a, b) => a + b.quantity, 0);
  return (
    <>
      <div className="announcement">
        <span>A little discovery. A lot to love.</span>
        <span>
          Discover independent brands <ArrowUpRight size={13} />
        </span>
      </div>
      <header className="site-header">
        <div className="container main-nav">
          <Logo />
          <form
            className="search"
            onSubmit={(e) => {
              e.preventDefault();
              navigate({
                to: "/products",
                search: { q: search, category: "All", sort: "featured" },
              });
            }}
          >
            <Search size={19} />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search for something you'll love..."
              aria-label="Search products"
            />
            <span className="search-key">⌘ K</span>
          </form>
          <div className="header-actions">
            <Button
              asChild
              variant="ghost"
              size="icon"
              title={market.user ? `Signed in as ${market.user.name}` : "Your account"}
            >
              {market.user ? (
                <Link
                  to="/dashboard/$role"
                  params={{ role: workspaceOf(market.user) }}
                  search={{ section: "overview" }}
                  aria-label="Your workspace"
                >
                  <UserRound />
                </Link>
              ) : (
                <Link to="/login" aria-label="Your account">
                  <UserRound />
                </Link>
              )}
            </Button>
            <Button asChild variant="ghost" size="icon" title="Wishlist">
              <Link
                to="/dashboard/$role"
                params={{ role: "buyer" }}
                search={{ section: "wishlist" }}
                aria-label="Wishlist"
              >
                <Heart />
              </Link>
            </Button>
            <Button
              variant="ghost"
              className="bag-button"
              onClick={() => setBag(true)}
              aria-label={`Shopping bag, ${count} items`}
            >
              <ShoppingBag />
              <span className="cart-count">{count}</span>
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="mobile-menu"
              aria-label="Open menu"
              onClick={() => setMenu(!menu)}
            >
              {menu ? <X /> : <Menu />}
            </Button>
          </div>
        </div>
        <nav className={`container category-nav ${menu ? "open" : ""}`}>
          <div className="nav-links">
            <Link
              to="/products"
              search={{ category: "All", q: "", sort: "featured" }}
              className="all-categories"
            >
              <Menu size={16} /> All categories <ChevronDown size={13} />
            </Link>
            {["Electronics", "Fashion", "Home & Living", "Beauty & Care"].map((category) => (
              <Link key={category} to="/products" search={{ category, q: "", sort: "featured" }}>
                {category}
              </Link>
            ))}
            <Link
              to="/products"
              search={{ category: "All", q: "", sort: "deals" }}
              className="deals-link"
            >
              Today's deals <span className="tiny-dot" />
            </Link>
          </div>
          <Link
            to="/dashboard/$role"
            params={{ role: "seller" }}
            search={{ section: "overview" }}
            className="seller-link"
          >
            Become a seller <ArrowUpRight size={14} />
          </Link>
        </nav>
      </header>
      {bag && (
        <div className="drawer-backdrop" onClick={() => setBag(false)}>
          <aside
            className="cart-drawer"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-label="Shopping bag"
          >
            <div className="section-heading">
              <h2>
                Your bag <span className="muted">({count})</span>
              </h2>
              <Button
                variant="ghost"
                size="icon"
                aria-label="Close bag"
                onClick={() => setBag(false)}
              >
                <X />
              </Button>
            </div>
            {store.cart.length === 0 ? (
              <div className="empty">
                <ShoppingBag size={42} />
                <h3>Your next favorite is waiting</h3>
                <p>Your shopping bag is empty.</p>
                <Button onClick={() => setBag(false)}>
                  Keep exploring <ArrowRight />
                </Button>
              </div>
            ) : (
              <>
                <div className="cart-items">
                  {store.cart.map((item) => {
                    const p = products.find((p) => p.id === item.id);
                    if (!p) return null;
                    return (
                      <div className="cart-item" key={p.id}>
                        <img src={p.image} alt={p.name} />
                        <div>
                          <h4>{p.name}</h4>
                          <p>{money(p.price)}</p>
                          <div className="quantity">
                            <Button
                              variant="ghost"
                              size="icon"
                              aria-label="Decrease quantity"
                              onClick={() => store.change(p.id, item.quantity - 1)}
                            >
                              <Minus />
                            </Button>
                            <span>{item.quantity}</span>
                            <Button
                              variant="ghost"
                              size="icon"
                              aria-label="Increase quantity"
                              onClick={() => store.change(p.id, item.quantity + 1)}
                            >
                              <Plus />
                            </Button>
                          </div>
                        </div>
                        <Button
                          variant="ghost"
                          size="icon"
                          title="Remove"
                          aria-label={`Remove ${p.name}`}
                          onClick={() => store.change(p.id, 0)}
                        >
                          <Trash2 />
                        </Button>
                      </div>
                    );
                  })}
                </div>
                <div className="cart-total">
                  <div className="price-row">
                    <span>Subtotal</span>
                    <strong>
                      {money(
                        store.cart.reduce(
                          (a, item) =>
                            a +
                            (products.find((p) => p.id === item.id)?.price ?? 0) * item.quantity,
                          0,
                        ),
                      )}
                    </strong>
                  </div>
                  <p className="demo-note">
                    {market.live
                      ? "No payment is collected. Orders are saved to your account."
                      : "Demo bag · No payments are collected."}
                  </p>
                  <Button
                    className="w-full"
                    onClick={async () => {
                      const result = await store.checkout();
                      if (result === "ok") {
                        setBag(false);
                        navigate({
                          to: "/dashboard/$role",
                          params: { role: "buyer" },
                          search: { section: "orders" },
                        });
                      } else if (result === "login") {
                        setBag(false);
                        navigate({ to: "/login" });
                      }
                    }}
                  >
                    Continue to checkout <ArrowRight />
                  </Button>
                </div>
              </>
            )}
          </aside>
        </div>
      )}
    </>
  );
}
export function Footer() {
  const market = useMarket();
  return (
    <footer className="site-footer">
      <div className="container footer-top">
        <div>
          <Logo />
          <p>
            Great finds. Independent brands.
            <br />A marketplace made for you.
          </p>
        </div>
        <div>
          <h4>Explore</h4>
          <Link to="/products" search={{ q: "", category: "All", sort: "featured" }}>
            All products
          </Link>
          <Link to="/products" search={{ q: "", category: "All", sort: "deals" }}>
            Today's deals
          </Link>
        </div>
        <div>
          <h4>Your MarketHub</h4>
          <Link to="/dashboard/$role" params={{ role: "buyer" }} search={{ section: "overview" }}>
            Buyer account
          </Link>
          <Link to="/dashboard/$role" params={{ role: "seller" }} search={{ section: "overview" }}>
            Seller workspace
          </Link>
          <Link to="/dashboard/$role" params={{ role: "admin" }} search={{ section: "overview" }}>
            Admin workspace
          </Link>
        </div>
        <div>
          <h4>A little inspiration, in your inbox.</h4>
          <Newsletter />
        </div>
      </div>
      <div className="container footer-bottom">
        <span>© 2026 MarketHub. All rights reserved.</span>
        <span>
          {market.live ? "Connected to the MarketHub API" : "Frontend preview · Sample data only"}
        </span>
        <span>Thoughtfully curated. Independently sold.</span>
      </div>
    </footer>
  );
}
function Newsletter() {
  const store = useStore();
  return (
    <form
      className="newsletter"
      onSubmit={(e) => {
        e.preventDefault();
        store.notify("Thanks! Newsletter subscriptions are not connected yet.");
      }}
    >
      <input type="email" required placeholder="Your email address" aria-label="Newsletter email" />
      <Button variant="ghost" size="icon" type="submit" title="Subscribe" aria-label="Subscribe">
        <ArrowRight />
      </Button>
    </form>
  );
}
