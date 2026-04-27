# syntax=docker/dockerfile:1.7
# Numnia frontend — React 19 + Vite 8 + Babylon.js 9
# Multi-stage build: pnpm build → nginx static serve.

# ─── Stage 1: build ───────────────────────────────────────────────
FROM node:24-alpine AS build
WORKDIR /app

# Pin pnpm via corepack (matches package.json engines.pnpm ≥10.33)
RUN corepack enable && corepack prepare pnpm@10.33.0 --activate

# Install deps with cached store
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# Copy sources and build
COPY . .
RUN pnpm run build

# ─── Stage 2: runtime ─────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Drop default conf, install our SPA + /api proxy config
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/numnia.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
    CMD wget -qO- http://127.0.0.1/healthz | grep -q ok || exit 1
