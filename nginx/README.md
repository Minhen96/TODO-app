# Nginx Configuration for VPS Deployment

## Installation (Ubuntu/Debian)

```bash
# Install Nginx
sudo apt update
sudo apt install nginx

# Install Certbot for SSL
sudo apt install certbot python3-certbot-nginx
```

## Setup Steps

### 1. Copy configuration

```bash
sudo cp taskplatform.conf /etc/nginx/sites-available/
```

### 2. Update domain name

Edit `/etc/nginx/sites-available/taskplatform.conf`:
- Replace `taskplatform.example.com` with your actual domain

### 3. Initial setup (without SSL)

Before getting SSL certificate, temporarily modify the config:

```bash
# Comment out the HTTPS server block
# Only keep the HTTP server block
# Remove the redirect to HTTPS temporarily
```

### 4. Enable the site

```bash
sudo ln -s /etc/nginx/sites-available/taskplatform.conf /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 5. Get SSL certificate

```bash
sudo certbot --nginx -d taskplatform.example.com
```

### 6. Restore full configuration

Uncomment the HTTPS block and re-enable redirect.

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## Useful Commands

```bash
# Test configuration
sudo nginx -t

# Reload configuration
sudo systemctl reload nginx

# View access logs
sudo tail -f /var/log/nginx/taskplatform.access.log

# View error logs
sudo tail -f /var/log/nginx/taskplatform.error.log

# Check status
sudo systemctl status nginx
```

## Firewall Setup

```bash
sudo ufw allow 'Nginx Full'
sudo ufw enable
```
