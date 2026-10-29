# MarketHub – Multi-Vendor E-Commerce Frontend

MarketHub is a responsive multi-vendor e-commerce web application with three user roles: **Admin**, **Seller** and **Buyer**. This repository contains the frontend, built with React, TypeScript and Tailwind CSS using realistic sample data.

> **Project status:** The React frontend talks to the Java Servlet + JDBC backend in [`backend/`](backend/README.md) (login, catalog, cart, checkout, role dashboards). If no backend is reachable, the site falls back to a clearly labelled **sample-data preview**. Payments are not implemented (see [Roadmap](#roadmap)).

## Live Demo

- Live preview (frontend, Cloudflare Workers): https://market-hub-allinone.av773029.workers.dev
  - It shows live data only once the backend below is deployed and `BACKEND_URL` is set; until then it runs in sample-data preview mode.
- Presentation: [`docs/MarketHub-Presentation.pdf`](docs/MarketHub-Presentation.pdf)
- Screenshots: see the [`docs/screenshots`](docs/screenshots) folder

## Features

**Storefront**
- Homepage, product listing, product details, login and registration pages
- Search bar, category filters, product cards with images, prices and ratings
- Wishlist and sample shopping bag

**Admin dashboard**
- User management, product management, order management, activity monitoring

**Seller dashboard**
- Product listings, inventory management, order processing, sales reports, low-stock alerts

**Buyer dashboard**
- Order history and tracking, wishlist, recently viewed products, profile, addresses, payment methods

**Design**
- Consistent design system, responsive layouts, loading states and empty states

## Tech Stack

- React + TypeScript
- Tailwind CSS
- Vite
- Vitest (testing)
- ESLint + Prettier
- Bun / npm for package management

## Getting Started

Prerequisites: Node.js (v18+) and npm, or Bun.

```bash
git clone https://github.com/aditya-glitch-bit/market-hub-allinone.git
cd market-hub-allinone
npm install
npm run dev
```

Then open the local URL printed in the terminal (usually http://localhost:5173).

Other useful commands:

```bash
npm run build    # production build
npm run lint     # lint the code
npm test         # run tests (if configured)
```

## Backend (Java)

Java 17, Jakarta Servlets, JDBC (H2 by default, MySQL optional), embedded Jetty.

```bash
cd backend
mvn package
java -jar target/markethub-backend.jar   # http://localhost:8080/api/health
```

See [`backend/README.md`](backend/README.md) for configuration, the API list, the architecture and a mapping to the project marking rubrics.

## Deploying

**1. Backend on Render (free, Docker).** Push this repo to GitHub, then in Render choose *New + → Blueprint* and select the repository. `render.yaml` builds `backend/Dockerfile` and starts the API. When it is live, `https://<your-service>.onrender.com/api/health` should answer `{"status":"UP"}`. The free plan sleeps when idle (first request takes about a minute) and the embedded H2 file is reset on every restart, so demo data is re-created automatically. Use MySQL (`MARKETHUB_DB_URL`) for permanent data.

**2. Frontend on Cloudflare.** In the Worker's *Settings → Variables and Secrets* add `BACKEND_URL` = your Render URL (no trailing slash), then redeploy. The Worker forwards every `/api/...` request to the backend, so the login cookie stays on the site's own domain and no CORS setup is needed.

**Local development with the backend:**

```bash
cd backend && mvn exec:java      # API on :8080
bun run dev                      # site on :5173, /api is proxied to :8080
```

## Project Structure

```
market-hub-allinone/
├── public/            # Static assets
├── src/               # React frontend source (pages, components, data, utilities)
├── backend/           # Java Servlet + JDBC backend (see backend/README.md)
├── docs/              # Presentation (PDF) and screenshots
├── FRONTEND.md        # Detailed guide to the frontend files
├── roadmap.md         # Planned work
├── package.json
├── vite.config.ts
└── tsconfig.json
```

See [`FRONTEND.md`](FRONTEND.md) for a file-by-file explanation of `src/`.

## Roadmap

- [ ] Authentication with role-based access (Admin / Seller / Buyer)
- [x] Backend and database for products, users and orders (Java, `backend/`)
- [x] Connect the React frontend to the backend API (same-origin `/api` proxy)
- [ ] Payment integration
- [ ] Persistent cart and wishlist
- [ ] Real order tracking

See [`roadmap.md`](roadmap.md) for details.

## Author

Aditya – built as a project for GUVI.

## Acknowledgements

Initial interface generated with [Lovable](https://lovable.dev).
