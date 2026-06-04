# Código Arduino 
```c++
#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>

// ─── Pines ───────────────────────────────────────────────────────────────────
const int trigPin = 5;
const int echoPin = 4;
const int ledPin  = 15;

const int led100 = 16; 
const int led75  = 2;  
const int led50  = 14; 
const int led25  = 12; 
const int led0   = 13; 

// ─── Brillo de los LEDs ──────────────────────────────────────────────────────
const int BRILLO = 128; 

// ─── Firebase ────────────────────────────────────────────────────────────────
#define API_KEY      "AIzaSyBLxudNF_YsIF-sgwUULhvb-O65dpnjX3M"
#define DATABASE_URL "aquatracking-5bade-default-rtdb.firebaseio.com"

String userEmail    = "esp8266@gmail.com";
String userPassword = "12345678";
String moduleId     = "-O9AOGhgVPLt464eEULY";

FirebaseData   fbdo;
FirebaseAuth   auth;
FirebaseConfig config;

// ─── NTP ─────────────────────────────────────────────────────────────────────
const long   timeZone  = -5;
const char* ntpServer = "pool.ntp.org";

// ─── Variables de medicion ────────────────────────────────────────────────────
float distancia         = 0.0;
float porcentajeLlenado = -1.0;

float distanciaFiltrada = -1.0;
const float ALPHA = 0.25;

const float UMBRAL_OUTLIER     = 5.0; 
const int   MAX_RECHAZOS       = 3;    
int         rechazosConsecutivos = 0;

float porcentajeAnterior = -1.0;
const float DEADBAND = 1.0;

float temperaturaAmbiente = 20.0; 

const float MARGEN_LLENO = 0.5; 

// ─── Config dinamica desde Firebase ──────────────────────────────────────────
float alturaInstalacion = 16.0; 
float sensorNivelAgua   = 2.0;  
bool  configCargada     = false;

// ─── Tiempos ─────────────────────────────────────────────────────────────────
unsigned long sendDataPrevMillis = 0;
unsigned long lastConfigRead     = 0;

// ─── WiFi provisioning ───────────────────────────────────────────────────────
WiFiServer server(80);
String ssidInput     = "";
String passwordInput = "";

String pagina =
  "<!DOCTYPE html>"
  "<html lang='es'><head>"
  "<meta charset='utf-8'/>"
  "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>"
  "<title>AquaTrack - Configuracion Wi-Fi</title>"
  "<style>"
  "*{margin:0;padding:0;box-sizing:border-box;}"
  "body{"
    "min-height:100vh;"
    "display:flex;"
    "align-items:center;"
    "justify-content:center;"
    "background:#0f1923;"
    "font-family:'Segoe UI',system-ui,sans-serif;"
    "padding:16px;"
  "}"
  ".card{"
    "background:#162330;"
    "border:1px solid #1e3448;"
    "border-radius:16px;"
    "padding:36px 32px;"
    "width:100%;"
    "max-width:400px;"
    "box-shadow:0 8px 32px rgba(0,0,0,0.4);"
  "}"
  ".logo{"
    "display:flex;"
    "align-items:center;"
    "gap:10px;"
    "margin-bottom:28px;"
  "}"
  ".logo-icon{"
    "width:38px;height:38px;"
    "background:linear-gradient(135deg,#1a9fd4,#0d6a8f);"
    "border-radius:10px;"
    "display:flex;align-items:center;justify-content:center;"
    "font-size:20px;"
  "}"
  ".logo-text{"
    "font-size:20px;"
    "font-weight:700;"
    "color:#e8f4fa;"
    "letter-spacing:0.5px;"
  "}"
  ".logo-text span{color:#1a9fd4;}"
  "h2{"
    "font-size:15px;"
    "font-weight:400;"
    "color:#5a7a8f;"
    "margin-bottom:24px;"
    "line-height:1.4;"
  "}"
  ".field{margin-bottom:16px;}"
  "label{"
    "display:block;"
    "font-size:12px;"
    "font-weight:600;"
    "color:#5a7a8f;"
    "letter-spacing:0.8px;"
    "text-transform:uppercase;"
    "margin-bottom:6px;"
  "}"
  "input[type=text],"
  "input[type=password]{"
    "width:100%;"
    "padding:12px 14px;"
    "background:#0f1923;"
    "border:1px solid #1e3448;"
    "border-radius:8px;"
    "color:#e8f4fa;"
    "font-size:15px;"
    "outline:none;"
    "transition:border-color 0.2s;"
    "-webkit-appearance:none;"
  "}"
  "input:focus{border-color:#1a9fd4;}"
  "input::placeholder{color:#2a4a5e;}"
  "button{"
    "width:100%;"
    "padding:14px;"
    "margin-top:8px;"
    "background:linear-gradient(135deg,#1a9fd4,#0d6a8f);"
    "border:none;"
    "border-radius:8px;"
    "color:#fff;"
    "font-size:15px;"
    "font-weight:600;"
    "letter-spacing:0.5px;"
    "cursor:pointer;"
    "-webkit-appearance:none;"
  "}"
  ".divider{height:1px;background:#1e3448;margin:24px 0;}"
  ".hint{font-size:12px;color:#2a4a5e;text-align:center;}"
  "</style></head><body>"
  "<div class='card'>"
  "<div class='logo'>"
  "<div class='logo-icon'>&#x1F4A7;</div>"
  "<div class='logo-text'>Aqua<span>Track</span></div>"
  "</div>"
  "<h2>Conecta el dispositivo a tu red Wi-Fi para comenzar el monitoreo.</h2>"
  "<form action='/connect' method='GET'>"
  "<div class='field'>"
  "<label for='ssid'>Red Wi-Fi (SSID)</label>"
  "<input type='text' id='ssid' name='ssid' placeholder='Nombre de tu red' autocomplete='off'/>"
  "</div>"
  "<div class='field'>"
  "<label for='password'>Contrasena</label>"
  "<input type='password' id='password' name='password' placeholder='&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;' autocomplete='off'/>"
  "</div>"
  "<button type='submit'>Conectar</button>"
  "</form>"
  "<div class='divider'></div>"
  "<p class='hint'>AquaTrack &mdash; Monitor de nivel de agua</p>"
  "</div></body></html>";

// ─────────────────────────────────────────────────────────────────────────────
// Prototipos
// ─────────────────────────────────────────────────────────────────────────────
float   leerDistancia();
String getCurrentDate();
void   leerConfigFirebase();
void   verificarCalibracion();
void   actualizarLEDs();
void   iniciarFirebase();
void   ledOn(int pin);
void   ledOff(int pin);

// ─────────────────────────────────────────────────────────────────────────────
// SETUP
// ─────────────────────────────────────────────────────────────────────────────
void setup() {
  Serial.begin(115200);
  analogWriteRange(255);

  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
  pinMode(ledPin,  OUTPUT);
  pinMode(led100,  OUTPUT);
  pinMode(led75,   OUTPUT);
  pinMode(led50,   OUTPUT);
  pinMode(led25,   OUTPUT);
  pinMode(led0,    OUTPUT);

  ledOff(led100);
  ledOff(led75);
  ledOff(led50);
  ledOff(led25);
  ledOff(led0);

  WiFi.softAP("AQUATRACK", "12345678");
  Serial.println("[WiFi] Punto de acceso iniciado.");
  digitalWrite(ledPin, HIGH);

  server.begin();
  configTime(timeZone * 3600, 0, ntpServer);
  Serial.println("[Server] Servidor web iniciado.");
}

// ─────────────────────────────────────────────────────────────────────────────
// LOOP
// ─────────────────────────────────────────────────────────────────────────────
void loop() {

  // ── Provisioning WiFi ──────────────────────────────────────────────────────
  WiFiClient client = server.available();
  if (client) {
    Serial.println("[Server] Cliente conectado.");
    String header      = "";
    String currentLine = "";

    for (int i = 0; i < 2; i++) {
      digitalWrite(ledPin, HIGH); delay(500);
      digitalWrite(ledPin, LOW);  delay(500);
    }

    while (client.connected()) {
      if (client.available()) {
        char c = client.read();
        header += c;

        if (c == '\n') {
          if (currentLine.length() == 0) {
            client.println("HTTP/1.1 200 OK");
            client.println("Content-type:text/html");
            client.println("Connection: close");
            client.println();

            if (header.indexOf("GET /connect?") >= 0) {
              int ssidIndex     = header.indexOf("ssid=") + 5;
              int passwordIndex = header.indexOf("password=") + 9;
              ssidInput     = header.substring(ssidIndex, header.indexOf("&", ssidIndex));
              passwordInput = header.substring(passwordIndex, header.indexOf(" ", passwordIndex));
              ssidInput.replace("+", " ");
              passwordInput.replace("+", " ");

              Serial.println("[WiFi] Conectando a: " + ssidInput);
              WiFi.begin(ssidInput.c_str(), passwordInput.c_str());

              int attempts = 0;
              int ledState = LOW;
              while (WiFi.status() != WL_CONNECTED && attempts < 20) {
                ledState = (ledState == LOW) ? HIGH : LOW;
                digitalWrite(ledPin, ledState);
                delay(500);
                Serial.print(".");
                attempts++;
              }

              if (WiFi.status() == WL_CONNECTED) {
                Serial.println("\n[WiFi] Conectado. IP: " + WiFi.localIP().toString());
                client.println("<h2>Conexion exitosa</h2>");
                client.println("<p>IP: " + WiFi.localIP().toString() + "</p>");
                iniciarFirebase();
              } else {
                Serial.println("\n[WiFi] No se pudo conectar.");
                client.println("<h2>Error en la conexion</h2>");
                client.println("<p>No se pudo conectar a " + ssidInput + ".</p>");
              }
            } else {
              client.println(pagina);
            }

            client.println();
            break;
          } else {
            currentLine = "";
          }
        } else if (c != '\r') {
          currentLine += c;
        }
      }
    }
    client.stop();
    Serial.println("[Server] Cliente desconectado.");
  }

  // ── Ciclo de medicion y envio ──────────────────────────────────────────────
  if (WiFi.status() == WL_CONNECTED) {
    if (Firebase.ready() && (millis() - sendDataPrevMillis > 5000 || sendDataPrevMillis == 0)) {
      sendDataPrevMillis = millis();
      Serial.println("─────────────────────────────────");

      if (!configCargada || millis() - lastConfigRead > 60000) {
        leerConfigFirebase();
      }

      if (configCargada) {
        verificarCalibracion();
      }

      float lectura = leerDistancia();
      if (lectura <= 0) {
        Serial.println("[Sensor] Sin lectura valida. Se omite este ciclo.");
        return;
      }

      if (distanciaFiltrada >= 0 && fabs(lectura - distanciaFiltrada) > UMBRAL_OUTLIER) {
        rechazosConsecutivos++;
        if (rechazosConsecutivos >= MAX_RECHAZOS) {
          Serial.println("[Sensor] Cambio sostenido detectado. Filtro re-basado.");
          distanciaFiltrada    = lectura;
          rechazosConsecutivos = 0;
        } else {
          Serial.print("[Sensor] Outlier rechazado (");
          Serial.print(rechazosConsecutivos);
          Serial.print("/");
          Serial.print(MAX_RECHAZOS);
          Serial.print("): ");
          Serial.print(lectura);
          Serial.println(" cm");
        }
      } else {
        rechazosConsecutivos = 0;
        distanciaFiltrada = (distanciaFiltrada < 0)
                          ? lectura
                          : ALPHA * lectura + (1.0 - ALPHA) * distanciaFiltrada;
      }

      if (distanciaFiltrada < 0) {
        Serial.println("[Sensor] Sin valor estable aun. Esperando...");
        return;
      }

      distancia = distanciaFiltrada;
      Serial.print("[Sensor] Distancia filtrada: ");
      Serial.print(distancia, 2);
      Serial.println(" cm");

      float refLleno = sensorNivelAgua + MARGEN_LLENO;
      float porcentajeNuevo;
      if (distancia <= refLleno) {
        porcentajeNuevo = 100.0;
      } else if (distancia >= alturaInstalacion) {
        porcentajeNuevo = 0.0;
      } else {
        porcentajeNuevo = (alturaInstalacion - distancia)
                        / (alturaInstalacion - refLleno) * 100.0;
      }
      porcentajeNuevo = constrain(porcentajeNuevo, 0.0, 100.0);

      if (porcentajeAnterior >= 0 && fabs(porcentajeNuevo - porcentajeAnterior) < DEADBAND) {
        porcentajeLlenado = porcentajeAnterior;
      } else {
        porcentajeLlenado  = porcentajeNuevo;
        porcentajeAnterior = porcentajeNuevo;
      }

      Serial.print("[Sensor] Porcentaje: ");
      Serial.print(porcentajeLlenado, 1);
      Serial.println("%");

      actualizarLEDs();

      FirebaseJson json;
      json.set("porcentaje", String(porcentajeLlenado, 1));
      json.set("fecha",      getCurrentDate());

      String path = "/ModulesWifi/" + moduleId;
      Serial.print("[Firebase] Enviando a: ");
      Serial.println(path);

      if (Firebase.pushJSON(fbdo, path.c_str(), json)) {
        Serial.println("[Firebase] Enviado correctamente.");
      } else {
        Serial.print("[Firebase] Error: ");
        Serial.println(fbdo.errorReason());
      }
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Iniciar Firebase
// ─────────────────────────────────────────────────────────────────────────────
void iniciarFirebase() {
  config.api_key               = API_KEY;
  auth.user.email              = userEmail;
  auth.user.password           = userPassword;
  config.database_url          = DATABASE_URL;
  config.token_status_callback = tokenStatusCallback;

  Firebase.reconnectNetwork(true);
  Firebase.begin(&config, &auth);

  Serial.print("[Firebase] Autenticando");
  int attempts = 0;
  while (!Firebase.ready() && attempts < 15) {
    delay(1000);
    Serial.print(".");
    attempts++;
  }
  Serial.println();

  if (Firebase.ready()) {
    Serial.println("[Firebase] Autenticado.");
    leerConfigFirebase();
  } else {
    Serial.println("[Firebase] No se pudo autenticar. Se usaran valores por defecto.");
    configCargada = true;  
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Leer config desde Firebase
// ─────────────────────────────────────────────────────────────────────────────
void leerConfigFirebase() {
  String pathAltura = "/ModulesWifi/" + moduleId + "/config/alturaInstalacion";
  String pathSensor = "/ModulesWifi/" + moduleId + "/config/sensorNivelAgua";
  String pathTemp   = "/ModulesWifi/" + moduleId + "/config/temperatura";

  if (Firebase.getFloat(fbdo, pathAltura.c_str())) {
    alturaInstalacion = fbdo.floatData();
    Serial.print("[Config] alturaInstalacion: ");
    Serial.println(alturaInstalacion, 2);
  } else {
    Serial.print("[Config] alturaInstalacion no disponible. Usando: ");
    Serial.println(alturaInstalacion, 2);
  }

  if (Firebase.getFloat(fbdo, pathSensor.c_str())) {
    sensorNivelAgua = fbdo.floatData();
    Serial.print("[Config] sensorNivelAgua: ");
    Serial.println(sensorNivelAgua, 2);
  } else {
    Serial.print("[Config] sensorNivelAgua no disponible. Usando: ");
    Serial.println(sensorNivelAgua, 2);
  }

  if (Firebase.getFloat(fbdo, pathTemp.c_str())) {
    temperaturaAmbiente = fbdo.floatData();
    Serial.print("[Config] temperatura: ");
    Serial.println(temperaturaAmbiente, 1);
  }

  if (sensorNivelAgua >= alturaInstalacion) {
    Serial.println("[Config] ADVERTENCIA: sensorNivelAgua >= alturaInstalacion. Restaurando defaults.");
    alturaInstalacion = 16.0;
    sensorNivelAgua   = 2.0;
  }

  configCargada  = true;
  lastConfigRead = millis();
}

// ─────────────────────────────────────────────────────────────────────────────
// Verificar calibracion desde la app
// ─────────────────────────────────────────────────────────────────────────────
void verificarCalibracion() {

  // ── Calibrar LLENO (100%) ──
  String pathCalLleno = "/ModulesWifi/" + moduleId + "/config/calibrar";
  if (Firebase.getBool(fbdo, pathCalLleno.c_str()) && fbdo.boolData()) {
    Serial.println("[Calibrar] LLENO: midiendo...");
    float d = leerDistancia();
    if (d > 0) {
      String pathSensor = "/ModulesWifi/" + moduleId + "/config/sensorNivelAgua";
      if (Firebase.setFloat(fbdo, pathSensor.c_str(), d)) {
        sensorNivelAgua = d;
        Serial.print("[Calibrar] Nuevo sensorNivelAgua (lleno): ");
        Serial.println(d, 2);
      } else {
        Serial.print("[Calibrar] Error guardando lleno: ");
        Serial.println(fbdo.errorReason());
      }
    } else {
      Serial.println("[Calibrar] Lectura invalida (lleno).");
    }
    Firebase.setBool(fbdo, pathCalLleno.c_str(), false);
  }

  // ── Calibrar VACIO (0%) ──
  String pathCalVacio = "/ModulesWifi/" + moduleId + "/config/calibrarVacio";
  if (Firebase.getBool(fbdo, pathCalVacio.c_str()) && fbdo.boolData()) {
    Serial.println("[Calibrar] VACIO: midiendo...");
    float d = leerDistancia();
    if (d > 0) {
      String pathAltura = "/ModulesWifi/" + moduleId + "/config/alturaInstalacion";
      if (Firebase.setFloat(fbdo, pathAltura.c_str(), d)) {
        alturaInstalacion = d;
        Serial.print("[Calibrar] Nuevo alturaInstalacion (vacio): ");
        Serial.println(d, 2);
      } else {
        Serial.print("[Calibrar] Error guardando vacio: ");
        Serial.println(fbdo.errorReason());
      }
    } else {
      Serial.println("[Calibrar] Lectura invalida (vacio).");
    }
    Firebase.setBool(fbdo, pathCalVacio.c_str(), false);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers LED
// ─────────────────────────────────────────────────────────────────────────────
void ledOn(int pin) {
  if (pin == led100) digitalWrite(pin, LOW);
  else               analogWrite(pin, BRILLO);
}

void ledOff(int pin) {
  digitalWrite(pin, HIGH);
}

// ─────────────────────────────────────────────────────────────────────────────
// Actualizar LEDs 
// ─────────────────────────────────────────────────────────────────────────────
void actualizarLEDs() {
  ledOff(led100); ledOff(led75); ledOff(led50); ledOff(led25); ledOff(led0);

  if       (porcentajeLlenado >= 100) ledOn(led100);
  else if (porcentajeLlenado >= 75)  ledOn(led75);
  else if (porcentajeLlenado >= 50)  ledOn(led50);
  else if (porcentajeLlenado >  0)   ledOn(led25);
  else                               ledOn(led0);
}

// ─────────────────────────────────────────────────────────────────────────────
// Medir distancia con HC-SR04
// ─────────────────────────────────────────────────────────────────────────────
float leerDistancia() {
  const int N = 15;
  float lecturas[N];
  int   validas = 0;

  float velocidad = (331.3 + 0.606 * temperaturaAmbiente) / 10000.0;

  for (int i = 0; i < N; i++) {
    digitalWrite(trigPin, LOW);
    delayMicroseconds(2);
    digitalWrite(trigPin, HIGH);
    delayMicroseconds(10);
    digitalWrite(trigPin, LOW);

    long dur = pulseIn(echoPin, HIGH, 25000);
    if (dur > 0) {
      float d = dur * velocidad / 2.0;
      if (d >= 1.0 && d <= 400.0) lecturas[validas++] = d;
    }
    delay(30);
  }

  if (validas < 3) return -1.0;

  for (int i = 0; i < validas - 1; i++)
    for (int j = i + 1; j < validas; j++)
      if (lecturas[j] < lecturas[i]) {
        float t = lecturas[i]; lecturas[i] = lecturas[j]; lecturas[j] = t;
      }

  int recorte = validas / 5;
  float suma = 0.0;
  int   cuenta = 0;
  for (int i = recorte; i < validas - recorte; i++) {
    suma += lecturas[i];
    cuenta++;
  }

  if (cuenta == 0) return lecturas[validas / 2];  
  return suma / cuenta;
}

// ─────────────────────────────────────────────────────────────────────────────
// Fecha actual desde NTP
// ─────────────────────────────────────────────────────────────────────────────
String getCurrentDate() {
  time_t now = time(nullptr);
  struct tm* ti = localtime(&now);
  char fecha[11];
  strftime(fecha, sizeof(fecha), "%d/%m/%Y", ti);
  return String(fecha);
}
```
