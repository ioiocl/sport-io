#!/bin/sh

# Replace placeholder with actual WebSocket URL from environment variable
if [ -n "$WEBSOCKET_URL" ]; then
  echo "Configuring WebSocket URL: $WEBSOCKET_URL"
  
  # Update nginx config with actual URL
  sed -i "s|WEBSOCKET_URL_PLACEHOLDER|$WEBSOCKET_URL|g" /etc/nginx/conf.d/default.conf
  
  # Create config.js with runtime configuration
  cat > /usr/share/nginx/html/config.js << EOF
window.APP_CONFIG = {
  WEBSOCKET_URL: '$WEBSOCKET_URL'
};
EOF
else
  echo "WARNING: WEBSOCKET_URL not set, using localhost"
  cat > /usr/share/nginx/html/config.js << EOF
window.APP_CONFIG = {
  WEBSOCKET_URL: 'http://localhost:8083'
};
EOF
fi

# Start nginx
exec nginx -g 'daemon off;'
