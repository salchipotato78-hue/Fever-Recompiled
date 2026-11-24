// Temporales: estas funciones existían en las direcciones de Mario Kart Wii y el runtime
// las llama directamente. Rhythm Heaven Fever no tiene nada en esas direcciones, así que
// se definen vacías para que el programa enlace. Su comportamiento no es el real: son
// puntos de partida para la prueba de arranque, no la solución final.
#include "abi_bridge.h"
#include "ppc_runtime.h"

extern "C" {
void func_8012B830(CpuContext* ctx) { (void)ctx; }
void func_801A0620(CpuContext* ctx) { (void)ctx; }
void func_801A1ED8(CpuContext* ctx) { (void)ctx; }
void func_801A961C(CpuContext* ctx) { (void)ctx; }
void func_801AADE0(CpuContext* ctx) { (void)ctx; }
void func_801D8D30(CpuContext* ctx) { (void)ctx; }
void func_801D9E94(CpuContext* ctx) { (void)ctx; }
void func_8055531C(CpuContext* ctx) { (void)ctx; }
}
