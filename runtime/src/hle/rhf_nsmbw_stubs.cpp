// Build de Rhythm Heaven Fever (SOME01) solamente. El runtime llama a estas funciones de NSMBW
// (menú de pausa, red, efectos, caché de luz) que RHF no tiene. Son no-op: si alguna se llamara,
// no hace nada en vez de fallar el enlace. Se definen aquí porque no hay código traducido en esas
// direcciones de RHF.
#if defined(MKW_RHF_BUILD)

extern "C" void func_800B4DE0(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_800C90F0(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_800D0C00(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_800D0CB0(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_8015AC30(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_8015AD80(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_8015AE70(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_8015B030(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_801AB2F0(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_801ACDB0(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_801B6020(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_801ED3B0(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_8026C210(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_802C3CB0(struct CpuContext* ctx) { (void)ctx; }
extern "C" void func_802CD4B0(struct CpuContext* ctx) { (void)ctx; }

#endif  // MKW_RHF_BUILD
