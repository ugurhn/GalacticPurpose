# Galactic Purpose (`0.1.0-alpha.1` — Erken Geliştirme / WIP)

> **⚠️ UYARI / WORK-IN-PROGRESS NOTICE**
>
> **TR:** Bu proje **henüz tamamlanmamıştır (erken alfa / prototip aşamasındadır)** ve son kullanıcı için nihai bir sürüm değildir. Mekanikler, denge değerleri, modeller, dokular, ses miksajı ve performans optimizasyonları aktif olarak geliştirilmektedir. Hata (bug) veya eksikliklerle karşılaşabilirsiniz; şu an yalnızca **erken test ve geri bildirim** amacıyla paylaşılmaktadır.
>
> **EN:** This project is an **early work-in-progress (`0.1.0-alpha.1`) prototype**. It is **not finished or production-ready**—gameplay systems, assets, textures, audio balance, and UI are under active development and shared primarily for friend playtesting and feedback.

---

## Temel Özellikler / Highlights (WIP)
- **Horizon Lander Uzay Aracı:** Kalkış pistinden ateşleme (`[SPACE x2]` veya `[G]`), `> 15 m/s` sonrası dikleşen atmosfer tırmanışı, hız-konum denetimli (`PD`) vektör iticili otomatik iniş, kızak/ponton açılımı, krater/zemin çökmesinde gerçekçi gezegen yerçekimi ve patlama tepkisi.
- **Uçuş Yönlendirici (`Flight Director Transponder`):** Park halindeki gemiye sağ tıklayıp bağlayarak aynı boyuttaki herhangi bir hedef bloğa (`6–1000 m`) mesafe oranlı yakıtla, kalkıp kanat yatırarak yarım daire kavisli dönen otonom iniş/nakliye uçuşu gönderme.
- **Yörünge & Solucan Deliği Geçiş Kabini (`orbital_transit`):** Gerçek `0.00g` etki-tepki kabin fiziği (`[Shift]` Manyetik Bot, oturulabilir Kaptan Koltuğu), `512×256` yüzey/bulut dokulu gezegen yaklaşımı, `60 m` Köken Solucan Deliği dönüş yaklaşımı, `[G]` ile içine giriş yanışı ve yumuşak ekran geçiş (`fade`) efekti.
- **Devasa Çok Bloklu Solucan Deliği & Tekillik Çöküşü:** Özelleştirilmiş `ENERGY_GLOW` 3B küre/halka/ışın render sistemi, kararsız çekirdek patlamasında blok yutan yükselen tekillik ve krater şok dalgası.
- **4 Ötegezegen & Güneş Çöküşü / Mars Dünyalaştırma:** `Hephaestus-IV`, `Pelagia-9`, `Nyx-0`, `Sanctuary`, Erebus Kara Deliği gelgit yerçekimi ve Dünya güneş istikrarsızlığı fazları.

---

## 🎮 Oyun İçi Test Komutları / In-Game Test Commands

> Komutları sohbette çalıştırabilmek için dünyada Hileler / OP (`permission level 2`) açık olmalıdır.

### 1. En Hızlı Tam Uçuş Testi (Gözlemevi + Pist + Gemi)
Oyuncunun baktığı yöne stabil `45×41×45` Devasa Solucan Deliği, manyetik kalkış pisti ve tam yakıtlı **Horizon Lander** kurar; oyuncuyu doğrudan pistin yanına taşır:
```mcfunction
/galacticpurpose wormhole build colossal stable hephaestus_iv
```
Diğer rotalar:
```mcfunction
/galacticpurpose wormhole build colossal stable pelagia_9
/galacticpurpose wormhole build colossal stable nyx_0
/galacticpurpose wormhole build colossal stable sanctuary
```

Diğer Solucan Deliği / Çöküş / Gemi komutları:
```mcfunction
/galacticpurpose wormhole build colossal unstable
/galacticpurpose wormhole build small stable hephaestus_iv
/galacticpurpose wormhole build small unstable
/galacticpurpose wormhole collapse colossal
/galacticpurpose wormhole collapse small
/galacticpurpose ship spawn
/galacticpurpose transit hephaestus_iv
```

### 2. Gezegen Işınlanma ve Yörünge Gelgit Fazı
```mcfunction
/galacticpurpose goto hephaestus_iv
/galacticpurpose goto pelagia_9
/galacticpurpose goto nyx_0
/galacticpurpose goto sanctuary
/galacticpurpose return

/galacticpurpose orbit zenith
/galacticpurpose orbit horizon
/galacticpurpose orbit nadir
```

### 3. Dünya Güneş Çöküşü & Mars Dünyalaştırma Testleri
```mcfunction
/galacticpurpose codex
/galacticpurpose status
/galacticpurpose setday 30

/galacticpurpose mars status
/galacticpurpose mars testmode true
/galacticpurpose mars set 50 50 50
/galacticpurpose mars redgiant true
/galacticpurpose mars storm countdown
/galacticpurpose mars storm warning
/galacticpurpose mars storm active
```

### 4. Hızlı Eşya Alma (`/give`)
```mcfunction
/give @s galacticpurpose:flight_director
/give @s galacticpurpose:starship_fuel_cell 16
/give @s galacticpurpose:starship_blueprint
/give @s galacticpurpose:quantum_core
/give @s galacticpurpose:mission_codex
```

---

## 🚀 Kontroller (Uçuş Özeti)
- **Gemiye Binme / Kargo:** Sağ Tık ile gemiye bin · `Shift + Sağ Tık` ile kargo/yakıt ekranı · `[I]` gemideyken kargo ekranı
- **Ateşleme / Kalkış:** Gemideyken `[SPACE x2]` veya `[G]`
- **Yörünge Kabini (`orbital_transit`):** `[Shift]` ile Manyetik Bot · Kaptan Koltuğuna Sağ Tık ile sabitlen · Varış sayacı bitince `[G]` (veya ön Köprü Dümenine Sağ Tık) ile atmosfere / solucan deliğine gir
- **Uçuş Yönlendirici (`Flight Director`):** Elindeyken park halindeki gemiye Sağ Tıkla (bağlar), ardından `6–1000 m` mesafedeki herhangi bir bloğa Sağ Tıkla

---

## 🛠️ Kurulum ve Geliştirici Komutları
- **Hedef Sürüm:** Minecraft `1.21.1` • Fabric Loader `0.20.1+` • Fabric API `0.116.17+1.21.1` • Java `21+`
- **TeamReborn Energy `4.1.0`:** Mod `.jar` içine gömülüdür (`include`).

Windows'ta tek tıkla test sürümünü açmak için repo ana klasöründeki **[run-test-client.bat](run-test-client.bat)** dosyasına **çift tıklayabilirsiniz** (doğrudan `.\gradlew.bat runClient` başlatır).

Kaynaktan komut satırıyla açmak veya `.jar` derlemek için:
```bash
# Doğrudan geliştirici istemcisini başlat
./gradlew runClient

# Derlenebilir mod .jar paketini oluştur (build/libs/ altında çıkar)
./gradlew build

# Tüm JUnit birim testlerini + Fabric GameTest süitini çalıştır
./gradlew test runGameTest
```
*(Windows PowerShell'de `./gradlew` yerine `.\gradlew.bat` kullanabilirsiniz.)*

---

## 📜 Lisans & Ses Kaynakçası
- Kod ve özgün varlıklar **[MIT License](LICENSE)** altındadır.
- Roket motor sesleri kamu malı **NASA** kayıtlarından (`CC PDM 1.0`) türetilmiştir — detaylar için [CREDITS.md](CREDITS.md).
