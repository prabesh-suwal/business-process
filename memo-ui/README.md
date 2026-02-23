# Memo UI

Enterprise Memo Management System frontend. Supports memo creation, workflow-based approval routing, task inbox, dynamic forms, rich-text editing, attachments, and BPMN/DMN workflow design.

## Tech Stack

- **React 18** + **React Router**
- **Vite** (dev server & build)
- **Tailwind CSS** + **shadcn/ui** components
- **Axios** — API client with SSO token management
- **Tiptap** — rich-text editor
- **bpmn-js / dmn-js** — workflow designer
- **React JSON Schema Form** (`@rjsf`) — dynamic forms
- **Sonner** — toast notifications

## Getting Started

```bash
# Install dependencies
npm install

# Start dev server (default: http://localhost:5176). Uses .env.development
npm run dev

# Production build
npm run build
```

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `VITE_GATEWAY_URL` | Product Gateway base URL | `http://localhost:8086` |
| `VITE_BASE_URL` | base url for admin UI/FE | `/` |
