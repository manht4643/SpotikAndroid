#!/bin/bash
# =================================================
# Скрипт для настройки SMTP и Telegram бота
# Запускай на сервере: bash /opt/spotik/server/configure-auth.sh
# =================================================

set -e

echo "╔════════════════════════════════════════════════╗"
echo "║   Love u — Настройка Email и Telegram          ║"
echo "╚════════════════════════════════════════════════╝"
echo ""

# ─── SMTP (Gmail) ───────────────────────────────────
echo "═══ Настройка Email (Gmail SMTP) ═══"
echo ""
echo "Для отправки кодов подтверждения нужен Gmail аккаунт с App Password."
echo ""
echo "Инструкция:"
echo "  1. Включите двухфакторную аутентификацию: https://myaccount.google.com/security"
echo "  2. Создайте App Password: https://myaccount.google.com/apppasswords"
echo "  3. Название приложения: Love u"
echo "  4. Скопируйте 16-символьный пароль (без пробелов)"
echo ""

read -p "Gmail адрес (например, your@gmail.com): " SMTP_USER
read -p "App Password (16 символов): " SMTP_PASS

echo ""

# ─── Telegram Bot ───────────────────────────────────
echo "═══ Настройка Telegram бота ═══"
echo ""
echo "Инструкция:"
echo "  1. Откройте @BotFather в Telegram"
echo "  2. Отправьте /newbot"
echo "  3. Имя бота: Love u Auth"
echo "  4. Username бота: loveu_auth_bot (или любой свободный)"
echo "  5. Скопируйте токен бота"
echo ""

read -p "Bot Token (от BotFather): " TG_TOKEN
read -p "Bot Username (без @): " TG_USERNAME

echo ""

# ─── Обновляем systemd service ──────────────────────
echo "Обновляю конфигурацию сервера..."

cat > /etc/systemd/system/spotik-server.service <<EOF
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
Environment=SMTP_HOST=smtp.gmail.com
Environment=SMTP_PORT=587
Environment=SMTP_USERNAME=${SMTP_USER}
Environment=SMTP_PASSWORD=${SMTP_PASS}
Environment=SMTP_FROM=${SMTP_USER}
Environment=TELEGRAM_BOT_TOKEN=${TG_TOKEN}
Environment=TELEGRAM_BOT_USERNAME=${TG_USERNAME}

[Install]
WantedBy=multi-user.target
EOF

# Перезапуск
systemctl daemon-reload
systemctl restart spotik-server
sleep 3

echo ""
echo "═══ Проверка ═══"
systemctl status spotik-server --no-pager | head -10
echo ""
echo "✅ Готово! Сервер перезапущен с новыми настройками."
echo ""
echo "Проверь логи: journalctl -u spotik-server -f"

