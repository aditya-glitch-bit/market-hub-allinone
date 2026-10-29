import { useEffect, useState } from "react";
import { Link, getRouteApi } from "@tanstack/react-router";
import {
  ChevronRight,
  Star,
  Heart,
  ShoppingBag,
  Minus,
  Plus,
  Check,
  Truck,
  RotateCcw,
  ShieldCheck,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { money } from "@/data/demo";
import { useMarket } from "./market";
import { ProductCard } from "./ProductCard";
import { useStore } from "./store";
const api = getRouteApi("/products/$id");
export function Details() {
  const { id } = api.useParams();
  const { products, ready } = useMarket();
  const p = products.find((p) => p.id === id);
  const [quantity, setQuantity] = useState(1);
  const store = useStore();
  useEffect(() => {
    if (p) store.view(p.id);
    setQuantity(1);
  }, [id]);
  if (!p && !ready)
    return (
      <div className="container empty" role="status">
        <h1>Loading product…</h1>
      </div>
    );
  if (!p)
    return (
      <div className="container empty">
        <h1>Product not found</h1>
        <Button asChild>
          <Link to="/products" search={{ q: "", category: "All", sort: "featured" }}>
            Back to marketplace
          </Link>
        </Button>
      </div>
    );
  return (
    <main className="container">
      <div className="page-intro">
        <div className="breadcrumb">
          <Link to="/">Home</Link>
          <ChevronRight size={12} />
          <Link to="/products" search={{ q: "", category: p.category, sort: "featured" }}>
            {p.category}
          </Link>
          <ChevronRight size={12} />
          <span>{p.name}</span>
        </div>
      </div>
      <div className="detail-layout">
        <img className="detail-photo" src={p.image} alt={p.name} width={368} height={496} />
        <section className="detail-info">
          <span className="eyebrow">{p.seller}</span>
          <h1>{p.name}</h1>
          <div className="rating">
            <Star size={15} fill="currentColor" />
            <strong>{p.rating}</strong>
            <span>· {p.reviews} reviews</span>
          </div>
          <div className="detail-price">
            <strong>{money(p.price)}</strong>
            {p.oldPrice > 0 && <del>{money(p.oldPrice)}</del>}
          </div>
          <p>
            {p.category === "Electronics"
              ? "Beautifully designed sound for your everyday. A clean, comfortable design meets rich audio — at home, at work, and everywhere in between."
              : "Considered details, beautiful materials, and everyday versatility. A thoughtfully made favorite from an independent brand, ready to become part of your day."}
          </p>
          <div className="stock-label">
            <Check size={15} />
            {p.stock > 0
              ? p.stock < 5
                ? `Only ${p.stock} left · Ready to ship`
                : "In stock · Ready to ship"
              : "Out of stock"}
          </div>
          <div className="detail-buttons">
            <div className="quantity">
              <Button
                variant="ghost"
                size="icon"
                aria-label="Decrease quantity"
                onClick={() => setQuantity(Math.max(1, quantity - 1))}
              >
                <Minus />
              </Button>
              <span>{quantity}</span>
              <Button
                variant="ghost"
                size="icon"
                aria-label="Increase quantity"
                onClick={() => setQuantity(quantity + 1)}
              >
                <Plus />
              </Button>
            </div>
            <Button disabled={p.stock <= 0} onClick={() => store.add(p.id, quantity)}>
              <ShoppingBag />
              Add to bag
            </Button>
            <Button
              variant="outline"
              size="icon"
              aria-label="Save product"
              title="Save to wishlist"
              onClick={() => store.toggleWish(p.id)}
            >
              <Heart fill={store.wishlist.includes(p.id) ? "currentColor" : "none"} />
            </Button>
          </div>
          <div className="detail-facts">
            <div>
              <Truck size={17} />
              Delivery estimate: 3–5 business days
            </div>
            <div>
              <RotateCcw size={17} />
              Easy returns within 30 days
            </div>
            <div>
              <ShieldCheck size={17} />
              Sold by {p.seller}
            </div>
          </div>
          <p className="demo-note">
            Sample product · Delivery and returns are illustrative, not a live offer.
          </p>
          <details className="dash-section">
            <summary>Product details & care</summary>
            <p>
              Made for everyday use. Store in a cool, dry place and clean gently with a soft cloth.
              Product specifications shown in this preview are illustrative.
            </p>
          </details>
        </section>
      </div>
      <section className="section">
        <div className="section-heading">
          <h2>A few more good finds</h2>
        </div>
        <div className="product-grid">
          {products
            .filter((item) => item.id !== id)
            .slice(0, 4)
            .map((item) => (
              <ProductCard key={item.id} product={item} />
            ))}
        </div>
      </section>
    </main>
  );
}
