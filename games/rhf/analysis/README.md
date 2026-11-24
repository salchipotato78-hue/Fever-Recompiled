# analysis/

Esta carpeta NO trae los archivos generados (symbols.csv, dol_functions.csv, hle_functions.csv)
porque se construyen a partir de tu propio archivo de símbolos del juego.

Para generarlos, desde la raíz del clon de wiiparty-recomp:

    python games\rhf\tools\make_analysis.py ruta\al\symbols.txt games\rhf\analysis

Consulta el README principal para más detalles.
