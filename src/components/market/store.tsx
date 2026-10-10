import { createContext, useContext, useState, type ReactNode } from 'react';
import { products } from '@/data/demo';
type CartItem={id:string;quantity:number};
type Store={cart:CartItem[];wishlist:string[];recent:string[];add:(id:string,quantity?:number)=>void;change:(id:string,quantity:number)=>void;toggleWish:(id:string)=>void;view:(id:string)=>void;notice:string;notify:(text:string)=>void};
const Context=createContext<Store|null>(null);
export function DemoStore({children}:{children:ReactNode}){
 const [cart,setCart]=useState<CartItem[]>([]);const [wishlist,setWishlist]=useState<string[]>([]);const [recent,setRecent]=useState<string[]>([]);const [notice,setNotice]=useState('');
 function notify(text:string){setNotice(text);setTimeout(()=>setNotice(''),3500)}
 function add(id:string,quantity=1){if(!products.some(p=>p.id===id))return;setCart(prev=>prev.some(p=>p.id===id)?prev.map(p=>p.id===id?{...p,quantity:p.quantity+quantity}:p):[...prev,{id,quantity}]);notify('Added to your bag')}
 function change(id:string,quantity:number){setCart(prev=>quantity<=0?prev.filter(p=>p.id!==id):prev.map(p=>p.id===id?{...p,quantity}:p))}
 function toggleWish(id:string){setWishlist(prev=>prev.includes(id)?prev.filter(x=>x!==id):[...prev,id])}
 function view(id:string){setRecent(prev=>[id,...prev.filter(x=>x!==id)].slice(0,8))}
 return <Context.Provider value={{cart,wishlist,recent,add,change,toggleWish,view,notice,notify}}>{children}{notice&&<div className="toast" role="status">{notice}</div>}</Context.Provider>
}
export function useStore(){const store=useContext(Context);if(!store)throw new Error('DemoStore is required');return store}
