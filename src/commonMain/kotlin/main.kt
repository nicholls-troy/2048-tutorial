import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.input.KeysEvents.*
import com.soywiz.korge.service.storage.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korio.async.ObservableProperty
import kotlin.collections.set
import kotlin.properties.*
import kotlin.random.*

//We need to declare these at top-level, to use them in other files. For Double variables write 0.0 as default, for the
//BitmapFont there is a special delegate for that - Delegates.notNull().
var cellSize: Double = 0.0
var fieldSize: Double = 0.0
var leftIndent: Double = 0.0
var topIndent: Double = 0.0
var font: BitmapFont by Delegates.notNull()

//We also need to add several functions outside the main function. The first two are columnX(number) and rowY(number)
//functions that take number of type Int as a column/row number and return real x/y position for it
fun columnX(number: Int) = leftIndent + 10 + (cellSize + 10) * number
fun rowY(number: Int) = topIndent + 10 + (cellSize + 10) * number

//Create an instance of PositionMap
var map = PositionMap()

//To manage all blocks on the game field, we need to specify two new variables: blocks hash map containing the
//blocks with their ids, and freeId indicating the next available id for a new block
val blocks = mutableMapOf<Int, Block>()
var freeId = 0

//Define a History variable
var history: History by Delegates.notNull()

//We also need to add a utility function numberFor(blockId)
fun numberFor(blockId: Int) = blocks[blockId]!!.number

//In this function we remove a block view with the corresponding id from the blocks map and then remove this view from
//its parent container
fun deleteBlock(blockId: Int) = blocks.remove(blockId)!!.removeFromParent()

//The ObservableProperty class has a constructor with the initial value, a value property to get the current value, and
//two very useful functions - observe(handler) and update(value). The first one allows you to add a new handler that will
//observe and handle new values of the property when the property is updated. The second one allows you to update the
//property value. If we use this class for our score properties, we'll be able to observe and update them from different
//places inside and outside our main class.
val score = ObservableProperty(0)
val best = ObservableProperty(0)

var isAnimationRunning = false
var isGameOver = false



suspend fun main() = Korge(width = 480, height = 640, title = "2048", bgcolor = RGBA(253, 247, 240)) {
	//Import the font before using it
	font = resourcesVfs["clear_sans.fnt"].readBitmapFont()

	//Initialize the History variable in the main() function
	val storage = views.storage
	//and a few new lines here
	history = History(storage.getOrNull("history")) {
		storage["history"] = it.toString()
	}

	//and here we'll update the best score
	best.update(storage.getOrNull("best")?.toInt() ?: 0)

	//Now we need to define the property observers. The first one will handle updates of the score property and update
	//the best property value. The second one will handle update of the best property and save its value in the storage.
	score.observe {
		if (it > best.value) best.update(it)
	}
	best.observe {
		//here we'll update the value in the storage
		storage["best"] = it.toString()
	}

	//NativeStorage allows you to save and restore on String values, that's why we had to call toInt() and toString in
	//the code above.

	//Define a few main sizes for the game. Since we need 4 cells and small indents between them to fit on the screen
	//we can divide the size of the scene by 5 (we get the scene width via views.virtualWidth)
	cellSize = views.virtualWidth / 5.0

	//Let's define cell indents equal to 10px. Now we can calculate the size of the whole field and the indent from the
	//left edge of the screen. The last size value we need to specify is the indent from the top edge of the screen.
	fieldSize = 50 + 4 * cellSize
	leftIndent = (views.virtualWidth - fieldSize)
	topIndent = 150.0

	//View DSL is the recommended way, it provides a convenient way of working with a structured view tree.
	//It uses inline and extension functions of Kotlin to create a special view DSL.
	//Use the function roundRect it adds the view to the container by itself
	//We assign the roundRect object to the variable bgField because we'll need this object later
	val bgField = roundRect(fieldSize, fieldSize, 5.0, fill = Colors["#b9aea0"]) {
//		x = leftIndent
//		y = topIndent
		//We can simplify this code a bit by using a special function - position(x,y)
		position(leftIndent, topIndent)
	}

	//Here we position our Graphics object, specify the color via fill function and draw a rounded rectangle with it.
	//We specify the rectangle position relative to its Graphics container, not the root container! We also specify its
	//size and rounding radius. Let's do it easier and take advantage of two for cycles
	graphics {
		position(leftIndent, topIndent)
		fill(Colors["#cec0b2"]) {
			for (i in 0..3) {
				for (j in 0..3) {
					roundRect(10.0 + (10.0 + cellSize) * i, 10.0 + (10.0 + cellSize) * j, cellSize, cellSize, 5.0)
				}
			}
		}
	}

	//Define the logo background
	val bgLogo = roundRect(cellSize, cellSize, 5.0, fill = Colors["#edc403"]) {
		position(leftIndent, 30.0)
	}

	//Now we can add a text view for our logo. We specify a text string, a text size, a text color and a font. We use
	//centerOn function to center it on our logo background
	text("2048", cellSize * 0.5, Colors.WHITE, font).centerOn(bgLogo)

	//Using Relative positioning we can specify a position of a view relative to other views. There are extension
	//functions like alignRightToLeftOf, alignTopToBottomOf, centerBetween and centerOn. Let's use some of them to
	//create and position score blocks
	val bgBest = roundRect(cellSize * 1.5, cellSize * 0.8, 5.0, fill = Colors["#bbae9e"]) {
		alignRightToRightOf(bgField)
		alignTopToTopOf(bgLogo)
	}

	//Now add text views for the current score and the best score the same way. We will also specify text bounds for
	//score number views and align them, so they will be shown correctly with different numbers.
	text("BEST", cellSize * 0.25, RGBA(239, 226, 210), font) {
		centerXOn(bgBest)
		alignTopToTopOf(bgBest, 5.0)
	}
	// here is a new change
	text(best.value.toString(), cellSize * 0.5, Colors.WHITE, font) {
		setTextBounds(Rectangle(0.0, 0.0, bgBest.width, cellSize - 24.0))
		alignment = TextAlignment.MIDDLE_CENTER
		alignTopToTopOf(bgBest, 12.0)
		centerXOn(bgBest)
		// and here is another one
		best.observe {
			text = it.toString()
		}
	}

	val bgScore = roundRect(cellSize * 1.5, cellSize * 0.8, 5.0, fill = Colors["#bbae9e"]) {
		alignRightToLeftOf(bgBest, 24)
		alignTopToTopOf(bgBest)
	}

	text("SCORE", cellSize * 0.25, RGBA(239, 226, 210), font) {
		centerXOn(bgScore)
		alignTopToTopOf(bgScore, 5.0)
	}
	// here is a new change
	text(score.value.toString(), cellSize * 0.5, Colors.WHITE, font) {
		setTextBounds(Rectangle(0.0, 0.0, bgScore.width, cellSize - 24.0))
		alignment = TextAlignment.MIDDLE_CENTER
		alignTopToTopOf(bgScore, 12.0)
		centerXOn(bgScore)
		// and here is another one
		score.observe {
			text = it.toString()
		}
	}

	//Here we will use container {...} block for each button and add a background and an image to it. It will help us
	//specify onClick events in the next step. This way a user will be able to click on a whole container as a button.
	val btnSize = cellSize * 0.3
	//Import images into game
	val restartImg = resourcesVfs["restart.png"].readBitmap()
	val undoImg = resourcesVfs["undo.png"].readBitmap()
	val restartBlock = container {
		val background = roundRect(btnSize, btnSize, 5.0, fill = RGBA(185, 174, 160))
		image(restartImg) {
			size(btnSize * 0.8, btnSize * 0.8)
			centerOn(background)
		}
		alignTopToBottomOf(bgBest, 5)
		alignRightToRightOf(bgField)
		onClick {
			this@Korge.restart()
		}
	}
	val undoBlock = container {
		val background = roundRect(btnSize, btnSize, 5.0, fill = RGBA(185, 174, 160))
		image(undoImg) {
			size(btnSize * 0.6, btnSize * 0.6)
			centerOn(background)
		}
		alignTopToTopOf(restartBlock)
		alignRightToLeftOf(restartBlock, 5.0)
		//When a user clicks on the undo block, we need to undo the last move he/she made by restoring the previous state
		//of the field. We can get this state from the history.undo call.
		onClick {
			this@Korge.restoreField(history.undo())
		}
	}

	//Now we call generateBlock() to generate a new block when the game starts
//	generateBlock()
	//restore the field if its state was saved during previous game launch. If it wasn't, we need to generate a new block.
	if (!history.isEmpty()) {
		restoreField(history.currentElement)
	} else {
		generateBlockAndSave()
	}


	//Now we can define keys.down listener for onKeyDown event.
	//Inside down block, we get a KeyEvent as it. KeyEvent has special properties like type (in this case - Key.Type.DOWN),
	//id, key, keyCode and character.
//	keys.down {
//		when (it.key) {
//			Key.LEFT -> moveBlocksTo(Direction.LEFT)
//			Key.RIGHT -> moveBlocksTo(Direction.RIGHT)
//			Key.UP -> moveBlocksTo(Direction.TOP)
//			Key.DOWN -> moveBlocksTo(Direction.BOTTOM)
//			else -> Unit
//		}
//	}

	//It will listen to mouse events and check if the user swipes via their mouse. A swipe is a gesture when the mouse
	//is pressed, moved a few pixels at some direction and then released. The onSwipe listener lets us define a
	//SwipeDirection - one of 4 possible movement directions, and a threshold - the quantity of pixels a mouse should be
	//moved to generate this event (once the event is generated, it won't be generated again until a user releases the mouse).
	onSwipe(20.0) {
		when (it.direction) {
			SwipeDirection.LEFT -> moveBlocksTo(Direction.LEFT)
			SwipeDirection.RIGHT -> moveBlocksTo(Direction.RIGHT)
			SwipeDirection.TOP -> moveBlocksTo(Direction.TOP)
			SwipeDirection.BOTTOM -> moveBlocksTo(Direction.BOTTOM)
		}
	}
}

//Now let's add the function moveBlocksTo(direction).
fun Stage.moveBlocksTo(direction: Direction) {
//Add a few preliminary checks. We need to check whether the animation is already running when a gamer presses a key
//or swipes. After that we need to check if there are available moves or the game is over (in the second case we'll
//show a special overlay offering to start the game over). So let's add two special flags isAnimationRunning and
//isGameOver before the main function. If the animation is running, we simply return from the function. Otherwise we
//check if there are available moves.
	if (isAnimationRunning) return
	if (!map.hasAvailableMoves()) {
		if (!isGameOver) {
			isGameOver = true
			showGameOver {
				isGameOver = false
				restart()
			}
		}
		return
	}


	//Here we create two mutable lists: one for moves (pairs of the moving block id and a new position) and the other one
	//for merges (triples of two merging block ids and a new position).
	val moves = mutableListOf<Pair<Int, Position>>()
	val merges = mutableListOf<Triple<Int, Int, Position>>()

	//First we need to calculate a new map and all moves and merges of the blocks on the field. Then, if the old map and
	//the new map are different, we need to animate the changes
	//Then we calculate a new map - we pass a copy of the current map that will be changed during calculation, a direction
	//of the movement and our two lists.
	val newMap = calculateNewMap(map.copy(), direction, moves, merges)

	//we check that two maps are different (using equals operator defined in PositionMap class). If so, we set isAnimationRunning
	//to true and show an animation of moves and merges. When the animation ends, we assign the new map to the current one,
	//generate a new block and set isAnimationRunning flag to false.
	if (map != newMap) {
		isAnimationRunning = true
		showAnimation(moves, merges) {
			// when animation ends
			map = newMap
			generateBlockAndSave()
			isAnimationRunning = false

			//We define a new variable points with the number of points that should be added to the current score, we go
			//through the merges list and add the block's value to the points variable. Then we just add collected points
			//to the current score and update it.
			val points = merges.sumOf { numberFor(it.first).value }
			score.update(score.value + points)
		}
	}
}

//Create a function in which moves, merges and a new map should be calculated.
fun calculateNewMap(
	map: PositionMap,
	direction: Direction,
	moves: MutableList<Pair<Int, Position>>,
	merges: MutableList<Triple<Int, Int, Position>>
): PositionMap {
	//First, we create a new PositionMap instance, define a start index based on the direction (it's a number of a column
	//or a row with which we start calculating) and define a current index of a column or a row (columnRow variable).
	val newMap = PositionMap()
	val startIndex = when (direction) {
		Direction.LEFT, Direction.TOP -> 0
		Direction.RIGHT, Direction.BOTTOM -> 3
	}
	var columnRow = startIndex

	//Second, we define an inner function newPosition that takes a line (row/column) number and returns the next position
	//based on the movement direction.
	fun newPosition(line: Int) = when (direction) {
		Direction.LEFT -> Position(columnRow++, line)
		Direction.RIGHT -> Position(columnRow--, line)
		Direction.TOP -> Position(line, columnRow++)
		Direction.BOTTOM -> Position(line, columnRow--)
	}

	//Third, we go through all the lines (from 0 to 3), get current position that has a block and get current block id,
	//get next position that has a block and get next id, then check whether the current number and the next number are
	//the same or not. If yes - we merge them: we clear the next position on the map (the current one will no longer be
	//viewed after the current check), define the current id in the new position on the new map and add a triple to the
	//merges list. If no - we move the current number: we define the current id in the new position on the new map and
	//add a pair to the moves list. Then we update the position of the current block and repeat the previous steps.
	for (line in 0..3) {
		var curPos = map.getNotEmptyPositionFrom(direction, line)
		columnRow = startIndex
		while (curPos != null) {
			val newPos = newPosition(line)
			val curId = map[curPos.x, curPos.y]
			map[curPos.x, curPos.y] = -1

			val nextPos = map.getNotEmptyPositionFrom(direction, line)
			val nextId = nextPos?.let { map[it.x, it.y] }
			//two blocks are equal
			if (nextId != null && numberFor(curId) == numberFor(nextId)) {
				//merge these blocks
				map[nextPos.x, nextPos.y] = -1
				newMap[newPos.x, newPos.y] = curId
				merges += Triple(curId, nextId, newPos)
			} else {
				//add old block
				newMap[newPos.x, newPos.y] = curId
				moves += Pair(curId, newPos)
			}
			curPos = map.getNotEmptyPositionFrom(direction, line)
		}
	}
	//After going through all the positions is done, we return a new map.
	return newMap
}

//The animation is suspending, so we should use launchImmediately function and a Stage instance to create a coroutine in
//which the animation will be running.
fun Stage.showAnimation(
	moves: List<Pair<Int, Position>>,
	merges: List<Triple<Int, Int, Position>>,
	onEnd: () -> Unit
) = launchImmediately {
	//Here we use animateSequence function to create an Animator instance. Inside it we define a parallel animation and
	//a block with onEnd() callback.
	animateSequence {
		//Inside parallel block we go through the moves list and define a movement animation via moveTo extension function
		//in Animator, we also go through the merges list and define a sequence animation for each merge triple.
		parallel {
			moves.forEach { (id, pos) ->
				blocks[id]!!.moveTo(columnX(pos.x), rowY(pos.y), 0.15.seconds, Easing.LINEAR)
			}
			merges.forEach { (id1, id2, pos) ->
				//Inside sequence block we execute parallel movement animation of both blocks
				sequence {
					parallel {
						blocks[id1]!!.moveTo(columnX(pos.x), rowY(pos.y), 0.15.seconds, Easing.LINEAR)
						blocks[id2]!!.moveTo(columnX(pos.x), rowY(pos.y), 0.15.seconds, Easing.LINEAR)
					}
					//then we execute code in block element where we delete both blocks and create a new one with the id
					//of the first block
					block {
						//.next() isn't working
						val nextNumber = numberFor(id1).next()
						deleteBlock(id1)
						deleteBlock(id2)
						createNewBlockWithId(id1, nextNumber, pos)
					}
					//then we use a sequenceLazy animation element to animate scale of a block.
					sequenceLazy {
						animateScale(blocks[id1]!!)
					}
					//Note: this code inside sequence is executed right away when the showAnimation function is called,
					//the code inside sequenceLazy is executed only when the animation inside this animation element
					//should be shown. That's why we use sequenceLazy to animate scale of a block - the block that we
					//animate there is created only in the block element, it's not available when the whole animation
					//starts. We don't use sequenceLazy everywhere because this animation function is quite expensive in
					//comparison with sequence.
				}
			}
		}
		block {
			onEnd()
		}
	}
}

//The last function we need to add is animateScale(block). It uses Animator as a receiver and calls its tween function
//twice to animate position and scale at the same time.
fun Animator.animateScale(block: Block) {
	val x = block.x
	val y = block.y
	val scale = block.scale
	//These tweening animations will be executed sequentially because the animateScale function is called inside
	//sequenceLazy animation block.
	tween(
		block::x[x - 4],
		block::y[y - 4],
		block::scale[scale + 0.1],
		time = 0.1.seconds,
		easing = Easing.LINEAR
	)
	tween(
		block::x[x],
		block::y[y],
		block::scale[scale],
		time = 0.1.seconds,
		easing = Easing.LINEAR
	)
}

//Now we need to add a special function showGameOver that will show a game over overlay and execute a special callback
//(provided as a parameter onRestart) after a gamer clicks "Try again". This function should be called on a Container
//(and it is called on our Stage), thus we can add other views to our stage
fun Container.showGameOver(onRestart: () -> Unit) = container {
	//In the first line, there is a container {...} call. This way we add an overlay container to the Stage, this
	//container when the gamer clicks "Try again". All other views added inside the showGameOver function will actually
	//be added to this overlay container, so their coordinates should be determined relative to the container bounds.

	fun restart() {
		this@container.removeFromParent()
		onRestart()
	}

	position(leftIndent, topIndent)

	roundRect(fieldSize, fieldSize, 5.0, fill = Colors["#FFFFFF33"])

	//We'll use a text view for title and a uiText view for clickable text "Try again". UIText is a special view in KorGE
	//that lets you define different text formats (color, size and font) for normal, over and down states.
	text("Game Over", 60.0, Colors.BLACK, font) {
		centerBetween(0.0, 0.0, fieldSize, fieldSize)
		y -= 60
	}
	uiText("Try again", 120.0, 35.0) {
		centerBetween(0.0, 0.0, fieldSize, fieldSize)
		y += 20.0
		textSize = 40.0
		textFont = font
		textColor = RGBA(0, 0, 0)
		onOver { textColor = RGBA(90, 90, 90) }
		onOut { textColor = RGBA(0, 0, 0) }
		onDown { textColor = RGBA(120, 120, 120) }
		onUp { textColor = RGBA(120, 120, 120) }
		//We also need to define an onClick {...} listener on the text "Try again" that will remove the overlay container
		//from the stage and call onRestart callback.
		onClick { restart() }
	}

	keys.down {
		when (it.key) {
			//The same action should be performed when a gamer presses Enter or Space keys.
			Key.ENTER, Key.SPACE -> restart()
			else -> Unit
		}
	}
}

//Here we create a new PositionMap object and assign it to the map property, remove all Block views added to the blocks
//mutable map as values, from the parent container, then we clear the blocks map (i.e. remove all map entries and make
//map empty) and generate a new Block on the currently empty field.
fun Container.restart() {
	map = PositionMap()
	blocks.values.forEach { it.removeFromParent() }
	blocks.clear()
	//update score when the user chooses to restart the game.
	score.update(0)
	//clear the history, so after closing and reopening the game the old game state can't be restored (instead, the new
	//game state should be restored).
	history.clear()
	generateBlockAndSave()
}

//Create a new function Container.restoreField(History.Element) in which the field will be cleared, its state and the
//current score will be updated to the previous values, and blocks on the field will be replaced by the old ones.
fun Container.restoreField(history: History.Element) {
	map.forEach { if (it != -1) deleteBlock(it) }
	map = PositionMap()
	score.update(history.score)
	freeId = 0
	val numbers = history.numberIds.map {
		if (it >= 0 && it < Number.values().size)
			Number.values()[it]
		else null
	}
	numbers.forEachIndexed { i, number ->
		if (number != null) {
			val newId = createNewBlock(number, Position(i % 4, i / 4))
			map[i % 4, i / 4] = newId
		}
	}
}

//After the start of the game and after block movement, we need to generate a new block on the game field. Let's create
// a special function generateBlock with Container receiver that will do that. It should get a random free position from
// our PositionMap (if such a position exists), select a number for the new block (in 90% of cases - "2" or ** Number.ZERO,
// in 10% - ** "4" or Number.ONE), create this block and add it to map
fun Container.generateBlockAndSave() {
	val position = map.getRandomFreePosition() ?: return
	val number = if (Random.nextDouble() < 0.9) Number.ZERO else Number.ONE
	val newId = createNewBlock(number, position)
	map[position.x, position.y] = newId
	//Here we use undefined function PositionMap.toNumberIds().
	history.add(map.toNumberIds(), score.value)
}

//The last function we need to add now is createNewBlock(number, position) that takes number and position, calculates
//new id, calls createNewBlock(id, number, position) function and returns the new id
fun Container.createNewBlock(number: Number, position: Position): Int {
	val id = freeId++
	createNewBlockWithId(id, number, position)
	return id
}

//The next function is createNewBlockWithId(id, number, position). It takes id, number and position for a new block,
//creates this block and adds it to the blocks map
fun Container.createNewBlockWithId(id: Int, number: Number, position: Position) {
	//Notice that we use block() function here, so a new block will be added to the receiver Container automatically.
	blocks[id] = block(number).position(columnX(position.x), rowY(position.y))
}