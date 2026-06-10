#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <Wire.h>
#include <Adafruit_ADS1X15.h>
#include <OneWire.h>
#include <DallasTemperature.h>

// =====================================================
// WIFI
// =====================================================
#define WIFI_SSID "free"
#define WIFI_PASSWORD "123456789"

// =====================================================
// FIREBASE CONFIG
// Isi kembali sesuai Firebase kamu
// =====================================================
#define API_KEY "AIzaSyAgqd7izjsF4dj32FDUjGe3KYnl4JQxONA"
#define DATABASE_URL "cleanlake-7cb91-default-rtdb.firebaseio.com"
#define DATABASE_SECRET "c2HNnan45yE2BpDOsTABHhiK950qsnEtzOpbSBub"

// =====================================================
// LOKASI
// =====================================================
// String lokasi = "Yoka";
String lokasi = "Batas_Kota";
// String lokasi = "Yobeh";

// =====================================================
// PIN NODEMCU ESP8266
// =====================================================
#define ONE_WIRE_BUS D4   // DS18B20 DATA
#define SDA_PIN D2        // ADS1115 SDA
#define SCL_PIN D1        // ADS1115 SCL

// =====================================================
// SENSOR OBJECT
// =====================================================
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);
Adafruit_ADS1115 ads;

// =====================================================
// FIREBASE OBJECT
// =====================================================
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// =====================================================
// KONFIGURASI ADS1115
// =====================================================
// GAIN_ONE = ±4.096V
// Resolusi ADS1115 = 0.125mV = 0.000125V
const float ADS_LSB = 0.000125;

// Naikkan sampel agar nilai pH lebih stabil
const int SAMPLE_COUNT = 25;

// =====================================================
// KALIBRASI pH SOFTWARE - SUDAH DISESUAIKAN
// =====================================================
// Dari log kamu:
// Air mineral masuk ke sensor berada sekitar 2.796V - 2.975V.
// Kita ambil tengahnya: 2.90V dianggap pH 7.0.
//
// Cuka/asam stabil sekitar 3.304V dengan pH sekitar 3.8.
// Maka: 3.304V dianggap pH 3.8.
//
// Jika setelah upload air mineral belum pH 7,
// ubah PH_NEUTRAL_VOLT sesuai Volt A0 saat sensor masuk air mineral.
//
// Jika setelah upload cuka belum pH 3.8,
// ubah PH_ACID_VOLT sesuai Volt A0 saat sensor masuk cuka.
const float PH_NEUTRAL_VALUE = 7.00;
const float PH_NEUTRAL_VOLT  = 2.900;

const float PH_ACID_VALUE    = 3.80;
const float PH_ACID_VOLT     = 3.304;

// Batas keamanan dan diagnosa
const float PH_VOLT_TOO_HIGH = 3.20;
const float PH_VOLT_DANGER   = 3.30;

// Jika hasil pH sangat ekstrem, biasanya probe di udara / belum stabil
const float PH_INVALID_LOW   = 0.5;
const float PH_INVALID_HIGH  = 13.5;

// =====================================================
// KALIBRASI TDS
// =====================================================
// Kalau pakai cairan standar TDS, contoh 342 ppm:
// TDS_K_VALUE = 342 / hasil_sensor
const float TDS_K_VALUE = 1.00;
const float TDS_TEMP_COEF = 0.02;

// =====================================================
// KALIBRASI KEKERUHAN
// =====================================================
// Dari log terakhir A2 sekitar 1.446V - 1.502V.
// Jika itu air jernih, pakai clear sekitar 1.50V.
const float TURB_VOLT_CLEAR = 1.500;
const float TURB_NTU_CLEAR  = 0.00;

const float TURB_VOLT_DIRTY = 0.800;
const float TURB_NTU_DIRTY  = 100.00;

// =====================================================
// KALIBRASI SUHU
// =====================================================
const float TEMP_OFFSET = 0.00;

// =====================================================
// INTERVAL KIRIM FIREBASE
// =====================================================
const unsigned long SEND_INTERVAL = 10000; // 10 detik
unsigned long lastSendTime = 0;

// =====================================================
// STATUS SENSOR
// =====================================================
bool suhuValid = false;
bool phPerluKalibrasi = false;
bool phTeganganBahaya = false;
bool phTidakValid = false;

// =====================================================
// SETUP
// =====================================================
void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println();
  Serial.println("======================================");
  Serial.println(" SISTEM MONITORING KUALITAS AIR");
  Serial.println(" NodeMCU ESP8266 + ADS1115 + Firebase");
  Serial.println(" Kalibrasi pH Software Revisi");
  Serial.println("======================================");

  Wire.begin(SDA_PIN, SCL_PIN);

  Serial.println("Mengecek ADS1115...");
  if (!ads.begin()) {
    Serial.println("❌ ADS1115 tidak terdeteksi!");
    Serial.println("Cek kabel:");
    Serial.println("NodeMCU D2 -> SDA ADS1115");
    Serial.println("NodeMCU D1 -> SCL ADS1115");
    Serial.println("NodeMCU 3V3 -> VDD ADS1115");
    Serial.println("NodeMCU GND -> GND ADS1115");

    while (1) {
      delay(1000);
    }
  }

  ads.setGain(GAIN_ONE);
  Serial.println("✅ ADS1115 terdeteksi.");

  sensors.begin();
  Serial.println("✅ DS18B20 dimulai.");

  connectWiFi();

  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  config.signer.tokens.legacy_token = DATABASE_SECRET;

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  Serial.println("🔥 Firebase siap.");
  Serial.println("✅ Sistem siap membaca sensor.");
}

// =====================================================
// LOOP
// =====================================================
void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("⚠️ WiFi terputus. Mencoba koneksi ulang...");
    WiFi.reconnect();
    delay(2000);
    return;
  }

  if (millis() - lastSendTime >= SEND_INTERVAL) {
    lastSendTime = millis();

    // Baca semua channel ADS1115
    float rawA0 = readADSRawAverage(0);
    float rawA1 = readADSRawAverage(1);
    float rawA2 = readADSRawAverage(2);
    float rawA3 = readADSRawAverage(3);

    float voltA0 = rawToVoltage(rawA0);
    float voltA1 = rawToVoltage(rawA1);
    float voltA2 = rawToVoltage(rawA2);
    float voltA3 = rawToVoltage(rawA3);

    // Mapping sensor
    float phVoltage   = voltA0;
    float tdsVoltage  = voltA1;
    float turbVoltage = voltA2;

    // Baca suhu
    float suhu = readTemperature();
    float suhuUntukTDS = suhuValid ? suhu : 25.0;

    // Hitung sensor
    float phValue   = roundTo1(calculatePH(phVoltage));
    float tdsValue  = roundTo1(calculateTDS(tdsVoltage, suhuUntukTDS));
    float turbValue = roundTo1(calculateTurbidity(turbVoltage));

    if (suhuValid) {
      suhu = roundTo1(suhu);
    } else {
      suhu = 0.0;
    }

    // Reset status pH
    phPerluKalibrasi = false;
    phTeganganBahaya = false;
    phTidakValid = false;

    if (phVoltage >= PH_VOLT_TOO_HIGH) {
      phPerluKalibrasi = true;
    }

    if (phVoltage >= PH_VOLT_DANGER) {
      phTeganganBahaya = true;
    }

    if (phValue <= PH_INVALID_LOW || phValue >= PH_INVALID_HIGH) {
      phTidakValid = true;
    }

    printSensorData(
      rawA0, voltA0,
      rawA1, voltA1,
      rawA2, voltA2,
      rawA3, voltA3,
      phValue,
      tdsValue,
      turbValue,
      suhu
    );

    sendToFirebase(
      phValue,
      tdsValue,
      turbValue,
      suhu,
      rawA0, voltA0,
      rawA1, voltA1,
      rawA2, voltA2,
      rawA3, voltA3
    );
  }
}

// =====================================================
// WIFI CONNECT
// =====================================================
void connectWiFi() {
  Serial.print("🔌 Menghubungkan ke WiFi");

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int retry = 0;

  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
    retry++;

    if (retry > 40) {
      Serial.println();
      Serial.println("❌ WiFi gagal terhubung. Restart NodeMCU...");
      ESP.restart();
    }
  }

  Serial.println();
  Serial.println("✅ WiFi Terhubung!");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());
}

// =====================================================
// BACA RAW ADS1115 RATA-RATA
// =====================================================
float readADSRawAverage(uint8_t channel) {
  long totalAdc = 0;

  for (int i = 0; i < SAMPLE_COUNT; i++) {
    totalAdc += ads.readADC_SingleEnded(channel);
    delay(10);
  }

  return totalAdc / (float)SAMPLE_COUNT;
}

// =====================================================
// RAW ADS KE VOLT
// =====================================================
float rawToVoltage(float raw) {
  float voltage = raw * ADS_LSB;

  if (voltage < 0) {
    voltage = 0;
  }

  return voltage;
}

// =====================================================
// BACA SUHU DS18B20
// =====================================================
float readTemperature() {
  sensors.requestTemperatures();
  float temp = sensors.getTempCByIndex(0);

  if (temp == DEVICE_DISCONNECTED_C || temp < -50 || temp > 100) {
    suhuValid = false;
    Serial.println("⚠️ DS18B20 tidak terbaca. Cek kabel DATA D4 dan resistor 4.7K.");
    return 0.0;
  }

  suhuValid = true;
  return temp + TEMP_OFFSET;
}

// =====================================================
// HITUNG pH - KALIBRASI SOFTWARE 2 TITIK
// =====================================================
float calculatePH(float voltage) {
  // Rumus:
  // pH = m * voltage + b
  //
  // Karena sensor kamu:
  // voltage naik -> pH turun / makin asam

  float selisihVolt = PH_ACID_VOLT - PH_NEUTRAL_VOLT;

  if (fabs(selisihVolt) < 0.001) {
    return 7.0;
  }

  float slope = (PH_ACID_VALUE - PH_NEUTRAL_VALUE) / selisihVolt;
  float intercept = PH_NEUTRAL_VALUE - (slope * PH_NEUTRAL_VOLT);

  float ph = (slope * voltage) + intercept;

  return constrainFloat(ph, 0.0, 14.0);
}

// =====================================================
// HITUNG TDS
// =====================================================
float calculateTDS(float voltage, float temperature) {
  float compensationCoefficient = 1.0 + TDS_TEMP_COEF * (temperature - 25.0);

  if (compensationCoefficient <= 0) {
    compensationCoefficient = 1.0;
  }

  float compensationVoltage = voltage / compensationCoefficient;

  float tds = (
    133.42 * pow(compensationVoltage, 3)
    - 255.86 * pow(compensationVoltage, 2)
    + 857.39 * compensationVoltage
  ) * 0.5;

  tds = tds * TDS_K_VALUE;

  if (tds < 0) {
    tds = 0;
  }

  return tds;
}

// =====================================================
// HITUNG KEKERUHAN
// =====================================================
float calculateTurbidity(float voltage) {
  float selisihVolt = TURB_VOLT_DIRTY - TURB_VOLT_CLEAR;

  if (fabs(selisihVolt) < 0.001) {
    return 0.0;
  }

  float slope = (TURB_NTU_DIRTY - TURB_NTU_CLEAR) / selisihVolt;
  float intercept = TURB_NTU_CLEAR - (slope * TURB_VOLT_CLEAR);

  float ntu = (slope * voltage) + intercept;

  if (ntu < 0) {
    ntu = 0;
  }

  return ntu;
}

// =====================================================
// TAMPILKAN DATA SERIAL MONITOR
// =====================================================
void printSensorData(float rawA0, float voltA0,
                     float rawA1, float voltA1,
                     float rawA2, float voltA2,
                     float rawA3, float voltA3,
                     float phValue,
                     float tdsValue,
                     float turbValue,
                     float suhu) {
  Serial.println();
  Serial.println("======================================");
  Serial.println("Lokasi: " + lokasi);

  Serial.println("--------- CEK ADS1115 ---------");
  Serial.printf("ADS A0 / pH        Raw: %.0f | Volt: %.3f V\n", rawA0, voltA0);
  Serial.printf("ADS A1 / TDS       Raw: %.0f | Volt: %.3f V\n", rawA1, voltA1);
  Serial.printf("ADS A2 / Kekeruhan Raw: %.0f | Volt: %.3f V\n", rawA2, voltA2);
  Serial.printf("ADS A3 / Cadangan  Raw: %.0f | Volt: %.3f V\n", rawA3, voltA3);

  Serial.println("--------- HASIL SENSOR ---------");
  Serial.printf("pH             : %.1f\n", phValue);
  Serial.printf("TDS            : %.1f ppm\n", tdsValue);
  Serial.printf("Kekeruhan      : %.1f NTU/skala\n", turbValue);

  if (suhuValid) {
    Serial.printf("Suhu           : %.1f °C\n", suhu);
  } else {
    Serial.println("Suhu           : ERROR / sensor tidak terbaca");
  }

  Serial.println("--------- KALIBRASI pH AKTIF ---------");
  Serial.printf("Air mineral/netral : %.1f pH = %.3f V\n", PH_NEUTRAL_VALUE, PH_NEUTRAL_VOLT);
  Serial.printf("Cuka/asam          : %.1f pH = %.3f V\n", PH_ACID_VALUE, PH_ACID_VOLT);

  Serial.println("--------- DIAGNOSA ---------");

  if (phTidakValid) {
    Serial.println("ℹ️ pH ekstrem. Jika probe di udara, nilai pH memang tidak valid.");
    Serial.println("   pH hanya valid saat probe terendam cairan.");
  }

  if (phPerluKalibrasi) {
    Serial.println("⚠️ pH: Tegangan A0 tinggi / mendekati batas.");
    Serial.println("   Masih bisa terbaca, tapi sebaiknya jangan melebihi 3.3V.");
  }

  if (phTeganganBahaya) {
    Serial.println("⚠️ pH: Tegangan mendekati/lebih dari 3.3V.");
    Serial.println("   Jika ADS1115 memakai 3.3V, gunakan pembagi tegangan agar aman.");
  }

  if (voltA1 < 0.05) {
    Serial.println("⚠️ TDS: Tegangan sangat kecil. Cek OUT TDS ke A1, VCC, dan GND.");
  }

  if (voltA2 < 0.05) {
    Serial.println("⚠️ Kekeruhan: Tegangan sangat kecil. Cek OUT sensor kekeruhan ke A2.");
  }

  if (voltA3 > 0.05) {
    Serial.println("ℹ️ A3 ada tegangan. Jika tidak dipakai, pin A3 kemungkinan floating.");
  }

  if (voltA0 > 3.3 || voltA1 > 3.3 || voltA2 > 3.3 || voltA3 > 3.3) {
    Serial.println("⚠️ Ada tegangan ADS di atas 3.3V.");
    Serial.println("   Hati-hati, ADS1115 3.3V tidak aman menerima tegangan lebih.");
  }
}

// =====================================================
// KIRIM DATA KE FIREBASE
// =====================================================
void sendToFirebase(float phValue,
                    float tdsValue,
                    float turbValue,
                    float suhu,
                    float rawA0, float voltA0,
                    float rawA1, float voltA1,
                    float rawA2, float voltA2,
                    float rawA3, float voltA3) {
  String basePath = "/Lokasi/" + lokasi;

  if (!Firebase.ready()) {
    Serial.println("⚠️ Firebase belum siap!");
    return;
  }

  FirebaseJson json;

  // Data utama
  json.set("pH", phValue);
  json.set("TDS", tdsValue);
  json.set("Kekeruhan", turbValue);
  json.set("Suhu", suhu);

  // Data ADS
  json.set("ADS/A0_pH/Raw", rawA0);
  json.set("ADS/A0_pH/Volt", voltA0);

  json.set("ADS/A1_TDS/Raw", rawA1);
  json.set("ADS/A1_TDS/Volt", voltA1);

  json.set("ADS/A2_Kekeruhan/Raw", rawA2);
  json.set("ADS/A2_Kekeruhan/Volt", voltA2);

  json.set("ADS/A3_Cadangan/Raw", rawA3);
  json.set("ADS/A3_Cadangan/Volt", voltA3);

  // Kalibrasi aktif
  json.set("Kalibrasi_pH/PH_NEUTRAL_VALUE", PH_NEUTRAL_VALUE);
  json.set("Kalibrasi_pH/PH_NEUTRAL_VOLT", PH_NEUTRAL_VOLT);
  json.set("Kalibrasi_pH/PH_ACID_VALUE", PH_ACID_VALUE);
  json.set("Kalibrasi_pH/PH_ACID_VOLT", PH_ACID_VOLT);

  // Status sensor
  json.set("Status/pH", getPHStatus(phValue, voltA0));
  json.set("Status/TDS", getTDSStatus(tdsValue, voltA1));
  json.set("Status/Kekeruhan", getTurbidityStatus(turbValue));
  json.set("Status/Suhu", suhuValid ? getTemperatureStatus(suhu) : "Sensor Error");

  // Diagnosa
  json.set("Diagnosa_pH/Perlu_Kalibrasi", phPerluKalibrasi);
  json.set("Diagnosa_pH/Tegangan_Bahaya", phTeganganBahaya);
  json.set("Diagnosa_pH/Tidak_Valid", phTidakValid);
  json.set("Diagnosa_pH/Catatan", getPHDiagnosticNote(phValue, voltA0));

  // Info
  json.set("Info/Lokasi", lokasi);
  json.set("Info/WiFi_IP", WiFi.localIP().toString());
  json.set("Info/Millis", millis());

  if (Firebase.updateNode(fbdo, basePath, json)) {
    Serial.println("✅ Data berhasil dikirim ke Firebase!");
  } else {
    Serial.print("❌ Gagal kirim data: ");
    Serial.println(fbdo.errorReason());
  }
}

// =====================================================
// STATUS pH
// =====================================================
String getPHStatus(float ph, float voltage) {
  if (phTidakValid) {
    return "Tidak Valid / Probe Mungkin di Udara";
  }

  if (voltage >= PH_VOLT_DANGER) {
    return "Asam - Tegangan Tinggi";
  }

  if (ph < 6.5) {
    return "Asam";
  }

  if (ph > 8.5) {
    return "Basa";
  }

  return "Normal";
}

// =====================================================
// CATATAN DIAGNOSA pH
// =====================================================
String getPHDiagnosticNote(float ph, float voltage) {
  if (phTidakValid) {
    return "Nilai pH ekstrem. Abaikan jika probe sedang di udara. pH hanya valid saat probe terendam cairan.";
  }

  if (voltage >= PH_VOLT_DANGER) {
    return "Tegangan pH mendekati/melebihi 3.3V. Gunakan pembagi tegangan agar ADS1115 aman.";
  }

  if (voltage >= PH_VOLT_TOO_HIGH) {
    return "Tegangan pH tinggi, tetapi masih bisa dipakai untuk pembacaan asam. Lebih aman gunakan pembagi tegangan.";
  }

  return "Pembacaan pH normal berdasarkan kalibrasi software.";
}

// =====================================================
// STATUS TDS
// =====================================================
String getTDSStatus(float tds, float voltage) {
  if (voltage < 0.05) {
    return "Sensor TDS Belum Terbaca";
  }

  if (tds <= 300) {
    return "Baik";
  }

  if (tds <= 600) {
    return "Sedang";
  }

  if (tds <= 1000) {
    return "Tinggi";
  }

  return "Sangat Tinggi";
}

// =====================================================
// STATUS KEKERUHAN
// =====================================================
String getTurbidityStatus(float ntu) {
  if (ntu <= 5) {
    return "Jernih";
  }

  if (ntu <= 25) {
    return "Agak Keruh";
  }

  if (ntu <= 100) {
    return "Keruh";
  }

  return "Sangat Keruh";
}

// =====================================================
// STATUS SUHU
// =====================================================
String getTemperatureStatus(float suhu) {
  if (suhu < 20) {
    return "Dingin";
  }

  if (suhu <= 32) {
    return "Normal";
  }

  return "Panas";
}

// =====================================================
// PEMBULATAN 1 ANGKA
// =====================================================
float roundTo1(float value) {
  return roundf(value * 10) / 10.0;
}

// =====================================================
// BATAS NILAI FLOAT
// =====================================================
float constrainFloat(float value, float minValue, float maxValue) {
  if (value < minValue) {
    return minValue;
  }

  if (value > maxValue) {
    return maxValue;
  }

  return value;
}