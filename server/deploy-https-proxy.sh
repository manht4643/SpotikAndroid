#!/bin/bash
set -e

echo "╔══════════════════════════════════════════════════╗"
echo "║  Love u API — Настройка HTTPS reverse proxy      ║"
echo "╚══════════════════════════════════════════════════╝"

# ── 1. Check that Ktor API is running ──
echo ""
echo "=== [1/4] Проверяем API сервер на порту 8080 ==="
if ss -tlnp | grep -q ':8080'; then
    echo "✓ API сервер запущен на порту 8080"
else
    echo "✗ API сервер НЕ запущен. Запустите: systemctl start spotik-server"
    exit 1
fi

# ── 2. Ensure nginx is installed ──
echo ""
echo "=== [2/4] Проверяем / устанавливаем nginx ==="
if ! command -v nginx &> /dev/null; then
    apt-get update -qq
    apt-get install -y -qq nginx
    systemctl enable nginx
    echo "✓ nginx установлен"
else
    echo "✓ nginx уже установлен: $(nginx -v 2>&1)"
fi

# ── 3. Check SSL certificate (Let's Encrypt) ──
echo ""
echo "=== [3/4] Проверяем SSL сертификат ==="
CERT_PATH="/etc/letsencrypt/live/avacorebot.online/fullchain.pem"
if [ -f "$CERT_PATH" ]; then
    echo "✓ SSL сертификат найден"
    openssl x509 -in "$CERT_PATH" -noout -dates 2>/dev/null | head -2
else
    echo "SSL сертификат не найден — получаем через certbot..."
    if ! command -v certbot &> /dev/null; then
        apt-get install -y -qq certbot python3-certbot-nginx
    fi
    # Stop nginx briefly for standalone cert
    systemctl stop nginx 2>/dev/null || true
    certbot certonly --standalone -d avacorebot.online --non-interactive --agree-tos --email admin@avacorebot.online || {
        echo "Не удалось получить сертификат. Пробуем через nginx plugin..."
        systemctl start nginx
        certbot --nginx -d avacorebot.online --non-interactive --agree-tos --email admin@avacorebot.online
    }
    systemctl start nginx 2>/dev/null || true
    echo "✓ SSL сертификат получен"
fi

# ── 4. Configure nginx ──
echo ""
echo "=== [4/4] Настраиваем nginx reverse proxy ==="

# Backup existing config
if [ -f /etc/nginx/sites-enabled/default ]; then
    cp /etc/nginx/sites-enabled/default /etc/nginx/sites-enabled/default.bak.$(date +%s) 2>/dev/null || true
fi

# Check if there's already a server block for avacorebot.online
EXISTING_CONF=$(grep -rl "avacorebot.online" /etc/nginx/sites-enabled/ 2>/dev/null | head -1)

if [ -n "$EXISTING_CONF" ]; then
    echo "Найден существующий конфиг: $EXISTING_CONF"

    # Check if /api/ location already exists
    if grep -q "location /api/" "$EXISTING_CONF"; then
        echo "✓ Блок /api/ уже настроен"
    else
        echo "Добавляем блок /api/ в существующий конфиг..."

        # Insert API location block before the last closing brace of the ssl server block
        # Find the ssl server block and add location /api/ inside it
        sed -i '/server_name.*avacorebot.online/,/^}/ {
            /location \/ {/i\
    # ── Love u API reverse proxy ──\
    location /api/ {\
        proxy_pass         http://127.0.0.1:8080/api/;\
        proxy_http_version 1.1;\
        proxy_set_header   Host              $host;\
        proxy_set_header   X-Real-IP         $remote_addr;\
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;\
        proxy_set_header   X-Forwarded-Proto $scheme;\
        proxy_connect_timeout 10s;\
        proxy_read_timeout    30s;\
        proxy_send_timeout    30s;\
    }\
\
    location /health {\
        proxy_pass http://127.0.0.1:8080/health;\
        proxy_http_version 1.1;\
        proxy_set_header Host $host;\
    }
        }' "$EXISTING_CONF"

        echo "✓ Блок /api/ добавлен"
    fi
else
    echo "Создаём новый конфиг для avacorebot.online..."

    # Check for letsencrypt options file
    SSL_OPTIONS=""
    if [ -f /etc/letsencrypt/options-ssl-nginx.conf ]; then
        SSL_OPTIONS="include /etc/letsencrypt/options-ssl-nginx.conf;"
    fi
    SSL_DHPARAM=""
    if [ -f /etc/letsencrypt/ssl-dhparams.pem ]; then
        SSL_DHPARAM="ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;"
    fi

    cat > /etc/nginx/sites-available/avacorebot-api.conf <<NGINX
server {
    listen 443 ssl http2;
    server_name avacorebot.online;

    ssl_certificate     /etc/letsencrypt/live/avacorebot.online/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/avacorebot.online/privkey.pem;
    ${SSL_OPTIONS}
    ${SSL_DHPARAM}

    # ── API reverse proxy ──
    location /api/ {
        proxy_pass         http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header   Host              \$host;
        proxy_set_header   X-Real-IP         \$remote_addr;
        proxy_set_header   X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 10s;
        proxy_read_timeout    30s;
        proxy_send_timeout    30s;
    }

    location /health {
        proxy_pass http://127.0.0.1:8080/health;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
    }

    location / {
        root /var/www/html;
        try_files \$uri \$uri/ /index.html;
    }
}
NGINX

    ln -sf /etc/nginx/sites-available/avacorebot-api.conf /etc/nginx/sites-enabled/
    echo "✓ Конфиг создан"
fi

# Test and reload
echo ""
echo "Проверяем конфигурацию nginx..."
nginx -t 2>&1

echo ""
echo "Перезагружаем nginx..."
systemctl reload nginx

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║  ✓ ГОТОВО!                                       ║"
echo "║                                                  ║"
echo "║  API доступен по адресу:                         ║"
echo "║  https://avacorebot.online/api/                  ║"
echo "║                                                  ║"
echo "║  Проверка:                                       ║"
echo "║  curl https://avacorebot.online/health           ║"
echo "╚══════════════════════════════════════════════════╝"

# Quick test
echo ""
echo "=== Тест API через HTTPS ==="
curl -s https://avacorebot.online/health 2>/dev/null && echo "" || echo "⚠ API пока не отвечает через HTTPS"

