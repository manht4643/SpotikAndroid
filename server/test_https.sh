#!/bin/bash
# Test email via HTTPS
echo "=== Test email/send-code via HTTPS ==="
curl -s -X POST https://avacorebot.online/api/email/send-code \
  -H 'Content-Type: application/json' \
  -d '{"email":"gzhhsgxgxv@gmail.com"}'
echo ""
echo "=== Test register via HTTPS ==="
curl -s -X POST https://avacorebot.online/api/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test_delete_me@gmail.com","password":"test123456","name":"Test","age":20,"city":"Almetievsk"}'
echo ""
echo "DONE"

