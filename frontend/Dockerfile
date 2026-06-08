ARG DOCKER_REGISTRY=docker.m.daocloud.io

FROM ${DOCKER_REGISTRY}/library/node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./
COPY .npmrc ./

RUN npm install

COPY . .

RUN npm run build

FROM ${DOCKER_REGISTRY}/library/nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

RUN rm -rf /app

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
