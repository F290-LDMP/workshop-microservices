#!/usr/bin/env python3
"""Verifica consistência eventual pelo Gateway; requer Compose em execução."""
import json
import os
import secrets
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

BASE = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
TIMEOUT = float(os.environ.get("SMOKE_TIMEOUT", "120"))
PRODUCT_ID = secrets.randbelow(1_000_000_000) + 1

def request(method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    try:
        with urlopen(Request(BASE + path, data=data, method=method,
                             headers={"Content-Type": "application/json"}), timeout=10) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None
    except HTTPError as error:
        raw = error.read()
        return error.code, json.loads(raw) if raw else None

def eventually(description, condition):
    deadline = time.monotonic() + TIMEOUT
    last = None
    while time.monotonic() < deadline:
        try:
            last = condition()
            if last:
                return
        except (URLError, TimeoutError, ConnectionError, subprocess.SubprocessError) as error:
            last = str(error)
        time.sleep(0.5)
    raise AssertionError(f"Timeout: {description}; último resultado: {last}")

def complete():
    status, aggregate = request("GET", f"/product-composite/{PRODUCT_ID}")
    return status == 200 and aggregate.get("name") == "Workshop RabbitMQ" and \
        len(aggregate.get("reviews", [])) == 1 and \
        len(aggregate.get("recommendations", [])) == 1 and \
        aggregate["recommendations"][0]["rate"] == 5

def children_deleted():
    for service, port, path in [("review", 7003, "review"), ("recommendation", 7002, "recommendation")]:
        raw = subprocess.check_output(["docker", "compose", "exec", "-T", service + "-service",
            "curl", "-fsS", f"http://localhost:{port}/{path}?productId={PRODUCT_ID}"], timeout=15)
        if json.loads(raw) != []:
            return False
    return True

def main():
    eventually("Gateway/Eureka disponíveis", lambda: request("GET", "/product-composite/0")[0] == 422)
    payload = {"productId": PRODUCT_ID, "name": "Workshop RabbitMQ", "weight": 100,
               "recommendations": [{"recommendationId": 1, "author": "Ana", "rate": 5, "content": "Ótimo"}],
               "reviews": [{"reviewId": 1, "author": "Bia", "subject": "Qualidade", "content": "OK"}]}
    status, body = request("POST", "/product-composite", payload)
    assert status == 202, (status, body)
    eventually("agregado completo", complete)
    # The same command payload must be safe to deliver again.
    status, body = request("POST", "/product-composite", payload)
    assert status == 202, (status, body)
    eventually("agregado sem duplicação", complete)
    status, body = request("DELETE", f"/product-composite/{PRODUCT_ID}")
    assert status == 202, (status, body)
    eventually("produto excluído", lambda: request("GET", f"/product-composite/{PRODUCT_ID}")[0] == 404)
    eventually("reviews e recommendations excluídas", children_deleted)
    print(f"OK: criação, consulta, repetição e exclusão assíncronas; productId={PRODUCT_ID}")

if __name__ == "__main__":
    main()
