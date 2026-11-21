# Galactic Purpose — Tasarım ve Mimari Dokümanı

**Tarih:** 2026-11-20  
**Hedef Platform:** Minecraft 1.21.1 • Fabric Loader • Java 21 • Galacticraft 5 (Pre-Alpha Uyumlu)  
**Lisans & Açık Kaynak:** MIT License (Telif hakkı güvenli, %100 özgün içerik ve varlıklar)

---

## 1. Temel Tasarım Felsefesi

1. **Anti-God-Mode Kuralı:** Oyuncuyu her koşulda ölümsüz yapan tek tip süper zırh veya alet eklenmez. Her zırh ve ekipman **durumsaldır (situational)** ve belirgin bir bedel/denge taşır:
   - *Ağır Kurşun Radyasyon Zırhı:* Güneş fırtınası radyasyonunu engeller, ancak düşük yakın dövüş koruması ve ağırlık kaynaklı hareket hızı cezası verir.
   - *Termal Kalkanlı Zırh (Rota A):* Kavrulmuş Dünya / Hephaestus-IV aşırı ısısına karşı korur.
   - *Basınç Dayanımlı Zırh (Rota B):* Pelagia-9 derin okyanus ezilme basıncına karşı korur.
2. **Adaletli Tehlike (No Unfair Soft-Lock):**
   - Dünya felaketi ilk 15–20+ gün **Grace Period** sonrası kademeli başlar (eşikler konfigüre edilebilir).
   - Çevresel sıcaklık/radyasyon yalnızca **gökyüzüne açık yüzeyde** ölümcüldür; yeraltı mağaraları ve sığınaklar güvenli kalır.
   - Güneş Fırtınaları önceden uyarı verir ve geri sayım gösterir.
3. **GC5 Pre-Alpha Dayanıklı Mimari (`SpaceCompat`):**
   - Resmi Galacticraft 5 (`TeamGalacticraft/Galacticraft`) 1.21.1 Fabric için pre-alpha aşamasındadır ve resmi Maven sunucusu kesintili/güncellenmektedir; ayrıca Mars boyutu upstream pre-alpha'da henüz tam değildir (topluluk fork'u *Boobcat Edition* içinde mevcuttur).
   - Bu nedenle çekirdek mod mekaniği `SpaceCompat` arayüzünün arkasında tutulur; enerji `teamreborn:energy:4.1.0` API'si üzerinden standart olarak bağlanır, Galacticraft API çağrıları (örn. `LevelOxygenAccessor`) çalışma zamanında varsa köprülenir.

---

## 2. Özgün Senaryo Akışı (5 Aşama — Telifsiz Özgün Evren)

1. **Aşama 1 — Dünya'nın (Overworld) Çöküşü:**
   - Güneş çekirdeğindeki kararsızlık nedeniyle Grace Period sonrasında gündüzleri aşırı ısı ve güneş radyasyonu, geceleri dondurucu soğuk başlar. Oyuncu Ay üzerinden **Mars'a** kaçmak zorundadır.
2. **Aşama 2 — Mars'ın Baharı (Terraforming):**
   - `ServerLevel` bazlı `PersistentState` üzerinde `Heat`, `Pressure`, `Oxygen` ve `Terraforming Index (Ti)` tutulur.
   - Enerji tüketen makineler (*Thermal Heater*, *Atmospheric Pressure Drill*, *Flora Oxygenator*, *Atmosphere Condenser*) ile Mars ısıtılır ve basınçlandırılır.
   - Gökyüzü kızıldan maviye döner, buzlar erir, çimenler ve mutasyonlu bitkiler yayılır, oksijen %100 olduğunda `LevelOxygenAccessor` / Mixin ile kasksız nefes alma açılır, meteor yağmurları durur.
3. **Aşama 3 — İkinci Kıyamet (Kızıl Dev & Güneş Fırtınaları):**
   - Güneş Kızıl Dev evresine geçerek büyür ve kızıllaşır. Güneş rüzgarları Mars atmosferini uzaya sıyırmaya başlar (`Ti`, Oksijen ve Basınç geriye düşer; gökyüzü tekrar turuncu-kızıla döner).
   - Periyodik **Güneş Fırtınası (CME)** olayları gerçekleşir. **Solar Telemetry Monitor** yaklaşan fırtınayı geri sayımla bildirir. Oyuncu **Kurşun-Takviyeli Beton Sığınak / Kurşun Saklanma Kapsülü** içinde veya **Ağır Kurşun Zırh** giyerek radyasyondan korunur.
   - Mars artık kalıcı ev değil, yalnızca biyosfer bitkilerinden damıtılan **egzotik biyo-yakıtı** üretmek için ayakta tutulan geçici bir üs haline gelir.
4. **Aşama 4 — Wormhole Stabilizer (Multiblock):**
   - Yüksek enerji ve soğutma dengesi gerektiren çok bloklu uzay-zaman bükücü inşa edilir. **Derin Uzay Teleskobu** ile Güneş Sistemi dışındaki tek sığınak bölgesi olan **Erebus Tekilliği (Kara Delik Sistemi)** koordinatları çözülür.
5. **Aşama 5 — Erebus Kara Delik Sistemi & 3 Ötegezegen + Final:**
   - Özel `DimensionSpecialEffects` sky renderer ile güneş yerine **Erebus Kara Deliği** (olay ufku, dönen yığılma diski ve kütleçekimsel merceklenme kemeri) gökyüzünde görünür.
   - **Rota A — Hephaestus-IV:** Lav nehirleri, eski keşif enkazları/günlükleri → **Termal Kalkanlı Zırh** → Kavrulmuş Dünya sığınaklarından **Füzyon Çekirdeği** kurtarılır.
   - **Rota B — Pelagia-9:** Gökyüzünde dev Erebus Kara Deliği asılı derin okyanus gezegeni → **Basınç Dayanımlı Zırh** → Abis tabanından **Biyo-Stabilizatör** alınır.
   - **Rota C — Nyx-0:** Zifiri karanlıkta donmuş serseri gezegen → Yeraltı kristallerinden **Kuantum Çekirdeği** üretilir.
   - **Final:** 3 anahtar bileşenle solucan deliği menzili genişletilip **Sanctuary (Yeni Yuva)** gezegenine son kapı açılır.

---

## 3. Performans, TPS ve FPS Koruma Mimarisi

- **Dünya / Mars İklim Hesaplamaları:** Oyuncu başına her tick yerine **10 veya 20 tick'te bir** periyodik kontrol; gökyüzü görüş kontrolü için `Level.canSeeSky` + hafif cache kullanımı.
- **Buz Erimesi ve Çimenleşme:** Tüm dünyayı taramak kesinlikle yasaktır. Tick başına sabit **blok bütçesi (ör. max 8–16 blok/tick)** sadece aktif yüklü chunk'larda işlenir. Yeni yüklenen chunk'larda mevcut `Ti` seviyesine göre tek seferlik **Lazy Catch-up** uygulanır; yapay yapılara taşma engellenir.
- **Unloaded Makineler:** `PersistentState` içinde aktif makinelerin net üretim/tüketim oranı tutularak chunk yüklenmemişken gereksiz block-entity tick yükü oluşturmadan deterministik ilerleme sağlanır.
- **Kara Delik (Erebus) Rendering & FPS:** Pahalı ray-marching shader yerine hafif **katmanlı vertex/quad mesh + önceden üretilmiş yüksek kaliteli doku + isteğe bağlı hafif GLSL shader** kullanılır; FPS düşüşü minimumda kalır ve Iris/Sodium kullanan sistemlerle çakışmaz.
- **Ağ Trafiği (Network Sync):** `Ti`, sıcaklık ve fırtına geri sayımı her tick senkronize edilmez; sadece anlamlı değişimde (`delta > eşik` veya 1–2 saniyede bir) kompakt S2C packet gönderilir.

---

## 4. Telif ve Açık Kaynak Politikası

- **Lisans:** MIT License (Galacticraft 5 ve Fabric ekosistemiyle tam uyumlu).
- **Telifsiz İsimlendirme ve Varlıklar:** Film/oyun markalarından doğrudan kopya isim, doku, model veya ses kullanılmaz (ör. *Gargantua* yerine *Erebus Singularity*, *Vegetube* yerine *Flora Oxygenator*). Tüm blok/eşya dokuları, modeller ve hikaye günlükleri proje içinde sıfırdan oluşturulur.

---

## 5. Modüler Paket Yapısı

```text
src/
├── main/java/com/galacticpurpose/
│   ├── GalacticPurpose.java          # Mod entrypoint & genel başlatma
│   ├── config/                       # Konfigürasyon (Grace period, fırtına, bütçeler)
│   ├── compat/                       # SpaceCompat arayüzü, GC5 adaptörü, fallback
│   ├── climate/                      # Overworld çöküşü, Mars Solar Storm & radyasyon
│   ├── terraforming/                 # PersistentState, Ti hesabı, bütçeli dünya dönüşümü
│   ├── registry/                     # ModBlocks, ModItems, ModBlockEntities, ModEntities
│   └── wormhole/                     # Multiblock, stabilite/enerji mantığı, portal
├── client/java/com/galacticpurpose/client/
│   ├── GalacticPurposeClient.java    # Client entrypoint
│   ├── sky/                          # Mars Ti gökyüzü dönüşümü & Erebus Kara Deliği renderer
│   └── hud/                          # Termometre, radyasyon, fırtına sayacı ve Ti HUD
└── main/resources/
    ├── fabric.mod.json
    ├── galacticpurpose.mixins.json
    └── assets/galacticpurpose/lang/  # en_us.json & tr_tr.json
```
