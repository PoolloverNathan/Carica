package poollovernathan.figura.carica

import kotlinx.html.*
import net.minecraft.locale.Language
import org.figuramc.figura.lua.FiguraAPIManager
import org.figuramc.figura.lua.docs.LuaFieldDoc
import org.figuramc.figura.lua.docs.LuaMethodDoc
import org.figuramc.figura.lua.docs.LuaMethodOverload
import org.figuramc.figura.lua.docs.LuaTypeDoc
import org.figuramc.figura.lua.api.*
import org.figuramc.figura.lua.api.action_wheel.ActionWheelAPI
import org.figuramc.figura.lua.api.data.DataAPI
import org.figuramc.figura.lua.api.data.ResourcesAPI
import org.figuramc.figura.lua.api.entity.PlayerAPI
import org.figuramc.figura.lua.api.event.EventsAPI
import org.figuramc.figura.lua.api.json.JsonAPI
import org.figuramc.figura.lua.api.keybind.KeybindAPI
import org.figuramc.figura.lua.api.math.MatricesAPI
import org.figuramc.figura.lua.api.math.VectorsAPI
import org.figuramc.figura.lua.api.nameplate.NameplateAPI
import org.figuramc.figura.lua.api.net.NetworkingAPI
import org.figuramc.figura.lua.api.particle.ParticleAPI
import org.figuramc.figura.lua.api.ping.PingAPI
import org.figuramc.figura.lua.api.sound.SoundAPI
import org.figuramc.figura.lua.api.vanilla_model.VanillaModelAPI
import org.figuramc.figura.lua.api.world.WorldAPI
import org.figuramc.figura.math.vector.FiguraVec2
import org.figuramc.figura.math.vector.FiguraVec3
import org.figuramc.figura.math.vector.FiguraVec4
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

// val canonVars by lazy { FiguraAPIManager.API_GETTERS.map { it.value.apply(null)::class to it.key }.toMap() }
val canonVars = mapOf(
    ActionWheelAPI::class to "action_wheel",
    AnimationAPI::class to "animations",
    AvatarAPI::class to "avatar",
    ClientAPI::class to "client",
    ConfigAPI::class to "config",
    DataAPI::class to "data",
    EventsAPI::class to "events",
    FileAPI::class to "file",
    HostAPI::class to "host",
    JsonAPI::class to "json",
    KeybindAPI::class to "keybinds",
    MatricesAPI::class to "matrices",
    // models
    NameplateAPI::class to "nameplate",
    NetworkingAPI::class to "net",
    ParticleAPI::class to "particles",
    PingAPI::class to "pings",
    PlayerAPI::class to "player",
    RaycastAPI::class to "raycast",
    RendererAPI::class to "renderer",
    ResourcesAPI::class to "resources",
    SoundAPI::class to "sounds",
    TextureAPI::class to "textures",
    VanillaModelAPI::class to "vanilla_model",
    VectorsAPI::vec to "vec",
    VectorsAPI::class to "vectors",
    WorldAPI::class to "world",
).apply {
    FiguraAPIManager.API_GETTERS.runForEach {
        if (key != "user") {
            require(any { it.value == key }) { "Global variable '$key' is not canon" }
        }
    }
}

internal fun BODY.generateDocs() {
    FiguraAPIManager.WHITELISTED_CLASSES.map(Class<*>::kotlin).pairWith { it.findAnnotation<LuaTypeDoc>() }
        .liftSecondNull()/*.sortedBy { it.second.name }*/.shuffled().forEach { (k, td) ->
            div("sect") {
                val members: List<Either<Pair<KProperty1<*, *>, LuaFieldDoc>, Pair<KFunction<*>, LuaMethodDoc>>> = mutableListOf<Either<Pair<KProperty1<*, *>, LuaFieldDoc>, Pair<KFunction<*>, LuaMethodDoc>>>().apply {
                    k.declaredMemberProperties.forEach {
                        it.takeIf { it.javaField != null }?.findAnnotation<LuaFieldDoc>()?.let { an ->
                            add(Either.makeLeft(it to an))
                        }
                    }
                    k.declaredMemberFunctions.forEach {
                        it.findAnnotation<LuaMethodDoc>()?.let { an ->
                            add(Either.makeRight(it to an))
                        }
                    } // sortBy { it.foldIndifferent { it.first.name } }
                    shuffle()
                }
                id = td.name
                div("jumpbar") {
                    ul {
                        members.forEach {
                            li {
                                a("#${td.name}.${it.foldIndifferent { it.first.name }}") {
                                    +it.foldIndifferent { it.first.name }
                                }
                            }
                        }
                    }
                }
                h1 {
                    a("#${td.name}") {
                        +td.name
                    }
                }
                Language.getInstance().getOrDefault("figura.docs." + td.value).split("\n").forEach {
                    p {
                        +it
                    }
                }
                div("pane pane1") {
                    members.forEach {
                        h2 {
                            id = "${td.name}.${it.foldIndifferent { it.first.name }}"
                            +it.foldIndifferent { it.first.name }
                        }
                        div("pane pane2") {
                            Language.getInstance().getOrDefault(
                                "figura.docs." + it.fold({ it.second.value }, { it.second.value })
                            ).split("\n").forEach {
                                p {
                                    +it
                                }
                            }
                            div("code") {
                                it.bimap({ it.first }, { it.first }).let {
                                    genExamples(it)
                                }
                            }
                        }
                    }
                }
            }
        }
}

fun DIV.genExamples(member: Either<KProperty1<*, *>, KFunction<*>>) {
    val owner = member.fold({ it.javaField }, { it.javaMethod })!!.declaringClass.kotlin
    member.runFold({
        span("nocopy") {
            formatType(owner)
        }
        +"."
        span("text4") {
            +name
        }
        span("nocopy") {
            +": "
            formatType(returnType.jvmErasure)
        }
    }, f@{
        val ann = findAnnotation<LuaMethodDoc>()!!
        (ann.overloads.takeUnless { it.size == 1 && it[0].argumentNames.isEmpty() && it[0].argumentTypes.isEmpty() } ?: arrayOf(
            LuaMethodOverload(
                argumentTypes = parameters.map { it.type.jvmErasure }.toTypedArray(),
                argumentNames = parameters.map { it.name ?: "" }.toTypedArray(),
                returnType    = returnType.jvmErasure
            )
        )).mapIndexed { i, it ->
            it.run {
                if (i != 0) + "\n"
                when {
                    returnType == Void::class
                        || name.startsWith("set")
                        -> {}
                    else                                                -> {
                        val retName = when {
                            name.startsWith(prefix = "get")
                                 -> name.drop(3).replaceFirstChar(Char::lowercase)
                            name.startsWith(prefix = "is")
                                 -> name
                            else -> null
                        } ?: when (this@f) {
                            ConfigAPI::load
                                -> return@mapIndexed
                            else
                                -> null
                        } ?: when (returnType) {
                            FiguraVec2::class,
                            FiguraVec3::class,
                            FiguraVec4::class
                            -> "vec"
                            else
                            -> "value"
                        }
                        span("text1") {
                            +"local "
                        }
                        span("text4") {
                            +retName
                        }
                        span("nocopy") {
                            +": "
                            formatType(returnType)
                        }
                        +" = "
                    }
                }
                span("nocopy") {
                    formatType(owner, includeCanon = true)
                }
                +if (javaMethod!!.modifierSet.isStatic) "." else ":"
                span("text5") {
                    +name
                }
                +"("
                argumentNames.zip(argumentTypes).forEachIndexed { i, (n, ty) ->
                    if (i != 0) + ", "
                    span("text4") {
                        +when {
                            n   != ""
                                -> n
                            name.startsWith("set")
                                && name.length > 3
                                && argumentTypes.size == 1
                                -> name.drop(3).replaceFirstChar(Char::lowercase)
                            else
                                -> valueParameters.getOrNull(0)?.name
                                ?: javaMethod!!.parameters.getOrNull(0)?.name
                                ?: "arg$i"
                        }
                        +": "
                    }
                    span("nocopy") {
                        formatType(ty)
                    }
                }
                +")"
            }
        }
    })
}

fun FlowOrPhrasingContent.formatType(ty: KClass<*>, includeCanon: Boolean = false) {
    ty.run {
        val displayName = findAnnotation<LuaTypeDoc>()?.name
            ?: simpleName
            ?: jvmName
        val stdClass = "abbr text6" + if (includeCanon) " nocopy" else ""
        when {
            isSubclassOf(String::class)                             -> span(stdClass) {
                title = "A string, which can be typed in your code using '…' or \"…\"."
                +"string"
            }

            isSubclassOf(Integer::class) || isSubclassOf(Long::class) -> span(stdClass) {
                title = "A whole number, such as 1, -5, or 2+2, but not 2.5."
                +"integer"
            }

            isSubclassOf(Byte::class)                               -> span(stdClass) {
                title = "A whole number between -128 and 127. Values that do not fit will be wrapped; e.g. 128 → -128, 130 → -126, etc."
                +"byte"
            }

            isSubclassOf(Short::class)                              -> span(stdClass) {
                title = "A whole number between -65536 and 65535. Values that do not fit will be wrapped; e.g. 65538 → -65534."
                +"short"
            }

            isSubclassOf(Number::class)                             -> span(stdClass) {
                title = "A number, whole or fractional: e.g. 2, 3.7, -6, or 1+2."
                +"number"
            }

            isSubclassOf(Boolean::class)                            -> span(stdClass) {
                title = "A value with only two possibilities: true (yes) or false (no)."
                +"boolean"
            }

            includeCanon && foundIn(canonVars) -> span("abbr text4") {
                title = "A global variable, provided by Figura, of type '$displayName'"
                +getFrom(canonVars)!!
            }

            FiguraAPIManager.WHITELISTED_CLASSES.contains(java)-> a("#" + (findAnnotation<LuaTypeDoc>()?.name ?: simpleName ?: jvmName), classes = "text6 nocopy") {
                title = "Click to jump to documentation for this type. Press the scroll wheel to open documentation in a new tab."
                +displayName
            }

            else                                                    -> span("strike text6 nocopy") {
                title = "This type has no custom message and is not whitelisted. This is a bug — please report it!"
                +displayName
            }
        }
    }
}