-- =============================================================
-- DEMO / DEVELOPMENT DATA ONLY — NEVER RUN ON A PRODUCTION DATABASE.
-- It creates demo accounts that share one well-known password; a production
-- installation must contain no default accounts or default passwords.
-- =============================================================
-- Sarbazi Library System - Comprehensive Seed Data
-- Run: docker exec -i library_db su -s /bin/sh postgres -c "psql -U libraryuser -d library_db" < seed.sql
-- Password for all new users: User1234!
-- =============================================================

BEGIN;

-- =============================================================
-- 1. UPDATE EXISTING DATA
-- =============================================================

UPDATE books SET
    title = 'غربزدگی',
    description = 'کتابی درباره تأثیر فرهنگ و تمدن غرب بر جوامع شرقی، نوشته جلال آل‌احمد',
    publication_year = 1341,
    publisher = 'انتشارات رواق'
WHERE id = 1;

UPDATE libraries SET
    description = 'کتابخانه اصلی و مرکزی سامانه با مجموعه‌ای گسترده از آثار ادبی و علمی',
    default_borrow_duration_days = 21
WHERE id = 1;

UPDATE libraries SET
    description = 'کتابخانه تخصصی حوزه فناوری اطلاعات و علوم کامپیوتر',
    default_borrow_duration_days = 14
WHERE id = 2;

-- =============================================================
-- 2. NEW USERS (password: User1234!)
-- BCrypt hash of "User1234!":
-- $2b$10$FrTB.0FuOe/Gfz0x0igPPOwzJ/B0k5bbwF1uiAeu.2NqTX4nVhawy
-- =============================================================

INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number, system_role, account_status, created_at, updated_at)
VALUES
    (4,  'ali.ahmadi@email.com',     '$2b$10$FrTB.0FuOe/Gfz0x0igPPOwzJ/B0k5bbwF1uiAeu.2NqTX4nVhawy', 'علی',   'احمدی',    '09121110001', 'USER', 'ACTIVE', NOW() - INTERVAL '60 days', NOW()),
    (5,  'sara.hosseini@email.com',  '$2b$10$FrTB.0FuOe/Gfz0x0igPPOwzJ/B0k5bbwF1uiAeu.2NqTX4nVhawy', 'سارا',  'حسینی',    '09121110002', 'USER', 'ACTIVE', NOW() - INTERVAL '55 days', NOW()),
    (6,  'mohsen.karimi@email.com',  '$2b$10$FrTB.0FuOe/Gfz0x0igPPOwzJ/B0k5bbwF1uiAeu.2NqTX4nVhawy', 'محسن',  'کریمی',    '09121110003', 'USER', 'ACTIVE', NOW() - INTERVAL '45 days', NOW()),
    (7,  'maryam.rezaei@email.com',  '$2b$10$FrTB.0FuOe/Gfz0x0igPPOwzJ/B0k5bbwF1uiAeu.2NqTX4nVhawy', 'مریم',  'رضایی',    '09121110004', 'USER', 'ACTIVE', NOW() - INTERVAL '40 days', NOW()),
    (8,  'reza.mohammadi@email.com', '$2b$10$FrTB.0FuOe/Gfz0x0igPPOwzJ/B0k5bbwF1uiAeu.2NqTX4nVhawy', 'رضا',   'محمدی',    '09121110005', 'USER', 'ACTIVE', NOW() - INTERVAL '30 days', NOW()),
    (9,  'zahra.safari@email.com',   '$2b$10$FrTB.0FuOe/Gfz0x0igPPOwzJ/B0k5bbwF1uiAeu.2NqTX4nVhawy', 'زهرا',  'صفاری',    '09121110006', 'USER', 'ACTIVE', NOW() - INTERVAL '25 days', NOW()),
    (10, 'amir.moradi@email.com',    '$2b$10$FrTB.0FuOe/Gfz0x0igPPOwzJ/B0k5bbwF1uiAeu.2NqTX4nVhawy', 'امیر',  'مرادی',    '09121110007', 'USER', 'ACTIVE', NOW() - INTERVAL '20 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- 3. NEW LIBRARIES
-- =============================================================

INSERT INTO libraries (id, name, description, owner_id, auto_membership_approval, default_borrow_duration_days, is_active, created_at, updated_at)
VALUES
    (3, 'کتابخانه دانشگاه صنعتی شریف',
        'کتابخانه دانشگاهی شامل منابع علمی، درسی و پژوهشی برای دانشجویان و اساتید',
        4, false, 14, true, NOW() - INTERVAL '50 days', NOW()),
    (4, 'کتابخانه عمومی اصفهان',
        'کتابخانه عمومی با مجموعه‌ای متنوع از کتاب‌های عمومی، ادبیات و علوم اجتماعی',
        5, true, 21, true, NOW() - INTERVAL '40 days', NOW()),
    (5, 'کتابخانه تخصصی فناوری اطلاعات',
        'منابع تخصصی حوزه امنیت، شبکه، پایگاه داده و مهندسی نرم‌افزار',
        10, false, 7, true, NOW() - INTERVAL '15 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- 4. BOOKS
-- =============================================================

-- Library 1: کتابخانه مرکزی (ادبیات کلاسیک و معاصر)
INSERT INTO books (id, library_id, title, author, publisher, publication_year, description, created_at, updated_at)
VALUES
    (2,  1, 'صد سال تنهایی',
         'گابریل گارسیا مارکز', 'نشر نیلوفر', 1967,
         'رمان جادویی-واقع‌گرایانه‌ای درباره خانواده بوئندیا در دهکده ماکوندو؛ یکی از برجسته‌ترین آثار ادبیات لاتین',
         NOW() - INTERVAL '50 days', NOW()),
    (3,  1, 'کلیدر',
         'محمود دولت‌آبادی', 'انتشارات چشمه', 1978,
         'حماسه‌ای ده‌جلدی از زندگی مردم خراسان در دهه‌های اول قرن چهاردهم شمسی',
         NOW() - INTERVAL '49 days', NOW()),
    (4,  1, 'سووشون',
         'سیمین دانشور', 'انتشارات خوارزمی', 1969,
         'اولین رمان فارسی نوشته یک زن؛ روایتی از زندگی در شیراز دوران جنگ جهانی دوم',
         NOW() - INTERVAL '48 days', NOW()),
    (5,  1, 'بوف کور',
         'صادق هدایت', 'انتشارات امیرکبیر', 1936,
         'اثر برجسته مدرنیسم ایرانی؛ روایتی سورئالیستی و ذهنی از یأس و تنهایی',
         NOW() - INTERVAL '47 days', NOW()),
    (6,  1, 'شازده کوچولو',
         'آنتوان دو سنت‌اگزوپری', 'نشر قدیانی', 1943,
         'داستانی کلاسیک درباره شازده‌ای از سیاره‌ای دیگر که به زمین می‌آید؛ اثری فلسفی برای همه سنین',
         NOW() - INTERVAL '46 days', NOW()),
    (7,  1, 'جنایت و مکافات',
         'فئودور داستایوفسکی', 'نشر نیلوفر', 1866,
         'رمان کلاسیک روسی درباره دانشجویی که مرتکب قتل می‌شود و کشمکش روحی پس از آن',
         NOW() - INTERVAL '45 days', NOW()),
    (8,  1, 'بینوایان',
         'ویکتور هوگو', 'نشر علمی و فرهنگی', 1862,
         'حماسه بزرگ انسانی درباره ژان والژان و مبارزه انسان برای عدالت و رستگاری',
         NOW() - INTERVAL '44 days', NOW()),
    (9,  1, 'مرشد و مارگریتا',
         'میخائیل بولگاکف', 'نشر ماهی', 1967,
         'رمانی کلاسیک سوررئالیستی درباره ابلیس که به مسکو می‌آید؛ نقدی بر جامعه شوروی',
         NOW() - INTERVAL '43 days', NOW()),
    (10, 1, 'هزار خورشید تابان',
         'خالد حسینی', 'نشر ثالث', 2007,
         'روایت دلنشین دو زن افغان در طول سه دهه جنگ، اشغال و آزادی',
         NOW() - INTERVAL '42 days', NOW()),

-- Library 2: کتابخانه فاوا (مهندسی و فناوری)
    (11, 2, 'کد تمیز',
         'رابرت سی. مارتین', 'نشر آفرینگان', 2008,
         'راهنمای عملی برای نوشتن کدهای خوانا، قابل نگهداری و حرفه‌ای',
         NOW() - INTERVAL '40 days', NOW()),
    (12, 2, 'طراحی الگو',
         'گروه چهار نفره (GoF)', 'نشر آفرینگان', 1994,
         'کتاب بنیادین درباره ۲۳ الگوی طراحی نرم‌افزار شیءگرا',
         NOW() - INTERVAL '39 days', NOW()),
    (13, 2, 'برنامه‌نویس عملگرا',
         'اندرو هانت و دیوید توماس', 'نشر آفرینگان', 1999,
         'راهنمای جامع برای بهتر شدن به عنوان یک مهندس نرم‌افزار حرفه‌ای',
         NOW() - INTERVAL '38 days', NOW()),
    (14, 2, 'بازآرایی کد',
         'مارتین فاولر', 'نشر آفرینگان', 1999,
         'تکنیک‌های بهبود طراحی کد موجود بدون تغییر رفتار آن',
         NOW() - INTERVAL '37 days', NOW()),
    (15, 2, 'مبانی هوش مصنوعی',
         'استوارت راسل و پیتر نورویگ', 'انتشارات دانشگاهی', 2020,
         'جامع‌ترین مرجع آموزش هوش مصنوعی برای دانشجویان و متخصصان',
         NOW() - INTERVAL '36 days', NOW()),
    (16, 2, 'یادگیری ماشین با پایتون',
         'سباستیان راشکا', 'نشر پندار پارس', 2019,
         'آموزش عملی یادگیری ماشین و یادگیری عمیق با کتابخانه‌های پایتون',
         NOW() - INTERVAL '35 days', NOW()),
    (17, 2, 'معماری نرم‌افزار',
         'لن باس، پل کلمنتس', 'نشر آفرینگان', 2021,
         'اصول و روش‌های معماری نرم‌افزارهای مدرن و مقیاس‌پذیر',
         NOW() - INTERVAL '34 days', NOW()),
    (18, 2, 'سیستم‌های توزیع‌شده',
         'اندرو تاننبام', 'نشر دانشگاهی', 2016,
         'مبانی و معماری سیستم‌های توزیع‌شده، از اصول تا پیاده‌سازی',
         NOW() - INTERVAL '33 days', NOW()),

-- Library 3: کتابخانه دانشگاه شریف (علوم پایه و مهندسی)
    (19, 3, 'فیزیک هالیدی - جلد اول',
         'هالیدی، رزنیک، وکر', 'انتشارات فاطمی', 2013,
         'مرجع اصلی فیزیک دانشگاهی برای مکانیک، حرارت و امواج',
         NOW() - INTERVAL '30 days', NOW()),
    (20, 3, 'ریاضی عمومی یک',
         'جورج توماس', 'انتشارات فاطمی', 2018,
         'حساب دیفرانسیل و انتگرال برای دانشجویان مهندسی و علوم پایه',
         NOW() - INTERVAL '29 days', NOW()),
    (21, 3, 'شیمی عمومی',
         'ریموند چنگ', 'انتشارات فاطمی', 2019,
         'جامع‌ترین مرجع شیمی عمومی برای دانشجویان سال اول دانشگاه',
         NOW() - INTERVAL '28 days', NOW()),
    (22, 3, 'مکانیک کوانتومی',
         'دیوید گریفیتس', 'انتشارات دانشگاهی', 2017,
         'مقدمه‌ای بر مکانیک کوانتومی برای فیزیکدانان و مهندسین',
         NOW() - INTERVAL '27 days', NOW()),
    (23, 3, 'الکترومغناطیس',
         'دیوید گریفیتس', 'انتشارات دانشگاهی', 2017,
         'مرجع اصلی الکترومغناطیس کلاسیک برای دوره کارشناسی',
         NOW() - INTERVAL '26 days', NOW()),
    (24, 3, 'آمار و احتمال مهندسی',
         'والپول، مایرز', 'انتشارات دانشگاهی', 2016,
         'روش‌های آماری و احتمالاتی برای مهندسان و دانشمندان',
         NOW() - INTERVAL '25 days', NOW()),

-- Library 4: کتابخانه عمومی اصفهان (کتاب‌های عمومی)
    (25, 4, 'تاریخ ایران باستان',
         'حسن پیرنیا', 'انتشارات اساطیر', 1990,
         'پژوهشی جامع درباره تاریخ ایران از آغاز تا پایان دوران ساسانی',
         NOW() - INTERVAL '25 days', NOW()),
    (26, 4, 'اقتصاد در یک درس',
         'هنری هزلیت', 'نشر دنیای اقتصاد', 1946,
         'مقدمه‌ای روان و قابل فهم بر اصول اقتصاد بازار آزاد',
         NOW() - INTERVAL '24 days', NOW()),
    (27, 4, 'روانشناسی مثبت‌نگر',
         'مارتین سلیگمن', 'نشر رشد', 2011,
         'مبانی علمی شادی، رفاه و رشد شخصی بر اساس روانشناسی مدرن',
         NOW() - INTERVAL '23 days', NOW()),
    (28, 4, 'هنر جنگ',
         'سان تزو', 'نشر دنیای نو', 2000,
         'کلاسیک نظامی چینی با کاربردهای مدرن در مدیریت و رهبری',
         NOW() - INTERVAL '22 days', NOW()),
    (29, 4, 'تفکر سریع و کند',
         'دانیل کانمن', 'نشر ترجمان', 2011,
         'کاوش در دو سیستم تفکر انسانی و تأثیر آن‌ها بر تصمیم‌گیری',
         NOW() - INTERVAL '21 days', NOW()),
    (30, 4, 'عادت‌های اتمی',
         'جیمز کلیر', 'نشر میلکان', 2018,
         'روشی ساده و اثبات‌شده برای ساختن عادات خوب و ترک عادات بد',
         NOW() - INTERVAL '20 days', NOW()),

-- Library 5: کتابخانه تخصصی فناوری اطلاعات
    (31, 5, 'امنیت شبکه و رمزنگاری',
         'ویلیام استالینگز', 'نشر دانشگاهی', 2020,
         'مرجع جامع امنیت شبکه، رمزنگاری و پروتکل‌های امن',
         NOW() - INTERVAL '12 days', NOW()),
    (32, 5, 'پایگاه داده پیشرفته',
         'رامز النصری و شامکانت ناواته', 'انتشارات فاطمی', 2015,
         'اصول، طراحی و پیاده‌سازی پایگاه‌های داده رابطه‌ای و NoSQL',
         NOW() - INTERVAL '11 days', NOW()),
    (33, 5, 'شبکه‌های کامپیوتری',
         'اندرو تاننبام', 'نشر دانشگاهی', 2019,
         'جامع‌ترین مرجع شبکه‌های کامپیوتری از لایه فیزیکی تا کاربرد',
         NOW() - INTERVAL '10 days', NOW()),
    (34, 5, 'سیستم عامل: مفاهیم',
         'سیلبرشاتز، گالوین و گاگنه', 'نشر دانشگاهی', 2018,
         'مرجع اصلی سیستم‌عامل برای دانشجویان کامپیوتر و مهندسی نرم‌افزار',
         NOW() - INTERVAL '9 days', NOW()),
    (35, 5, 'رمزنگاری کاربردی',
         'بروس اشنایر', 'نشر دانشگاهی', 2015,
         'پروتکل‌ها، الگوریتم‌ها و کدهای برنامه‌نویسی در رمزنگاری',
         NOW() - INTERVAL '8 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- 5. BOOK COPIES
-- =============================================================

INSERT INTO book_copies (id, book_id, library_id, copy_number, status, created_at, updated_at)
VALUES
-- Book 2 (صد سال تنهایی): 3 copies
    (22, 2, 1, 1, 'BORROWED',   NOW() - INTERVAL '50 days', NOW()),
    (23, 2, 1, 2, 'AVAILABLE',  NOW() - INTERVAL '50 days', NOW()),
    (24, 2, 1, 3, 'AVAILABLE',  NOW() - INTERVAL '50 days', NOW()),
-- Book 3 (کلیدر): 3 copies
    (25, 3, 1, 1, 'BORROWED',   NOW() - INTERVAL '49 days', NOW()),
    (26, 3, 1, 2, 'AVAILABLE',  NOW() - INTERVAL '49 days', NOW()),
    (27, 3, 1, 3, 'AVAILABLE',  NOW() - INTERVAL '49 days', NOW()),
-- Book 4 (سووشون): 3 copies
    (28, 4, 1, 1, 'AVAILABLE',  NOW() - INTERVAL '48 days', NOW()),
    (29, 4, 1, 2, 'AVAILABLE',  NOW() - INTERVAL '48 days', NOW()),
    (30, 4, 1, 3, 'AVAILABLE',  NOW() - INTERVAL '48 days', NOW()),
-- Book 5 (بوف کور): 3 copies
    (31, 5, 1, 1, 'AVAILABLE',  NOW() - INTERVAL '47 days', NOW()),
    (32, 5, 1, 2, 'AVAILABLE',  NOW() - INTERVAL '47 days', NOW()),
    (33, 5, 1, 3, 'AVAILABLE',  NOW() - INTERVAL '47 days', NOW()),
-- Book 6 (شازده کوچولو): 3 copies
    (34, 6, 1, 1, 'AVAILABLE',  NOW() - INTERVAL '46 days', NOW()),
    (35, 6, 1, 2, 'AVAILABLE',  NOW() - INTERVAL '46 days', NOW()),
    (36, 6, 1, 3, 'AVAILABLE',  NOW() - INTERVAL '46 days', NOW()),
-- Book 7 (جنایت و مکافات): 3 copies
    (37, 7, 1, 1, 'AVAILABLE',  NOW() - INTERVAL '45 days', NOW()),
    (38, 7, 1, 2, 'AVAILABLE',  NOW() - INTERVAL '45 days', NOW()),
    (39, 7, 1, 3, 'AVAILABLE',  NOW() - INTERVAL '45 days', NOW()),
-- Book 8 (بینوایان): 3 copies
    (40, 8, 1, 1, 'AVAILABLE',  NOW() - INTERVAL '44 days', NOW()),
    (41, 8, 1, 2, 'AVAILABLE',  NOW() - INTERVAL '44 days', NOW()),
    (42, 8, 1, 3, 'AVAILABLE',  NOW() - INTERVAL '44 days', NOW()),
-- Book 9 (مرشد و مارگریتا): 3 copies
    (43, 9, 1, 1, 'AVAILABLE',  NOW() - INTERVAL '43 days', NOW()),
    (44, 9, 1, 2, 'AVAILABLE',  NOW() - INTERVAL '43 days', NOW()),
    (45, 9, 1, 3, 'AVAILABLE',  NOW() - INTERVAL '43 days', NOW()),
-- Book 10 (هزار خورشید تابان): 3 copies
    (46, 10, 1, 1, 'AVAILABLE', NOW() - INTERVAL '42 days', NOW()),
    (47, 10, 1, 2, 'AVAILABLE', NOW() - INTERVAL '42 days', NOW()),
    (48, 10, 1, 3, 'AVAILABLE', NOW() - INTERVAL '42 days', NOW()),

-- Book 11 (کد تمیز): 2 copies
    (49, 11, 2, 1, 'AVAILABLE', NOW() - INTERVAL '40 days', NOW()),
    (50, 11, 2, 2, 'AVAILABLE', NOW() - INTERVAL '40 days', NOW()),
-- Book 12 (طراحی الگو): 2 copies
    (51, 12, 2, 1, 'AVAILABLE', NOW() - INTERVAL '39 days', NOW()),
    (52, 12, 2, 2, 'AVAILABLE', NOW() - INTERVAL '39 days', NOW()),
-- Book 13 (برنامه‌نویس عملگرا): 2 copies
    (53, 13, 2, 1, 'AVAILABLE', NOW() - INTERVAL '38 days', NOW()),
    (54, 13, 2, 2, 'AVAILABLE', NOW() - INTERVAL '38 days', NOW()),
-- Book 14 (بازآرایی کد): 2 copies
    (55, 14, 2, 1, 'AVAILABLE', NOW() - INTERVAL '37 days', NOW()),
    (56, 14, 2, 2, 'AVAILABLE', NOW() - INTERVAL '37 days', NOW()),
-- Book 15 (مبانی هوش مصنوعی): 2 copies
    (57, 15, 2, 1, 'BORROWED',  NOW() - INTERVAL '36 days', NOW()),
    (58, 15, 2, 2, 'AVAILABLE', NOW() - INTERVAL '36 days', NOW()),
-- Book 16 (یادگیری ماشین): 2 copies
    (59, 16, 2, 1, 'AVAILABLE', NOW() - INTERVAL '35 days', NOW()),
    (60, 16, 2, 2, 'AVAILABLE', NOW() - INTERVAL '35 days', NOW()),
-- Book 17 (معماری نرم‌افزار): 2 copies
    (61, 17, 2, 1, 'AVAILABLE', NOW() - INTERVAL '34 days', NOW()),
    (62, 17, 2, 2, 'AVAILABLE', NOW() - INTERVAL '34 days', NOW()),
-- Book 18 (سیستم‌های توزیع‌شده): 2 copies
    (63, 18, 2, 1, 'AVAILABLE', NOW() - INTERVAL '33 days', NOW()),
    (64, 18, 2, 2, 'AVAILABLE', NOW() - INTERVAL '33 days', NOW()),

-- Book 19 (فیزیک هالیدی): 2 copies
    (65, 19, 3, 1, 'BORROWED',  NOW() - INTERVAL '30 days', NOW()),
    (66, 19, 3, 2, 'AVAILABLE', NOW() - INTERVAL '30 days', NOW()),
-- Book 20 (ریاضی عمومی): 2 copies
    (67, 20, 3, 1, 'AVAILABLE', NOW() - INTERVAL '29 days', NOW()),
    (68, 20, 3, 2, 'AVAILABLE', NOW() - INTERVAL '29 days', NOW()),
-- Book 21 (شیمی عمومی): 2 copies
    (69, 21, 3, 1, 'AVAILABLE', NOW() - INTERVAL '28 days', NOW()),
    (70, 21, 3, 2, 'AVAILABLE', NOW() - INTERVAL '28 days', NOW()),
-- Book 22 (مکانیک کوانتومی): 2 copies
    (71, 22, 3, 1, 'AVAILABLE', NOW() - INTERVAL '27 days', NOW()),
    (72, 22, 3, 2, 'AVAILABLE', NOW() - INTERVAL '27 days', NOW()),
-- Book 23 (الکترومغناطیس): 2 copies
    (73, 23, 3, 1, 'AVAILABLE', NOW() - INTERVAL '26 days', NOW()),
    (74, 23, 3, 2, 'AVAILABLE', NOW() - INTERVAL '26 days', NOW()),
-- Book 24 (آمار و احتمال): 2 copies
    (75, 24, 3, 1, 'AVAILABLE', NOW() - INTERVAL '25 days', NOW()),
    (76, 24, 3, 2, 'AVAILABLE', NOW() - INTERVAL '25 days', NOW()),

-- Book 25 (تاریخ ایران باستان): 3 copies
    (77, 25, 4, 1, 'BORROWED',  NOW() - INTERVAL '25 days', NOW()),
    (78, 25, 4, 2, 'AVAILABLE', NOW() - INTERVAL '25 days', NOW()),
    (79, 25, 4, 3, 'AVAILABLE', NOW() - INTERVAL '25 days', NOW()),
-- Book 26 (اقتصاد در یک درس): 3 copies
    (80, 26, 4, 1, 'AVAILABLE', NOW() - INTERVAL '24 days', NOW()),
    (81, 26, 4, 2, 'AVAILABLE', NOW() - INTERVAL '24 days', NOW()),
    (82, 26, 4, 3, 'AVAILABLE', NOW() - INTERVAL '24 days', NOW()),
-- Book 27 (روانشناسی مثبت‌نگر): 3 copies
    (83, 27, 4, 1, 'AVAILABLE', NOW() - INTERVAL '23 days', NOW()),
    (84, 27, 4, 2, 'AVAILABLE', NOW() - INTERVAL '23 days', NOW()),
    (85, 27, 4, 3, 'AVAILABLE', NOW() - INTERVAL '23 days', NOW()),
-- Book 28 (هنر جنگ): 3 copies
    (86, 28, 4, 1, 'AVAILABLE', NOW() - INTERVAL '22 days', NOW()),
    (87, 28, 4, 2, 'AVAILABLE', NOW() - INTERVAL '22 days', NOW()),
    (88, 28, 4, 3, 'AVAILABLE', NOW() - INTERVAL '22 days', NOW()),
-- Book 29 (تفکر سریع و کند): 3 copies
    (89, 29, 4, 1, 'AVAILABLE', NOW() - INTERVAL '21 days', NOW()),
    (90, 29, 4, 2, 'AVAILABLE', NOW() - INTERVAL '21 days', NOW()),
    (91, 29, 4, 3, 'AVAILABLE', NOW() - INTERVAL '21 days', NOW()),
-- Book 30 (عادت‌های اتمی): 3 copies
    (92, 30, 4, 1, 'AVAILABLE', NOW() - INTERVAL '20 days', NOW()),
    (93, 30, 4, 2, 'AVAILABLE', NOW() - INTERVAL '20 days', NOW()),
    (94, 30, 4, 3, 'AVAILABLE', NOW() - INTERVAL '20 days', NOW()),

-- Book 31 (امنیت شبکه): 2 copies
    (95, 31, 5, 1, 'AVAILABLE', NOW() - INTERVAL '12 days', NOW()),
    (96, 31, 5, 2, 'AVAILABLE', NOW() - INTERVAL '12 days', NOW()),
-- Book 32 (پایگاه داده): 2 copies
    (97, 32, 5, 1, 'BORROWED',  NOW() - INTERVAL '11 days', NOW()),
    (98, 32, 5, 2, 'AVAILABLE', NOW() - INTERVAL '11 days', NOW()),
-- Book 33 (شبکه‌های کامپیوتری): 2 copies
    (99, 33, 5, 1, 'AVAILABLE', NOW() - INTERVAL '10 days', NOW()),
    (100, 33, 5, 2, 'AVAILABLE', NOW() - INTERVAL '10 days', NOW()),
-- Book 34 (سیستم عامل): 2 copies
    (101, 34, 5, 1, 'AVAILABLE', NOW() - INTERVAL '9 days', NOW()),
    (102, 34, 5, 2, 'AVAILABLE', NOW() - INTERVAL '9 days', NOW()),
-- Book 35 (رمزنگاری کاربردی): 2 copies
    (103, 35, 5, 1, 'AVAILABLE', NOW() - INTERVAL '8 days', NOW()),
    (104, 35, 5, 2, 'AVAILABLE', NOW() - INTERVAL '8 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- 6. LIBRARY MEMBERSHIPS
-- =============================================================

INSERT INTO library_memberships (id, user_id, library_id, role, status, approved_by, created_at, updated_at)
VALUES
-- User 4 (علی احمدی)
    (7,  4, 3, 'ADMIN',  'APPROVED', 1, NOW() - INTERVAL '50 days', NOW()),
    (8,  4, 1, 'MEMBER', 'APPROVED', 1, NOW() - INTERVAL '48 days', NOW()),
-- User 5 (سارا حسینی)
    (9,  5, 4, 'ADMIN',  'APPROVED', 1, NOW() - INTERVAL '40 days', NOW()),
    (10, 5, 1, 'MEMBER', 'APPROVED', 1, NOW() - INTERVAL '38 days', NOW()),
    (11, 5, 2, 'MEMBER', 'APPROVED', 1, NOW() - INTERVAL '37 days', NOW()),
-- User 6 (محسن کریمی)
    (12, 6, 1, 'MEMBER', 'APPROVED', 1, NOW() - INTERVAL '44 days', NOW()),
    (13, 6, 2, 'MEMBER', 'APPROVED', 1, NOW() - INTERVAL '43 days', NOW()),
    (14, 6, 3, 'MEMBER', 'APPROVED', 4, NOW() - INTERVAL '40 days', NOW()),
-- User 7 (مریم رضایی)
    (15, 7, 1, 'MEMBER', 'APPROVED', 1, NOW() - INTERVAL '38 days', NOW()),
    (16, 7, 4, 'MEMBER', 'APPROVED', 5, NOW() - INTERVAL '35 days', NOW()),
    (17, 7, 3, 'MEMBER', 'PENDING',  NULL, NOW() - INTERVAL '5 days',  NOW()),
-- User 8 (رضا محمدی)
    (18, 8, 2, 'MEMBER', 'APPROVED', 1, NOW() - INTERVAL '28 days', NOW()),
    (19, 8, 5, 'MEMBER', 'APPROVED', 10, NOW() - INTERVAL '12 days', NOW()),
    (20, 8, 1, 'MEMBER', 'PENDING',  NULL, NOW() - INTERVAL '3 days',  NOW()),
-- User 9 (زهرا صفاری)
    (21, 9, 1, 'MEMBER', 'APPROVED', 1, NOW() - INTERVAL '23 days', NOW()),
    (22, 9, 4, 'MEMBER', 'APPROVED', 5, NOW() - INTERVAL '20 days', NOW()),
    (23, 9, 5, 'MEMBER', 'PENDING',  NULL, NOW() - INTERVAL '2 days',  NOW()),
-- User 10 (امیر مرادی)
    (24, 10, 5, 'ADMIN',  'APPROVED', 1, NOW() - INTERVAL '15 days', NOW()),
    (25, 10, 2, 'MEMBER', 'APPROVED', 1, NOW() - INTERVAL '13 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- 7. BORROWS
-- =============================================================

INSERT INTO borrows (id, user_id, library_id, book_id, borrow_type, book_copy_id, status,
                     approved_by, borrow_date, due_date, return_date, created_at, updated_at)
VALUES
-- ACTIVE APPROVED borrows
    -- User 6: صد سال تنهایی (OVERDUE: due 7 days ago)
    (7,  6, 1, 2,  'PHYSICAL', 22, 'APPROVED', 1,
         NOW() - INTERVAL '14 days', NOW() - INTERVAL '7 days', NULL,
         NOW() - INTERVAL '14 days', NOW()),
    -- User 7: کلیدر
    (8,  7, 1, 3,  'PHYSICAL', 25, 'APPROVED', 1,
         NOW() - INTERVAL '5 days', NOW() + INTERVAL '9 days', NULL,
         NOW() - INTERVAL '5 days', NOW()),
    -- User 8: مبانی هوش مصنوعی
    (9,  8, 2, 15, 'PHYSICAL', 57, 'APPROVED', 1,
         NOW() - INTERVAL '3 days', NOW() + INTERVAL '11 days', NULL,
         NOW() - INTERVAL '3 days', NOW()),
    -- User 9: تاریخ ایران باستان
    (10, 9, 4, 25, 'PHYSICAL', 77, 'APPROVED', 5,
         NOW() - INTERVAL '2 days', NOW() + INTERVAL '19 days', NULL,
         NOW() - INTERVAL '2 days', NOW()),
    -- User 4: فیزیک هالیدی
    (11, 4, 3, 19, 'PHYSICAL', 65, 'APPROVED', 4,
         NOW() - INTERVAL '7 days', NOW() + INTERVAL '7 days', NULL,
         NOW() - INTERVAL '7 days', NOW()),
    -- User 10: پایگاه داده (OVERDUE: due 14 days ago)
    (12, 10, 5, 32, 'PHYSICAL', 97, 'APPROVED', 10,
         NOW() - INTERVAL '21 days', NOW() - INTERVAL '14 days', NULL,
         NOW() - INTERVAL '21 days', NOW()),

-- REQUESTED borrows (pending approval)
    -- User 5: تفکر سریع و کند
    (13, 5, 4, 29, 'PHYSICAL', NULL, 'REQUESTED', NULL,
         NULL, NULL, NULL,
         NOW() - INTERVAL '1 day', NOW()),
    -- User 6: کد تمیز (different book from borrow 7)
    (14, 6, 2, 11, 'PHYSICAL', NULL, 'REQUESTED', NULL,
         NULL, NULL, NULL,
         NOW() - INTERVAL '12 hours', NOW()),
    -- User 8: شبکه‌های کامپیوتری (different book from borrow 9)
    (15, 8, 5, 33, 'PHYSICAL', NULL, 'REQUESTED', NULL,
         NULL, NULL, NULL,
         NOW() - INTERVAL '6 hours', NOW()),

-- RETURNED borrows (historical)
    -- User 6: کلیدر (returned before current borrow of same book by user 7)
    (16, 6, 1, 3,  'PHYSICAL', 26, 'RETURNED', 1,
         NOW() - INTERVAL '25 days', NOW() - INTERVAL '11 days', NOW() - INTERVAL '12 days',
         NOW() - INTERVAL '25 days', NOW() - INTERVAL '12 days'),
    -- User 7: بوف کور (returned)
    (17, 7, 1, 5,  'PHYSICAL', 31, 'RETURNED', 1,
         NOW() - INTERVAL '34 days', NOW() - INTERVAL '20 days', NOW() - INTERVAL '21 days',
         NOW() - INTERVAL '34 days', NOW() - INTERVAL '21 days'),
    -- User 9: اقتصاد در یک درس (returned)
    (18, 9, 4, 26, 'PHYSICAL', 80, 'RETURNED', 5,
         NOW() - INTERVAL '18 days', NOW() - INTERVAL '4 days',  NOW() - INTERVAL '3 days',
         NOW() - INTERVAL '18 days', NOW() - INTERVAL '3 days'),
    -- User 2: صد سال تنهایی (returned, now user 6 has it)
    (19, 2, 1, 2,  'PHYSICAL', 24, 'RETURNED', 1,
         NOW() - INTERVAL '40 days', NOW() - INTERVAL '26 days', NOW() - INTERVAL '27 days',
         NOW() - INTERVAL '40 days', NOW() - INTERVAL '27 days'),
    -- User 4: ریاضی عمومی (returned)
    (20, 4, 3, 20, 'PHYSICAL', 67, 'RETURNED', 4,
         NOW() - INTERVAL '42 days', NOW() - INTERVAL '28 days', NOW() - INTERVAL '30 days',
         NOW() - INTERVAL '42 days', NOW() - INTERVAL '30 days'),
    -- User 1: سووشون (returned)
    (21, 1, 1, 4,  'PHYSICAL', 28, 'RETURNED', 1,
         NOW() - INTERVAL '44 days', NOW() - INTERVAL '30 days', NOW() - INTERVAL '33 days',
         NOW() - INTERVAL '44 days', NOW() - INTERVAL '33 days'),
    -- User 5: عادت‌های اتمی (returned)
    (22, 5, 4, 30, 'PHYSICAL', 92, 'RETURNED', 5,
         NOW() - INTERVAL '35 days', NOW() - INTERVAL '14 days', NOW() - INTERVAL '16 days',
         NOW() - INTERVAL '35 days', NOW() - INTERVAL '16 days'),

-- REJECTED borrow
    -- User 7: مکانیک کوانتومی (rejected - not a member yet)
    (23, 7, 3, 22, 'PHYSICAL', NULL, 'REJECTED', NULL,
         NULL, NULL, NULL,
         NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days')
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- 8. UPDATE SEQUENCES (critical after explicit ID inserts)
-- =============================================================

SELECT setval('users_id_seq',       (SELECT MAX(id) FROM users));
SELECT setval('libraries_id_seq',   (SELECT MAX(id) FROM libraries));
SELECT setval('books_id_seq',       (SELECT MAX(id) FROM books));
SELECT setval('book_copies_id_seq', (SELECT MAX(id) FROM book_copies));
SELECT setval('library_memberships_id_seq', (SELECT MAX(id) FROM library_memberships));
SELECT setval('borrows_id_seq',     (SELECT MAX(id) FROM borrows));

COMMIT;

-- =============================================================
-- SUMMARY
-- =============================================================
SELECT 'users'               AS table_name, COUNT(*) AS total FROM users
UNION ALL SELECT 'libraries',               COUNT(*) FROM libraries
UNION ALL SELECT 'books',                   COUNT(*) FROM books
UNION ALL SELECT 'book_copies',             COUNT(*) FROM book_copies
UNION ALL SELECT 'library_memberships',     COUNT(*) FROM library_memberships
UNION ALL SELECT 'borrows',                 COUNT(*) FROM borrows
ORDER BY table_name;
