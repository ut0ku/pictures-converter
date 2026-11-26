import json
import os
import struct
import uuid
import urllib.error
import urllib.request
import zlib
from datetime import datetime, timedelta, timezone

import pytest


BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080").rstrip("/")
ADMIN_EMAIL = os.getenv("ADMIN_EMAIL", "admin@example.com")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD")
CLIENT_PASSWORD = "pytest-password-123"


def api_request(method, path, *, payload=None, token=None, headers=None, body=None):
    request_headers = dict(headers or {})
    if token:
        request_headers["Authorization"] = f"Bearer {token}"
    if payload is not None:
        body = json.dumps(payload).encode()
        request_headers["Content-Type"] = "application/json"

    request = urllib.request.Request(
        f"{BASE_URL}{path}",
        data=body,
        headers=request_headers,
        method=method,
    )
    try:
        response = urllib.request.urlopen(request, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, response.headers, response.read()


def json_body(response):
    return json.loads(response.decode())


def register_client():
    email = f"pytest-{uuid.uuid4().hex}@example.com"
    status, _, body = api_request(
        "POST",
        "/api/auth/register",
        payload={"email": email, "password": CLIENT_PASSWORD},
    )
    assert status == 200, body.decode(errors="replace")
    return email, json_body(body)


def login_admin():
    if not ADMIN_PASSWORD:
        pytest.skip("Set ADMIN_PASSWORD to test admin endpoints")
    status, _, body = api_request(
        "POST",
        "/api/auth/login",
        payload={"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD},
    )
    assert status == 200, body.decode(errors="replace")
    admin = json_body(body)
    assert admin["role"] == "ADMIN"
    return admin


def png_1x1():
    def chunk(name, data):
        return (
            struct.pack(">I", len(data))
            + name
            + data
            + struct.pack(">I", zlib.crc32(name + data) & 0xFFFFFFFF)
        )

    header = struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0)
    pixel = zlib.compress(b"\x00\xff\x00\x00\xff")
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(
        b"IDAT", pixel
    ) + chunk(b"IEND", b"")


def multipart_png(guest_key, output_format="PNG"):
    boundary = f"pytest-{uuid.uuid4().hex}"
    image = png_1x1()
    body = (
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="file"; filename="pixel.png"\r\n'
        "Content-Type: image/png\r\n\r\n"
    ).encode() + image + (
        f"\r\n--{boundary}\r\n"
        'Content-Disposition: form-data; name="format"\r\n\r\n'
        f"{output_format}\r\n"
        f"--{boundary}--\r\n"
    ).encode()
    return body, {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "X-Guest-Key": guest_key,
    }


@pytest.fixture
def client_account():
    return register_client()


@pytest.fixture
def client_session(client_account):
    email, _ = client_account
    status, _, body = api_request(
        "POST",
        "/api/auth/login",
        payload={"email": email, "password": CLIENT_PASSWORD},
    )
    assert status == 200, body.decode(errors="replace")
    return email, json_body(body)


@pytest.fixture
def client_conversion(client_session):
    _, session = client_session
    guest_key = uuid.uuid4().hex
    image, headers = multipart_png(guest_key)
    status, _, body = api_request(
        "POST",
        "/api/convert",
        body=image,
        headers={**headers, "Authorization": f"Bearer {session['accessToken']}"},
    )
    assert status == 200, body.decode(errors="replace")
    return session, json_body(body)


def test_register_endpoint(client_account):
    email, registered = client_account
    assert registered["email"] == email
    assert registered["role"] == "CLIENT"
    assert registered["accessToken"]
    assert registered["refreshToken"]


def test_login_endpoint(client_account):
    email, _ = client_account
    status, _, body = api_request(
        "POST",
        "/api/auth/login",
        payload={"email": email, "password": CLIENT_PASSWORD},
    )
    assert status == 200, body.decode(errors="replace")
    assert json_body(body)["email"] == email


def test_refresh_endpoint(client_session):
    _, session = client_session
    status, _, body = api_request(
        "POST",
        "/api/auth/refresh",
        payload={"refreshToken": session["refreshToken"]},
    )
    assert status == 200, body.decode(errors="replace")
    renewed = json_body(body)
    assert renewed["refreshToken"] != session["refreshToken"]

    status, _, _ = api_request(
        "POST",
        "/api/auth/refresh",
        payload={"refreshToken": session["refreshToken"]},
    )
    assert status == 401


def test_me_endpoint(client_session):
    email, session = client_session
    status, _, body = api_request("GET", "/api/auth/me", token=session["accessToken"])
    assert status == 200, body.decode(errors="replace")
    assert json_body(body)["email"] == email


def test_profile_endpoint(client_session):
    _, session = client_session
    status, _, body = api_request(
        "PATCH",
        "/api/auth/profile",
        payload={"displayName": "Pytest Client"},
        token=session["accessToken"],
    )
    assert status == 200, body.decode(errors="replace")
    assert json_body(body)["displayName"] == "Pytest Client"


def test_authenticated_quota_endpoint(client_session):
    _, session = client_session
    status, _, body = api_request("GET", "/api/quota", token=session["accessToken"])
    assert status == 200, body.decode(errors="replace")
    assert json_body(body)["tier"] == "CLIENT"


def test_guest_quota_endpoint():
    guest_key = uuid.uuid4().hex
    status, _, body = api_request(
        "GET", "/api/quota", headers={"X-Guest-Key": guest_key}
    )
    assert status == 200, body.decode(errors="replace")
    assert json_body(body)["tier"] == "GUEST"


def test_quota_updates_after_each_conversion(client_session):
    _, session = client_session
    status, _, body = api_request("GET", "/api/quota", token=session["accessToken"])
    assert status == 200
    initial_quota = json_body(body)

    for _ in range(2):
        image, headers = multipart_png(uuid.uuid4().hex)
        status, _, body = api_request(
            "POST",
            "/api/convert",
            body=image,
            headers={**headers, "Authorization": f"Bearer {session['accessToken']}"},
        )
        assert status == 200, body.decode(errors="replace")

    status, _, body = api_request("GET", "/api/quota", token=session["accessToken"])
    assert status == 200
    updated_quota = json_body(body)
    assert updated_quota["dailyUsed"] == initial_quota["dailyUsed"] + 2
    assert updated_quota["totalUsedBytes"] > initial_quota["totalUsedBytes"]


def test_authenticated_convert_endpoint(client_session):
    _, session = client_session
    image, headers = multipart_png(uuid.uuid4().hex)
    status, _, body = api_request(
        "POST",
        "/api/convert",
        body=image,
        headers={**headers, "Authorization": f"Bearer {session['accessToken']}"},
    )
    assert status == 200, body.decode(errors="replace")
    conversion = json_body(body)
    assert conversion["resultFormat"] == "PNG"
    assert conversion["id"]


def test_guest_convert_endpoint():
    guest_key = uuid.uuid4().hex
    image, headers = multipart_png(guest_key)
    status, _, body = api_request(
        "POST", "/api/convert", body=image, headers=headers
    )
    assert status == 200, body.decode(errors="replace")
    assert json_body(body)["resultFormat"] == "PNG"


@pytest.mark.parametrize(
    ("output_format", "content_type", "signature"),
    [
        pytest.param("WEBP", "image/webp", b"WEBP", id="WEBP"),
        pytest.param("GIF", "image/gif", b"GIF8", id="GIF"),
        pytest.param("TIFF", "image/tiff", b"II*\x00", id="TIFF"),
        pytest.param("HEIC", "image/heic", b"ftyp", id="HEIC"),
        pytest.param("AVIF", "image/avif", b"ftyp", id="AVIF"),
    ],
)
def test_extended_output_format_conversion(output_format, content_type, signature):
    guest_key = uuid.uuid4().hex
    image, headers = multipart_png(guest_key, output_format)
    status, _, body = api_request(
        "POST", "/api/convert", body=image, headers=headers
    )
    assert status == 200, body.decode(errors="replace")
    conversion = json_body(body)
    assert conversion["resultFormat"] == output_format

    status, response_headers, result = api_request(
        "GET",
        conversion["downloadUrl"],
        headers={"X-Guest-Key": guest_key},
    )
    assert status == 200
    assert response_headers.get_content_type() == content_type
    if output_format == "WEBP":
        assert result.startswith(b"RIFF") and result[8:12] == signature
    elif output_format in {"HEIC", "AVIF"}:
        assert result[4:8] == signature
    else:
        assert result.startswith(signature)


def test_conversion_history_endpoint(client_conversion):
    session, conversion = client_conversion
    status, _, body = api_request(
        "GET",
        "/api/conversions?page=0&size=10",
        token=session["accessToken"],
    )
    assert status == 200, body.decode(errors="replace")
    assert conversion["id"] in [item["id"] for item in json_body(body)["content"]]


def test_conversion_download_endpoint(client_conversion):
    session, conversion = client_conversion
    status, headers, body = api_request(
        "GET",
        conversion["downloadUrl"],
        token=session["accessToken"],
    )
    assert status == 200
    assert headers.get_content_type() == "image/png"
    assert body.startswith(b"\x89PNG\r\n\x1a\n")


def test_guest_conversion_download_requires_matching_guest_key():
    guest_key = uuid.uuid4().hex
    image, headers = multipart_png(guest_key)
    status, _, body = api_request(
        "POST", "/api/convert", body=image, headers=headers
    )
    assert status == 200, body.decode(errors="replace")
    conversion = json_body(body)

    status, _, body = api_request(
        "GET",
        conversion["downloadUrl"],
        headers={"X-Guest-Key": guest_key},
    )
    assert status == 200
    assert body.startswith(b"\x89PNG\r\n\x1a\n")

    status, _, _ = api_request(
        "GET",
        conversion["downloadUrl"],
        headers={"X-Guest-Key": uuid.uuid4().hex},
    )
    assert status == 403


def test_admin_stats_endpoint_requires_admin_role():
    status, _, _ = api_request("GET", "/api/admin/stats")
    assert status == 403

    _, client = register_client()
    status, _, _ = api_request(
        "GET", "/api/admin/stats", token=client["accessToken"]
    )
    assert status == 403

    admin = login_admin()
    status, _, body = api_request(
        "GET", "/api/admin/stats", token=admin["accessToken"]
    )
    assert status == 200
    stats = json_body(body)
    assert "conversions" in stats
    assert "server" in stats


def test_admin_user_quota_endpoints_require_admin_role():
    status, _, _ = api_request("GET", "/api/admin/users")
    assert status == 403

    _, client = register_client()
    status, _, _ = api_request(
        "GET", "/api/admin/users", token=client["accessToken"]
    )
    assert status == 403
    status, _, _ = api_request(
        "POST",
        "/api/admin/users/1/bonus",
        payload={"extraConversions": 5, "extraMegabytes": 25},
        token=client["accessToken"],
    )
    assert status == 403


def test_admin_can_grant_additive_24_hour_daily_bonus():
    email, client = register_client()
    admin = login_admin()

    status, _, body = api_request(
        "GET", "/api/admin/users", token=admin["accessToken"]
    )
    assert status == 200, body.decode(errors="replace")
    user = next(user for user in json_body(body) if user["email"] == email)

    status, _, body = api_request(
        "POST",
        f"/api/admin/users/{user['id']}/bonus",
        payload={"extraConversions": 5, "extraMegabytes": 25},
        token=admin["accessToken"],
    )
    assert status == 200, body.decode(errors="replace")
    grant = json_body(body)
    assert grant["bonusDailyConversions"] == 5
    assert grant["bonusDailyMegabytes"] == 25
    expires_at = datetime.fromisoformat(grant["bonusExpiresAt"].replace("Z", "+00:00"))
    assert timedelta(0) < expires_at - datetime.now(timezone.utc) <= timedelta(hours=24)

    status, _, body = api_request(
        "POST",
        f"/api/admin/users/{user['id']}/bonus",
        payload={"extraConversions": 2, "extraMegabytes": 5},
        token=admin["accessToken"],
    )
    assert status == 200, body.decode(errors="replace")
    grant = json_body(body)
    assert grant["bonusDailyConversions"] == 7
    assert grant["bonusDailyMegabytes"] == 30

    status, _, body = api_request(
        "GET", "/api/quota", token=client["accessToken"]
    )
    assert status == 200, body.decode(errors="replace")
    quota = json_body(body)
    assert quota["dailyLimit"] == 157
    assert quota["totalLimitBytes"] == 530 * 1024 * 1024
    assert quota["bonusDailyConversions"] == 7
    assert quota["bonusDailyBytes"] == 30 * 1024 * 1024
    assert quota["bonusExpiresAt"] is not None
