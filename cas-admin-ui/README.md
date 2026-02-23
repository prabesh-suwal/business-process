# CAS Admin UI

Admin dashboard for the Central Authentication & Authorization Service (CAS). Provides management interfaces for users, roles, products, scopes, and workflow process templates (BPMN designer).

## Tech Stack

- **React 18** + **React Router**
- **Vite** (dev server & build)
- **bpmn-js** — BPMN 2.0 modeler & properties panel

## Getting Started

```bash
# Install dependencies
npm install

# Start dev server (default: http://localhost:5173). Uses .env.development
npm run dev

# Production build
npm run build
```

## Environment Variables for Development Environment

| Variable | Description | Default |
|---|---|---|
| `VITE_API_URL` | Backend Gateway URL for admin | `http://localhost:8080` |
| `VITE_BASE_URL` | base url for admin UI/FE | `/` |

## Docker

```bash
docker build -t cas-admin-ui .
docker run -p 80:80 cas-admin-ui
```
