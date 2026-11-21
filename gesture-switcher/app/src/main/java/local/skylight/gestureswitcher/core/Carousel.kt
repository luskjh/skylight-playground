package local.skylight.gestureswitcher.core

class Carousel<T>(items: List<T>, startIndex: Int = 0) {
    private val values = items.toList()
    var index: Int = if (values.isEmpty()) -1 else Math.floorMod(startIndex, values.size)
        private set

    val current: T? get() = values.getOrNull(index)
    val size: Int get() = values.size

    fun next(): T? = move(1)
    fun previous(): T? = move(-1)

    fun move(delta: Int): T? {
        if (values.isEmpty()) return null
        index = Math.floorMod(index + delta, values.size)
        return values[index]
    }

    fun indexOf(predicate: (T) -> Boolean): Int = values.indexOfFirst(predicate)
    fun select(newIndex: Int): T? {
        if (newIndex !in values.indices) return current
        index = newIndex
        return current
    }
}
