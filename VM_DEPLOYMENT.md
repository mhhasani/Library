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
- رمزگذاری دیسک: **فعال (LUKS)** — داده‌ها و رویدادنگاری روی دیسک رمزنگاری‌شده ذخیره شوند
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

## مرحله ۴ — ساخت بسته‌ی آفلاین (روی سیستم مبدا، دارای اینترنت)

```bash
cd /path/to/Sarbazi
./scripts/export-bundle.sh          # همه‌ی ایمیج‌ها: app، frontend، keycloak، postgres
tar -czf library-bundle.tar.gz bundle/
sha256sum library-bundle.tar.gz     # این مقدار را جداگانه یادداشت کنید
```

بسته شامل `SHA256SUMS` است و روی سرور پیش از نصب بررسی می‌شود.

---

## مرحله ۵ — Export دیتابیس نسخه‌ی قبلی (فقط در صورت ارتقا)

```bash
docker exec library_db pg_dump --no-owner --no-privileges -U <DB_USER قبلی> library_db > sarbazi_db.sql
```

---

## مرحله ۶ — انتقال فایل‌ها به VM

```bash
VM_IP=192.168.149.128   # IP سرور VM خود را جایگزین کنید
VM_USER=library          # username VM خود را جایگزین کنید

scp library-bundle.tar.gz sarbazi_db.sql ${VM_USER}@${VM_IP}:~/
```

---

## مرحله ۷ — نصب روی VM (بدون اینترنت)

```bash
ssh library@192.168.149.128
sha256sum library-bundle.tar.gz            # با مقدار مرحله‌ی ۴ مقایسه کنید
tar -xzf library-bundle.tar.gz && cd bundle
./run.sh
```

`run.sh` صحت همه‌ی فایل‌ها را بررسی می‌کند، ایمیج‌ها را load می‌کند، فایل `.env` را با رمزهای
تصادفی می‌سازد (نام میزبان/IP سرور را بپرسد، همان را وارد کنید) و سرویس‌ها را اجرا می‌کند.
هیچ رمز یا حساب پیش‌فرضی وجود ندارد.

### بازگرداندن داده‌های نسخه‌ی قبلی (فقط در صورت ارتقا)

```bash
docker compose stop app
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"'
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < ~/sarbazi_db.sql
./db-upgrade.sh                            # دسترسی‌های نقش کم‌دسترسی اپ
docker compose up -d
```

برنامه پس از راه‌اندازی، کاربران قبلی را خودکار به Keycloak منتقل می‌کند؛ هر کاربر با رمز
قبلی وارد می‌شود و باید رمز جدید (مطابق خط‌مشی) و برنامه‌ی Authenticator را ثبت کند.

---

## مرحله ۸ — دسترسی به پروژه

| سرویس | آدرس |
|---|---|
| سامانه (رابط کاربری، API و ورود) | `https://VM_IP:3000` (نشانی دقیق: `APP_PUBLIC_URL` در `.env`) |
| کنسول مدیریت Keycloak | فقط از خود سرور: `ssh -L 8180:127.0.0.1:8180` سپس `http://127.0.0.1:8180/auth/admin` |
| پایگاه داده | از بیرون در دسترس نیست |

جزئیات نخستین ورود، حذف حساب موقت مدیریت Keycloak و نصب گواهی سازمانی در `DEPLOYMENT.md` آمده است.

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

- پس از انتقال VM، اگر IP یا نام میزبان تغییر کند، `APP_PUBLIC_URL` و `TLS_COMMON_NAME` را در
  `.env` اصلاح کنید و نشانی‌های client در Keycloak (Redirect URI) را نیز به‌روز کنید
- برای به‌روزرسانی، بسته‌ی جدید را بسازید و با `./run.sh` نصب کنید (فایل `.env` موجود حفظ می‌شود)
