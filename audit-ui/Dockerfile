FROM node:20-alpine AS build
# cache-bust: force rebuild to inject SpreadJS license env vars (2026-04-05)
ARG VITE_SPREADJS_LICENSE=""
ARG VITE_SPREADJS_DESIGNER_LICENSE=""
WORKDIR /app
COPY package.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
# Re-run setup-tinymce.sh after COPY . . because COPY overwrites the
# public/tinymce/ directory that postinstall created during npm install.
# The gitignored public/tinymce/ files are lost when COPY replaces them
# with the (empty) git version.
RUN sh scripts/setup-tinymce.sh
ENV VITE_SPREADJS_LICENSE=$VITE_SPREADJS_LICENSE
ENV VITE_SPREADJS_DESIGNER_LICENSE=$VITE_SPREADJS_DESIGNER_LICENSE
RUN npx vite build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV NGINX_PORT=80
ENV BACKEND_URL=http://energyauditplatform.railway.internal:8080
EXPOSE 80
