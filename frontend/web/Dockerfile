FROM node:24-alpine AS builder
WORKDIR /workspace

COPY package.json package-lock.json ./
COPY frontend/web/package.json frontend/web/package.json
COPY frontend/admin/package.json frontend/admin/package.json
COPY frontend/packages/api-client/package.json frontend/packages/api-client/package.json
RUN npm ci

COPY frontend/web frontend/web
RUN npm run build --workspace @personal-blog/web

FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /workspace/frontend/web/.output ./

USER node
EXPOSE 3000
CMD ["node", "server/index.mjs"]
