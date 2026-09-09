# =====================================================================
# WordFlow 前端镜像（多阶段构建）
#   阶段 1：Node 构建 Vue 产物
#   阶段 2：Nginx 托管静态资源并反向代理 /api 与 /uploads
# =====================================================================

FROM node:22-alpine AS build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
