# Cambios del primer hito (DVD / OS / VI) — NO COMPILADOS

Probado: solo el header runtime/include/guest_abi/guest_region.h con g++ (identidad para Rhf y PAL).
Sin probar: traductor C# (no hay dotnet en el entorno), runtime completo, build Android.

1. runtime/include/guest_abi/guest_region.h: región Rhf = 3 (identidad), kCount = 4, Facts de SOME01
   (letra E, gameCode 'SOME', viTvFormat NTSC = 0). mem1ArenaLo es placeholder igual a NTSC-U, sin verificar.
2. translator/src/Translator.Core/GuestAddressTable.cs: enum Rhf = 3, "SOME01" => Rhf, Map identidad para Rhf.
3. Literales de override (dvd.cpp, os_init.cpp, os_reset.cpp, os_interrupt.cpp): 12 funciones de DVD/OS
   remapeadas a direcciones de SOME01 (ver projects/rhythmheaven-some01/remap_candidates.csv).
   Los nombres de función conservan el sufijo MKW (p. ej. DVDInit_8015EA1C); es cosmético.
4. Pendiente: VIWaitForRetrace (dirección MKW no localizada en el runtime), override de VIGetTvFormat
   (la literal 801BACD8 se remapeó en su variante minúscula; confirmar que el registro existe),
   regeneración de la traducción y prueba en dispositivo.

## Segundo hito: remapeo de overrides a SOME01 (sin compilar en Android)

Verificado contra Assets/main.dol (SHA-256 e6ebeb5c…2367ff): todas las direcciones de override caen dentro del DOL.
Las 60 filas `resolved` de remap_candidates.csv coinciden con el nombre de MAP.txt en la dirección SOME01.

1. 49 PPC_NATIVE_OVERRIDE cambiados de dirección MKW a dirección SOME01 (según el CSV resuelto):
   ios.cpp (1), audio/audio.cpp (13), os/os_interrupt.cpp (12), os/os_init.cpp (16),
   os/os_cache.cpp (4), os/os_reset.cpp (1), os/os_scheduler.cpp (1), vi.cpp (1).
   Los nombres de función conservan el sufijo MKW (cosmético).
2. VIWaitForRetrace: override en 0x800F1400 (antes 801B99EC), según MAP.txt.
3. VIGetTvFormat: añadido PPC_NATIVE_OVERRIDE en 0x800F2710 (antes no estaba enlazado).

Pendiente (no tocado en este hito):
- ~500 overrides de la lista needs_review del CSV (sc.cpp, bt_sysconf.cpp, esp.cpp, vi.cpp, pad.cpp,
  gx_stub_*, y otros) siguen en direcciones MKW. En SOME01 no apuntan a la función correcta.
- Constantes de datos MKW_GADDR(...) (p. ej. kViRetraceCountAddr 80386be4) no están remapeadas;
  son direcciones de datos y no están en el CSV.
- Job Android (Gradle/NDK): falta tomarlo de NSMB android/, que no está en este repo.
- No se pudo compilar: el entorno no tiene el toolchain de Android, SDL3 ni aurora.

## Tercer hito: needs_review resueltos por nombre base (sin compilar)

4. 79 overrides (esp, sc, ios, vi, bt_sysconf, nand_async, nand_api, os_scheduler, os_init, os_thread,
   os_alarm, os_cache, os_sleep) remapeados a SOME01. El destino sale de MAP.txt quitando los sufijos
   _HLE, _stub y _XXXXXXXX del nombre del CSV. Son coincidencias únicas. 14 destinos no tienen prólogo
   claro en el DOL (parecen funciones hoja); conviene probarlos en dispositivo.
5. No hay direcciones duplicadas entre overrides.

Pendiente tras este hito: 412 overrides en direcciones MKW. 205 son gx_fatal_stubs.cpp (stubs sin nombre
SDK), y el resto son gx_* (stubs), nand_isfs, audio, pad, dvd, os_context, os_time, y otros. No tienen nombre
en MAP.txt, así que no se pueden remapear por nombre. Como el DOL de MKW no está en el repo, tampoco hay
con qué comparar firmas. Mientras sigan así, en SOME01 estos stubs pueden engancharse a funciones del juego.

## Cuarto hito: validación con main_1.dol (Mario Kart Wii) y 3 remapeos de gx_fatal_stubs

Entrada: /mnt/user-data/uploads/main_1.dol (sha256 d2beec1b…5995694, 2749248 bytes). Verificado como
MKW por cadenas (RevoKartR.SMAP, gamestats.nintendowifi). Las direcciones MKW de los 60 resueltos
contienen el código esperado (p. ej. AIInitDMA en 80123FCC escribe a CC005030).

6. Intento de remapeo por firma de código (MKW -> SOME01): 1 de 54 correctos de forma única. La
   compilación de cada juego cambia prólogos y registros, así que no es fiable. No se aplicó.
7. gx_fatal_stubs.cpp: 3 overrides remapeados por nombre exacto en MAP.txt (GX__CPInterruptHandler,
   GX__SetBreakPtCallback, GX__SetBreakPtCallback variante 800FE480). Se descartaron 2 destinos duplicados
   y 10 sin prólogo de función.

Pendiente: ~402 overrides GX fatal stubs y otros siguen en direcciones MKW. Sus nombres internos
(__GX__Shutdown, GX__EnableBreakPt, etc.) no están en MAP.txt. Decisión abierta: quitar esos overrides
(el código traducido de SOME01 se ejecutaría en vez de un stub que hace fallo) o dejarlos.

## Quinto hito: overrides MKW desactivados

8. 409 overrides que seguían en direcciones MKW sin destino en SOME01 se desactivaron. Cada línea es ahora
   un comentario `/* sin enlace: dirección MKW XXXXXXXX pendiente de remapear */`. El nombre de la función
   y su cuerpo se conservan. Así el juego ejecuta el código traducido de SOME01 en esa dirección, en lugar
   de enganchar un stub MKW o una función equivocada. Incluye 7 macros multilínea.
9. Ningún override activo queda en una dirección MKW pendiente (verificado con búsqueda).

Verificación: la comprobación de sintaxis con g++ falla antes de llegar al código (error de cabecera del
entorno, igual que en el original), así que no valida estos cambios. Hay que compilar con el toolchain de
Android antes de probar.

Pendiente: remapear esos 409 cuando haya una fuente fiable (un mapa MKW->SOME01 del decomp). El job Android
sigue sin existir (falta NSMB android/). Constantes MKW_GADDR sin remapear.

## Sexto hito: cruce con RMCE01 (símbolos de Mario Kart Wii)

Entrada: /mnt/user-data/uploads/RMCE01.txt (sha256 4e6777…a1ab9f2, 4237 símbolos).

10. 17 overrides de los 409 desactivados se reactivaron en su dirección SOME01. Criterio: nombre base
    igual en MAP.txt (SOME01) con una sola coincidencia, destino con prólogo de función en main.dol de SOME01,
    y sin destinos repetidos. Los 392 restantes siguen desactivados con su comentario.
11. Hallazgo importante: las direcciones MKW de los overrides no coinciden con RMCE01. Ejemplos:
    AIInitDMA está en 80123F2C en RMCE01 (la repo lo tenía en 80123FCC, que RMCE01 llama AIGetDMAStartAddr);
    GXDrawDone está en 8016EA10 en RMCE01 (la repo lo tenía en 8016EAB0). Eso significa que las direcciones
    MKW de la repo vienen de otro build, y la confirmación por dirección no sirve. Solo se usó el nombre.
    Lo mismo aplica a la columna mkw_addr del CSV.
12. RMCE01 solo tiene símbolos de funciones (pos/len/sym). No hay símbolos de datos, así que los
    MKW_GADDR siguen sin remapear.

Pendiente: 392 overrides sin nombre SDK en MAP.txt o sin prólogo claro en SOME01 (muchos son stubs GX internos).

## Séptimo hito: symbols.txt de SOME01 (conhlee/rhf)

Entrada: symbols.txt de config/SOME01 (8103 funciones, mismo número que MAP.txt).

13. 90 overrides más reactivados. Fuente: nombre base en symbols.txt (único, tamaño > 0). symbols.txt ya
    marca tipo función y tamaño, así que no se exigió prólogo. Se omitieron fragmentos caseD/switch.
14. Corrección: una restauración anterior dejó 8 líneas con dos GX_FATAL_STUB y el override apuntando
    a la dirección MKW. Se corrigieron en gx_fatal_stubs.cpp (líneas 165 y similares).

Pendiente: los fragmentos caseD/switch (186) siguen sin override a propósito. Hay ~200 overrides sin nombre
SDK ni nombre en symbols.txt. Constantes MKW_GADDR sin remapear. Job de Android pendiente.

## Octavo hito: MKW-only desactivado o remapeado (sin compilar)

15. src/hle/sc.cpp: SCGetProductArea_HLE devuelve 1 (USA) y SCGetProductGameRegion_HLE devuelve 'E'.
    Antes leían la tabla de región de MKW (MKW_GADDR(8029CEB0/8029CEF8)) dentro de la memoria de SOME01.
    Limitación: solo sirve para SOME01 (USA). Para otras regiones hace falta la tabla real de SOME01.
16. src/system_bridge.cpp: los constructores estáticos del DOL usan el rango de SOME01 (_ctors 0x801F74E0 a
    _dtors 0x801F7580, según symbols.txt). Antes usaban MKW_GADDR(80244DE0/80244EA0).
17. src/system_bridge.cpp: el bloque de constructores de StaticR.rel queda con rango vacío (0 a 0), porque
    SOME01 no tiene StaticR.rel.
18. LookupProductRegion (sc.cpp) queda sin llamadas; no se borró.

Pendiente: los 150 usos de MKW_GADDR en la capa HLE (GX, OS, VI, DVD, NAND, AX, DSP, scheduler) siguen
apuntando a direcciones MKW. Módulos de host (dynamic_aspect, friend_code_keyboard, boot_race) no se tocaron.

## Noveno hito: scheduler (inicio de la capa HLE)

19. src/fiber_manager.cpp: kSchedulerPendingFlagAddr pasa de MKW_GADDR(80386920) a 0x803206C0, que es
    RunQueueBits en symbols.txt de SOME01. La semántica coincide: bit (31-prioridad) indica cola no vacía.
Sin resolver todavía (no hay símbolo confiable en SOME01):
- kThreadQueueArrayAddr (MKW_GADDR(803477b0)): array RunQueue de 32 colas. No aparece con nombre.
- kSchedulerReschedCounterAddr (MKW_GADDR(8038691c)) y kSchedulerIdleFlagAddr (MKW_GADDR(80386918)):
  posible mapeo a Reschedule/RunQueueHint, pero no verificado con código de SOME01.
