FROM node:24-alpine AS builder
WORKDIR /workspace

COPY package.json package-lock.json ./
COPY frontend/web/package.json frontend/web/package.json
COPY frontend/admin/package.json frontend/admin/package.json
COPY frontend/packages/api-client/package.json frontend/packages/api-client/package.json
COPY frontend/packages/types/package.json frontend/packages/types/package.json
COPY frontend/packages/shared-ui/package.json frontend/packages/shared-ui/package.json
RUN npm ci

COPY frontend/admin frontend/admin
COPY frontend/packages/api-client frontend/packages/api-client
RUN npm run build --workspace @personal-blog/admin

FROM nginx:1.28-alpine
COPY frontend/admin/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/frontend/admin/dist /usr/share/nginx/html
EXPOSE 80
