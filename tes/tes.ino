#include <Wire.h>
#include <Adafruit_ADS1X15.h>
#include <OneWire.h>
#include <DallasTemperature.h>

// === ADS1115 ===
Adafruit_ADS1115 ads;  // default address 0x48

// === DS18B20 Suhu Air ===
#define ONE_WIRE_BUS D3
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);

// Kalibrasi pH sensor
float calibration_value = 21.34;  // sesuaikan jika perlu

void setup() {
  Serial.begin(115200);
  Wire.begin(D2, D1);  // SDA = D2, SCL = D1

  if (!ads.begin()) {
    Serial.println("Gagal menemukan ADS1115. Periksa koneksi.");
    while (1);
  }

  sensors.begin();  // Inisialisasi suhu
}

void loop() {
  // === Baca nilai dari ADS1115 ===
  int16_t adc_ph = ads.readADC_SingleEnded(0);
  int16_t adc_tds = ads.readADC_SingleEnded(1);
  int16_t adc_turb = ads.readADC_SingleEnded(2);

  // Konversi tegangan (ADS1115: 16-bit, 0-3.3V default)
  float voltage_ph = adc_ph * 0.1875 / 1000.0;   // 0.1875mV/bit
  float voltage_tds = adc_tds * 0.1875 / 1000.0;
  float voltage_turb = adc_turb * 0.1875 / 1000.0;

  // === Konversi ke nilai sensor ===
  // pH Sensor (rumus bisa disesuaikan berdasarkan datasheet/kalibrasi)
  float phValue = 7 + ((2.5 - voltage_ph) / calibration_value);

  // TDS Sensor (rumus dari Gravity TDS sensor v1.0)
  float tdsValue = (133.42 * pow(voltage_tds, 3)) - (255.86 * pow(voltage_tds, 2)) + (857.39 * voltage_tds);

  // Turbidity Sensor (contoh: nilai tegangan tinggi = jernih, rendah = keruh)
  float turbidity = mapFloat(voltage_turb, 4.5, 2.5, 0, 100);  // Sesuaikan dengan sensor

  // === Baca Suhu Air ===
  sensors.requestTemperatures();
  float suhuAir = sensors.getTempCByIndex(0);

  // === Tampilkan hasil ===
  Serial.println("=== Sensor Air ===");
  Serial.print("pH: ");
  Serial.println(phValue, 2);

  Serial.print("TDS (ppm): ");
  Serial.println(tdsValue, 2);

  Serial.print("Turbidity (% jernih): ");
  Serial.println(turbidity, 2);

  Serial.print("Suhu Air (°C): ");
  Serial.println(suhuAir, 2);

  delay(500);
}

// Fungsi untuk mapping float (seperti map biasa)
float mapFloat(float x, float in_min, float in_max, float out_min, float out_max) {
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}
