import com.soywiz.kds.iterators.*

//When we launch the game, we need to restore the game state. In order to do that, we can get a String? value from the
//NativeStorage. Ths value is nullable, because there may be no value saved in the storage. After we get the value, we
//need to restore the history. So let's pass this value to the History where we'll restore the state. We also need to add
//a special callback that will be called when we update current History.

//This from string property we should get our saved history. Since the history is conceptually just a list of Element objects
class History(from: String?, private val onUpdate: (History) -> Unit) {

    //This Element should know about positions of blocks at some point of the history. It also should know about the
    //score at that point. Since block positions are always 16, let's create an IntArray with 16 values in the Element
    //class. Those values will represent the ids of the blocks placed on each one of 16 positions. Another property in
    //Element will contain the score.
    class Element(val numberIds: IntArray, val score: Int)

    //Define a property to contain the Element object
    private val history = mutableListOf<Element>()

    //Add a new property currentElement that will return the last element in the history.
    val currentElement: Element get() = history.last()

    //We should save the field state (block positions) on every move.
    init {
        from?.split(';')?.fastForEach {
            val element = elementFromString(it)
            history.add(element)
        }
    }

    private fun elementFromString(string: String): Element {
        val numbers = string.split(',').map { it.toInt() }
        if (numbers.size != 17) throw IllegalArgumentException("Incorrect history")
        return Element(IntArray(16) { numbers[it] }, numbers[16])
    }

    //Add a new Element (number ids and score) to the history
    fun add(numberIds: IntArray, score: Int) {
        history.add(Element(numberIds, score))
        onUpdate(this)
    }

    //Undo the last move with returning a new current history Element
    fun undo(): Element {
        if (history.size > 1) {
            history.removeAt(history.size - 1)
            onUpdate(this)
        }
        return history.last()
    }

    //Clear the whole history (when restarting a game)
    fun clear() {
        history.clear()
        onUpdate(this)
    }

    //check if the history is empty
    fun isEmpty() = history.isEmpty()

    //Let's add a reverse action in the History - a special toString() function that will convert a History instance to
    //its string representation
    override fun toString(): String {
        return history.joinToString(";") {
            it.numberIds.joinToString(",") + "," + it.score
        }
    }

}