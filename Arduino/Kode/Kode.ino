#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <Wire.h>
#include <Adafruit_ADS1X15.h>
#include <OneWire.h>
#include <DallasTemperature.h>

// ===================== WIFI =====================
#define WIFI_SSID "free"
#define WIFI_PASSWORD ""

// ===================== FIREBASE CONFIG =====================
#define API_KEY "AIzaSyAgqd7izjsF4dj32FDUjGe3KYnl4JQxONA"
#define DATABASE_URL "cleanlake-7cb91-default-rtdb.firebaseio.com"
#define DATABASE_SECRET "c2HNnan45yE2BpDOsTABHhiK950qsnEtzOpbSBub"

// ===================== LOKASI =====================
// String lokasi = "Yoka"; 
// String lokasi = "Batas_Kota";
String lokasi = "Yobeh"; 

// ===================== SENSOR =====================
#define ONE_WIRE_BUS D4
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);
Adafruit_ADS1115 ads;

// ===================== FIREBASE OBJECTS =====================
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// ===================== SETUP =====================
void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println();
  Serial.print("🔌 Menghubungkan ke WiFi");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println("\n✅ WiFi Terhubung!");
  Serial.println(WiFi.localIP());

  // 🔥 Konfigurasi Firebase
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  config.signer.tokens.legacy_token = DATABASE_SECRET;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  ads.begin();
  ads.setGain(GAIN_ONE); // ±4.096V cocok untuk sensor pH 0–3.3V
  sensors.begin();

  Serial.println("🔥 Sistem siap membaca sensor...");
}

// ===================== LOOP =====================
void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("⚠️ WiFi terputus, mencoba koneksi ulang...");
    WiFi.reconnect();
    delay(2000);
    return;
  }

  // Baca ADC dari sensor
  int16_t adc0 = ads.readADC_SingleEnded(0); // pH
  int16_t adc1 = ads.readADC_SingleEnded(1); // TDS
  int16_t adc2 = ads.readADC_SingleEnded(2); // Turbidity

  // Hitung nilai sensor
  float phValue = roundTo1(mapPh(adc0));
  float tdsValue = roundTo1(mapTDS(adc1));
  float turbValue = roundTo1(mapTurbidity(adc2));

  sensors.requestTemperatures();
  float suhu = roundTo1(sensors.getTempCByIndex(0));

  // Tampilkan hasil
  Serial.println("===============================");
  Serial.println("Lokasi: " + lokasi);
  Serial.printf("pH          : %.1f\n", phValue);
  Serial.printf("TDS (ppm)   : %.1f\n", tdsValue);
  Serial.printf("Kekeruhan   : %.1f\n", turbValue);
  Serial.printf("Suhu (°C)   : %.1f\n", suhu);

  // Kirim ke Firebase
  String basePath = "/Lokasi/" + lokasi;

  if (Firebase.ready()) {
    bool ok = true;
    ok &= Firebase.setFloat(fbdo, basePath + "/pH", phValue);
    ok &= Firebase.setFloat(fbdo, basePath + "/TDS", tdsValue);
    ok &= Firebase.setFloat(fbdo, basePath + "/Kekeruhan", turbValue);
    ok &= Firebase.setFloat(fbdo, basePath + "/Suhu", suhu);

    if (ok) {
      Serial.println("✅ Data berhasil dikirim ke Firebase!");
    } else {
      Serial.print("❌ Gagal kirim data: ");
      Serial.println(fbdo.errorReason());
    }
  } else {
    Serial.println("⚠️ Firebase belum siap!");
  }

  delay(10000); // kirim setiap 10 detik
}

// ===================== FUNGSI PEMBULATAN =====================
float roundTo1(float value) {
  return roundf(value * 10) / 10.0;
}

// ===================== FUNGSI KALIBRASI SENSOR =====================
float mapPh(int adcValue) {
  float voltage = adcValue * 0.000125; 
  float Vneutral = 2.56;             
  float Vslope   = 0.16;           
  float ph = 7 + ((Vneutral - voltage) / Vslope);
  return constrain(ph, 0, 14);
}

float mapTDS(int adcValue) {
  float voltage = adcValue * 0.000125;
  float tdsValue = (133.42 * pow(voltage, 3) 
                  - 255.86 * pow(voltage, 2) 
                  + 857.39 * voltage) * 0.5;
  return tdsValue;
}

float mapTurbidity(int adcValue) {
  float voltage = adcValue * 0.000125;
  float ntu = 3000 - (voltage * 1000);
  return max(0.0, ntu / 100.0);
}
