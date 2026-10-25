#!/usr/bin/env python3
"""Emite token de service account do laboratório; imprime somente o access token."""
import json
import os
import sys
from urllib.parse import urlencode
from urllib.request import Request, urlopen

client = sys.argv[1] if len(sys.argv) > 1 else 'writer'
if client not in ('reader', 'writer'):
    raise SystemExit('Uso: python3 scripts/keycloak-token.py [reader|writer]')
url = os.environ.get('KEYCLOAK_URL', 'http://localhost:8180').rstrip('/')
body = urlencode({'grant_type': 'client_credentials', 'client_id': 'workshop-' + client,
                  'client_secret': os.environ.get('KEYCLOAK_CLIENT_SECRET', client + '-secret')}).encode()
with urlopen(Request(url + '/realms/workshop/protocol/openid-connect/token', data=body), timeout=15) as response:
    print(json.load(response)['access_token'])
