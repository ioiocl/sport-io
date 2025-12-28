FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production=false || npm install

COPY . .
RUN npm run build

FROM nginx:alpine

# Copy built files
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx template (will be replaced at runtime)
COPY nginx.conf.template /etc/nginx/conf.d/default.conf

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 80

# Use entrypoint to inject runtime config
ENTRYPOINT ["/entrypoint.sh"]
