"""Informe de mi búsqueda de empleo, a partir de GET /estadisticas.

Script auxiliar: solo usa la biblioteca estándar de Python (sin pip install).

Uso:
    1. Inicia sesión (POST /auth/login) y copia el "accessToken" (dura 15 minutos).
    2. export API_TOKEN="el-token"        (en PowerShell: $env:API_TOKEN = "el-token")
    3. python scripts/informe.py          (opcional: API_URL, por defecto http://localhost:8080)
"""

import json
import os
import sys
import urllib.error
import urllib.request

ANCHO_BARRA = 30


def pedir_estadisticas(url_base, token):
    """Llama a /estadisticas con el token y devuelve el JSON como diccionario."""
    peticion = urllib.request.Request(
        url_base.rstrip("/") + "/estadisticas",
        headers={"Authorization": "Bearer " + token},
    )
    try:
        with urllib.request.urlopen(peticion, timeout=60) as respuesta:
            return json.load(respuesta)
    except urllib.error.HTTPError as error:
        if error.code == 401:
            salir("El token no es válido o ha caducado (dura 15 minutos). Inicia sesión otra vez.")
        salir("La API respondió con el error " + str(error.code))
    except urllib.error.URLError as error:
        salir("No se pudo conectar con " + url_base + ": " + str(error.reason))


def porcentaje(parte, total):
    if total == 0:
        return "-"
    return str(round(parte * 100 / total)) + "%"


def barra(valor, total):
    if total == 0:
        return ""
    return "#" * round(valor * ANCHO_BARRA / total)


def mostrar_informe(datos):
    embudo = datos["embudo"]
    tiempo = datos["tiempoDeRespuesta"]
    total = embudo["postuladas"]

    print("Informe de mi búsqueda de empleo")
    print("================================")
    print("Postulaciones: " + str(total))
    if total == 0:
        print("Todavía no hay postulaciones.")
        return

    print()
    print("Respuestas de las empresas")
    print("  Respondieron:  " + str(tiempo["conRespuesta"]) + " (" + porcentaje(tiempo["conRespuesta"], total) + ")")
    print("  Sin respuesta: " + str(tiempo["sinRespuesta"]) + " (" + porcentaje(tiempo["sinRespuesta"], total) + ")")
    if tiempo["mediaDias"] is None:
        print("  Tiempo hasta la primera respuesta: todavía no ha respondido ninguna")
    else:
        print("  Tiempo hasta la primera respuesta: " + str(tiempo["mediaDias"]) + " días de media")
        print("  (solo cuentan las que respondieron; las demás no entran como 0 días)")

    print()
    print("Embudo (cuántas llegaron alguna vez a cada paso)")
    pasos = [
        ("Postuladas", embudo["postuladas"]),
        ("Respondieron", embudo["respondieron"]),
        ("Entrevista", embudo["entrevista"]),
        ("Oferta", embudo["oferta"]),
    ]
    for nombre, valor in pasos:
        print("  " + nombre.ljust(13) + str(valor).rjust(4) + "  " + barra(valor, total))


def salir(mensaje):
    print("Error: " + mensaje, file=sys.stderr)
    sys.exit(1)


def main():
    token = os.environ.get("API_TOKEN")
    if not token:
        salir("Falta la variable de entorno API_TOKEN (mira las instrucciones al principio del archivo).")
    url_base = os.environ.get("API_URL", "http://localhost:8080")
    mostrar_informe(pedir_estadisticas(url_base, token))


if __name__ == "__main__":
    main()
