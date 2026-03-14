# רדיו ישראל – Android App

## ⚡ התחלה מהירה

### שלב 1 – הורד לוגואים (פעם אחת בלבד)
```bash
pip install requests          # רק אם requests לא מותקן
python download_logos.py
```
זה מוריד את כל לוגואי התחנות לתוך `app/src/main/assets/logos/`

### שלב 2 – בנה APK
```bash
# Debug APK (לבדיקה מהירה):
./gradlew assembleDebug

# Release APK (ללא debug info, קטן יותר):
./gradlew assembleRelease
```
הקובץ יהיה ב:
- Debug:   `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

### שלב 3 – העבר לטלפון
```bash
# עם USB:
adb install app/build/outputs/apk/debug/app-debug.apk

# או שלח את ה-APK ב-WhatsApp/Drive ופתח בטלפון
# (חייב להפעיל "התקנה ממקורות לא ידועות" בהגדרות)
```

---

## מה מוטמע באפליקציה (offline-ready)

| רכיב | היכן | מצב |
|------|------|-----|
| רשימת תחנות | `assets/rlive_kcm_streams.json` | ✅ מוטמע מלא |
| לוגואי תחנות | `assets/logos/{id}.jpg` | ✅ לאחר הרצת download_logos.py |
| ספריות (ExoPlayer, Glide) | בתוך ה-APK | ✅ מוטמע |
| הזרמת אודיו | URLs חיצוניים | 🌐 דורש אינטרנט (הכרחי) |

---

## מפתחות Qin

| מקש | פעולה |
|-----|-------|
| ↑ | ווליום + |
| ↓ | ווליום - |
| ← | תחנה קודמת |
| → | תחנה הבאה |
| 1 | נגן / השהה |

---

## הוספת תחנות

ערוך `app/src/main/assets/rlive_kcm_streams.json` → הוסף לתוך `"stations"` ולתוך `"categories"`.



