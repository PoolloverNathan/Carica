package poollovernathan.figura.carica

import com.sun.net.httpserver.*
import kotlinx.html.*
import java.net.InetSocketAddress
import kotlin.reflect.full.*


@Suppress("unused")
fun init() {
    val svr = HttpServer.create(InetSocketAddress("0.0.0.0", 38293), 3)
    svr.createContext("/") { it.handleRequest() }
    svr.start()
    println("Carica Documentation Viewer is running: http://carica.localhost:38293/docs")
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun HttpExchange.handleRequest() = when {
    requestMethod != "GET" -> replyWith(405) {
        html {
            head {
                stdhead()
                title("405 Method Not Allowed")
            }
            body {
                h1 {
                    +"405 Method Not Allowed"
                }
                p {
                    +"Cannot $requestMethod"
                }
            }
        }
    }
    requestURI.path.startsWith("/@/") -> {
        object {}.javaClass.classLoader.getResourceAsStream("assets/carica/www/" + requestURI.path.drop(3))?.run {
            val bytes = readAllBytes()
            sendResponseHeaders(200, bytes.size.toLong())
            responseBody.runUsing {
                write(bytes)
                flush()
            }
        } ?: display404()
    }
    requestURI.path == "/docs" -> replyWith(200) {
        html {
            head {
                stdhead()
                title("Carica Documentation Viewer")
            }
            body { generateDocs() }
        }
    }
    else -> display404()
}

