import re
import sys

conf_path = "/etc/nginx/sites-enabled/almet-dating"
with open(conf_path, "r") as f:
    content = f.read()

# Remove broken Love u block (from previous failed attempt)
pattern = r'\n\s*# [^\n]*Love u[^\n]*\n.*?proxy_send_timeout\s+\d+s;\s*\n\s*\}\n'
content = re.sub(pattern, '\n', content, flags=re.DOTALL)

# Correct location block with pipe-separated alternatives
alternatives = "register|login|logout|me|auth/|health"
correct = """
    # Love u Dating API (Ktor :8080)
    location ~ ^/api/(%s) {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 10s;
        proxy_read_timeout 30s;
        proxy_send_timeout 30s;
    }

""" % alternatives

# Find insertion point - before generic /api/ block
markers = ["    # API ", "    location /api/ {"]
inserted = False
for marker in markers:
    idx = content.find(marker)
    if idx >= 0:
        content = content[:idx] + correct + content[idx:]
        inserted = True
        print("Block inserted before: " + marker.strip())
        break

if not inserted:
    print("ERROR: Could not find insertion point!", file=sys.stderr)
    sys.exit(1)

with open(conf_path, "w") as f:
    f.write(content)
print("Config saved successfully")

