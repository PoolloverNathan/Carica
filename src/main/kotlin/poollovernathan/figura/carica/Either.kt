@file:Suppress("unused")

package poollovernathan.figura.carica

sealed class Either<out L, out R> {
    abstract fun <C> fold(a: (L) -> C, b: (R) -> C): C

    private class Left<L>(val left: L): Either<L, Void>() {
        override fun <C> fold(a: (L) -> C, b: (Void) -> C) = a(left)
    }
    private class Right<R>(val right: R): Either<Void, R>() {
        override fun <C> fold(a: (Void) -> C, b: (R) -> C) = b(right)
    }
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun<L, R> makeLeft (left: L):  Either<L, R> = Left(left)   as Either<L, R>
        @Suppress("UNCHECKED_CAST")
        fun<L, R> makeRight(right: R): Either<L, R> = Right(right) as Either<L, R>
    }
}

inline val<L: Any, R>      Either<L, R>.left    get() = fold({ it    }, { null  })
inline val<L,      R: Any> Either<L, R>.right   get() = fold({ null  }, { it    })
inline val<L,      R>      Either<L, R>.isLeft  get() = fold({ true  }, { false })
inline val<L,      R>      Either<L, R>.isRight get() = fold({ false }, { true  })
inline fun<L, R, L1, R1> Either<L, R>.bimap(crossinline f: (L) -> L1, crossinline g: (R) -> R1): Either<L1, R1> = fold({ Either.makeLeft(f(it)) }, { Either.makeRight(g(it)) })
inline fun<L, R, T> Either<L, R>.mapLeft (crossinline f: (L) -> T): Either<T, R> = bimap({ f(it) }, { it })
inline fun<L, R, T> Either<L, R>.mapRight(crossinline f: (R) -> T): Either<L, T> = bimap({ it }, { f(it) })
@Suppress("NOTHING_TO_INLINE")
inline fun<T, R> Either<T, T>.foldIndifferent(noinline f: (T) -> R) = fold(f, f)
fun <L, R, C> Either<L, R>.runFold(a: L.() -> C, b: R.() -> C) = fold({ it.a() }, { it.b() })