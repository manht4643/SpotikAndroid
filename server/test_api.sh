#!/bin/bash
# Test email send
curl -s http://localhost:8080/api/email/send-code \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{"email":"gzhhsgxgxv@gmail.com"}'
echo ""
echo "---"
# Test Telegram init
curl -s http://localhost:8080/api/auth/telegram/init \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{}'
echo ""
echo "---"
# Test health
curl -s http://localhost:8080/health
echo ""
echo "TESTS_DONE"

