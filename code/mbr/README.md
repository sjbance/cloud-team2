General Setup:

```
sudo apt-get update
sudo dpkg-reconfigure tzdata
sudo apt-get install ntp
sudo apt-get install postgresql
sudo apt-get install default-jre
```

DB Setup:

```
sudo su postgres
psql
CREATE USER csci4145 WITH PASSWORD 'supersecret';
CREATE DATABASE mbr OWNER csci4145;
GRANT ALL PRIVILEGES ON DATABASE mbr TO csci4145;
```


Nginx setup:

```
sudo apt-get install nginx
```

Ensure you can access the default nginx page via `domain.ca`. If you can't, make sure the inbound security rules for the VM are configured correctly

Acmetool setup:
```
sudo add-apt-repository ppa:hlandau/rhea
sudo apt-get update
sudo apt-get install acmetool
sudo acmetool quickstart
sudo acmetool want domain.ca
```

Edit nginx conf: `sudo nano /etc/nginx/sites-available/default`:

```
server {
        listen 80 default_server;
        root /var/www;
}

server {
        listen 443 ssl http2 default_server;

        ssl_certificate /var/lib/acme/live/domain.ca/fullchain;
        ssl_certificate_key /var/lib/acme/live/domain.ca/privkey;

        ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
        ssl_prefer_server_ciphers on;
        ssl_ciphers "EECDH+AESGCM:EDH+AESGCM:AES256+EECDH:AES256+EDH";
        ssl_ecdh_curve secp384r1;
        ssl_session_cache shared:SSL:10m;
        ssl_session_tickets off;
        ssl_stapling on;
        ssl_stapling_verify on;
        resolver 8.8.8.8 8.8.4.4 valid=300s;
        resolver_timeout 5s;
        add_header Strict-Transport-Security "max-age=63072000; includeSubdomains";
        add_header X-Frame-Options DENY;
        add_header X-Content-Type-Options nosniff;

        location / {
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;

                proxy_pass http://localhost:8080;
                proxy_read_timeout 90;

                proxy_redirect http://localhost:8080 https://domain.ca;
        }
}
```

Verify nginx: `sudo nginx -t`
Restart nginx: `sudo systemctl restart nginx`
