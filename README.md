# ⚡ Hız Testi — TCL C855 Android TV

TCL C855 ve tüm Android TV cihazlar için Türkçe ağ + sistem hız testi uygulaması.

---

## 📊 Özellikler

| Metrik | Yöntem |
|---|---|
| ⬇ İndirme hızı | Cloudflare'den 10 MB gerçek indirme |
| ⬆ Yükleme hızı | Download/ping'e dayalı akıllı tahmin |
| 🏓 Ping / Gecikme | 4x HTTP round-trip ortalaması |
| 📶 Wi-Fi sinyali | RSSI (dBm), link hızı, SSID |
| 🖥 CPU kullanımı | /proc/stat anlık ölçüm |
| 🧠 RAM | Kullanılan / Toplam bellek |

---

## 🚀 APK Nasıl Alınır? (Android Studio Olmadan)

### ✅ Yöntem 1 — GitHub Actions (Ücretsiz, Bulutta Derleme)

**Adım 1:** GitHub'da ücretsiz hesap aç → [github.com](https://github.com)

**Adım 2:** Yeni repo oluştur
- `+` → "New repository" → İsim ver (örn: `hiz-testi`) → Create

**Adım 3:** Bu ZIP'teki dosyaları repoya yükle
- "uploading an existing file" linkine tıkla
- Tüm dosyaları sürükle bırak → "Commit changes"

**Adım 4:** APK otomatik derlenir!
- `Actions` sekmesine git
- `Android TV - APK Derle` workflow'u yeşil ✅ olunca tıkla
- **Artifacts** bölümünden `SpeedTestTV-debug-apk` indir
- ZIP içinden `app-debug.apk` çıkar

---

## 📺 APK'yı TV'ye Yükleme

### Yöntem A — USB Bellek (En Kolay)
1. `app-debug.apk`'yı USB belleğe kopyala
2. USB'yi TCL C855'e tak
3. **Ayarlar → Cihaz Tercihleri → Güvenlik → Bilinmeyen kaynaklar: AÇ**
4. Dosya Yöneticisi'nden `app-debug.apk`'yı bul → Yükle

### Yöntem B — ADB (Wi-Fi ile, USB gerekmez)
```bash
# 1. TV'de ADB'yi aç:
#    Ayarlar → Cihaz Tercihleri → Hakkında → Derleme Numarası'na 7 kez bas
#    Geliştirici Seçenekleri → USB Hata Ayıklama: AÇIK
#    Geliştirici Seçenekleri → Ağ üzerinden ADB hata ayıklama: AÇIK

# 2. TV'nin IP'sini bul:
#    Ayarlar → Ağ → Wi-Fi → Gelişmiş

# 3. Bilgisayardan bağlan:
adb connect 192.168.1.XXX:5555

# 4. APK yükle:
adb install app-debug.apk
```

---

## 🎮 Kullanım

- **OK / Enter** → Testi başlat / durdur
- **Yukarı/Aşağı D-pad** → Navigasyon
- CPU ve RAM otomatik olarak her **3 saniyede** güncellenir
- Test süresi yaklaşık **15-20 saniye**

---

## 📁 Proje Yapısı

```
SpeedTestTV/
├── .github/workflows/build.yml       ← Otomatik APK derleyici
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/tcl/speedtest/ui/
│       │   └── MainActivity.java     ← Tüm uygulama mantığı
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/{colors,strings,styles}.xml
│           └── drawable/{card_bg,banner}.xml
├── build.gradle
├── gradle.properties
├── gradlew / gradlew.bat
└── settings.gradle
```

---

## ⚠️ Notlar

- **Yükleme hızı** tahmindir — gerçek upload sunucusuna ihtiyaç duymaz
- Wi-Fi yerine Ethernet kullanıyorsanız "Ethernet bağlantısı" gösterir
- `app-debug.apk` imzasız debug sürümüdür, kişisel kullanım için uygundur
