#!/bin/bash
# Fix nginx config for Love u API
set -e

CONF="/etc/nginx/sites-enabled/almet-dating"

# Remove the broken location block inserted by the previous attempt
python3 -c "
import re
with open('$CONF', 'r') as f:
    content = f.read()

# Remove the broken Love u block
pattern = r'\s*# ── Love u Mobile Dating API.*?\n\s*}\n'
content = re.sub(pattern, '\n', content, flags=re.DOTALL)

# Now insert the correct block before '# API бэкенда на /api/'
correct_block = '''
    # ── Love u Mobile Dating API (Ktor :8080) ──
    location ~ ^/api/(register|login|logout|me|auth/|health) {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \\\$host;
        proxy_set_header X-Real-IP \\\$remote_addr;
        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \\\$scheme;
        proxy_connect_timeout 10s;
        proxy_read_timeout 30s;
        proxy_send_timeout 30s;
    }

'''

content = content.replace('# API бэкенда на /api/', correct_block + '    # API бэкенда на /api/')

with open('$CONF', 'w') as f:
    f.write(content)

print('Config updated successfully')
"

nginx -t && systemctl reload nginx && echo "NGINX RELOADED OK"

