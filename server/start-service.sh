#!/bin/bash
set -e

# Create systemd service
cat > /etc/systemd/system/spotik-server.service <<'EOF'
[Unit]
Description=Spotik Dating API Server
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/spotik/server
ExecStart=/usr/bin/java -jar /opt/spotik/server/build/libs/spotik-server.jar
Restart=always
RestartSec=5
Environment=DATABASE_URL=jdbc:postgresql://localhost:5432/spotik
Environment=DATABASE_USER=spotik
Environment=DATABASE_PASSWORD=spotik_secret
Environment=JWT_SECRET=sp0t1k-jwt-s3cret-pr0d-2024-xK9mN2pL

[Install]
WantedBy=multi-user.target
EOF

# Stop if already running
systemctl stop spotik-server 2>/dev/null || true

# Enable and start
systemctl daemon-reload
systemctl enable spotik-server
systemctl start spotik-server
sleep 3

# Check status
systemctl status spotik-server --no-pager | head -15
echo "---"
curl -s http://localhost:8080/ || echo "CURL_FAILED"
echo ""
echo "SERVER_STARTED"

