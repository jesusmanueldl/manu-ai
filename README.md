# MANU — Modelo Autónomo Neural Útil

<img width="1122" height="1402" alt="MANU_infografia_QR_GitHub" src="https://github.com/user-attachments/assets/1179490c-5363-46c6-9c91-901c8052d6f6" />


Asistente conversacional local para Android. MANU combina un modelo GGUF
ejecutado en el dispositivo con herramientas nativas de Android y memoria
persistentemente local mediante SQLite.

## Qué incluye

- Chat Android nativo en Java.
- Inferencia local con Qwen 2.5 1.5B Instruct Q4_K_M y llama.cpp.
- Puente JNI para ARM64.
- Memoria de conversación en SQLite.
- Herramientas locales: RAM, almacenamiento, batería, CPU y aplicaciones.
- Apertura de aplicaciones reconocidas y preparación de SMS con confirmación.
- Gráfica visual de la inferencia y métrica aproximada de generación.
- Interfaz sin servidores y sin enviar conversaciones a internet.

## Qué no incluye

Por tamaño y licencias, este repositorio no contiene:

- Pesos del modelo GGUF.
- APKs, builds, logs o bases de datos.
- Datos personales o capturas del teléfono.
- La copia completa de llama.cpp.

Consulta [docs/REPLICATION.md](docs/REPLICATION.md) para descargar las
dependencias y reproducir el proyecto.

## Inicio rápido

```bash
git clone https://github.com/jesusmanueldl/manu-ai.git
cd manu-ai
git clone --depth 1 https://github.com/ggml-org/llama.cpp third_party/llama.cpp
```

Descarga el modelo por separado:

```powershell
python -m pip install -U huggingface_hub
.\scripts\download-model.ps1
```

Después abre el proyecto en Android Studio, sincroniza Gradle y ejecútalo en
un dispositivo Android ARM64 con depuración USB autorizada.

## Prompt del sistema

El prompt base de MANU está en
[`prompts/MANU_SYSTEM_PROMPT.txt`](prompts/MANU_SYSTEM_PROMPT.txt). Define la
identidad **Modelo Autónomo Neural Útil**, el comportamiento local, la
privacidad y la regla de no inventar acciones.

## Seguridad

MANU separa el modelo de las herramientas. El modelo propone o interpreta una
petición; la aplicación decide si una herramienta es válida y ejecuta la acción.
SMS, llamadas, borrados y cambios sensibles deben pedir confirmación explícita.

## Licencias

Revisa las licencias de este proyecto, Qwen y llama.cpp antes de redistribuir
una aplicación o un modelo. Los pesos se descargan directamente desde su
repositorio correspondiente y no se redistribuyen aquí.

## Estado

La versión actual es una base funcional para conversación local y herramientas
del dispositivo. La cámara, visión local, búsqueda web y automatizaciones
avanzadas quedan como fases posteriores.
