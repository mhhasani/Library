# گزارش کار و راهنمای ادامه در سشن بعدی

> این فایل برای این است که در یک سشن جدید، بدون نیاز به گفت‌وگوی قبلی، کار ادامه پیدا کند.
> در سشن جدید کافی است بنویسید: «فایل `SESSION_HANDOFF.md` را بخوان و از همان‌جا ادامه بده».

- **مخزن:** `mhhasani/library`
- **شاخه‌ی کار:** `claude/session-tokens-ehaprw` (هنوز PR ساخته نشده است)
- **آخرین کامیت پیش از این گزارش:** `b2cbac3`
- **تاریخ:** مهر ۱۴۰۵ (اکتبر ۲۰۲۶)

---

## ۱. هدف پروژه و خواسته‌ها

سامانه‌ی کتابخانه (Spring Boot + React) باید **همه‌ی بندهای چک‌لیست امنیتی** (فایل xlsx با ۱۱ تب)
را در سطح **«خیلی محرمانه»** پاس کند تا تأییدیه‌ی امنیتی بگیرد.

**الزامات و تصمیم‌های کاربر:**
- منطق اصلی برنامه دست نخورد و چیزی خراب نشود.
- **Keycloak داخلی** اضافه شود (built-in). کانفیگ Keycloak سازمانی ممکن است بعداً برسد، پس همه‌چیز
  generic و قابل تنظیم با env باشد.
- **MFA قابل تنظیم** باشد و پیش‌فرض برای همه اجباری.
- **همه‌چیز داکرایز** باشد تا حد ممکن.
- سامانه روی **سرور بدون اینترنت** اجرا می‌شود (نصب با بسته‌ی آفلاین).
- کد تمیز و درست، از ساده به سخت.
- قواعد git: فقط روی شاخه‌ی بالا push شود، PR بدون درخواست ساخته نشود، و هیچ شناسه‌ی مدلی
  در مخزن نیاید.

## ۲. وضعیت فعلی: تمام‌شده ✅

همه‌ی ۱۱ تب پیاده‌سازی شده‌اند. گزارش ردیف‌به‌ردیف در **[SECURITY_COMPLIANCE.md](SECURITY_COMPLIANCE.md)** است.

| آزمون | نتیجه |
|---|---|
| `mvn test` (بک‌اند) | ۷۲۰ تست، همه پاس |
| `mvn -f keycloak/extensions test` | همه پاس |
| `cd frontend && npm run build` | موفق |
| تست سرتاسری با مرورگر (Playwright) روی استک محلی واقعی | موفق (جزئیات در بخش ۶) |

`docker compose` در محیط سشن قابل اجرا نبود و فقط پیکربندی آن بررسی شد (`docker compose config`).
**اولین کار پیشنهادی در سشن بعد:** اجرای کامل روی یک سرور با Docker (بخش ۸).

## ۳. کامیت‌ها (به ترتیب)

| کامیت | محتوا |
|---|---|
| `2aa1134` | سخت‌سازی پایه: حذف رمزهای پیش‌فرض، لاگ متمرکز (logback، شش سطح)، مدیریت خطا بدون افشای جزئیات، سرآیندهای امنیتی، بررسی magic bytes فایل‌ها |
| `004e8f4` | استقرار: nginx فقط با TLS، شبکه‌های `edge` و `data` (internal)، نقش DML-only پایگاه داده، نصاب `generate-env.sh`، بسته‌ی آفلاین با SHA256SUMS و امضای GPG اختیاری |
| `3474371` | رویدادنگاری امنیتی: زنجیره‌ی هش SHA-256، تریگر فقط-افزودنی، گزارش و خروجی CSV برچسب‌خورده |
| `9486e03` | طبقه‌بندی: سطح طبقه‌بندی برای کتاب، clearance برای کاربر، `ClassificationGuard`، مهر PDF، برچسب چاپ |
| `ddd6264` | اعتبارسنجی ورودی: `@SafeText`، پاک‌سازی Jackson، `RequestParameterGuardFilter` |
| `bfe4eaf` | افزونه‌های Keycloak: کپچای آفلاین، MFA شرطی، تغییر رمز سخت‌گیرانه، hash provider برای bcrypt، تم فارسی، realm |
| `3f27f68` | ورود OIDC (الگوی BFF) و نشست‌های سخت‌شده در پایگاه داده؛ حذف JWT |
| `a06a731` | فرانت‌اند روی نشست Keycloak و رفع باگ‌های یافته‌شده در تست سرتاسری |
| `3e112ec` | داکرایز کردن Keycloak و اتصال آن به compose |
| `b2cbac3` | پشتیبان‌گیری و بازیابی رمزنگاری‌شده، گزارش انطباق، به‌روزرسانی همه‌ی مستندات |

## ۴. معماری

```
مرورگر ──HTTPS──► nginx (frontend، تنها پورت منتشرشده)
                    ├── /        → React SPA
                    ├── /api/*   → app (Spring Boot)  ──┐
                    └── /auth/*  → keycloak            ──┴──► postgres (شبکه‌ی internal)
                                                              پایگاه‌های جدا: library_db و keycloak
کنسول مدیریت Keycloak: فقط 127.0.0.1:8180 روی خود سرور (از بیرون /auth/admin و realm master → 404)
```

### ورود و نشست
- **الگوی BFF:** اپ کلاینت محرمانه‌ی OIDC است (Authorization Code + PKCE) و توکن‌ها هرگز به
  مرورگر نمی‌رسند.
- **دو نشانی issuer:** `ClientRegistration` صریح (`OidcClientConfig`) از issuer عمومی برای
  مرورگر و آدرس داخلی (`OIDC_INTERNAL_ISSUER_URI`) برای backchannel استفاده می‌کند.
- **نشست:** Spring Session JDBC؛ جدول‌ها را Liquibase می‌سازد و ستون‌ها از نوع BYTEA هستند.
- **کوکی `LIBSESSION`:**
  - رمزنگاری AES-256-GCM (`EncryptingCookieSerializer`)
  - `HttpOnly`، `Secure` و `SameSite=Strict`، بدون `Max-Age`
  - فقط cookie tracking
- **`SessionSecurityFilter`** در هر درخواست:
  - نشست را به IP و User-Agent متصل نگه می‌دارد.
  - idle timeout را از تنظیمات اعمال می‌کند. درخواست‌هایی با سرآیند `X-Background-Request`
    فعالیت حساب نمی‌شوند.
  - نقش و وضعیت کاربر را از پایگاه داده دوباره می‌خواند.
  - تا اطلاعیه‌ی امنیتی تأیید نشود، مسیرهای `/v1/**` (به‌جز `/v1/auth/**`) پاسخ
    `NOTICE_REQUIRED` می‌دهند.
- **تک‌نشستی:** هنگام ورود، نشست‌های دیگر کاربر حذف می‌شوند، به‌جز
  `request.getRequestedSessionId()`.
- **احراز هویت مجدد:** `@RequiresRecentAuthentication` پاسخ `REAUTH_REQUIRED` می‌دهد، کاربر با
  `prompt=login` و `max_age=0` دوباره وارد می‌شود و به همان صفحه برمی‌گردد.
- **CSRF:** کوکی `XSRF-TOKEN` همراه با سرآیند، فقط برای درخواست‌های نوشتنی با کوکی.
- **خروج:** `POST /v1/auth/logout` نشست اپ و نشست Keycloak را با هم پایان می‌دهد.

### افزونه‌های Keycloak (`keycloak/extensions`)
- **`library-captcha-password-form`:** کپچای تصویری بدون نیاز به فونت یا اینترنت (شناسه‌ی
  authenticator حداکثر ۳۶ نویسه).
- **MFA شرطی:** با attribute `libraryMfaRequired` در realm کنترل می‌شود.
- **`StrictUpdatePassword`:** رمز فعلی لازم است و رمز جدید باید دست‌کم ۴ نویسه‌ی تازه داشته باشد.
  متد `create()` باید override بماند؛ نسخه‌ی پایه نمونه‌ی built-in برمی‌گرداند.
- **provider `bcrypt`:** کاربران قدیمی با رمز قبلی وارد می‌شوند و رمزشان به الگوریتم جدید ارتقا
  می‌یابد. کتابخانه‌ی jBCrypt داخل افزونه shade شده است.
- **تم `library`:** تم فارسی با نمایشگر قدرت رمز.
- **realm (`keycloak/realm/library-realm.json`):**
  - خط‌مشی رمز، شامل regex ضد دنباله
  - brute-force protection
  - نگهداری رویدادها به مدت یک سال
  - کلاینت با PKCE

### کلاس‌های مهم اپ
- `config/`: `SecurityConfig`، `OidcClientConfig`، `SessionConfig`، `InputSanitizationConfig`، `RuntimeHygiene`
- `security/`: `AppOidcUserService`، `UserProvisioningService`، `LibraryAuthorizationRequestResolver`،
  `OidcLoginSuccessHandler`، `SessionSecurityFilter`، `OidcLogoutHandler`، `AuthSessionService`، `ClassificationGuard`
- `keycloak/`: `KeycloakAdminClient`، `KeycloakRealmSync` (تنظیمات → realm)، `UserMigrationService`، `KeycloakStartupSync`
- `settings/SecuritySettingsService` (جدول `security_settings` با قیدهای CHECK)
- `audit/`، `labeling/`، `validation/`، `web/`
- Liquibase: changelogهای ۲۷ تا ۲۹ برای فیلدهای ممیزی، طبقه‌بندی، فیلدهای SSO، جدول‌های نشست و تنظیمات امنیتی

### فرانت‌اند
- **`services/api.jsx`:** نشست کوکی و interceptor برای پاسخ‌های `REAUTH_REQUIRED`، `NOTICE_REQUIRED` و پایان نشست.
- **`context/AuthContext.jsx`:** نشست سرور و قفل در صورت عدم فعالیت.
- **کامپوننت‌ها:** `SecurityNotice` و `SecurityNoticeGate`، `PrintLabel`، `ClassificationBadge`، `TemporaryPasswordModal`.
- **صفحه‌های جدید:** `SystemAuditLogsPage`، `SystemSecuritySettingsPage`، و تب امنیت حساب در پروفایل.
- **nginx (`frontend/nginx/`):**
  - TLS 1.2 و 1.3
  - سرآیندهای امنیتی و HSTS
  - `limit_req`
  - بازنویسی `X-Forwarded-For` و ارسال `X-Forwarded-Port`
  - ساخت گواهی خودامضا در صورت نبود گواهی

### استقرار
- **فایل‌های compose:** `docker-compose.yml` (build) و `docker-compose.release.yml` (ایمیج‌های آماده برای حالت آفلاین).
- **`deploy/postgres/`:** نقش اپ و پایگاه Keycloak؛ هم در init و هم در `db-upgrade.sh` استفاده می‌شود.
- **`scripts/`:**
  - `generate-env.sh`: ساخت `.env`
  - `db-upgrade.sh`: ارتقای volume موجود
  - `backup.sh` و `restore.sh`: پشتیبان و بازیابی
  - `export-bundle.sh`: ساخت بسته‌ی آفلاین
  - `import-and-run.sh`: در بسته با نام `run.sh` قرار می‌گیرد.
- **جای اسکریپت‌ها:** همه هم از `scripts/` در مخزن و هم از ریشه‌ی بسته‌ی آفلاین کار می‌کنند.
  ریشه‌ی پروژه را با وجود فایل `docker-compose.yml` تشخیص می‌دهند.
- **`mvn -Psecurity-scan verify`:** اجرای OWASP Dependency-Check. اگر آسیب‌پذیری با CVSS ۷ یا بیشتر پیدا شود، build شکست می‌خورد.

## ۵. مستندات موجود در مخزن

| فایل | محتوا |
|---|---|
| `SECURITY_COMPLIANCE.md` | مدل تهدید و وضعیت ردیف‌به‌ردیف ۱۱ تب (برای ارائه به ارزیاب) |
| `DEPLOYMENT.md` | نصب، نخستین ورود، حذف حساب موقت Keycloak، TLS، نصب آفلاین، ارتقا، تنظیمات، رویدادنگاری، پشتیبان‌گیری |
| `VM_DEPLOYMENT.md` | نصب روی VM با بسته‌ی آفلاین و انتقال داده |
| `README.md` | معماری، پیکربندی، ساختار پروژه (انگلیسی) |
| `TESTING.md`, `DEMO_GUIDE.md` | به‌روزشده برای ورود از راه Keycloak |

## ۶. چگونه تست سرتاسری انجام شد

استک محلی بدون Docker بالا آمد:
- **پایگاه داده:** PostgreSQL 16 سیستمی، با پایگاه‌های `library_db` و `keycloak`.
- **Keycloak:** نسخه‌ی 26.8.0 به‌صورت dist، با افزونه‌ها و `kc.sh build`، روی پورت 8180 و پورت مدیریت 9100.
- **اپ:** `java -jar target/library-management-system-1.0.0.jar` روی پورت 8080.
- **nginx:** روی 8443 با template `frontend/nginx`، بعد از جایگزینی متغیرها.
- **مرورگر:** Playwright و Chromium (`/opt/pw-browsers`)، با یک اسکریپت Python برای ساخت کد TOTP.

موارد تأییدشده:
- **ورود:** کپچای اشتباه رد شد و ورود کاربر منتقل‌شده با رمز bcrypt کار کرد.
- **ثبت MFA:** کار کرد.
- **قوانین رمز:** این موارد رد شدند:
  - کمتر از ۴ نویسه‌ی جدید
  - رمز فعلی اشتباه
  - رمز دنباله‌دار
  - رمز تکراری
- **اطلاعیه‌ی امنیتی:** آخرین ورود و تعداد تلاش‌های ناموفق را نشان داد.
- **تنظیمات:** تغییرشان به Keycloak همگام شد.
- **نشست:**
  - احراز هویت مجدد کار کرد و کاربر به همان صفحه برگشت.
  - تک‌نشستی اعمال شد.
  - خروج هر دو نشست را بست.
- **قفل حساب:** بعد از ۳ تلاش ناموفق فعال شد و پیام خطا یکسان ماند.
- **پشتیبان و بازیابی:** `pg_restore --clean` روی هر دو پایگاه داده‌ی واقعی موفق بود. فایل
  دست‌کاری‌شده و عبارت عبور اشتباه رد شدند.

> فایل‌های این استک محلی در scratchpad سشن قبلی بودند و در سشن جدید وجود ندارند.
> در سشن جدید ساده‌تر است مستقیماً `docker compose` روی سرور تست شود.

## ۷. باگ‌ها و نکته‌های مهمی که حل شدند (برای جلوگیری از تکرار)
- **پشت nginx خطای CORS 403 می‌آمد:** پورت در درخواست forward نمی‌شد. با ارسال
  `X-Forwarded-Port` و `server.tomcat.remoteip.port-header` حل شد.
- **ثبت ماژول Jackson:** باید به شکل bean از نوع `Module` باشد. استفاده از `modulesToInstall`
  ماژول‌های Boot را حذف می‌کند.
- **نشست JDBC:** کلاس `User` باید `Serializable` باشد. در Liquibase با H2 در حالت PostgreSQL
  باید از `BYTEA` استفاده شود، نه `BLOB`.
- **NPE در resolver:** `Map.of().get(null)` خطای NPE می‌دهد؛ در resolver گارد اضافه شد.
- **ورود دوباره در همان نشست:** نشست فعلی خودش را حذف می‌کرد. حالا `endOtherSessions` نشست فعلی را مستثنا می‌کند.
- **account action مثل ورود جدید رفتار می‌کرد:** حالا `REAUTH_PENDING` ثبت می‌شود و با همان کاربر تطبیق داده می‌شود.
- **ارتقای دسترسی:** صدور رمز موقت فقط در اختیار `SUPER_ADMIN` است.
- **ترتیب فراخوانی در فرانت‌اند:** `PrintLabel` و صفحه‌ها باید داخل `SecurityNoticeGate` باشند،
  وگرنه پیش از تأیید اطلاعیه API را صدا می‌زنند.
- **npm audit:** دستور `npm audit fix --omit=dev` وابستگی‌های dev را حذف کرد. بعد از آن `npm ci` لازم است.
- **`seed.sql`:** هش رمز با رمز ادعاشده نمی‌خواند (باگ از قبل). اصلاح شد. این فایل فقط برای نمایش است.

## ۸. کارهای باقی‌مانده و پیشنهاد برای سشن بعد

### فنی (قابل انجام در کد)
1. **اجرای کامل `docker compose up -d --build` روی سرور واقعی:**
   - این سناریوها بررسی شوند:
     - ورود اول با `BOOTSTRAP_SUPER_ADMIN_EMAIL`
     - ثبت MFA
     - پشتیبان‌گیری و بازیابی
   - ساخت بسته‌ی آفلاین (`export-bundle.sh`) و نصب آن روی یک ماشین بدون اینترنت آزمایش شود.
2. **react-router:** یک هشدار امنیتی با شدت متوسط دارد (SSR و backslash-redirect) که در این
   برنامه کاربرد ندارد. رفع آن ارتقای نسخه‌ی اصلی لازم دارد.
3. **رمز موقت مدیر:** به‌شکل `String` از اپ عبور می‌کند. می‌توان آن را به `char[]` تبدیل کرد و بعد
   از استفاده پاک کرد.
4. **برچسب طبقه‌بندی:** فعلاً در سطح رکورد است. اگر ارزیاب برچسب در سطح فیلد بخواهد، باید اضافه شود.
5. **ساخت PR:** فقط با درخواست کاربر.

### وابسته به سازمان
1. **Keycloak سازمانی:** پس از دریافت کانفیگ و اسکیما، `OIDC_ISSUER_URI`،
   `OIDC_INTERNAL_ISSUER_URI` و `OIDC_CLIENT_SECRET` تنظیم شود و تنظیمات
   `library-realm.json` در realm سازمان اعمال شود. موارد اعمال‌شدنی:
   - کلاینت با PKCE و `redirect_uri` دقیق
   - خط‌مشی رمز و brute-force
   - flow کپچا و MFA شرطی
   - تم
   - attribute `libraryMfaRequired`
   - نگهداری رویدادها
   - **دسترسی ادمین کلاینت:** کلاینت اپ برای همگام‌سازی تنظیمات به نقش‌های `realm-management`
     نیاز دارد. اگر سازمان این دسترسی را ندهد، باید `KeycloakRealmSync` را غیرفعال یا اختیاری کرد.
2. **PKI و CA نیروهای مسلح:** برای امضای دیجیتال و ورود با گواهی (X.509/mTLS در nginx و Keycloak).
3. **گواهی TLS سازمانی:** جایگزین گواهی خودامضا در volume `tls_certs`.
4. **تأیید الگوریتم‌ها:** الگوریتم‌های فعلی AES-256-GCM، SHA-256/512، PBKDF2 و TLS 1.2/1.3 هستند.
   تأیید آن‌ها از طرف مرجع مربوط لازم است.
5. **مبهم‌سازی bytecode بک‌اند:** با ابزاری که سازمان تأیید کند.
6. **زیرساخت:** جداسازی فیزیکی سرورها (در صورت الزام) و رمزنگاری دیسک.
7. **حذف حساب موقت مدیریت Keycloak** بعد از نصب.

## ۹. فرمان‌های سریع

```bash
git checkout claude/session-tokens-ehaprw && git pull
mvn test                                  # بک‌اند
mvn -f keycloak/extensions test           # افزونه‌های Keycloak
cd frontend && npm ci && npm run build    # فرانت‌اند

./scripts/generate-env.sh && docker compose up -d --build     # استک کامل
./scripts/backup.sh /mnt/backup                               # پشتیبان رمزنگاری‌شده
./scripts/export-bundle.sh                                    # بسته‌ی آفلاین
```

## ۱۰. متن پیشنهادی برای شروع سشن بعد

```
روی مخزن mhhasani/library و شاخه‌ی claude/session-tokens-ehaprw کار می‌کنیم.
اول فایل SESSION_HANDOFF.md و SECURITY_COMPLIANCE.md را بخوان.
قواعد: منطق اصلی دست نخورد، همه‌چیز داکرایز و آفلاین بماند، روی همین شاخه push کن، بدون درخواست PR نساز.
کار بعدی: <اینجا کار مورد نظر را بنویسید، مثلاً اتصال به Keycloak سازمانی با این کانفیگ: ...>
```
