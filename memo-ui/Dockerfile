FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production=false
COPY . .

# 3. Run the build (Vite will now bake /memo/ into the index.html)
RUN npm run build

FROM nginx:1.27-alpine

# Copy the built assets to Nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy your internal nginx.conf (the one that handles internal routing)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]



