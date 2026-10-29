import { createFileRoute } from "@tanstack/react-router";
import { Details } from "@/components/market/Details";
import { products } from "@/data/demo";
export const Route = createFileRoute("/products/$id")({
  head: ({ params }) => {
    const p = products.find((p) => p.id === params.id);
    const title = `${p?.name ?? "Product"} — MarketHub`;
    const description = p
      ? `Discover ${p.name} by ${p.seller} at MarketHub.`
      : "Discover products from independent sellers at MarketHub.";
    return {
      meta: [
        { title },
        { name: "description", content: description },
        { property: "og:title", content: title },
        { property: "og:description", content: description },
        { property: "og:type", content: "website" },
        { name: "twitter:card", content: "summary_large_image" },
      ],
    };
  },
  component: Details,
});
