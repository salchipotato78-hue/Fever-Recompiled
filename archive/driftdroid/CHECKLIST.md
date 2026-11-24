# Checklist

## Hecho
- [x] Identificar el juego: `SOME01`, NTSC-U
- [x] Verificar `main.dol` (SHA-256, entrada `0x80004050`, secciones)
- [x] Localizar un mapa de funciones compatible (8.103 funciones dentro del código)
- [x] Crear la estructura del repositorio
- [x] Calcular las bases SDA (`r13` = 0x80326EC0, `r2` = 0x80320FE0), verificadas contra las secciones de símbolos
- [x] Convertir el mapa de símbolos al formato del traductor (`tools/symbols_to_map.py`)

## Pendiente
- [ ] Verificar el origen y la licencia del mapa de funciones antes de usarlo
- [ ] Clonar DriftDroid como base del traductor y del runtime
- [ ] Ejecutar el traductor sobre el DOL y revisar errores de instrucciones no soportadas
- [ ] Compilar la versión de escritorio (Linux) para probar que el código arranca
- [ ] Retirar de la configuración todo lo específico de Mario Kart (Retro Rewind, Retro-WFC, Rooms, Ranks, Mii)
- [ ] Adaptar la entrada: toques de pantalla en lugar de la conducción de MKW
- [ ] Empaquetar la APK para Android
