ARG IMAGE_PREFIX=docker.m.daocloud.io/library/

FROM ${IMAGE_PREFIX}node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 80
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "80"]
