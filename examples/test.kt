@file:Suppress("UNUSED_VARIABLE", "UNREACHABLE_CODE", "UnusedReceiverParameter")
@file:JvmName("KotlinShowcase")

package com.example.showcase

import kotlin.contracts.*
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty as Foo
import kotlin.reflect.*
import kotlin.reflect.full.*


// ─────────────────────────────────────────────
// 0. ST PACKAGE ISSUES
// ─────────────────────────────────────────────

import org.amshove.kluent.`should not be null`


environment.seat("foo")
environment.set("foo")

data class CodeKey(
    var code: String,
    /* Block comment */
    // Line comment
    var key: String
    /* Block comment */
    // Line comment
)

fun <T : Comparable<T>> sort(list: List<T>) {  ... }

open class Shape
class Rectangle: Shape()


// Extension function on nullable Any
fun Any?.toString(): String {
    if (this == null) return "null"
    // After null check, `this` is smart-cast to non-nullable Any
    // So this call resolves to the regular toString() function
    return toString()
}

fun <T> copyWhenGreater(list: List<T>, threshold: T): List<String>
    where T : Comparable<T> {
    return list.filter { it > threshold }.map { it.toString() }
}

interface Source<out T> {
    fun nextT(): T
}

// ─────────────────────────────────────────────
// 1. ANNOTATIONS
// ─────────────────────────────────────────────

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Fancy(val level: Int = 1, val tag: String = "default")

@Repeatable
@Target(AnnotationTarget.CLASS)
annotation class Tag(val name: String)


// ─────────────────────────────────────────────
// 2. SEALED CLASS HIERARCHY
// ─────────────────────────────────────────────

sealed class Result<out T> {
    data class Success<T>(val value: T, val metadata: Map<String, Any> = emptyMap()) : Result<T>()
    data class Failure(val error: Throwable, val code: Int = -1) : Result<Nothing>()
    data object Loading : Result<Nothing>()
    data object Empty : Result<Nothing>()
}

sealed interface Shape {
    val area: Double
    val perimeter: Double

    data class Circle(val radius: Double) : Shape {
        override val area get() = Math.PI * radius * radius
        override val perimeter get() = 2 * Math.PI * radius
    }

    data class Rectangle(val width: Double, val height: Double) : Shape {
        override val area get() = width * height
        override val perimeter get() = 2 * (width + height)
    }

    data class Triangle(val a: Double, val b: Double, val c: Double) : Shape {
        override val perimeter get() = a + b + c
        override val area: Double get() {
            val s = perimeter / 2
            return Math.sqrt(s * (s - a) * (s - b) * (s - c))
        }
    }
}

// ─────────────────────────────────────────────
// 3. ENUM CLASS
// ─────────────────────────────────────────────

enum class Direction(val degrees: Int, val vector: Pair<Int, Int>) {
    NORTH(0, 0 to 1),
    EAST(90, 1 to 0),
    SOUTH(180, 0 to -1),
    WEST(270, -1 to 0);

    val opposite: Direction get() = entries[(ordinal + 2) % 4]

    fun rotateClockwise(times: Int = 1): Direction = entries[(ordinal + times) % 4]

    companion object {
        fun fromDegrees(deg: Int): Direction = entries.first { it.degrees == deg % 360 }
    }
}

// ─────────────────────────────────────────────
// 4. INTERFACES WITH DEFAULT METHODS & DELEGATION
// ─────────────────────────────────────────────

interface Printable {
    fun print(): String
    fun prettyPrint(): String = "[ ${print()} ]"
}

interface Serializable {
    fun serialize(): ByteArray = print().toByteArray()
    fun print(): String
}

class Document(private val content: String) : Printable, Serializable {
    override fun print() = content
    override fun prettyPrint() = "📄 $content"
}

// Interface delegation
interface Logger {
    fun log(msg: String)
}

class ConsoleLogger : Logger {
    override fun log(msg: String) = println("[LOG] $msg")
}

class Service(logger: Logger) : Logger by logger {
    fun doWork() = log("Working...")
}

// ─────────────────────────────────────────────
// 5. GENERICS: VARIANCE, CONSTRAINTS, STAR PROJECTION
// ─────────────────────────────────────────────

class Box<out T : Any>(val value: T) {
    fun <R : Any> map(transform: (T) -> R): Box<R> = Box(transform(value))
}

class Sink<in T> {
    fun consume(item: T) = println("Consumed: $item")
}

fun <T> copyFrom(source: Box<T>, sink: Sink<T>) = sink.consume(source.value)

fun printBoxInfo(box: Box<*>) = println("Box contains: ${box.value}")

// Reified generics
inline fun <reified T> Any.isOfType(): Boolean = this is T

inline fun <reified T : Any> List<*>.filterByType(): List<T> = filterIsInstance<T>()

// Generic extension with multiple bounds
fun <T> T.clamp(min: T, max: T): T where T : Comparable<T> =
    when {
        this < min -> min
        this > max -> max
        else -> this
    }

// ─────────────────────────────────────────────
// 6. DATA CLASSES, COPY, DESTRUCTURING
// ─────────────────────────────────────────────

data class Person(
    val name: String,
    val age: Int,
    val email: String? = null
) {
    operator fun component4() = email?.substringBefore("@") ?: "no-email"
}

data class Point(val x: Double, val y: Double) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Point(x * scalar, y * scalar)
    operator fun unaryMinus() = Point(-x, -y)
    fun distanceTo(other: Point) = Math.sqrt((x - other.x).let { it * it } + (y - other.y).let { it * it })
}

// ─────────────────────────────────────────────
// 7. OBJECT, COMPANION OBJECT, SINGLETON
// ─────────────────────────────────────────────

object Registry {
    private val entries = mutableMapOf<String, Any>()

    fun register(key: String, value: Any) { entries[key] = value }
    fun lookup(key: String): Any? = entries[key]
    operator fun get(key: String) = lookup(key)
    operator fun set(key: String, value: Any) = register(key, value)
    val size: Int get() = entries.size
}

@Fancy(level = 3, tag = "factory")
class Config private constructor(val settings: Map<String, String>) {
    companion object Factory {
        private val DEFAULT = mapOf("theme" to "dark", "lang" to "en")

        fun default(): Config = Config(DEFAULT)
        fun fromMap(map: Map<String, String>): Config = Config(DEFAULT + map)

        @JvmStatic
        fun empty(): Config = Config(emptyMap())

        const val VERSION = "1.0.0"
    }

    operator fun get(key: String) = settings[key]
    override fun toString() = "Config(${settings.entries.joinToString { "${it.key}=${it.value}" }})"
}

// ─────────────────────────────────────────────
// 8. EXTENSION FUNCTIONS & PROPERTIES
// ─────────────────────────────────────────────

val String.palindrome: Boolean get() = this == this.reversed()
val String.words: List<String> get() = trim().split(Regex("\\s+"))
val Int.factorial: Long get() = if (this <= 1) 1L else this * (this - 1).factorial

fun String.truncate(max: Int, ellipsis: String = "…"): String =
    if (length <= max) this else take(max - ellipsis.length) + ellipsis

fun <T> List<T>.second(): T = this[1]
fun <T> List<T>.penultimate(): T = this[size - 2]

// Extension on nullable
fun String?.orDefault(default: String = "(empty)") = this ?: default

// Extension function with receiver type
fun StringBuilder.appendLine(value: Any?): StringBuilder = append(value).append('\n')

// ─────────────────────────────────────────────
// 9. HIGHER-ORDER FUNCTIONS & LAMBDAS
// ─────────────────────────────────────────────

typealias Predicate<T> = (T) -> Boolean
typealias Transform<A, B> = (A) -> B

fun <T> List<T>.partitionBy(predicate: Predicate<T>): Pair<List<T>, List<T>> =
    partition(predicate)

fun <T, R> T.pipe(vararg transforms: Transform<Any?, Any?>): Any? {
    @Suppress("UNCHECKED_CAST")
    return transforms.fold(this as Any?) { acc, fn -> fn(acc) }
}

fun <T> buildList(capacity: Int = 10, block: MutableList<T>.() -> Unit): List<T> =
    ArrayList<T>(capacity).apply(block).toList()

// Function composition
infix fun <A, B, C> ((A) -> B).andThen(other: (B) -> C): (A) -> C = { other(this(it)) }
infix fun <A, B, C> ((B) -> C).compose(other: (A) -> B): (A) -> C = { this(other(it)) }

// Currying
fun <A, B, C> ((A, B) -> C).curried(): (A) -> (B) -> C = { a -> { b -> this(a, b) } }

// Memoization
fun <T, R> ((T) -> R).memoize(): (T) -> R {
    val cache = mutableMapOf<T, R>()
    return { input -> cache.getOrPut(input) { this(input) } }
}

// ─────────────────────────────────────────────
// 10. SCOPE FUNCTIONS: let, run, with, apply, also
// ─────────────────────────────────────────────

fun scopeFunctionsDemo() {
    val name: String? = "Kotlin"

    // let: transform nullable
    val length = name?.let { it.length + it.count { c -> c.isUpperCase() } }

    // run: object config + return value
    val result = "hello".run {
        uppercase().reversed().take(3)
    }

    // with: operate on non-null receiver
    val sb = with(StringBuilder()) {
        append("Hello")
        append(", ")
        append("World")
        toString()
    }

    // apply: mutate + return self
    val list = mutableListOf<Int>().apply {
        addAll(1..5)
        removeIf { it % 2 == 0 }
        sort()
    }

    // also: side effects
    val processed = list
        .also { println("Before: $it") }
        .map { it * 2 }
        .also { println("After: $it") }
}

// ─────────────────────────────────────────────
// 11. COROUTINE-STYLE: SEQUENCES & LAZY EVALUATION
// ─────────────────────────────────────────────

val fibonacci: Sequence<Long> = sequence {
    var a = 0L
    var b = 1L
    while (true) {
        yield(a)
        val next = a + b
        a = b
        b = next
    }
}

val primes: Sequence<Int> = sequence {
    val sieve = mutableSetOf<Int>()
    var candidate = 2
    while (true) {
        if (sieve.none { candidate % it == 0 }) {
            yield(candidate)
            sieve.add(candidate)
        }
        candidate++
    }
}

fun generateTree(depth: Int): Sequence<String> = sequence {
    fun recurse(node: String, d: Int): suspend SequenceScope<String>.() -> Unit = {
        yield(node)
        if (d > 0) {
            recurse("$node-L", d - 1)()
            recurse("$node-R", d - 1)()
        }
    }
    recurse("root", depth)()
}

// ─────────────────────────────────────────────
// 12. OPERATOR OVERLOADING
// ─────────────────────────────────────────────

data class Matrix(val rows: Int, val cols: Int, private val data: DoubleArray = DoubleArray(rows * cols)) {

    operator fun get(r: Int, c: Int) = data[r * cols + c]
    operator fun set(r: Int, c: Int, v: Double) { data[r * cols + c] = v }

    operator fun plus(other: Matrix): Matrix {
        require(rows == other.rows && cols == other.cols)
        return Matrix(rows, cols, DoubleArray(rows * cols) { i -> data[i] + other.data[i] })
    }

    operator fun times(scalar: Double) =
        Matrix(rows, cols, DoubleArray(rows * cols) { i -> data[i] * scalar })

    operator fun times(other: Matrix): Matrix {
        require(cols == other.rows)
        return Matrix(rows, other.cols).also { result ->
            for (r in 0 until rows)
                for (c in 0 until other.cols)
                    result[r, c] = (0 until cols).sumOf { k -> this[r, k] * other[k, c] }
        }
    }

    operator fun unaryMinus() = this * -1.0
    operator fun contains(value: Double) = data.any { it == value }
    operator fun component1() = rows
    operator fun component2() = cols

    override fun toString() = (0 until rows).joinToString("\n") { r ->
        (0 until cols).joinToString(" ") { c -> "%.2f".format(this[r, c]) }
    }
}

// ─────────────────────────────────────────────
// 13. DELEGATED PROPERTIES
// ─────────────────────────────────────────────

class LazyLogger {
    val expensiveValue: String by lazy { "computed: ${System.nanoTime()}" }
    var observed: String by Delegates.observable("initial") { prop, old, new ->
        println("${prop.name}: $old → $new")
    }
    var validated: Int by Delegates.vetoable(0) { _, _, new -> new >= 0 }
}

// Custom delegate
class Clamped(private val min: Double, private val max: Double) :
    ReadWriteProperty<Any?, Double> {
    private var value = min
    override fun getValue(thisRef: Any?, property: KProperty<*>) = value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
        this.value = value.clamp(min, max)
    }
}

class Slider {
    var position: Double by Clamped(0.0, 1.0)
    var volume: Double by Clamped(0.0, 100.0)
}

// Map delegation
class Config2(map: Map<String, Any?>) {
    val host: String by map
    val port: Int by map
    val debug: Boolean by map
}

// ─────────────────────────────────────────────
// 14. CONTRACTS (EXPERIMENTAL)
// ─────────────────────────────────────────────

@OptIn(ExperimentalContracts::class)
fun requireNotEmpty(value: String?): String {
    contract {
        returns() implies (value != null)
    }
    return value?.takeIf { it.isNotBlank() } ?: error("Value must not be empty")
}

@OptIn(ExperimentalContracts::class)
inline fun <T> runIfNotNull(value: T?, block: (T) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (value != null) block(value)
}

// ─────────────────────────────────────────────
// 15. INLINE CLASSES (VALUE CLASSES)
// ─────────────────────────────────────────────

@JvmInline
value class UserId(val id: String) {
    init { require(id.isNotBlank()) { "UserId must not be blank" } }
    val isValid: Boolean get() = id.length in 8..64
}

@JvmInline
value class Celsius(val degrees: Double) {
    val inFahrenheit get() = Celsius((degrees * 9.0 / 5.0) + 32)
    val inKelvin get() = Celsius(degrees + 273.15)
    operator fun plus(other: Celsius) = Celsius(degrees + other.degrees)
    override fun toString() = "${degrees}°C"
}

// ─────────────────────────────────────────────
// 16. TYPE ALIASES
// ─────────────────────────────────────────────

typealias Matrix2D = Array<DoubleArray>
typealias EventHandler<T> = suspend (event: T) -> Unit
typealias StringMap = Map<String, String>
typealias Callback = () -> Unit

// ─────────────────────────────────────────────
// 17. DSL BUILDERS
// ─────────────────────────────────────────────

@DslMarker
annotation class HtmlDsl

@HtmlDsl
class HtmlTag(val name: String) {
    private val attributes = mutableMapOf<String, String>()
    private val children = mutableListOf<Any>()

    operator fun String.unaryPlus() { children.add(this) }

    infix fun attr(pair: Pair<String, String>) { attributes[pair.first] = pair.second }

    fun tag(name: String, block: HtmlTag.() -> Unit): HtmlTag {
        val child = HtmlTag(name).apply(block)
        children.add(child)
        return child
    }

    fun div(block: HtmlTag.() -> Unit) = tag("div", block)
    fun p(block: HtmlTag.() -> Unit) = tag("p", block)
    fun span(block: HtmlTag.() -> Unit) = tag("span", block)
    fun h1(block: HtmlTag.() -> Unit) = tag("h1", block)

    override fun toString(): String {
        val attrs = if (attributes.isEmpty()) "" else " " + attributes.entries.joinToString(" ") { """${it.key}="${it.value}"""" }
        val inner = children.joinToString("")
        return "<$name$attrs>$inner</$name>"
    }
}

fun html(block: HtmlTag.() -> Unit) = HtmlTag("html").apply(block)

// ─────────────────────────────────────────────
// 18. WHEN EXPRESSIONS (EXHAUSTIVE & COMPLEX)
// ─────────────────────────────────────────────

fun describeResult(result: Result<*>): String = when (result) {
    is Result.Success<*> -> "✅ Success: ${result.value} (meta: ${result.metadata})"
    is Result.Failure -> "❌ Failure [${result.code}]: ${result.error.message}"
    Result.Loading -> "⏳ Loading..."
    Result.Empty -> "📭 Empty"
}

fun classify(x: Any?): String = when (x) {
    null -> "null"
    is Int -> when {
        x < 0 -> "negative int"
        x == 0 -> "zero"
        x.and(x - 1) == 0 -> "power of two: $x"
        else -> "positive int: $x"
    }
    is String -> when {
        x.isEmpty() -> "empty string"
        x.palindrome -> "palindrome: $x"
        x.length > 100 -> "long string (${x.length})"
        else -> "string: $x"
    }
    is List<*> -> "list of ${x.size} items"
    is Map<*, *> -> "map with ${x.size} entries"
    in 0.0..1.0 -> "unit interval double"
    else -> "unknown: ${x::class.simpleName}"
}

// ─────────────────────────────────────────────
// 19. DESTRUCTURING & MULTI-RETURN
// ─────────────────────────────────────────────

operator fun <K, V> Map.Entry<K, V>.component1() = key
operator fun <K, V> Map.Entry<K, V>.component2() = value

fun parseRecord(csv: String): Triple<String, Int, Boolean> {
    val (name, age, active) = csv.split(",")
    return Triple(name.trim(), age.trim().toInt(), active.trim().toBoolean())
}

fun destructuringDemo() {
    val (a, b, c) = listOf(1, 2, 3)
    val (x, y) = Point(3.0, 4.0)
    val (name, age, email) = Person("Alice", 30, "alice@example.com")

    val map = mapOf("one" to 1, "two" to 2)
    for ((k, v) in map) println("$k = $v")

    val (evens, odds) = (1..10).partition { it % 2 == 0 }
}

// ─────────────────────────────────────────────
// 20. COLLECTIONS: ADVANCED OPERATIONS
// ─────────────────────────────────────────────

fun collectionsDemo() {
    val numbers = (1..20).toList()

    // Chained operations
    val result = numbers
        .asSequence()
        .filter { it % 2 == 0 }
        .map { it * it }
        .takeWhile { it < 100 }
        .runningFold(0) { acc, x -> acc + x }
        .drop(1)
        .toList()

    // Grouping & aggregation
    val words = listOf("apple", "banana", "cherry", "avocado", "blueberry", "apricot")
    val byLetter: Map<Char, List<String>> = words.groupBy { it.first() }
    val longestPerGroup: Map<Char, String> = words.groupBy { it.first() }
        .mapValues { (_, v) -> v.maxByOrNull { it.length }!! }

    // Windowing & chunking
    val chunked = numbers.chunked(4) { it.sum() }
    val windowed = numbers.windowed(size = 3, step = 2, partialWindows = true)

    // Flat operations
    val nested = listOf(listOf(1, 2), listOf(3, 4), listOf(5, 6))
    val flat = nested.flatten()
    val flatMapped = nested.flatMap { it.map { n -> n * 10 } }

    // Zip & unzip
    val zipped = numbers.zip(words) { n, w -> "$n:$w" }
    val (ns, ws) = words.zip(numbers).unzip()

    // scan (running aggregation)
    val runningSum = numbers.runningReduce { acc, n -> acc + n }

    // Associate
    val indexed: Map<Int, String> = words.associateBy { it.length }
    val valued: Map<String, Int> = words.associateWith { it.length }
}

// ─────────────────────────────────────────────
// 21. OPERATOR OVERLOADING ON COLLECTIONS
// ─────────────────────────────────────────────

class Bag<T>(private val items: MutableList<T> = mutableListOf()) : Iterable<T> {
    operator fun plus(item: T) = Bag(( items + item).toMutableList())
    operator fun minus(item: T) = Bag((items - item).toMutableList())
    operator fun get(index: Int) = items[index]
    operator fun contains(item: T) = item in items
    operator fun invoke(): List<T> = items.toList()
    override fun iterator() = items.iterator()
    val size get() = items.size
}

// ─────────────────────────────────────────────
// 22. TAIL RECURSION
// ─────────────────────────────────────────────

tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

tailrec fun flatten(lists: List<List<Int>>, acc: List<Int> = emptyList()): List<Int> =
    if (lists.isEmpty()) acc else flatten(lists.drop(1), acc + lists.first())

// ─────────────────────────────────────────────
// 23. REFLECTION
// ─────────────────────────────────────────────

fun reflectionDemo() {
    val kClass = Person::class
    println("Class: ${kClass.simpleName}")
    println("Is data: ${kClass.isData}")
    println("Members: ${kClass.memberProperties.map { it.name }}")

    val prop: KProperty1<Person, String> = Person::name
    val alice = Person("Alice", 30)
    println("Value via reflection: ${prop.get(alice)}")

    val constructor = kClass.primaryConstructor
    val bob = constructor?.call("Bob", 25, null)

    // Function references
    val upper: (String) -> String = String::uppercase
    val lengths: List<Int> = listOf("a", "bb", "ccc").map(String::length)
    val factory: (String, Int) -> Person = ::Person
}

// ─────────────────────────────────────────────
// 24. CONTEXTUAL & SCOPE: CONTEXT RECEIVERS (PREVIEW)
// ─────────────────────────────────────────────

// Simulated context pattern (context receivers are experimental)
interface TransactionContext {
    fun commit()
    fun rollback()
}

class Transaction : TransactionContext {
    private val ops = mutableListOf<String>()
    fun execute(op: String) = ops.add(op)
    override fun commit() = println("Committed: $ops")
    override fun rollback() = println("Rolled back: $ops")
}

fun <T> withTransaction(block: Transaction.() -> T): T {
    val tx = Transaction()
    return try {
        tx.block().also { tx.commit() }
    } catch (e: Exception) {
        tx.rollback()
        throw e
    }
}

// ─────────────────────────────────────────────
// 25. INLINE FUNCTIONS & REIFIED
// ─────────────────────────────────────────────

inline fun <T> measureTime(block: () -> T): Pair<T, Long> {
    val start = System.nanoTime()
    val result = block()
    return result to (System.nanoTime() - start)
}

inline fun <reified T : Enum<T>> enumByNameOrNull(name: String): T? =
    enumValues<T>().find { it.name.equals(name, ignoreCase = true) }

inline fun <reified T : Any> safeCast(value: Any): T? = value as? T

// noinline & crossinline
inline fun higherOrderMix(
    noinline regular: () -> Unit,
    crossinline crossInlined: () -> Unit
): () -> Unit {
    crossInlined()
    return { regular() }
}

// ─────────────────────────────────────────────
// 26. EXCEPTION HANDLING & RESOURCE MANAGEMENT
// ─────────────────────────────────────────────

class Resource(val name: String) : AutoCloseable {
    init { println("Opening $name") }
    fun use() = println("Using $name")
    override fun close() = println("Closing $name")
}

fun exceptionDemo() {
    val result = runCatching {
        Resource("DB").use { r ->
            r.use()
            check(true) { "Invariant violated" }
            42
        }
    }.onSuccess { println("Got: $it") }
     .onFailure { println("Failed: ${it.message}") }
     .getOrElse { -1 }

    try {
        throw IllegalStateException("Oops")
    } catch (e: IllegalArgumentException) {
        println("Arg error")
    } catch (e: IllegalStateException) {
        println("State error: ${e.message}")
    } finally {
        println("Always runs")
    }

    // Nothing type
    fun fail(msg: String): Nothing = throw RuntimeException(msg)
    val value: String = null ?: fail("Cannot be null")
}

// ─────────────────────────────────────────────
// 27. STRING TEMPLATES & RAW STRINGS
// ─────────────────────────────────────────────

fun stringDemo() {
    val name = "World"
    val multiLine = """
        |Hello, $name!
        |Today is a ${if (name.length > 3) "great" else "fine"} day.
        |${fibonacci.take(10).joinToString(", ")}
    """.trimMargin()

    val regex = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    val match = regex.find("Today: 2024-01-15")
    val (year, month, day) = match!!.destructured

    val escaped = "Tab:\t Newline:\n Unicode:\u2603"
    val raw = '\u0041' // 'A'
}

// ─────────────────────────────────────────────
// 28. OBJECT EXPRESSIONS (ANONYMOUS OBJECTS)
// ─────────────────────────────────────────────

fun anonymousObjectDemo() {
    val comparator = object : Comparator<String> {
        override fun compare(a: String, b: String) = a.length.compareTo(b.length)
    }

    val anonymousWithState = object {
        var count = 0
        fun increment() = ++count
        fun decrement() = --count
    }

    val sorted = listOf("banana", "fig", "cherry", "kiwi").sortedWith(comparator)
}

// ─────────────────────────────────────────────
// 29. FLOW-LIKE PIPELINE (pure stdlib, no coroutines import)
// ─────────────────────────────────────────────

class Pipeline<T>(private val source: Sequence<T>) {
    fun <R> map(transform: (T) -> R) = Pipeline(source.map(transform))
    fun filter(predicate: (T) -> Boolean) = Pipeline(source.filter(predicate))
    fun take(n: Int) = Pipeline(source.take(n))
    fun <R> fold(initial: R, operation: (R, T) -> R) = source.fold(initial, operation)
    fun toList() = source.toList()
    fun forEach(action: (T) -> Unit) = source.forEach(action)
    companion object {
        fun <T> of(vararg items: T) = Pipeline(items.asSequence())
        fun <T> from(iterable: Iterable<T>) = Pipeline(iterable.asSequence())
    }
}

// ─────────────────────────────────────────────
// 30. ENTRY POINT & DEMO
// ─────────────────────────────────────────────

fun main() {
    // Sealed classes & when
    val results: List<Result<Int>> = listOf(
        Result.Success(42, mapOf("source" to "db")),
        Result.Failure(RuntimeException("timeout"), 504),
        Result.Loading,
        Result.Empty
    )
    results.forEach { println(describeResult(it)) }

    // Data classes
    val p1 = Point(3.0, 4.0)
    val p2 = Point(1.0, 2.0)
    println(p1 + p2)
    println(-p1)
    println(p1.distanceTo(p2))

    // Enums
    println(Direction.NORTH.opposite)
    println(Direction.EAST.rotateClockwise(3))

    // Value classes
    val temp = Celsius(100.0)
    println("$temp = ${temp.inFahrenheit} = ${temp.inKelvin}")

    // Extension properties
    println("racecar".palindrome)
    println(10.factorial)
    println("Hello, World!".truncate(8))

    // Generics
    val box = Box(42)
    val strBox = box.map { "Number: $it" }
    printBoxInfo(strBox)
    println(42.isOfType<Int>())

    // DSL
    val page = html {
        div {
            attr("class" to "container")
            h1 { +"Welcome to Kotlin" }
            p {
                attr("id" to "intro")
                +"Kotlin is concise, safe, and expressive."
            }
        }
    }
    println(page)

    // Matrix
    val (rows, cols) = Matrix(2, 3)
    println("Matrix: ${rows}x${cols}")

    // Sequences
    println("Fibonacci: ${fibonacci.take(12).toList()}")
    println("Primes:    ${primes.take(12).toList()}")

    // Collections
    val pipeline = Pipeline.from(1..100)
        .filter { it % 2 == 0 }
        .map { it * it }
        .take(5)
        .toList()
    println("Pipeline: $pipeline")

    // Delegation
    val slider = Slider().apply {
        position = 2.5   // clamped to 1.0
        volume = -10.0   // clamped to 0.0
    }
    println("Slider: pos=${slider.position}, vol=${slider.volume}")

    // Tail recursion
    println("GCD(48, 18) = ${gcd(48, 18)}")

    // Transaction DSL
    withTransaction {
        execute("INSERT INTO users VALUES (1, 'Alice')")
        execute("UPDATE accounts SET balance = 100 WHERE user_id = 1")
    }

    // Bag
    val bag = Bag<String>() + "apple" + "banana" + "cherry"
    println("Bag has ${bag.size} items, contains apple: ${"apple" in bag}")
    println("Contents: ${bag()}")

    // Function composition
    val process = String::trim andThen String::lowercase andThen { it.reversed() }
    println(process("  HELLO WORLD  "))

    // Classify
    listOf(null, 0, -5, 8, "racecar", "hi", listOf(1,2,3), mapOf("a" to 1)).forEach {
        println(classify(it))
    }
}




