import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import Number.*



//Add a new class extending Container with one property number of type Number (the enum created in the previous section)
class Block(val number: Number) : Container() {
    //In this class inside init {} we define the views of which this block will consist of: a roundRect as background
    //and a text with the number value
    init {
        //Here we use undefined values: cellSize and font. That's actually the variables we created in our main function.
        //To make all needed variables available outside the main function, let's update the main.kt file a little bit.
        roundRect(cellSize, cellSize, 5.0, fill = number.color)
        val textColor = when (number) {
            ZERO, ONE -> Colors.BLACK
            else -> Colors.WHITE
        }
        text(number.value.toString(), textSizeFor(number), textColor, font).apply {
            centerBetween(0.0, 0.0, cellSize, cellSize)
        }
    }
}

//It should return the size of the text for the specified number.
private fun textSizeFor(number: Number) = when (number) {
    ZERO, ONE, TWO, THREE, FOUR, FIVE -> cellSize / 2
    SIX, SEVEN, EIGHT -> cellSize * 4 / 9
    NINE, TEN, ELEVEN, TWELVE -> cellSize * 2 / 5
    THIRTEEN, FOURTEEN, FIFTEEN -> cellSize * 7 / 20
    SIXTEEN -> cellSize * 3 / 10
}

//Now let's define a special extension function that creates a Block view instance and add it to the container
fun Container.block(number: Number) = Block(number).addTo(this)