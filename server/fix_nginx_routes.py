#!/usr/bin/env python3
"""Fix nginx config: add pipe separators to Love u API regex"""
import re

path = '/etc/nginx/sites-enabled/almet-dating'
with open(path, 'r') as f:
    content = f.read()

# Fix the broken regex — add | separators between route names
old_line = 'location ~ ^/api/(register'
# Find the full line and replace with correct regex
content = re.sub(
    r'location ~ \^/api/\([^)]+\)',
    r'location ~ ^/api/(register|login|logout|me|auth/|email/|telegram/|health)',
    content,
    count=1
)

with open(path, 'w') as f:
    f.write(content)

print('DONE: nginx config fixed')

