@file:Suppress("NOTHING_TO_INLINE")

package poollovernathan.figura.carica

import com.sun.net.httpserver.HttpExchange
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.lang.reflect.Method
import java.lang.reflect.Modifier

inline fun <A, B: Any> Collection<Pair<A, B?>>.liftSecondNull() = mapNotNull { it.second?.let { it2 -> it.first to it2 } }
inline fun <E, F> Collection<E>.pairWith(rhs: (E) -> F) = map { it to rhs(it) }
inline fun HttpExchange.replyWith(status: Int, makeBody: TagConsumer<String>.() -> Unit) {
    createHTML(false).apply {
        runCatching {
            makeBody()
        }.runOnFailure {
            printStackTrace()
            // basically redo the entire function
            // since this is inline, we can't recurse
            createHTML(false).apply {
                html {
                    head {
                        title("Internal Server Error")
                    }
                    body {
                        pre {
                            +stackTraceToString()
                        }
                    }
                }
                finalize().encodeToByteArray().run {
                    sendResponseHeaders(500, size.toLong())
                    responseBody.apply {
                        write(this@run)
                        flush()
                    }
                }
            }
            return // rocks fall. everyone dies.
        }
        finalize().encodeToByteArray().run {
            sendResponseHeaders(status, size.toLong())
            responseBody.apply {
                write(this@run)
                flush()
            }
        }
    }
}

inline fun <T> Result<T>.runOnFailure(f: Throwable.() -> Unit): Result<T> = onFailure { f(it) }

inline fun HttpExchange.display404() = replyWith(404) {
    html {
        head {
            title("404 Not Found")
        }
        body {
            h1 {
                +"404 Not Found"
            }
            p {
                +requestURI.path
            }
        }
    }
}

@Suppress("SpellCheckingInspection", "NOTHING_TO_INLINE")
inline fun HEAD.stdhead() {
    // link("localhost", "preconnect")
    // link("/@/Miracode.ttf", "preload")
    link("/@/styles.css", "stylesheet") {
        id = "Stylesheet"
    }
    // script {
    //     unsafe {
    //         +"""
    //             setInterval(() => {
    //                 Stylesheet.href = "/@/styles.css?" + Date.now()
    //             }, 5000)
    //         """.trimIndent()
    //     }
    // }
}

inline fun <T: AutoCloseable, R> T.runUsing(body: T.() -> R) = use(body)

@Suppress("unused")
@JvmInline
value class ModifierSet(val mod: Int) {
    inline val isPublic       get() = Modifier.isPublic(mod)
    inline val isProtected    get() = Modifier.isProtected(mod)
    inline val isPrivate      get() = Modifier.isPrivate(mod)
    inline val isAbstract     get() = Modifier.isAbstract(mod)
    inline val isStatic       get() = Modifier.isStatic(mod)
    inline val isFinal        get() = Modifier.isFinal(mod)
    inline val isTransient    get() = Modifier.isTransient(mod)
    inline val isVolatile     get() = Modifier.isVolatile(mod)
    inline val isSynchronized get() = Modifier.isSynchronized(mod)
    inline val isNative       get() = Modifier.isNative(mod)
    inline val isStrict       get() = Modifier.isStrict(mod)
    inline val isInterface    get() = Modifier.isInterface(mod)
}

inline val Method.modifierSet get() = ModifierSet(modifiers)
inline fun <K, V> Map<K, V>.runForEach(f: Map.Entry<K, V>.() -> Unit) = forEach(f)
inline fun <K> K.foundIn(map: Map<K, Any>) = map[this] != null
inline fun <K, V> K.getFrom(map: Map<K, V>): V? = map[this]