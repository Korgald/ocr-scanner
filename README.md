# OCR Scanner

Офлайн-сканер конспектов и шпаргалок для Android: распознаёт печатный текст
(кириллица + латиница) полностью без интернета.

## Возможности
- Съёмка (CameraX) и выбор из галереи (Photo Picker, без разрешений)
- Интерактивный кроп области распознавания угловыми ручками
- Офлайн OCR: Tesseract4Android (rus + eng), модели в assets/tessdata
- Редактируемый результат с процентом уверенности
- Заметки: сохранение, поиск, удаление (Room)
- Экспорт TXT/PDF; светлая/тёмная тема; масштаб текста 0.8-1.6

## Стек
Kotlin 2.0.21, Jetpack Compose + Material 3, Hilt 2.56.2, Navigation Compose 2.8.9,
Room 2.7.1, DataStore 1.0.0, CameraX 1.4.2, Tesseract4Android 4.7.0,
Gradle 8.11.1 / AGP 8.10.1 / JDK 21 (target JVM 17), minSdk 24 / targetSdk 36.

## Сборка
git clone https://github.com/Korgald/ocr-scanner.git
cd ocr-scanner
./gradlew assembleDebug

В манифесте нет сетевых разрешений: приложение полностью офлайн.
Готовый APK приложен к релизу v1.0.0 в разделе Releases.