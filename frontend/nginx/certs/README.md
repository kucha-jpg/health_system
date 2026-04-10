# TLS Certificates

Place your certificate and private key in this directory before enabling TLS compose override.

Required filenames:

- `server.crt`
- `server.key`

Quick self-signed certificate example (development only):

```bash
openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
  -keyout server.key -out server.crt -subj "/CN=localhost"
```
