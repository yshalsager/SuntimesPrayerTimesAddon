# إضافة مواقيت الصلاة لـ SuntimesWidget

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)](https://developer.android.com/jetpack/compose)
[![Design](https://img.shields.io/badge/Material-Material%203-757575)](https://m3.material.io/)
[![minSdk](https://img.shields.io/badge/minSdk-23-2ea44f)](https://developer.android.com/about/versions/android-6.0)

مواقيت الصلاة، أوقات النهي (المكروه) وأجزاء الليل كـ **Addon** لتطبيق **[SuntimesWidget](https://forrestguice.github.io/Suntimes/download/)** وكـ **مصدر تقاويم** لتطبيق **[Suntimes Calendars](https://forrestguice.github.io/Suntimes/help/addons/suntimescalendars/)**.

هذا المشروع يتجنب عمداً تنفيذ الخوارزميات الفلكية: بدلاً من ذلك يقوم بتفويض حسابات الشمس/الظل إلى تطبيق **[SuntimesWidget](https://forrestguice.github.io/Suntimes/download/)** المثبت عبر `ContentProvider` المصدّرة منه.

> [!CAUTION]
> **تنبيه:** تم تطوير هذا المشروع مع اعتماد كبير على مساعدات الذكاء الاصطناعي. معظم الشيفرة تم توليدها أو تعديلها باستخدام نموذج Codex 5.3 من OpenAI.

## لقطات شاشة

<table>
  <thead>
    <tr>
      <th align="left">English</th>
      <th align="left">Arabic (RTL)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>
        <div><strong>Home</strong> (today timeline)</div>
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01_home.png" alt="Home (EN)" style="max-width: 100%; height: auto;" />
      </td>
      <td>
        <div><strong>Home</strong> (خط اليوم)</div>
        <img src="fastlane/metadata/android/ar/images/phoneScreenshots/01_home.png" alt="Home (AR)" style="max-width: 100%; height: auto;" />
      </td>
    </tr>
    <tr>
      <td>
        <div><strong>Days</strong> (month cards)</div>
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02_calendar.png" alt="Calendar (EN)" style="max-width: 100%; height: auto;" />
      </td>
      <td>
        <div><strong>Days</strong> (بطاقات الشهر)</div>
        <img src="fastlane/metadata/android/ar/images/phoneScreenshots/02_calendar.png" alt="Calendar (AR)" style="max-width: 100%; height: auto;" />
      </td>
    </tr>
    <tr>
      <td>
        <div><strong>Settings</strong> (Material 3)</div>
        <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03_settings.png" alt="Settings (EN)" style="max-width: 100%; height: auto;" />
      </td>
      <td>
        <div><strong>Settings</strong> (الإعدادات)</div>
        <img src="fastlane/metadata/android/ar/images/phoneScreenshots/03_settings.png" alt="Settings (AR)" style="max-width: 100%; height: auto;" />
      </td>
    </tr>
  </tbody>
</table>

## ماذا يفعل هذا التطبيق؟

- يعرض خطاً زمنياً في Home مع تنقل يومي لامحدود بالسحب يميناً/يساراً، وقائمة شهرية ببطاقات الأيام (Days/Calendar).
- يوفّر أحداثاً يمكن استخدامها داخل **SuntimesWidget Alarms**:
  - الصلاة (الفجر، الفجر الإضافي ١ اختياري، الضحى، بداية/نهاية وقت العيد في أيام العيد، الظهر/الجمعة، العصر، المغرب، العشاء، العشاء الإضافي ١ اختياري)
  - حدود/نوافذ أوقات النهي (المكروه)
  - أجزاء الليل (منتصف الليل، الثلث الأخير، السدس الأخير)
- يدعم أحداث نافذة صلاة العيد في أيام العيد فقط:
  - `بداية وقت العيد = الشروق + 15 دقيقة`
  - `نهاية وقت العيد = الزوال (منتصف النهار الشمسي)`
- يدعم خانة إضافية واحدة للفجر وخانة إضافية واحدة للعشاء:
  - لكل خانة زاوية مستقلة
  - لكل خانة تسمية مخصصة قابلة للتعديل
  - الخانتان معطلتان افتراضياً وتختفيان من منتقي الأحداث/المزود عند التعطيل
- يوفّر ثلاثة تقاويم للقراءة فقط يمكن لتطبيقات تقاويم Suntimes اكتشافها:
  - `prayers`
  - `makruh`
  - `night`
- يمكن الضغط على الصلاة/حدث الليل داخل Home لفتح محرّر منبّهات المضيف مع اختيار الحدث مسبقاً (منبّهات الأحداث تتتبع تغيّر المواقيت تلقائياً، ويمكن ضبط الإزاحة +/-30 دقيقة من واجهة المضيف).
- يطبّق تمييزاً بصرياً لمستويات أوقات النهي (خفيف: من الفجر/بعد العصر، شديد: الشروق/الاستواء/الغروب) في الخط الزمني وبطاقات التقويم وملصقات الودجت.
- يوفّر ويدجت على الشاشة الرئيسية ("Prayer Times (Today)") بنفس نموذج بطاقة اليوم.
- يوفّر اختصارات للتطبيق تفتح Days و Settings عبر روابط عميقة `prayertimes://shortcuts/...`.
- يدعم الإنجليزية + العربية وواجهات RTL.
- يدعم وضع الثيم (النظام/فاتح/داكن) + اختيار Palette:
  - Parchment / Sapphire / Rose
  - ألوان ديناميكية على Android 12+ (Compose + خلفيات/شريط الودجت)

## المتطلبات

- Android `minSdk 23` (Marshmallow)
- وجود تطبيق **SuntimesWidget** مثبتاً (يدعم stable/nightly/legacy)
- منح صلاحيات الوصول (SuntimesWidget يحمي `ContentProvider` بصلاحيات `suntimes.*.permission.*`)

## المعمارية (تفويض الحسابات للتطبيق المضيف)

التطبيق مقسّم إلى:
- `core/*`: اكتشاف المضيف، العقود (contracts)، خرائط معرّفات الأحداث، وتحويرات/مشتقات صغيرة (أجزاء الليل، التاريخ الهجري)
- `provider/*`: `ContentProvider` الخاصة بالإضافة للتكامل مع المنبّهات وتغذية التقاويم
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
| Calendar Provider (exported)              |
|  - content://...calendar.provider/...     |
|  - used by Suntimes calendar discovery    |
|  - exposes prayers/makruh/night feeds     |
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

يكتشف Suntimes هذا التطبيق عبر سطحين مُصدّرين:
- اكتشاف إضافة المنبّهات عبر `suntimes.action.ADDON_EVENT`
- اكتشاف التقاويم عبر `suntimes.action.ADD_CALENDAR`

هذا المشروع يوفّر:
- `AddonRegistrationActivity` (واجهة اكتشاف بسيطة)
- `PrayerTimesProvider` (مُصدّر) يطبق نفس عقد مزود الأحداث الذي يتوقعه SuntimesWidget
- `EventPickerActivity` لـ `suntimes.action.PICK_EVENT`
- `CalendarDiscoveryAlias` لاكتشاف التقاويم
- `PrayerTimesCalendarProvider` (مُصدّر) لعرض تقاويم الصلاة/المكروه/الليل

يتم اشتقاق الـ authorities والإجراءات الداخلية من `${applicationId}`، لذلك يمكن تثبيت نسختي debug وrelease معاً بدون تعارض في الـ authorities أو الـ actions.

### مزود الأحداث

Authority:
- `${applicationId}.event.provider`

Paths:
- `content://.../eventTypes`
- `content://.../eventInfo/<eventId>`
- `content://.../eventCalc/<eventId>`

تجاوز الموقع (لكل استعلام / لكل منبّه):
- يدعم `eventCalc/<eventId>` تمرير حقول الموقع بنفس نمط المضيف داخل `selection` / `selectionArgs` (`latitude` و`longitude` و`altitude` اختياري، بالإضافة إلى `alarm_now`).
- في الأحداث المعتمدة على الشمس، يتم حساب الوقت وفق الموقع الممرر عندما تتوفر هذه الحقول.
- عند عدم تمرير حقول الموقع، يتم الحساب باستخدام الموقع الافتراضي/المحدد حالياً في التطبيق المضيف.

### مزود التقاويم

Authority:
- `${applicationId}.calendar.provider`

مراجع الاكتشاف:
- `content://.../calendar.provider/prayers/`
- `content://.../calendar.provider/makruh/`
- `content://.../calendar.provider/night/`

المسارات المدعومة:
- `content://.../<source>/calendarInfo`
- `content://.../<source>/calendarContent/<windowStart>-<windowEnd>`
- `content://.../<source>/calendarTemplateStrings`
- `content://.../<source>/calendarTemplateFlags`

سلوك التقاويم:
- `prayers` يصدّر أحداثاً نقطية للفجر، الفجر الإضافي ١ (عند التفعيل)، الضحى، بداية/نهاية وقت العيد، الظهر/الجمعة، العصر، المغرب، العشاء، والعشاء الإضافي ١ (عند التفعيل)
- `makruh` يصدّر أحداثاً على شكل نطاقات لفترات الفجر والشروق والزوال وما بعد العصر والغروب
- `night` يصدّر أحداثاً نقطية لمنتصف الليل والثلث الأخير والسدس الأخير

### الأحداث المتاحة

الصلوات:
- `PRAYER_FAJR`
- `PRAYER_FAJR_EXTRA_1` (اختياري؛ مخفي عند التعطيل)
- `PRAYER_DUHA`
- `PRAYER_EID_START` (في أيام العيد فقط)
- `PRAYER_EID_END` (في أيام العيد فقط)
- `PRAYER_DHUHR` (يُعرض كـ **الجمعة** يوم الجمعة)
- `PRAYER_ASR`
- `PRAYER_MAGHRIB`
- `PRAYER_ISHA`
- `PRAYER_ISHA_EXTRA_1` (اختياري؛ مخفي عند التعطيل)

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
  - فتح واجهة اختيار الموقع في التطبيق المضيف من الإعدادات (لا نقوم بتكرار قاعدة بيانات المواقع داخل الإضافة).
  - فتح شاشة منبّهات المضيف مباشرة من الإعدادات.
- طريقة حساب الصلاة:
  - Presets + Custom
  - زاوية الفجر
  - وضع العشاء (زاوية / دقائق ثابتة)
  - الفجر الإضافي ١ (معطل افتراضياً): مفتاح تفعيل + زاوية مخصصة + تسمية مخصصة
  - العشاء الإضافي ١ (معطل افتراضياً): مفتاح تفعيل + زاوية مخصصة + تسمية مخصصة
  - العشاء الإضافي ١ يعتمد دائماً على الزاوية (مستقل عن وضع دقائق العشاء الثابتة العام)
  - معامل العصر (Shafi=1 / Hanafi=2)
  - إزاحة المغرب بالدقائق
- أوقات النهي (المكروه):
  - Presets (Shafi/Hanafi) + زاوية + مدة الاستواء (zawal minutes)
  - نهاية نهي الشروق بالدقائق الثابتة بعد الشروق (`10`/`15`/`20`)
- الهجري:
  - Variant: Umm al-Qura / Diyanet
  - Day offset: -2..+2 (تصحيح يدوي)
- التقويم (Calendar):
  - أساس الشهر: ميلادي / هجري
  - تنسيق التاريخ الميلادي: Card / Medium / Long
  - إظهار/إخفاء صف أوقات النهي
  - إظهار/إخفاء صف أجزاء الليل
- الودجت:
  - إظهار/إخفاء صف أوقات النهي
  - إظهار/إخفاء صف أجزاء الليل
  - يستخدم مفاتيح إظهار مستقلة عن التقويم (غير مشتركة)
- المنبّهات والنسخ الاحتياطي:
  - تصدير ملف إعداد منبّهات قابل للاستيراد في المضيف (يتضمن الضحى، ويتضمن الإضافات فقط عند تفعيلها)
  - تصدير/استيراد إعدادات الإضافة بصيغة JSON
  - النسخ الاحتياطي يحفظ التسمية المخصصة الخام للإضافات حتى يبقى fallback المحلي صحيحاً بعد الاستعادة
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
- نسخة Gradle Wrapper: `9.4.1`
- [`mise`](https://mise.jdx.dev/) لإدارة الأدوات/الإصدارات (اختياري لكنه مفضّل)
- نسخ Release مفعّل فيها تقليل الحجم عبر R8 (`minifyEnabled` + `shrinkResources` + ProGuard optimize)
- يوجد مسار إطلاق آلي عبر GitHub Actions (`.github/workflows/release.yml`) يدعم dry-run، إنشاء tag/release، التحقق من توقيع APK، وتوليد ملاحظات الإصدار

أوامر مفيدة:

```bash
# Build debug APK
mise x java -- ./gradlew :app:assembleDebug

# Build release APK
mise x java -- ./gradlew :app:assembleRelease

# Run unit tests
mise x java -- ./gradlew :app:testDebugUnitTest

# Run instrumentation screenshots on a connected emulator/device
mise x java -- ./gradlew :app:connectedDebugAndroidTest
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
