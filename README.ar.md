# إضافة مواقيت الصلاة لـ SuntimesWidget

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)](https://developer.android.com/jetpack/compose)
[![Design](https://img.shields.io/badge/Material-Material%203-757575)](https://m3.material.io/)
[![minSdk](https://img.shields.io/badge/minSdk-21-2ea44f)](https://developer.android.com/about/versions/android-5.0)

مواقيت الصلاة، أوقات النهي (المكروه) وأجزاء الليل كـ **Addon** لتطبيق **SuntimesWidget**.

هذا المشروع يتجنب عمداً تنفيذ الخوارزميات الفلكية: بدلاً من ذلك يقوم بتفويض حسابات الشمس/الظل إلى تطبيق **SuntimesWidget** المثبت عبر `ContentProvider` المصدّرة منه.

## ماذا يفعل هذا التطبيق؟

- يعرض خطاً زمنياً لليوم (Home) وقائمة شهرية ببطاقات الأيام (Days).
- يوفّر أحداثاً يمكن استخدامها داخل **SuntimesWidget Alarms**:
  - الصلوات الخمس (الفجر، الظهر/الجمعة، العصر، المغرب، العشاء)
  - حدود/نوافذ أوقات النهي (المكروه)
  - أجزاء الليل (منتصف الليل، الثلث الأخير، السدس الأخير)
- يوفّر ويدجت على الشاشة الرئيسية ("Prayer Times (Today)") بنفس نموذج بطاقة اليوم.
- يدعم الإنجليزية + العربية وواجهات RTL.
- يدعم وضع الثيم (النظام/فاتح/داكن) + اختيار Palette:
  - Parchment / Sapphire / Rose
  - ألوان ديناميكية على Android 12+ (Compose + خلفيات/شريط الودجت)

## المتطلبات

- Android `minSdk 21` (Lollipop)
- وجود تطبيق **SuntimesWidget** مثبتاً (يدعم stable/nightly/legacy)
- منح صلاحيات الوصول (SuntimesWidget يحمي `ContentProvider` بصلاحيات `suntimes.*.permission.*`)

## المعمارية (تفويض الحسابات للتطبيق المضيف)

التطبيق مقسّم إلى:
- `core/*`: اكتشاف المضيف، العقود (contracts)، خرائط معرّفات الأحداث، وتحويرات/مشتقات صغيرة (أجزاء الليل، التاريخ الهجري)
- `provider/*`: `ContentProvider` الخاص بالإضافة (للتكامل مع SuntimesWidget)
- `ui/*` و`ui/compose/*`: واجهات Compose (Home/Days/Settings/Event Picker)
- `widget/*`: ويدجت RemoteViews + جدولة التحديثات

### تدفق البيانات

```
            +-------------------+
            |  SuntimesWidget   |
            |-------------------|
            | event.provider    |  content://<host>.event.provider/eventCalc/<eventId>
            | calculator.provider| content://<host>.calculator.provider/sun/<millis>
            +---------^---------+
                      |
                      |  queries (delegation)
                      |
+---------------------+----------------------+
|         PrayerTimesAddon (this app)       |
|-------------------------------------------|
| UI (Compose)                              |
|  - Home/Days/Settings/EventPicker         |
|  - reads via core helpers                 |
|                                           |
| Addon Provider (exported)                 |
|  - content://...event.provider/eventCalc  |
|  - used by SuntimesWidget alarms          |
|  - also used by widget + UI for eventCalc |
|                                           |
| Widget (RemoteViews)                      |
|  - reads prefs + provider results         |
+-------------------------------------------+
```

### سياسة "بدون إعادة اختراع"

نعتبر التطبيق المضيف هو مصدر الحقيقة لكل أحداث الشمس/الظل:
- مواقيت الصلاة تُحسب بتحويل إعدادات المستخدم إلى **معرّفات أحداث في المضيف** ثم سؤال المضيف عن `eventCalc/<eventId>`.
- نستخدم `calculator.provider` للحصول على أوقات الشمس "بالجملة" (الشروق/الغروب/الزوال) لتقليل عدد الاستعلامات ودعم حساب الأيام الماضية.

الاستثناء الوحيد المقصود: **Fallback** لتوقيت العصر عندما لا يوفّر المضيف حدث shadow-ratio:
- يتم استخدام ميل الشمس (declination) من المضيف + خط العرض لتحويل معامل العصر (1/2) إلى معرّف حدث "ارتفاع الشمس"، ثم يبقى حساب الوقت النهائي مفوضاً إلى المضيف.

## تكامل المضيف (Addon Contracts)

يكتشف SuntimesWidget الإضافات عبر Activity مُصدّرة تستقبل `suntimes.action.ADDON_EVENT` وتحتوي على `meta-data` يشير إلى URI الخاص بالـ provider.

هذا المشروع يوفّر:
- `AddonRegistrationActivity` (واجهة اكتشاف بسيطة)
- `PrayerTimesProvider` (مُصدّر) يطبق نفس عقد مزود الأحداث الذي يتوقعه SuntimesWidget
- `EventPickerActivity` لـ `suntimes.action.PICK_EVENT`

### الـ Provider المُصدّر

Authority:
- `com.yshalsager.suntimes.prayertimesaddon.event.provider`

Paths:
- `content://.../eventTypes`
- `content://.../eventInfo/<eventId>`
- `content://.../eventCalc/<eventId>`

### الأحداث المتاحة

الصلوات:
- `PRAYER_FAJR`
- `PRAYER_DHUHR` (يُعرض كـ **الجمعة** يوم الجمعة)
- `PRAYER_ASR`
- `PRAYER_MAGHRIB`
- `PRAYER_ISHA`

أجزاء الليل:
- `NIGHT_MIDPOINT`
- `NIGHT_LAST_THIRD`
- `NIGHT_LAST_SIXTH`

حدود أوقات النهي (المكروه):
- `MAKRUH_DAWN_START` / `MAKRUH_DAWN_END`
- `MAKRUH_SUNRISE_START` / `MAKRUH_SUNRISE_END`
- `MAKRUH_ZAWAL_START` / `MAKRUH_ZAWAL_END`
- `MAKRUH_AFTER_ASR_START` / `MAKRUH_AFTER_ASR_END`
- `MAKRUH_SUNSET_START` / `MAKRUH_SUNSET_END`

## الإعدادات (ملخص عالي المستوى)

- اختيار المضيف:
  - اكتشاف تلقائي لـ stable/nightly/legacy.
  - حفظ الـ authority الخاص بـ event provider.
- طريقة حساب الصلاة:
  - Presets + Custom
  - زاوية الفجر
  - وضع العشاء (زاوية / دقائق ثابتة)
  - معامل العصر (Shafi=1 / Hanafi=2)
  - إزاحة المغرب بالدقائق
- أوقات النهي (المكروه):
  - Presets (Shafi/Hanafi) + زاوية + مدة الاستواء (zawal minutes)
- الهجري:
  - Variant: Umm al-Qura / Diyanet
  - Day offset: -2..+2 (تصحيح يدوي)
- الودجت:
  - إظهار/إخفاء صف أوقات النهي
  - إظهار/إخفاء صف أجزاء الليل
- واجهة التطبيق:
  - اللغة: النظام / الإنجليزية / العربية
  - الثيم: النظام / فاتح / داكن
  - Palette: parchment / dynamic (Android 12+) / sapphire / rose

## الودجت (Widget)

اسم الودجت:
- "Prayer Times (Today)"

ملاحظات:
- الودجت يعتمد RemoteViews، لذلك يتم تطبيق الثيم بوضع خلفية (drawable) وألوان النصوص وقت التحديث.
- التحديث "أفضل محاولة": عند تغيّر إعدادات التطبيق، ومع جدولة منبّه للحد التالي المهم (الصلاة التالية/حدود النهي/تغيّر اليوم).

## الأدوات (Tooling)

هذا المشروع مبني باستخدام:
- Gradle Wrapper (`./gradlew`)
- [`mise`](https://mise.jdx.dev/) لإدارة الأدوات/الإصدارات (اختياري لكنه مفضّل)

أوامر مفيدة:

```bash
# Build debug APK
mise x java -- ./gradlew :app:assembleDebug

# Run unit tests
mise x java -- ./gradlew :app:testDebugUnitTest
```

## هيكل المشروع

```
app/src/main/java/com/yshalsager/suntimes/prayertimesaddon/
  core/        # contracts, mapping, prefs, Hijri, delegation helpers
  provider/    # exported addon provider for SuntimesWidget
  ui/          # Activities + ViewModels
  ui/compose/  # Compose screens and shared components
  widget/      # AppWidgetProvider + update glue
```

## الشكر/الاعتمادات

- **SuntimesWidget** (التطبيق المضيف وواجهات الإضافات) بواسطة Forrest Guice:  
  [forrestguice/SuntimesWidget](https://github.com/forrestguice/SuntimesWidget)
- Time4J (التقويم الهجري والـ variants):  
  [MenoData/Time4J](https://github.com/MenoData/Time4J)
- Jetpack Compose / Material 3:  
  [AndroidX Compose](https://developer.android.com/jetpack/compose)  
  [Material 3](https://m3.material.io/)

## الرخصة

GNU GPLv3 فقط. راجع `LICENSE`.
