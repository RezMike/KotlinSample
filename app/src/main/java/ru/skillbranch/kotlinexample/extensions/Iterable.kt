package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    val list = this.toMutableList()
    var elem: T
    do {
        elem = list.removeAt(list.size - 1)
    } while (!predicate.invoke(elem))
    return list
}
