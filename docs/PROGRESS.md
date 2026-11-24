# Registro de progreso

Orden cronológico de lo que se hizo y qué enseñó cada paso.

1. **Identificación del disco.** `boot.bin`: ID `SOME01`, NTSC-U, título RHYTHM HEAVEN FEVER.
2. **Ejecutable.** `main.dol`: entrada `0x80004050`, secciones de código y datos, bases SDA
   `r13 = 0x80326EC0` y `r2 = 0x80320FE0` (verificadas contra las secciones de símbolos).
3. **Mapa de símbolos.** 22.017 símbolos, 8.103 funciones, todas dentro del código del DOL.
4. **Primer intento con DriftDroid (Mario Kart).** Traducción completa (8.141 funciones) y compilación.
   Bloqueos encontrados, en orden:
   - Entrada fijada a Mario Kart (`0x800060A4`). Corregida a `0x80004050`.
   - Registros de hardware bloqueados por diseño (`0xCC003004`, PI). Se resolvió con memoria compartida.
   - Lectura de DSP bloqueada, después espera infinita sin lecturas: el juego esperaba respuesta de IOS.
   - Conclusión: las emulaciones del runtime estaban registradas en direcciones de Mario Kart.
5. **Cambio de base al motor de Wii Party.** Su motor es genérico y las sustituciones se enlazan por nombre.
   Bloqueos resueltos, en orden:
   - `rfi` sin soporte en funciones de configuración de memoria. Se sustituyeron por funciones que devuelven cero
     (prefijo `HleZero_`). Pasó de 38 a 31 instrucciones sin soporte.
   - Nombres con `\` en el mapa (`__sinit_\ExScene_cpp`) rompían el código C++ generado. Se reemplazó `\` por `_`.
   - `SelectThread` era `OSSelectThread` para el motor. Se renombró, sin añadirlo a las sustituciones
     (el motor la llama como código del juego).
   - Resultado: **el juego abre en PC** (`rhf.exe`) y pasa la inicialización de IOS.
6. **Pendiente en PC.** Verificar que el menú responde al teclado y que el juego avanza sin cerrarse.
