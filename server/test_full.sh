#!/bin/bash
echo "=== Full API test ==="

# 1. Health
echo "--- /health ---"
curl -s http://localhost:8080/health
echo ""

# 2. Login with existing user (just check response)
echo "--- /api/login ---"
curl -s -X POST http://localhost:8080/api/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"gzhhsgxgxv@gmail.com","password":"wrongpass"}'
echo ""

# 3. Telegram init
echo "--- /api/auth/telegram/init ---"
curl -s -X POST http://localhost:8080/api/auth/telegram/init \
  -H 'Content-Type: application/json' \
  -d '{}'
echo ""

# 4. Check HTTPS routing for ALL api paths
echo "--- HTTPS /api/register ---"
curl -s -o /dev/null -w "%{http_code}" -X POST https://avacorebot.online/api/register \
  -H 'Content-Type: application/json' -d '{}'
echo ""

echo "--- HTTPS /api/login ---"
curl -s -o /dev/null -w "%{http_code}" -X POST https://avacorebot.online/api/login \
  -H 'Content-Type: application/json' -d '{}'
echo ""

echo "--- HTTPS /api/me ---"
curl -s -o /dev/null -w "%{http_code}" https://avacorebot.online/api/me
echo ""

echo "--- HTTPS /api/email/send-code ---"
curl -s -o /dev/null -w "%{http_code}" -X POST https://avacorebot.online/api/email/send-code \
  -H 'Content-Type: application/json' -d '{"email":"test@test.com"}'
echo ""

echo "--- HTTPS /api/auth/telegram/init ---"
curl -s -o /dev/null -w "%{http_code}" -X POST https://avacorebot.online/api/auth/telegram/init \
  -H 'Content-Type: application/json' -d '{}'
echo ""

echo "ALL_TESTS_DONE"

