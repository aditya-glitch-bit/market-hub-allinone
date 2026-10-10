import { createFileRoute } from '@tanstack/react-router';
import { AuthPage } from '@/components/market/AuthPage';
export const Route=createFileRoute('/register')({head:()=>({meta:[{title:'Join the Marketplace — MarketHub'},{name:'description',content:'Preview joining MarketHub as a buyer or independent seller.'},{property:'og:title',content:'Join the Marketplace — MarketHub'},{property:'og:description',content:'Find your place in the MarketHub community.'},{property:'og:type',content:'website'},{name:'twitter:card',content:'summary_large_image'}]}),component:()=> <AuthPage register/>});
