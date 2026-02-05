# Kuckmal API Server Deployment Guide

This document describes how to deploy the Kuckmal API server to production.

## Domain Configuration

- **API Subdomain:** `api.kuckmal.cutthecrap.link`
- **Web App Domain:** `kuckmal.cutthecrap.link`

## Prerequisites

- Ubuntu/Debian server (or similar Linux distribution)
- Python 3.9+
- Nginx
- Domain DNS access

## DNS Setup

Add the following DNS record:

```
Type: A (or CNAME)
Name: api.kuckmal
Target: <your-server-ip>
TTL: 3600
```

## Server Setup

### 1. Install Dependencies

```bash
sudo apt update
sudo apt install python3 python3-pip python3-venv nginx certbot python3-certbot-nginx
```

### 2. Create Application User

```bash
sudo useradd -m -s /bin/bash kuckmal
sudo mkdir -p /opt/kuckmal
sudo chown kuckmal:kuckmal /opt/kuckmal
```

### 3. Deploy Application

```bash
sudo -u kuckmal bash
cd /opt/kuckmal

# Clone or copy the api directory
# git clone ... or scp -r api/ kuckmal@server:/opt/kuckmal/

# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r api/requirements.txt
pip install gunicorn
```

### 4. Create Systemd Service

Create `/etc/systemd/system/kuckmal-api.service`:

```ini
[Unit]
Description=Kuckmal API Server
After=network.target

[Service]
User=kuckmal
Group=kuckmal
WorkingDirectory=/opt/kuckmal/api
Environment="PATH=/opt/kuckmal/venv/bin"
ExecStart=/opt/kuckmal/venv/bin/gunicorn -w 4 -b 127.0.0.1:5000 "app:create_app()"
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Enable and start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable kuckmal-api
sudo systemctl start kuckmal-api
```

### 5. Configure Nginx

Create `/etc/nginx/sites-available/api.kuckmal.cutthecrap.link`:

```nginx
server {
    listen 80;
    server_name api.kuckmal.cutthecrap.link;

    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable the site:

```bash
sudo ln -s /etc/nginx/sites-available/api.kuckmal.cutthecrap.link /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 6. SSL/TLS Certificate (Let's Encrypt)

```bash
sudo certbot --nginx -d api.kuckmal.cutthecrap.link
```

Certbot will automatically update the Nginx configuration to:

```nginx
server {
    listen 443 ssl;
    server_name api.kuckmal.cutthecrap.link;

    ssl_certificate /etc/letsencrypt/live/api.kuckmal.cutthecrap.link/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.kuckmal.cutthecrap.link/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name api.kuckmal.cutthecrap.link;
    return 301 https://$host$request_uri;
}
```

### 7. Auto-Renewal

Certbot automatically sets up certificate renewal. Verify with:

```bash
sudo certbot renew --dry-run
```

## Database Management

### Location

The SQLite database is stored at `/opt/kuckmal/api/media.db` by default.

### Backup

Create a backup script at `/opt/kuckmal/backup.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/opt/kuckmal/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
cp /opt/kuckmal/api/media.db "$BACKUP_DIR/media_$DATE.db"
# Keep only last 7 backups
ls -t $BACKUP_DIR/media_*.db | tail -n +8 | xargs -r rm
```

Add to crontab:

```bash
crontab -e
# Add: 0 2 * * * /opt/kuckmal/backup.sh
```

## Verification

### Test API Health

```bash
curl https://api.kuckmal.cutthecrap.link/api/health
```

Expected response:
```json
{"status": "healthy", "service": "kuckmal-api"}
```

### Test CORS Headers

```bash
curl -I -H "Origin: https://kuckmal.cutthecrap.link" \
     https://api.kuckmal.cutthecrap.link/api/channels
```

Should include:
```
Access-Control-Allow-Origin: *
```

### Test from Web App

Open `https://kuckmal.cutthecrap.link` in a browser and verify that channel data loads correctly.

## Troubleshooting

### Check Service Status

```bash
sudo systemctl status kuckmal-api
sudo journalctl -u kuckmal-api -f
```

### Check Nginx Logs

```bash
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### Common Issues

1. **502 Bad Gateway**: The Flask app isn't running. Check `systemctl status kuckmal-api`.
2. **CORS errors**: Verify the API returns proper CORS headers.
3. **SSL errors**: Run `certbot renew` and check certificate validity.

## Updating the Application

```bash
# Stop the service
sudo systemctl stop kuckmal-api

# Update code
cd /opt/kuckmal/api
git pull  # or copy new files

# Update dependencies
source /opt/kuckmal/venv/bin/activate
pip install -r requirements.txt

# Restart
sudo systemctl start kuckmal-api
```
