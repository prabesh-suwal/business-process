## LOOK AT the .env file first, you should have GHCR Registry. GET it from github personal access token.

## for all
./build.sh --push


## for specific service
./build.sh --service cas-admin-ui --push
./build.sh --service memo-ui --push
./build.sh --service admin-gateway --push
./build.sh --service admin-gateway --service cas-admin-ui --push



## Then On SERVER.

scp docker-compose.base.yml docker-compose.memo.yml docker-compose.admin.yml deploy.sh nginx/growphase.conf .env 
IN /opt/growphase/

# then 
sudo nano /etc/postgresql/*/main/pg_hba.conf

# In pg_hba.conf, add:
host  all  all  172.17.0.0/16  md5
or
host  all  all  172.17.0.0/12  md5

sudo nano /etc/postgresql/*/main/postgresql.conf
# In postgresql.conf:
listen_addresses = '*'

sudo systemctl restart postgresql


sudo -u postgres psql <<EOF
CREATE DATABASE cas;
CREATE DATABASE memo_db;
CREATE DATABASE workflow_db;
CREATE DATABASE form_db;
CREATE DATABASE document_db;
CREATE DATABASE organization_db;
CREATE DATABASE policy_db;
CREATE DATABASE notification_db;
CREATE DATABASE audit_db;
CREATE DATABASE person_db;
CREATE DATABASE integration_db;
CREATE DATABASE lms_db;
EOF


## DELETE TABLE
sudo -u postgres psql <<EOF
DROP DATABASE IF EXISTS audit_db;
EOF



# THEN
sudo apt install nginx
sudo cp /opt/growphase/growphase.conf /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/growphase.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx


# then
cd /opt/growphase
chmod +x deploy.sh
./deploy.sh memo --pull     # Deploy memo stack
./deploy.sh admin --pull    # Deploy admin stack
# or
./deploy.sh all --pull      # Deploy everything




### DOCKER DOWN SCRIPT
docker compose -f docker-compose.base.yml down
docker compose -f docker-compose.admin.yml down
docker compose -f docker-compose.memo.yml down







### THINGS TO REMEMBER WHEN CREATING NEW FRONTEND AND DURING DEPLOYMENT ###
Each frontend should have its own folder, e.g.:

frontend/
├─ memo-ui/
├─ admin-ui/
├─ hr-ui/  (future)


Inside each frontend:

memo-ui/
├─ src/
│  ├─ App.jsx
│  ├─ main.jsx
│  └─ pages/
├─ public/
├─ .env.development
├─ .env.production
├─ vite.config.js
├─ Dockerfile
├─ nginx.conf
└─ package.json

2️⃣ vite.config.js

Purpose: tell Vite the base path for assets and routing.

Change: use env variable, do not hardcode /memo or /admin.

import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    base: env.VITE_BASE_URL || '/', // dynamic base path
    resolve: {
      alias: {
        // your aliases
      }
    },
    build: {
      commonjsOptions: {
        transformMixedEsModules: true,
      },
    },
  }
})


Dev: VITE_BASE_URL=/

Prod: VITE_BASE_URL=/memo/ or /admin/ or /hr/

3️⃣ main.jsx

Purpose: wrap App in BrowserRouter with correct basename.

Do this for every frontend:

import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'

const basename = (import.meta.env.VITE_BASE_URL || '/').replace(/\/$/, '')

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter basename={basename}>
      <App />
    </BrowserRouter>
  </React.StrictMode>
)


No changes needed in App.jsx for routing beyond normal Routes — do not add BrowserRouter there.

4️⃣ .env files

Each frontend should have:

.env.development
VITE_BASE_URL=/
VITE_GATEWAY_URL=http://localhost:<backend-port>

.env.production
VITE_BASE_URL=/<frontend-path>/
VITE_GATEWAY_URL=/<backend-api-path>


Example for admin-ui:

Production path: /admin/

API path: /admin-api

5️⃣ App.jsx

Normal Routes & PrivateRoute logic.

Do not hardcode the base — Vite + main.jsx basename handles it.

Example:

<Route path="/" element={<Layout />}>
  <Route index element={<DashboardPage />} />
  <Route path="users" element={<UsersPage />} />
</Route>


Routes are relative to basename.

6️⃣ Dockerfile

Use multi-stage build:

# Build stage
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]


Vite automatically uses .env.production when building in Docker.

7️⃣ nginx.conf (inside container)
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}


Do not hardcode /memo or /admin here.

The container always sees its frontend at /.

8️⃣ Host nginx (Ubuntu server)

Maps public paths to containers. Example:

# Memo UI
location /memo/ {
    proxy_pass http://memo_ui/;  # container root
}

# Memo API
location /memo-api/ {
    proxy_pass http://memo_gateway/;  # trailing slash if needed
    proxy_set_header Cookie $http_cookie;
    proxy_set_header Host $host;
}

# Admin UI
location /admin/ {
    proxy_pass http://admin_ui/;
}

# Admin API
location /admin-api/ {
    proxy_pass http://admin_gateway/;  # use trailing slash carefully
    proxy_set_header Cookie $http_cookie;
    proxy_set_header Host $host;
}


Rules:

UI container sees / as root → proxy_pass / → works

API container: always use trailing slash in proxy_pass to avoid 404s