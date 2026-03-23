#!/bin/bash
set -e

echo "=== [1/6] Добавляем SSH-ключ ==="
mkdir -p /root/.ssh
chmod 700 /root/.ssh
PUB_KEY="PUBKEY_PLACEHOLDER"
grep -qF "$PUB_KEY" /root/.ssh/authorized_keys 2>/dev/null || echo "$PUB_KEY" >> /root/.ssh/authorized_keys
chmod 600 /root/.ssh/authorized_keys
echo "SSH-ключ добавлен"

echo "=== [2/6] Обновляем пакеты ==="
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq ca-certificates curl gnupg lsb-release > /dev/null

echo "=== [3/6] Устанавливаем Docker ==="
if ! command -v docker &> /dev/null; then
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list
    apt-get update -qq
    apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin > /dev/null
    systemctl enable docker
    systemctl start docker
    echo "Docker установлен"
else
    echo "Docker уже установлен"
fi
docker --version
docker compose version

echo "=== [4/6] Создаём директорию проекта ==="
mkdir -p /opt/spotik/server

echo "=== [5/6] Проверяем порт 8080 ==="
if ss -tlnp | grep -q ':8080'; then
    echo "ВНИМАНИЕ: Порт 8080 уже занят!"
    ss -tlnp | grep ':8080'
else
    echo "Порт 8080 свободен"
fi

echo "=== [6/6] Проверяем порт 5432 ==="
if ss -tlnp | grep -q ':5432'; then
    echo "ВНИМАНИЕ: Порт 5432 уже занят!"
    ss -tlnp | grep ':5432'
else
    echo "Порт 5432 свободен"
fi

echo ""
echo "=== Сервер готов к деплою! ==="

