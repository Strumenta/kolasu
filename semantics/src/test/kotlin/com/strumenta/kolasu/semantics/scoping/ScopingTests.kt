package com.strumenta.kolasu.semantics.scoping

import com.strumenta.kolasu.model.Named
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

data class ASymbol(override val name: String, val index: Int = 0) : Named
data class BSymbol(override val name: String, val index: Int = 0) : Named

class ScopingTests {

    @Test
    fun scopeAddSymbols() {
        val theScope = scope {
            define("a", ASymbol("a", index = 0))
            define("b", BSymbol(name = "b", index = 0))
            define("b", ASymbol(name = "b", index = 1))
        }
        assertEquals(3, theScope.entries<Any>().count())
        assertEquals(2, theScope.entries<ASymbol>().count())
        assertEquals(1, theScope.entries<BSymbol>().count())
        assertEquals(1, theScope.entries<Any> { (name) -> name == "a" }.count())
        assertEquals(1, theScope.entries<ASymbol> { (name) -> name == "a" }.count())
        assertEquals(0, theScope.entries<BSymbol> { (name) -> name == "a" }.count())
        assertEquals(2, theScope.entries<Any> { (name) -> name == "b" }.count())
        assertEquals(1, theScope.entries<ASymbol> { (name) -> name == "b" }.count())
        assertEquals(1, theScope.entries<BSymbol> { (name) -> name == "b" }.count())
    }

    @Test
    fun lookupSymbolByNameInLocal() {
        val theScope = scope {
            define("a", ASymbol("a"))
            parents += scope {
                define("b", ASymbol("b"))
            }
        }
        assertEquals(ASymbol("a"), theScope.entries<Any> { (name) -> name == "a" }.firstOrNull())
    }

    @Test
    fun lookupSymbolByNameInParent() {
        val theScope = scope {
            define("a", ASymbol("a"))
            parents += scope {
                define("b", ASymbol("b"))
            }
        }
        assertEquals(ASymbol("b"), theScope.entries<Any> { (name) -> name == "b" }.firstOrNull())
    }

    @Test
    fun lookupSymbolByNameNotFound() {
        val theScope = scope {
            define("b", ASymbol("b"))
        }
        assertNull(theScope.entries<Any> { (name) -> name == "a" }.firstOrNull())
    }

    @Test
    fun lookupSymbolByNameAndTypeInLocal() {
        val theScope = scope {
            define("a", ASymbol("a"))
            parents += scope {
                define("a", BSymbol("b"))
            }
        }
        assertEquals(ASymbol("a"), theScope.entries<ASymbol> { (name) -> name == "a" }.firstOrNull())
    }

    @Test
    fun lookupSymbolByNameAndTypeInParent() {
        val theScope = scope {
            define("a", ASymbol("a"))
            parents += scope {
                define("a", BSymbol("a"))
            }
        }
        assertEquals(BSymbol("a"), theScope.entries<BSymbol> { (name) -> name == "a" }.firstOrNull())
    }

    @Test
    fun lookupSymbolByNameAndTypeNotFoundDifferentName() {
        val theScope = scope {
            define("a", ASymbol("a"))
        }
        assertNull(theScope.entries<ASymbol> { (name) -> name == "b" }.firstOrNull())
    }

    @Test
    fun lookupSymbolByNameAndTypeNotFoundDifferentType() {
        val theScope = scope {
            define("a", ASymbol("a"))
        }
        assertNull(theScope.entries<BSymbol> { (name) -> name == "a" }.firstOrNull())
    }
}
