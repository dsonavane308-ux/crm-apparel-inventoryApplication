FROM node:18-alpine AS builder
WORKDIR /app
COPY package.json ./
RUN npm install --include=dev
COPY . .
RUN npx vite build

FROM node:18-alpine
WORKDIR /app
RUN npm install -g serve
COPY --from=builder /app/dist /app/dist
EXPOSE 5173
CMD ["serve", "-s", "dist", "-l", "5173"]
