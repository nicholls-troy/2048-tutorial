import com.soywiz.kds.*
import kotlin.random.*

//Add enum class Position and class PositionMap. Position will have x and y properties indicating a position in
//PositionMap. PositionMap will contain the current state of our game field - in the constructor, there will be a
//special two-dimensional array from kds library containing block ids for each of 16 cells of the 4 x 4 game field
//(or -1 if there is no block in the current cell)
class Position(val x: Int, val y: Int)

//Create a special enum class Direction. This enum will have 4 values: LEFT, RIGHT, TOP and BOTTOM. They define the
//directions in which a user can move blocks.
enum class Direction {
    LEFT, RIGHT, TOP, BOTTOM
}

class PositionMap(private val array: IntArray2 = IntArray2(4, 4, -1)) {
    //Let's add a few simple functions... we will add more complex function as we write the game
    //getOrNull(x, y) - returns a Position object if there is a block in this position, otherwise returns null
    private fun getOrNull(x: Int, y: Int) = if (array.get(x, y) != -1) Position(x, y) else null

    //getNumber(x, y) - returns an Int value indicating the ordinal number of the Number enum element for the block in
    //this position, or -1 if there is no block
    private fun getNumber(x: Int, y: Int) = array.tryGet(x, y)?.let { blocks[it]?.number?.ordinal ?: -1 } ?: -1

    //Here we look at each position on the map and check if there is an adjacent position with the same number. If the
    //position is out of the field (like when x == -1 or y == 4) or there is no number there (the cell is empty), then
    //function getNumber will return -1, therefore covering the case when there is a single empty cell on the field with
    //no empty cells next to it (no cells with the same number).
    fun hasAvailableMoves(): Boolean {
        array.each { x, y, _ ->
            if (hasAdjacentEqualPosition(x, y)) return true
        }
        return false
    }

    private fun hasAdjacentEqualPosition(x: Int, y: Int) = getNumber(x, y).let {
        it == getNumber(x - 1, y) || it == getNumber(x + 1, y) || it == getNumber(x, y - 1) || it == getNumber(x, y + 1)
    }

    //Here we need to add a new function getNotEmptyPositionFrom(direction, line) in the PositionMap class that returns
    //a Position instance if the empty position is found, or null otherwise
    fun getNotEmptyPositionFrom(direction: Direction, line: Int): Position? {
        when (direction) {
            Direction.LEFT -> for (i in 0..3) getOrNull(i, line)?.let { return it }
            Direction.RIGHT -> for (i in 3 downTo 0) getOrNull(i, line)?.let { return it }
            Direction.TOP -> for (i in 0..3) getOrNull(line, i)?.let { return it }
            Direction.BOTTOM -> for (i in 3 downTo 0) getOrNull(line, i)?.let { return it }
        }
        return null
    }

    //Now we need to write getRandomFreePosition function in PositionMap. In this function, we count the quantity of
    //free positions (if this quantity is 0, then there are no free positions and we return null), choose a number for a
    //random free position and find it in the map
    fun getRandomFreePosition(): Position? {
        val quantity = array.count { it == -1 }
        if (quantity == 0) return null
        val chosen = Random.nextInt(quantity)
        var current = -1
        array.each { x, y, value ->
            if (value == -1) {
                current++
                if (current == chosen) {
                    return Position(x, y)
                }
            }
        }
        return null
    }

    //get(x, y) - an operator function that returns a block id for this position
    operator fun get(x: Int, y: Int) = array[x, y]

    //set(x, y) - an operator function that sets a block id for this position
    operator fun set(x: Int, y: Int, value: Int) {
        array[x, y] = value
    }

    //Create and return an IntArray containing ids of numbers placed on the field
    fun toNumberIds() = IntArray(16) { getNumber(it % 4, it / 4)}

    //forEach(action) - a function that calls this action for each block id in array
    fun forEach(action: (Int) -> Unit) { array.forEach(action) }

    //Add a new function copy(), that should create and return a new map with the same content
    fun copy() = PositionMap(array.copy(data = array.data.copyOf()))

    //equals(other) - checks whether the other object is PositionMap and whether positions of this and that map are equal
    override fun equals(other: Any?): Boolean {
        return (other is PositionMap) && this.array.data.contentEquals(other.array.data)
    }

    //hashCode() - delegates calculating hashCode to array
    override fun hashCode() = array.hashCode()
}



