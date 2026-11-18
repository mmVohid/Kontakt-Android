# Seyf Kontakt

Shifrlangan, to'liq oflayn kontakt daftari (Android, Kotlin + Jetpack Compose + Material 3).

## Kontakt maydonlari
Rasm, Ism, Familya, Otasining ismi, Telefon raqami, Pasport seriyasi, Pasport JSHSHIR, Yashash joyi, Avtomobil raqami, Izoh.

## Xavfsizlik
| Talab | Qanday amalga oshirilgan |
|---|---|
| AES-256 shifrlash | Barcha kontaktlar va rasmlar AES-256-GCM bilan shifrlanadi (har yozuvda yangi IV, autentifikatsiya tegi bilan). |
| Majburiy 6 xonali PIN | Birinchi ishga tushirishda PIN o'rnatilmaguncha ilovaga kirib bo'lmaydi. Oddiy PINlar (123456, 111111) rad etiladi. |
| Biometrik ochish | Ixtiyoriy. Kalit Keystore'da, faqat biometrika tasdig'idan keyin ishlaydi. Yangi barmoq izi qo'shilsa, kalit bekor bo'ladi va PIN so'raladi. |
| Ekran himoyasi majburiy | `FLAG_SECURE` doim yoqiq (o'chirish tugmasi yo'q): skrinshot, ekran yozish va "So'nggi ilovalar" ko'rinishi taqiqlangan. |
| Chiqishda avto-blok | Ilova fonga o'tsa yoki yopilsa, ekran o'chsa, darrov bloklanadi. Xotiradagi kalit nollar bilan o'chiriladi. |
| Telefon (ekran) himoyasi | Ekran o'chganda va qo'ng'iroq/boshqa ilovaga o'tilganda bloklanadi; harakatsizlikda ham bloklanadi (sozlanadi). |

Kalit ierarxiyasi: tasodifiy 256 bitli kalit (DEK) ma'lumotni shifrlaydi. DEK ikki qavat o'raladi: avval PIN'dan PBKDF2-HMAC-SHA256 (200 000 iteratsiya) bilan hosil qilingan kalit, keyin Android Keystore'dagi qurilmaga bog'langan kalit. Shuning uchun fayllar nusxalansa ham, boshqa qurilmada PINni terib ochib bo'lmaydi.

Boshqa choralar:
- `INTERNET` ruxsati yo'q: ilova tarmoqqa chiqa olmaydi.
- Backup o'chirilgan (`allowBackup=false`, bulutga ham, boshqa telefonga ham ko'chmaydi).
- PIN uchun tizim klaviaturasi ishlatilmaydi (maxsus raqamli klaviatura).
- Noto'g'ri PIN: 5 urinishdan keyin kutish vaqti oshadi (30 s, 1 daq, 5 daq, 15 daq, 1 soat).
- Pasport va JSHSHIR ekranda yashirin, "ko'z" tugmasi bilan ochiladi (15 soniyadan keyin yana yashirinadi).
- Nusxalangan ma'lumot "sezgir" deb belgilanadi va 30 soniyadan keyin buferdan tozalanadi (best-effort).
- Rasmlar kvadratga kesilib qayta kodlanadi: EXIF (GPS joylashuv) o'chadi.

**Muhim:** PINni unutsangiz, ma'lumotni tiklab bo'lmaydi. Qulf ekranidagi "PINni unutdim" tugmasi hamma narsani o'chirib, boshidan boshlaydi.

## APK yig'ish

### 1-usul: GitHub Actions (kompyuterda Android Studio shart emas)
1. Bu papkani GitHub'ga yangi repo qilib yuklang (`git init`, `git add .`, `git commit`, `git push`).
2. Repo sahifasida **Actions** bo'limiga kiring. "APK yig'ish" workflow o'zi ishga tushadi (yoki "Run workflow").
3. Tugagach, ish sahifasining pastidagi **Artifacts** bo'limidan `SeyfKontakt-APK` ni yuklab oling, ichida `app-debug.apk`.
4. Telefonga ko'chirib o'rnating ("noma'lum manbalardan o'rnatish"ga ruxsat bering).

### 2-usul: Android Studio
1. Android Studio (Ladybug yoki yangiroq) bilan papkani oching, Gradle sync tugashini kuting.
2. **Build > Build APK(s)**. APK: `app/build/outputs/apk/debug/app-debug.apk`.

### 3-usul: buyruq qatori
```
./gradlew assembleDebug
```
(JDK 17 va Android SDK 35 kerak.)

## Texnik ma'lumot
- minSdk 26 (Android 8.0), targetSdk 35
- Paket: `uz.seyf.kontakt`
- Kutubxonalar: Compose BOM 2024.12.01, Material 3, androidx.biometric 1.1.0, ExifInterface
