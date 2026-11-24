#include "wp/game.h"

#include <iterator>

namespace wp::game {

namespace {

// Rhythm Heaven Fever no usa módulos REL "sideways" como Wii Party. Lista vacía.
constexpr const char* kSidewaysModules[] = {""};

// TODO: revisar al primer intento. "boot", "inst" y "mg" son nombres de Wii Party:
// si el juego pide un módulo que no existe, hay que ajustarlos.
constexpr Description kDescription = {
    "Rhythm Heaven Fever",
    "rhythmheaven",
    "games/rhf/extracted",
    "games/rhf/nand",
    "games/rhf/settings.ini",
    "games/rhf/textures",
    "boot",
    kSidewaysModules,
    0,
    "inst",
    "mg",
};

}

const Description& description() {
    return kDescription;
}

}
