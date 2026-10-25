#!/usr/bin/env python3
"""Verifica os arquivos remotos por aplicação/profile e autenticação HTTP."""
import base64
import json
import os
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from urllib.parse import quote

base = os.environ.get('CONFIG_SERVER_URL', 'http://localhost:8888').rstrip('/')
label = os.environ.get('CONFIG_GIT_LABEL', 'codex/central-configuration').replace('/', '(_)')
auth = base64.b64encode((os.environ.get('CONFIG_SERVER_USER', 'config') + ':' +
                        os.environ.get('CONFIG_SERVER_PASSWORD', 'config-workshop')).encode()).decode()
path = '/product/docker/' + quote(label, safe='(_)')
try:
    urlopen(base + path, timeout=15).close()
    raise AssertionError('Config Server aceitou configuração sem autenticação')
except HTTPError as error:
    assert error.code == 401, error.code
for app in ['product', 'recommendation', 'review', 'product-composite', 'gateway', 'eureka-server']:
    for profile in ['local', 'docker']:
        request = Request(f'{base}/{app}/{profile}/' + quote(label, safe='(_)'),
                          headers={'Authorization': 'Basic ' + auth})
        with urlopen(request, timeout=20) as response:
            env = json.load(response)
        merged = {}
        for source in reversed(env['propertySources']):
            merged.update(source['source'])
        assert env['version'], env
        assert merged['info.workshop.application'] == app, env
        assert merged['info.workshop.profile'] == profile, env
        assert merged['info.workshop.lesson'], env
        if profile == 'docker' and app == 'product':
            assert merged['spring.rabbitmq.host'] == 'rabbitmq', env
        print(f'OK: {app}/{profile} commit={env["version"][:12]}')
