FROM node:20-alpine AS build
# cache-bust: force rebuild to inject SpreadJS license env vars (2026-04-05)
ARG VITE_SPREADJS_LICENSE=""
ARG VITE_SPREADJS_DESIGNER_LICENSE=""
WORKDIR /app
COPY package.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
ENV VITE_SPREADJS_LICENSE=$VITE_SPREADJS_LICENSE
ENV VITE_SPREADJS_DESIGNER_LICENSE=$VITE_SPREADJS_DESIGNER_LICENSE
RUN npx vite build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV NGINX_PORT=80
ENV BACKEND_URL=http://EnergyAuditPlatform:8080
EXPOSE 80
