import { createFileRoute } from "@tanstack/react-router";
import { Home } from '@/components/market/Home';
export const Route = createFileRoute("/")({
  head: () => ({meta: [
    {title:'MarketHub — Good Finds. Great Possibilities.'},
    {name:'description',content:'Discover thoughtful products from independent brands. Shop electronics, fashion, home, and more at MarketHub.'},
    {property:'og:title',content:'MarketHub — Good Finds. Great Possibilities.'},
    {property:'og:description',content:'Your next favorite, from an independent seller. Explore MarketHub.'},
    {property:'og:type',content:'website'},
    {name:'twitter:card',content:'summary_large_image'},
  ]}),
  component: Home,
});
