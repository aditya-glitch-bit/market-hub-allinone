# MarketHub – Multi-Vendor E-Commerce Frontend

MarketHub is a responsive multi-vendor e-commerce web application with three user roles: **Admin**, **Seller** and **Buyer**. This repository contains the frontend, built with React, TypeScript and Tailwind CSS using realistic sample data.

> **Project status:** The React frontend still shows sample data. A complete **Java Servlet + JDBC backend** lives in [`backend/`](backend/README.md) (REST API, DAO layer, H2/MySQL, concurrency). Connecting the frontend to the API, and payments, are the next steps (see [Roadmap](#roadmap)).

## Live Demo

- Live preview: _add your published Lovable / deployed URL here_
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
- [ ] Connect the React frontend to the backend API
- [ ] Payment integration
- [ ] Persistent cart and wishlist
- [ ] Real order tracking

See [`roadmap.md`](roadmap.md) for details.

## Author

Aditya – built as a project for GUVI.

## Acknowledgements

Initial interface generated with [Lovable](https://lovable.dev).
