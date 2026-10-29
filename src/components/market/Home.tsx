import { useState } from "react";
import { Link } from "@tanstack/react-router";
import {
  ArrowRight,
  ArrowUpRight,
  Truck,
  ShieldCheck,
  RotateCcw,
  Store,
  Headphones,
  Shirt,
  Armchair,
  Sparkles,
  Bike,
  BookOpen,
  Star,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import hero from "@/assets/market-hero.jpg";
import { categories } from "@/data/demo";
import { useMarket } from "./market";
import { ProductCard } from "./ProductCard";
const categoryIcons = [Headphones, Shirt, Armchair, Sparkles, Bike, BookOpen];
export function Home() {
  const { products } = useMarket();
  const [tab, setTab] = useState("Popular");
  const items =
    tab === "New arrivals"
      ? products.filter((p) => p.badge === "New arrival")
      : tab === "Best sellers"
        ? [...products].sort((a, b) => b.reviews - a.reviews).slice(0, 4)
        : products.slice(0, 4);
  return (
    <main>
      <section className="hero container">
        <img
          className="hero-image"
          src={hero}
          width={1536}
          height={768}
          alt="Sage wireless headphones and a compact speaker in a bright design studio"
        />
        <div className="hero-content">
          <div className="eyebrow">Discover your next favorite</div>
          <h1>
            Good finds.
            <br />
            Great possibilities<span>.</span>
          </h1>
          <p>
            Shop unique products from independent sellers.
            <br />
            All the things you love, in one place.
          </p>
          <Button asChild className="hero-button">
            <Link to="/products" search={{ q: "", category: "All", sort: "featured" }}>
              Explore the marketplace <ArrowRight />
            </Link>
          </Button>
          <div className="hero-foot">
            <Star size={13} fill="currentColor" />
            <span>
              <strong>4.9/5</strong> from 2,000+ happy shoppers
            </span>
          </div>
        </div>
        <div className="hero-pagination" aria-hidden="true">
          <span />
          <span />
          <span />
        </div>
      </section>
      <div className="container">
        <section className="benefits">
          {[
            { icon: Truck, title: "Fast, reliable delivery", text: "From their doorstep to yours" },
            {
              icon: ShieldCheck,
              title: "Shop with confidence",
              text: "Quality finds from trusted sellers",
            },
            { icon: RotateCcw, title: "Easy returns", text: "A little peace of mind, included" },
            {
              icon: Store,
              title: "Support independent sellers",
              text: "Small businesses. Big ideas.",
            },
          ].map((b) => (
            <div className="benefit" key={b.title}>
              <span className="benefit-icon">
                <b.icon size={26} strokeWidth={1.5} />
              </span>
              <div>
                <strong>{b.title}</strong>
                <p>{b.text}</p>
              </div>
            </div>
          ))}
        </section>
        <section className="section">
          <div className="section-heading">
            <div>
              <h2>A world of good finds</h2>
              <p>Whatever you're into, there's a little something for you.</p>
            </div>
            <Link
              className="text-link"
              to="/products"
              search={{ q: "", category: "All", sort: "featured" }}
            >
              View all categories <ArrowUpRight size={14} />
            </Link>
          </div>
          <div className="category-grid">
            {categories.map((category, i) => {
              const Icon = categoryIcons[i];
              return (
                <Link
                  key={category}
                  className="category-tile"
                  to="/products"
                  search={{ q: "", category, sort: "featured" }}
                >
                  {Icon && <Icon />}
                  <span>{category}</span>
                </Link>
              );
            })}
          </div>
        </section>
        <section className="section">
          <div className="section-heading">
            <div>
              <h2>Your next favorites</h2>
              <p>Handpicked finds. Loved by our community.</p>
            </div>
            <div className="product-toolbar">
              {["Popular", "New arrivals", "Best sellers"].map((t) => (
                <Button
                  key={t}
                  variant="ghost"
                  className={tab === t ? "active" : ""}
                  onClick={() => setTab(t)}
                >
                  {t}
                </Button>
              ))}
            </div>
          </div>
          <div className="product-grid">
            {items.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </section>
        <section className="curated-banner">
          <div>
            <div className="eyebrow">Independent looks good on you</div>
            <h2 className="mt-3">Small brands. Something special.</h2>
            <p>Discover the makers, dreamers, and doers behind your favorite finds.</p>
          </div>
          <Button asChild variant="outline">
            <Link to="/products" search={{ q: "", category: "All", sort: "featured" }}>
              Meet your next favorite <ArrowUpRight />
            </Link>
          </Button>
        </section>
        <section className="section">
          <div className="section-heading">
            <div>
              <h2>Fresh finds, just for you</h2>
              <p>A few more things we think you'll love.</p>
            </div>
            <Link
              className="text-link"
              to="/products"
              search={{ q: "", category: "All", sort: "featured" }}
            >
              Shop all products <ArrowRight size={14} />
            </Link>
          </div>
          <div className="product-grid">
            {products.slice(4).map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </section>
      </div>
    </main>
  );
}
