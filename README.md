# Código Arduino 
```c++
#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#include <string>

const int trigPin = 5;
const int echoPin = 4;     // Pin Echo del sensor HC-SR04
const int ledPin = 15;

const int led100 = 16;  // D0
const int led75 = 2;    // D4
const int led50 = 14;   // D5
const int led25 = 12;   // D6
const int led0 = 13;    // D7

float distancia;           // Variable para almacenar la distancia medida por el sensor
float porcentajeLlenado = -1;   // Variable para almacenar el porcentaje de llenado

// Creamos el objeto servidor en el puerto 80
WiFiServer server(80);

// Variables para almacenar las credenciales ingresadas
String ssidInput = "";
String passwordInput = "";

// Página web HTML con un formulario para ingresar SSID y contraseña
String pagina = "<!DOCTYPE html>"
"<html>"
"<head>"
"<meta charset='utf-8' />"
"<title>Configuración Wi-Fi ESP8266</title>"
"</head>"
"<body>"
"<center>"
"<h1>Conectar a una Red Wi-Fi</h1>"
"<form action='/connect' method='GET'>"
"SSID: <input type='text' name='ssid'><br>"
"Contraseña: <input type='password' name='password'><br><br>"
"<input type='submit' value='Conectar'>"
"</form>"
"</center>"
"</body>"
"</html>";

#define API_KEY  "AIzaSyBLxudNF_YsIF-sgwUULhvb-O65dpnjX3M";
#define DATABASE_URL "aquatracking-5bade-default-rtdb.firebaseio.com";
String userEmail = "esp8266@gmail.com";
String userPassword = "12345678";
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;
const long timeZone = -5;
const char* ntpServer = "pool.ntp.org";
unsigned long sendDataPrevMillis = 0;

String porcentaje = "-1"; 

void setup() {
  Serial.begin(115200);

  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
  pinMode(ledPin, OUTPUT);  

  pinMode(led100, OUTPUT);
  pinMode(led75, OUTPUT);
  pinMode(led50, OUTPUT);
  pinMode(led25, OUTPUT);
  pinMode(led0, OUTPUT);

  digitalWrite(led100, HIGH);
  digitalWrite(led75, HIGH);
  digitalWrite(led50, HIGH);
  digitalWrite(led25, HIGH);
  digitalWrite(led0, HIGH);

  // Configuramos el ESP8266 como punto de acceso
  WiFi.softAP("AQUATRACK", "12345678"); // Nombre y contraseña del punto de acceso
  Serial.println("Punto de acceso iniciado.");
  digitalWrite(ledPin, HIGH);
  // Iniciamos el servidor
  server.begin();
  configTime(timeZone * 3600, 0, ntpServer);
  Serial.println("Servidor web iniciado.");
}
//comparti wifi prenda, 
//acepta la contraseña partpatedar 2 veces,
//

void loop() {
  // Esperamos a que haya un cliente conectado
  WiFiClient client = server.available();

  if (client) {
    Serial.println("Cliente conectado.");
    String header = "";
    String currentLine = "";

   
    for (int i = 0; i < 2; i++) { 
      digitalWrite(ledPin, HIGH); 
      delay(500);              
      digitalWrite(ledPin, LOW); 
      delay(500);               
    }

    while (client.connected()) {
      if (client.available()) {
        char c = client.read();
        header += c;

        // Si se encuentra un salto de línea significa que la solicitud HTTP está completa
        if (c == '\n') {
          // Si la línea está vacía, significa que es el fin del request HTTP del cliente
          if (currentLine.length() == 0) {
            // Enviamos la respuesta HTTP
            client.println("HTTP/1.1 200 OK");
            client.println("Content-type:text/html");
            client.println("Connection: close");
            client.println();

            // Verificamos si hay una solicitud para conectarse
            if (header.indexOf("GET /connect?") >= 0) {
              // Extraemos los valores de SSID y password desde la cabecera
              int ssidIndex = header.indexOf("ssid=") + 5;
              int passwordIndex = header.indexOf("password=") + 9;
              ssidInput = header.substring(ssidIndex, header.indexOf("&", ssidIndex));
              passwordInput = header.substring(passwordIndex, header.indexOf(" ", passwordIndex));

              // Reemplazamos caracteres de espacio en blanco (+)
              ssidInput.replace("+", " ");
              passwordInput.replace("+", " ");

              // Intentamos conectar a la red Wi-Fi con los valores ingresados
              Serial.println("Conectando a la red Wi-Fi:");
              Serial.println("SSID: " + ssidInput);
              Serial.println("Password: " + passwordInput);
              WiFi.begin(ssidInput.c_str(), passwordInput.c_str());

              // Esperamos a que se conecte
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
                // Conexión exitosa
                Serial.println("\nConectado a " + ssidInput);
                Serial.println("IP: " + WiFi.localIP().toString());
                client.println("<h2>Conexión exitosa</h2>");
                client.println("<p>Conectado a " + ssidInput + "</p>");
                client.println("<p>IP: " + WiFi.localIP().toString() + "</p>");
                config.api_key = API_KEY;
                auth.user.email = userEmail;
                auth.user.password = userPassword;
                config.database_url = DATABASE_URL;
                config.token_status_callback = tokenStatusCallback;

                Firebase.reconnectNetwork(true);
                Firebase.begin(&config, &auth);
              } else {
                // Error en la conexión
                Serial.println("\nNo se pudo conectar.");
                client.println("<h2>Error en la conexión</h2>");
                client.println("<p>No se pudo conectar a " + ssidInput + ". Intente de nuevo.</p>");
              }
            } else {
              // Si no se envió una solicitud de conexión, mostramos la página de configuración
              client.println(pagina);
            }

            // Fin de la respuesta HTTP
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

    // Cerramos la conexión con el cliente
    client.stop();
    Serial.println("Cliente desconectado.");
    //3veces
  }

  // Enviamos datos a Firebase si está conectado
  if (WiFi.status() == WL_CONNECTED) {
    if (Firebase.ready() && (millis() - sendDataPrevMillis > 5000 || sendDataPrevMillis == 0)) {

      sendDataPrevMillis = millis();
      distancia = leerDistancia();

      // Calcular el porcentaje de llenado en función de la distancia
      if (distancia <= 2.0) {
        porcentajeLlenado = 100.0; // Si está a 2 cm o menos, el tanque está lleno
      } else if (distancia >= 15.5) {
        porcentajeLlenado = 0.0;   // Si está a 15.5 cm o más, el tanque está vacío
      } else {
        // Calcular el porcentaje linealmente entre 2 cm y 10.5 cm
        porcentajeLlenado = (15.5 - distancia) / (15.5 - 2.0) * 100.0;
      }

      // Mostrar el porcentaje de llenado en el monitor serie
      Serial.print("Porcentaje de llenado: ");
      Serial.print(porcentajeLlenado);
      Serial.println("%");

      if (porcentajeLlenado >= 100) {
        digitalWrite(led100, LOW);   // LOW para encender (ánodo común)
        digitalWrite(led75, HIGH);   // HIGH para apagar
        digitalWrite(led50, HIGH);
        digitalWrite(led25, HIGH);
        digitalWrite(led0, HIGH);
      } else if (porcentajeLlenado >= 75) {
        digitalWrite(led100, HIGH);
        digitalWrite(led75, LOW);    // LOW para encender
        digitalWrite(led50, HIGH);
        digitalWrite(led25, HIGH);
        digitalWrite(led0, HIGH);
      } else if (porcentajeLlenado >= 50) {
        digitalWrite(led100, HIGH);
        digitalWrite(led75, HIGH);
        digitalWrite(led50, LOW);    // LOW para encender
        digitalWrite(led25, HIGH);
        digitalWrite(led0, HIGH);
      } else if (porcentajeLlenado >= 25) {
        digitalWrite(led100, HIGH);
        digitalWrite(led75, HIGH);
        digitalWrite(led50, HIGH);
        digitalWrite(led25, LOW);    // LOW para encender
        digitalWrite(led0, HIGH);
      } else {
        digitalWrite(led100, HIGH);
        digitalWrite(led75, HIGH);
        digitalWrite(led50, HIGH);
        digitalWrite(led25, HIGH);
        digitalWrite(led0, LOW);     // LOW para encender
      }
       
      FirebaseJson json;
        json.set("porcentaje", String(porcentajeLlenado));
        json.set("fecha", getCurrentDate());

        String moduleId = "-O9AOGhgVPLt464eEULY";
        String path = "/ModulesWifi/" + moduleId;

        if (Firebase.pushJSON(fbdo, path.c_str(), json)) {
            Serial.println("Datos enviados correctamente a Firebase.");
        } else {
            Serial.print("Error al enviar datos: ");
            Serial.println(fbdo.errorReason());
        }
            
    }
  }
}

float leerDistancia() {
  // Generar un pulso de 10 µs en el pin Trig
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  // Leer el tiempo que tarda el pulso en regresar al pin Echo
  long duracion = pulseIn(echoPin, HIGH);

  // Calcular la distancia en cm (velocidad del sonido es 0.034 cm/us)
  float distancia = duracion * 0.034 / 2;

  return distancia;
}

String getCurrentDate() {
  time_t now = time(nullptr);
  struct tm *timeInfo = localtime(&now);
  char fecha[11];
  strftime(fecha, sizeof(fecha), "%d/%m/%Y", timeInfo);
  return String(fecha);
}
```
