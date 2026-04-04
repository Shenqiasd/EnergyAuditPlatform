FROM node:20-alpine AS build
ARG VITE_SPREADJS_LICENSE=""
WORKDIR /app
COPY package.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
ENV VITE_SPREADJS_LICENSE=$VITE_SPREADJS_LICENSE
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV NGINX_PORT=80
ENV BACKEND_URL=http://backend:8080
EXPOSE 80
