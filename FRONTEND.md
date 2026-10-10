# MarketHub frontend

## Pages
- `src/routes/index.tsx`: homepage.
- `src/routes/products.tsx`: catalog with search, categories, sale filtering, and sorting.
- `src/routes/products.$id.tsx`: product details.
- `src/routes/login.tsx` and `register.tsx`: presentation-only account forms.
- `src/routes/dashboard.$role.tsx`: Admin, Seller, and Buyer sample workspaces.

## Reusable interface
- `src/components/market/Shell.tsx`: navigation, search, cart drawer, footer, newsletter presentation.
- `ProductCard.tsx`: product image, price, rating, wishlist, and add-to-bag controls.
- `Home.tsx`, `Catalog.tsx`, `Details.tsx`, `AuthPage.tsx`: shopping and account page content.
- `Dashboard.tsx`: role-specific navigation, summary cards, charts, tables, sample editing forms, and buyer account sections.
- `store.tsx`: in-memory cart, wishlist, recently viewed products, and feedback messages.
- `src/styles.css`: shared design tokens, typography, layout, and small-screen rules.
- `src/assets/`: generated campaign and product photography.
- `src/data/demo.ts`: fictional products, users, and orders, deliberately separate from production data.

## Preview boundaries
Authentication, authorization, payments, subscriptions, and database persistence are **not implemented**. Workspaces are publicly accessible previews, not secure role gates. Login and registration forms validate basic input but do not transmit it or create accounts. Cart, wishlist, profile, inventory, users, and order edits exist only in memory and reset on refresh. Payment details are never collected. Seller/admin edits affect only their sample workspace, not the storefront. Product shipping estimates, returns, ratings, shopper counts, and seller records are illustrative sample content.

## Review
Use the footer or account page to open each sample workspace. The header searches products, heart icons save favorites, and the bag supports quantities and removal. Admin users can toggle sample user status and update sample orders. Sellers can edit/add/remove sample listings, change inventory, restock demo alerts, and export sample sales CSV. Buyers can track sample orders, save a sample profile, and add/remove fictional addresses.