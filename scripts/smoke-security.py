#!/usr/bin/env python3
"""Valida JWT real, scopes e proteção direta; requer Keycloak e Compose iniciados."""
import os
import subprocess
import sys
from urllib.request import Request, urlopen
from urllib.error import HTTPError


def status(base, method='GET', token=None, path='/product-composite/0'):
    headers = {'Authorization': 'Bearer ' + token} if token else {}
    try:
        with urlopen(Request(base + path, method=method, headers=headers), timeout=15) as response:
            return response.status
    except HTTPError as error:
        return error.code

reader = subprocess.check_output([sys.executable, 'scripts/keycloak-token.py', 'reader'], text=True).strip()
writer = subprocess.check_output([sys.executable, 'scripts/keycloak-token.py', 'writer'], text=True).strip()
for base in [os.environ.get('BASE_URL', 'http://localhost:8080'), os.environ.get('COMPOSITE_URL', 'http://localhost:7100')]:
    assert status(base) == 401, base
    assert status(base, token='invalid') == 401, base
    assert status(base, token=reader) == 422, base
    assert status(base, 'DELETE', reader) == 403, base
    assert status(base, 'DELETE', writer) == 422, base
print('OK: JWT real, 401/403 e validação autorizada no Gateway e no composite')
subprocess.run([sys.executable, 'scripts/smoke-messaging.py'], env={**os.environ, 'ACCESS_TOKEN': writer}, check=True)
