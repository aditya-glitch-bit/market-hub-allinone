import { createFileRoute } from '@tanstack/react-router';
import { AuthPage } from '@/components/market/AuthPage';
export const Route=createFileRoute('/login')({head:()=>({meta:[{title:'Welcome Back — MarketHub'},{name:'description',content:'Your MarketHub sign-in preview and sample workspaces.'},{property:'og:title',content:'Welcome Back — MarketHub'},{property:'og:description',content:'Return to your world of good finds.'},{property:'og:type',content:'website'},{name:'twitter:card',content:'summary_large_image'}]}),component:AuthPage});
