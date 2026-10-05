FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
# Reproducible install pinned by package-lock.json (integrity hashes verified)
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
RUN apk add --no-cache openssl \
    && rm /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY nginx/proxy-headers.conf /etc/nginx/snippets/proxy-headers.conf
COPY nginx/10-ensure-tls-cert.sh /docker-entrypoint.d/10-ensure-tls-cert.sh
ENV BACKEND_HOST=app
ENV BACKEND_PORT=8080
EXPOSE 443
CMD ["nginx", "-g", "daemon off;"]
