FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
# Templated via the official image's docker-entrypoint.d/20-envsubst-on-templates.sh:
# envsubst only replaces vars that exist in the container's real env (BACKEND_HOST/BACKEND_PORT),
# so nginx's own runtime variables ($host, $remote_addr, ...) are left untouched.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV BACKEND_HOST=app
ENV BACKEND_PORT=8080
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
