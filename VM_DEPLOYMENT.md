# راهنمای استقرار پروژه روی VM

این راهنما توضیح می‌دهد چگونه پروژه Sarbazi را روی یک VM جدید (Ubuntu Server) مستقر کنید و آن را به یک سرور ویندوزی منتقل نمایید.

---

## پیش‌نیازها

- VMware Workstation Pro 17 روی سیستم مبدا (Linux)
- ISO اوبونتو سرور: `ubuntu-24.04-live-server-amd64.iso`
- Docker نصب‌شده روی سیستم مبدا

---

## مرحله ۱ — ساخت VM در VMware

1. در VMware: **File → New Virtual Machine → Typical**
2. ISO فایل اوبونتو سرور را انتخاب کنید
3. سیستم‌عامل: **Linux → Ubuntu 64-bit**
4. تنظیمات پیشنهادی:
   - RAM: **4096 MB**
   - CPU: **2 cores**
   - Disk: **20 GB** (Store as single file)

---

## مرحله ۲ — نصب Ubuntu Server

در حین نصب:

- نوع نصب: **Ubuntu Server (minimized)**
- شبکه: اگر وصل نشد، **Continue without network** بزنید
- Storage: پیش‌فرض (**Use entire disk + LVM**)
- رمزگذاری: **غیرفعال**
- **OpenSSH server** را فعال کنید
- username و password را یادداشت کنید

---

## مرحله ۳ — نصب Docker روی VM

پس از لاگین به VM:

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker $USER
```

IP سرور را یادداشت کنید:

```bash
ip a | grep "inet " | grep -v 127
```

---

## مرحله ۴ — Build کردن Docker Images (روی سیستم مبدا)

> **نکته:** برای جلوگیری از build طولانی Maven، از `Dockerfile.quick` استفاده کنید که از jar آماده استفاده می‌کند.

### اگر کد Java تغییر کرده:

```bash
cd /path/to/Sarbazi

# اول jar بساز
mvn package -DskipTests

# سپس image سریع بساز
docker build -f Dockerfile.quick -t sarbazi-app .
docker compose build frontend
```

### اگر کد تغییر نکرده (فقط می‌خواهی export کنی):

```bash
cd /path/to/Sarbazi

# Build فقط frontend (در صورت تغییر)
docker compose build frontend
```

### Export images:

```bash
# Export app و frontend
docker save sarbazi-app sarbazi-frontend | gzip > sarbazi-images.tar.gz

# Export PostgreSQL image
docker pull postgres:16-alpine
docker save postgres:16-alpine | gzip > postgres.tar.gz
```

> **توجه:** فایل `.dockerignore` باید خط `target/` را comment داشته باشد تا `Dockerfile.quick` کار کند:
> ```
> # target/
> ```

---

## مرحله ۵ — Export دیتابیس (روی سیستم مبدا)

```bash
# اگر container دیتابیس خاموش است، اول روشن کنید
docker start library_db
sleep 3

# Dump گرفتن
docker exec library_db pg_dump -U libraryuser library_db > sarbazi_db.sql
```

---

## مرحله ۶ — انتقال فایل‌ها به VM

```bash
VM_IP=192.168.149.128   # IP سرور VM خود را جایگزین کنید
VM_USER=library          # username VM خود را جایگزین کنید

scp sarbazi-images.tar.gz postgres.tar.gz sarbazi_db.sql ${VM_USER}@${VM_IP}:~/
```

---

## مرحله ۷ — راه‌اندازی روی VM

از طریق SSH به VM وصل شوید:

```bash
ssh library@192.168.149.128
```

### Load کردن images:

```bash
docker load < sarbazi-images.tar.gz
docker load < postgres.tar.gz
```

### ساخت docker-compose.yml:

```bash
cat > docker-compose.yml << 'EOF'
services:
  postgres:
    image: postgres:16-alpine
    container_name: library_db
    environment:
      POSTGRES_DB: library_db
      POSTGRES_USER: libraryuser
      POSTGRES_PASSWORD: librarypass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U libraryuser -d library_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    image: sarbazi-app
    container_name: library_app
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: library_db
      DB_USER: libraryuser
      DB_PASSWORD: librarypass
      JWT_SECRET: local-dev-secret-change-this-in-production-now
      STORAGE_PATH: /data/library-files
    ports:
      - "8080:8080"
    volumes:
      - library_files:/data/library-files
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  frontend:
    image: sarbazi-frontend
    container_name: library_frontend
    ports:
      - "3000:80"
    depends_on:
      - app
    restart: unless-stopped

volumes:
  postgres_data:
  library_files:
EOF
```

### بالا آوردن سرویس‌ها:

```bash
sudo docker compose up -d
```

### Import دیتابیس:

```bash
# پاک کردن schema خالی که اپ ساخته
sudo docker exec -i library_db psql -U libraryuser -c \
  "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" library_db

# Import دیتا
sudo docker exec -i library_db psql -U libraryuser library_db < sarbazi_db.sql
```

### تایید راه‌اندازی:

```bash
sudo docker compose ps
sudo docker exec -i library_db psql -U libraryuser library_db -c "SELECT COUNT(*) FROM users;"
```

---

## مرحله ۸ — دسترسی به پروژه

| سرویس    | آدرس                          |
|----------|-------------------------------|
| Frontend | http://VM_IP:3000             |
| Backend  | http://VM_IP:8080             |
| Database | VM_IP:5432                    |

---

## مرحله ۹ — Export VM برای انتقال به سرور ویندوز

روی سیستم مبدا در VMware:

1. **File → Export to OVF**
2. فرمت **OVA** را انتخاب کنید
3. فایل `.ova` را به سرور ویندوز منتقل کنید

### Import روی سرور ویندوز:

1. VMware Workstation یا VMware Player را نصب کنید
2. **File → Open** و فایل `.ova` را انتخاب کنید
3. VM را روشن کنید — پروژه بدون نیاز به تنظیم اضافه بالا می‌آید

---

## نکات مهم

- پس از انتقال VM، IP ممکن است تغییر کند — با `ip a` چک کنید
- برای production حتماً `JWT_SECRET` را تغییر دهید
- برای update پروژه، فقط کافی است image جدید build، export، و load کنید
