import p1 from '@/assets/products/product-1.jpg';
import p2 from '@/assets/products/product-2.jpg';
import p3 from '@/assets/products/product-3.jpg';
import p4 from '@/assets/products/product-4.jpg';
import p5 from '@/assets/products/product-5.jpg';
import p6 from '@/assets/products/product-6.jpg';
import p7 from '@/assets/products/product-7.jpg';
import p8 from '@/assets/products/product-8.jpg';
// Fictional demo data only. No production data or persistence is connected.
export const products = [
 {id:'headphones',name:'Studio Wireless Headphones',seller:'Sound Collective',category:'Electronics',price:129,oldPrice:179,rating:4.8,reviews:128,image:p1,badge:'Best seller',stock:24},
 {id:'sneakers',name:'Everyday Leather Sneakers',seller:'Urban Step',category:'Fashion',price:89,oldPrice:110,rating:4.7,reviews:86,image:p2,badge:'20% off',stock:18},
 {id:'vase',name:'Sage Ceramic Vase',seller:'The Living Edit',category:'Home & Living',price:38,oldPrice:0,rating:4.9,reviews:64,image:p3,badge:'New arrival',stock:4},
 {id:'backpack',name:'The Weekend Backpack',seller:'Roam Supply',category:'Sports & Outdoors',price:65,oldPrice:85,rating:4.6,reviews:92,image:p4,badge:'',stock:31},
 {id:'watch',name:'Classic Silver Watch',seller:'Minute Studio',category:'Fashion',price:149,oldPrice:189,rating:4.8,reviews:57,image:p5,badge:'',stock:12},
 {id:'perfume',name:'Amber Eau de Parfum',seller:'Botanical House',category:'Beauty & Care',price:72,oldPrice:0,rating:4.9,reviews:43,image:p6,badge:'New arrival',stock:3},
 {id:'speaker',name:'Portable Mini Speaker',seller:'Sound Collective',category:'Electronics',price:49,oldPrice:69,rating:4.7,reviews:105,image:p7,badge:'',stock:28},
 {id:'sunglasses',name:'Essential Sun Frames',seller:'Urban Step',category:'Fashion',price:45,oldPrice:0,rating:4.6,reviews:38,image:p8,badge:'',stock:16},
];
export type Product = typeof products[number];
export const categories=['Electronics','Fashion','Home & Living','Beauty & Care','Sports & Outdoors','Books & More'];
export const demoOrders=[
 {id:'MH-1048',customer:'Olivia Martin',date:'Oct 9, 2026',product:'Studio Wireless Headphones',total:129,status:'Shipped'},
 {id:'MH-1047',customer:'James Wilson',date:'Oct 8, 2026',product:'Everyday Leather Sneakers',total:89,status:'Processing'},
 {id:'MH-1046',customer:'Emma Thompson',date:'Oct 7, 2026',product:'Sage Ceramic Vase',total:38,status:'Delivered'},
 {id:'MH-1045',customer:'Noah Davis',date:'Oct 6, 2026',product:'The Weekend Backpack',total:65,status:'Delivered'},
];
export const demoUsers=[{name:'Olivia Martin',email:'olivia@example.com',role:'Buyer'},{name:'Sound Collective',email:'hello@example.com',role:'Seller'},{name:'James Wilson',email:'james@example.com',role:'Buyer'},{name:'The Living Edit',email:'living@example.com',role:'Seller'}];
export const money=(n:number)=>new Intl.NumberFormat('en-US',{style:'currency',currency:'USD',maximumFractionDigits:2}).format(n);
