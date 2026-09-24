# Jamix Fashion Platform

The e-commerce base for Jamix Fashion: Spring Boot, Node.js, Thymeleaf, React and Vue, split into micro-services and
micro-frontends. The original static landing page (`../index.html`, `../CSS`, `../JS`, `../IMG`) is still in place and has not been changed.

## Architecture

```
                             ┌───────────────────────────────────────────────┐
  Browser / crawler ───────▶ │ Gateway (nginx / Traefik / cloud LB)          │
                             │  /             → catalog-service  (SSR)       │
                             │  /img/*, /css/*→ catalog-service  (static)    │
                             │  /api/v1/*     → catalog-service  (REST)      │
                             │  /api/leads    → leads-bff                    │
                             │  /tienda/*     → storefront-react (static)    │
                             │  /admin/*      → admin-vue        (static)    │
                             └───────┬──────────────┬──────────────┬─────────┘
                                     │              │              │
          ┌──────────────────────────▼───┐   ┌──────▼─────────┐    │  static bundles (CDN-able)
          │ catalog-service  :8080       │   │ leads-bff :4000│    │  ┌──────────────────────────┐
          │ Spring Boot 4 · Java 21      │   │ Node · Express │    └─▶│ storefront-react (React) │
          │                              │   │                │       │   GET /api/v1/products   │
          │ • Thymeleaf SSR: hero,       │   │ • POST /api/   │       ├──────────────────────────┤
          │   catalogue, wholesale,      │   │   leads        │       │ admin-vue (Vue 3)        │
          │   contact form (SEO)         │   │ • validates,   │       │   POST /api/v1/auth/login│
          │ • REST /api/v1/products      │   │   redirects to │       │   /api/v1/admin/** (JWT) │
          │ • JWT: /auth/login, /orders, │   │   wa.me, fires │       └──────────────────────────┘
          │   /admin/** (Spring Security)│   │   webhook      │
          │ • JPA + Flyway → MySQL (TLS) │   └──────┬─────────┘
          └──────────────────────────────┘          │ optional
                                                    ▼
                                    n8n / Zapier / WhatsApp Cloud API adapter
```

How the pieces talk to each other:

| From → To | How | Auth |
|---|---|---|
| Crawler / visitor → `/` | Thymeleaf HTML rendered on the server, with JSON-LD. Filters are plain links (`?categoria=ella`) | Public |
| SSR contact form → leads-bff | Plain HTML `POST` (no JavaScript). The BFF answers with a **303 redirect to `wa.me`**, or back to `/?lead=error#contacto` | Public, rate-limited, honeypot |
| React → catalog-service | `fetch('/api/v1/products')`, same origin behind the gateway (Vite proxy in dev) | Public |
| Vue admin → catalog-service | `POST /api/v1/auth/login` → JWT → `Authorization: Bearer …` on `/api/v1/admin/**` | `ROLE_ADMIN` |
| Any client → orders | `POST /api/v1/orders` with a JWT | `ROLE_CUSTOMER` / `ROLE_ADMIN` |
| leads-bff → business | Optional `LEADS_WEBHOOK_URL`, fire-and-forget, so a failure never blocks the customer | — |

The micro-frontends share **one design language**: [`shared/jamix-theme.css`](shared/jamix-theme.css) holds the
Tailwind v4 `@theme` tokens (brand colours, fonts, `animate-fade-in`, `hover-lift` and `stagger-children`). The
Thymeleaf page, React and Vue all import it.

### Why Vue for the admin, and how Angular would coexist

Vue fits a small back-office: light, fast to build and easy to hand over. The composition is **route-based**: each
front end is an independent static bundle that the gateway mounts under its own path (`/tienda/`, `/admin/`). They
share no JavaScript runtime, only the design tokens and the REST contract. So an Angular app joins the same way:

1. Build it with `ng build --base-href /reportes/`.
2. Add a gateway route `/reportes/* → angular-dist/`.
3. Have it import `shared/jamix-theme.css` and call the same `/api/v1/*` endpoints with the same JWT.

If different frameworks ever need to appear on the **same page**, expose each one as a Web Component (Angular
Elements, Vue `defineCustomElement`, or a React wrapper) or use Module Federation / single-spa. Route-based
composition is the simplest option that works, so start there.

## Services

### Quick start (everything at once)

```bash
cd platform
npm run setup   # first time only: npm install in all four services
npm run dev     # starts Spring Boot, waits until it answers, then the BFF and both front ends
npm run dev:h2  # same, but the catalog uses an in-memory database (no MySQL / .env needed)
npm test        # Spring integration tests + BFF tests
```

Requirements: JDK 21+, Node 22.9+ and MySQL 8+. Maven isn't needed because the Maven wrapper (`mvnw`) downloads it.
Database setup (one-time SQL script + `catalog-service/.env`) is described in the [root README](../README.md).

### Database

- **MySQL**, reached over TLS (`sslMode=REQUIRED`; accounts are created with `REQUIRE SSL`).
- **Flyway** owns the schema: [`db/migration`](catalog-service/src/main/resources/db/migration) runs at startup,
  `V1` creates the tables and `V2` loads the catalogue. Hibernate only validates (`ddl-auto: validate`).
  Never edit an applied migration: add `V3__...sql`.
- **Two accounts:** `jamix_migrator` (DDL, used by Flyway) and `jamix_app` (DML only, used by the app).
- **Credentials** come from environment variables or the git-ignored `catalog-service/.env`
  ([template](catalog-service/.env.example)). Without `DB_PASSWORD` the app stops before connecting.
- **Tests** run the same migrations on H2 in MySQL mode (profile `h2`).
Ctrl+C stops every service.

If a service is already running in another terminal, `npm run dev` reuses it instead of failing with
"port already in use" (and leaves it running on Ctrl+C). If a port is taken by some other program, it stops
before starting anything and tells you how to find that process (`netstat -ano | findstr :8080`).

### One service at a time

Every folder answers to `npm run dev`. In `catalog-service` it's a thin wrapper around the Maven wrapper
(`mvnw spring-boot:run`); Spring Boot itself doesn't use npm.

| Folder | Stack | Port | Run |
|---|---|---|---|
| [`catalog-service`](catalog-service) | Spring Boot 4.1, Spring Security (JWT), JPA, Thymeleaf, Tailwind CLI | 8080 | `npm run dev` (= `mvnw spring-boot:run`) |
| [`leads-bff`](leads-bff) | Node ≥ 22.9, Express 5, helmet, express-rate-limit | 4000 | `npm install && npm run dev` |
| [`storefront-react`](storefront-react) | React 19, Vite 8, Tailwind 4 | 5173 | `npm install && npm run dev` |
| [`admin-vue`](admin-vue) | Vue 3.5, Vite 8, Tailwind 4 | 5174 | `npm install && npm run dev` |

When starting them by hand, start the catalog service first: the store and the admin read from it.

- SSR storefront: http://localhost:8080
- React catalogue: http://localhost:5173 (proxies `/api` and `/img` to :8080)
- Admin: http://localhost:5174. Dev users are `admin` / `admin123` and `cliente` / `cliente123` (see below)

After editing the Thymeleaf template, rebuild its CSS: `cd catalog-service && npm install && npm run build:css`
(or `npm run watch:css`).

### Tests

```bash
cd catalog-service && npm test     # (= mvnw test) 8 integration tests: security rules, wholesale pricing, stock rollback, SSR page
cd leads-bff && npm test            # 9 tests: JSON + no-JS form flows, validation, Lima-time dates, honeypot
```

## REST API (catalog-service)

| Method & path | Role | Notes |
|---|---|---|
| `GET /api/v1/products?category=ella\|el\|ninos` | public | Availability only (`inStock`), never exact stock |
| `GET /api/v1/products/{slug}` | public | |
| `POST /api/v1/auth/login` | public | `{username, password}` → `{accessToken, expiresIn, roles}` |
| `POST /api/v1/orders` | CUSTOMER, ADMIN | Merges repeated lines; **wholesale price from 6 units** per product; the whole order fails (409) if any line lacks stock |
| `GET /api/v1/orders/mine` | CUSTOMER, ADMIN | |
| `GET/POST /api/v1/admin/products` | ADMIN | |
| `PUT /api/v1/admin/products/{id}` | ADMIN | Body must include the `version` the admin saw |
| `PATCH /api/v1/admin/products/{id}/stock` | ADMIN | `{stock, version}`. A stale version returns 409, so an order is never silently overwritten |
| `DELETE /api/v1/admin/products/{id}` | ADMIN | |

Errors follow RFC 9457 (`application/problem+json`).

## Before going to production

- **Secrets:** set `JAMIX_JWT_SECRET` (≥ 32 bytes), `JAMIX_ADMIN_PASSWORD` and `JAMIX_CUSTOMER_PASSWORD`.
  Passwords may be pre-hashed (`{bcrypt}$2a$…`). The app logs a warning while it's using the dev secret.
- **Users:** the in-memory accounts are a bootstrap. Move to a JPA-backed `UserDetailsService`, or an IdP such as
  Keycloak or Auth0 via `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
- **Database:** use `sslMode=VERIFY_IDENTITY` with the server's CA certificate, restrict the accounts' host to the
  app server's IP, and schedule backups (`mysqldump` or managed snapshots).
- **Admin token:** it lives in `sessionStorage`. The next hardening step is an httpOnly cookie issued by a BFF.
- **Gateway:** set `TRUST_PROXY=1` on leads-bff and add rate limiting on `/api/v1/auth/login`.
- **Size-based prices** (e.g. Vestido Túnica S/ 50–60): orders currently charge the "from" price. Add product
  variants (size → price, stock) before selling these garments online.
