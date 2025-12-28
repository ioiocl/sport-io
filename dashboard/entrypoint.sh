#!/bin/sh

# Replace placeholder with actual WebSocket URL from environment variable
if [ -n "$WEBSOCKET_URL" ]; then
  echo "Configuring WebSocket URL: $WEBSOCKET_URL"
  
  # Extract host from URL (remove https:// or http://)
  WEBSOCKET_HOST=$(echo "$WEBSOCKET_URL" | sed -e 's|^https\?://||' -e 's|/.*$||')
  echo "Extracted WebSocket Host: $WEBSOCKET_HOST"
  
  # Update nginx config with actual URL and host
  sed -i "s|WEBSOCKET_URL_PLACEHOLDER|$WEBSOCKET_URL|g" /etc/nginx/conf.d/default.conf
  sed -i "s|WEBSOCKET_HOST_PLACEHOLDER|$WEBSOCKET_HOST|g" /etc/nginx/conf.d/default.conf
  
  # Create config.js with runtime configuration
  cat > /usr/share/nginx/html/config.js << EOF
window.APP_CONFIG = {
  WEBSOCKET_URL: '$WEBSOCKET_URL'
};
EOF
  
  echo "✅ Nginx configuration updated:"
  echo "   Backend URL: $WEBSOCKET_URL"
  echo "   Backend Host: $WEBSOCKET_HOST"
  
  # Show the actual nginx config for debugging
  echo ""
  echo "📋 Nginx proxy configuration:"
  grep -A 15 "location /api/" /etc/nginx/conf.d/default.conf || echo "Could not display config"
  
else
  echo "WARNING: WEBSOCKET_URL not set, using localhost"
  sed -i "s|WEBSOCKET_URL_PLACEHOLDER|http://localhost:8083|g" /etc/nginx/conf.d/default.conf
  sed -i "s|WEBSOCKET_HOST_PLACEHOLDER|localhost|g" /etc/nginx/conf.d/default.conf
  
  cat > /usr/share/nginx/html/config.js << EOF
window.APP_CONFIG = {
  WEBSOCKET_URL: 'http://localhost:8083'
};
EOF
fi

# Start nginx
echo "🚀 Starting nginx..."
exec nginx -g 'daemon off;'
