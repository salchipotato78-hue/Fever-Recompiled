#include "wp/fiber.h"

#ifdef _WIN32
#include <windows.h>
#elif defined(__ANDROID__) && defined(__aarch64__)
// Android arm64: el NDK no trae ucontext (getcontext/makecontext/swapcontext). Se usa un cambio de
// contexto en ensamblador, igual que el runtime de WarioRecomp (platform/android/co_switch.S).
#include <cstdint>
#include <cstdlib>
#else
#include <ucontext.h>

#include <cstdlib>
#endif

namespace wp::fiber {

#ifdef _WIN32

namespace {

struct Start {
    Entry entry;
    void* parameter;
};

thread_local Handle g_current = nullptr;

VOID CALLBACK run(PVOID data) {
    Start start = *static_cast<Start*>(data);
    delete static_cast<Start*>(data);
    start.entry(start.parameter);
}

}

Handle adopt_current_thread() {
    g_current = ConvertThreadToFiber(nullptr);
    return g_current;
}

Handle current() {
    return g_current;
}

Handle create(size_t stack_size, Entry entry, void* parameter) {
    return CreateFiber(stack_size, run, new Start{entry, parameter});
}

void switch_to(Handle target) {
    g_current = target;
    SwitchToFiber(target);
}

void destroy(Handle fiber) {
    DeleteFiber(fiber);
}

#elif defined(__ANDROID__) && defined(__aarch64__)

// Cambio de contexto para arm64 (copiado del runtime de WarioRecomp; AArch64 ELF, marco de 240 bytes).
// mkw_co_switch(destino, origen): guarda el SP actual en *origen y carga el SP de *destino.
// mkw_co_init(tope_de_pila, entrada, argumento): crea un marco inicial y devuelve su SP.
// x18 no se toca: en Android es el puntero de la shadow call stack del hilo.
asm(R"ASM(
    .text
    .p2align 2
    .globl mkw_co_switch
    .type mkw_co_switch, %function
mkw_co_switch:
    sub sp, sp, #240
    stp x19, x20, [sp, #16]
    stp x21, x22, [sp, #32]
    stp x23, x24, [sp, #48]
    stp x25, x26, [sp, #64]
    stp x27, x28, [sp, #80]
    stp x29, x30, [sp, #96]
    stp q8, q9, [sp, #112]
    stp q10, q11, [sp, #144]
    stp q12, q13, [sp, #176]
    stp q14, q15, [sp, #208]
    mov x2, sp
    str x2, [x1]

    ldr x2, [x0]
    mov sp, x2
    ldp x19, x20, [sp, #16]
    ldp x21, x22, [sp, #32]
    ldp x23, x24, [sp, #48]
    ldp x25, x26, [sp, #64]
    ldp x27, x28, [sp, #80]
    ldp x29, x30, [sp, #96]
    ldp q8, q9, [sp, #112]
    ldp q10, q11, [sp, #144]
    ldp q12, q13, [sp, #176]
    ldp q14, q15, [sp, #208]
    add sp, sp, #240
    ret
    .size mkw_co_switch, . - mkw_co_switch

    .globl mkw_co_init
    .type mkw_co_init, %function
mkw_co_init:
    bic x0, x0, #0xf
    sub x0, x0, #240
    str x1, [x0, #16]
    str x2, [x0, #24]
    str xzr, [x0, #96]
    adrp x3, mkw_co_entry_trampoline
    add x3, x3, :lo12:mkw_co_entry_trampoline
    str x3, [x0, #104]
    ret
    .size mkw_co_init, . - mkw_co_init

    .type mkw_co_entry_trampoline, %function
mkw_co_entry_trampoline:
    mov x0, x20
    blr x19
    brk #0
    .size mkw_co_entry_trampoline, . - mkw_co_entry_trampoline

    .section .note.GNU-stack, "", %progbits
)ASM");

extern "C" void mkw_co_switch(void** targetSp, void** sourceSp);
extern "C" void* mkw_co_init(void* stackTop, void (*entry)(void*), void* argument);

namespace {

struct Fiber {
    void* savedSp = nullptr;
    void* stack = nullptr;
    Entry entry = nullptr;
    void* parameter = nullptr;
};

thread_local Fiber* g_current = nullptr;

void run(void* argument) {
    Fiber* self = static_cast<Fiber*>(argument);
    self->entry(self->parameter);
    std::abort();
}

}

Handle adopt_current_thread() {
    g_current = new Fiber;
    return g_current;
}

Handle current() {
    return g_current;
}

Handle create(size_t stack_size, Entry entry, void* parameter) {
    Fiber* fiber = new Fiber;
    fiber->stack = std::malloc(stack_size);
    fiber->entry = entry;
    fiber->parameter = parameter;
    char* top = static_cast<char*>(fiber->stack) + stack_size;
    fiber->savedSp = mkw_co_init(top, run, fiber);
    return fiber;
}

void switch_to(Handle target) {
    Fiber* from = g_current;
    Fiber* to = static_cast<Fiber*>(target);
    g_current = to;
    mkw_co_switch(&to->savedSp, &from->savedSp);
}

void destroy(Handle fiber) {
    Fiber* target = static_cast<Fiber*>(fiber);
    std::free(target->stack);
    delete target;
}

#else

namespace {

struct Fiber {
    ucontext_t context;
    void* stack = nullptr;
    Entry entry = nullptr;
    void* parameter = nullptr;
};

thread_local Fiber* g_current = nullptr;
thread_local Fiber* g_starting = nullptr;

void run() {
    Fiber* self = g_starting;
    self->entry(self->parameter);
    std::abort();
}

}

Handle adopt_current_thread() {
    g_current = new Fiber;
    return g_current;
}

Handle current() {
    return g_current;
}

Handle create(size_t stack_size, Entry entry, void* parameter) {
    Fiber* fiber = new Fiber;
    fiber->stack = std::malloc(stack_size);
    fiber->entry = entry;
    fiber->parameter = parameter;
    getcontext(&fiber->context);
    fiber->context.uc_stack.ss_sp = fiber->stack;
    fiber->context.uc_stack.ss_size = stack_size;
    fiber->context.uc_link = nullptr;
    makecontext(&fiber->context, run, 0);
    return fiber;
}

void switch_to(Handle target) {
    Fiber* from = g_current;
    Fiber* to = static_cast<Fiber*>(target);
    g_current = to;
    g_starting = to;
    swapcontext(&from->context, &to->context);
}

void destroy(Handle fiber) {
    Fiber* target = static_cast<Fiber*>(fiber);
    std::free(target->stack);
    delete target;
}

#endif

}
