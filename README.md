# Yerel Satranç (Android)

İki Android cihazın aynı Wi-Fi ağı üzerinden, internet sunucusu kullanmadan satranç oynaması için başlangıç Android Studio projesi.

## Özellikler
- Bir cihazda **Oyunu Kur**, diğer cihazda kurucu IP'sini girip **Bağlan**.
- Yerel TCP bağlantısı, port `47821`.
- Yasal hamle kontrolü için Chesslib; rok, geçerken alma, şah/mat ve beraberlik durumları kütüphane tarafından yönetilir.
- Terfide otomatik vezir seçimi.
- Android 8.0+ hedefi.

## APK oluşturma
Bu arşiv kaynak projedir; APK bu çalışma ortamında derlenmedi ve cihaz üzerinde test edilmedi.

1. Android Studio'da bu klasörü açın.
2. Gradle senkronizasyonunun tamamlanmasını bekleyin (ilk kurulumda internet, Android SDK ve Maven bağımlılığını indirmek için gerekir).
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)** seçin.
4. APK genellikle `app/build/outputs/apk/debug/app-debug.apk` konumunda oluşur.

Alternatif olarak GitHub'a yükleyip `.github/workflows/android.yml` iş akışını çalıştırarak Actions sayfasından APK artifact'i indirebilirsiniz.

## Yerel ağda oynama
- İki telefonu aynı Wi-Fi ağına bağlayın. Misafir ağı / istemci izolasyonu açık olmamalı.
- Kurucu cihazda **Oyunu Kur** düğmesine basın ve ekranda gösterilen IPv4 adresini diğer telefona yazın.
- İkinci telefonda **Bağlan** düğmesine basın. Kurucu Beyaz, bağlanan Siyah oynar.
- Android veya Wi-Fi yönlendiricisi yerel ağ iletişimini engelliyorsa bağlantı kurulamayabilir.

## Notlar
Bu bir ilk sürüm prototipidir. Ağ kopması sonrası otomatik yeniden bağlanma, oyun saati, hamle geçmişi ve terfi taşı seçimi henüz yoktur. Terfi otomatik vezire yapılır. Ağ bağlantısı ve kurallar gerçek iki cihazda ayrıca test edilmelidir.
