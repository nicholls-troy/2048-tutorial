import com.soywiz.korim.color.RGBA

//Add enum class Number here. This enum will have two properties: value(Int) and color(RGBA). The value will define a
//number that we will show in blocks, the color -  a color that blocks with this number will be painted.
enum class Number(val value: Int, val color: RGBA) {
    //Since the field may contain a maximum of 16 blocks with different numbers (two of them may be two "4") and a user
    //will be able to merge them into one block with the next number, we define 17 values in our enum.
    ZERO(2, RGBA(240, 228, 218)),
    ONE(4, RGBA(236, 224, 201)),
    TWO(8, RGBA(255, 178, 120)),
    THREE(16, RGBA(254, 150, 92)),
    FOUR(32, RGBA(247, 123, 97)),
    FIVE(64, RGBA(235, 88, 55)),
    SIX(128, RGBA(236, 220, 146)),
    SEVEN(256, RGBA(240, 212, 121)),
    EIGHT(512, RGBA(244, 206, 96)),
    NINE(1024, RGBA(248, 200, 71)),
    TEN(2048, RGBA(256, 194, 46)),
    ELEVEN(4096, RGBA(104, 130, 249)),
    TWELVE(8192, RGBA(51, 85, 247)),
    THIRTEEN(16384, RGBA(10, 47, 222)),
    FOURTEEN(32768, RGBA(9, 43, 202)),
    FIFTEEN(65536, RGBA(181, 37, 188)),
    SIXTEEN(131072, RGBA(166, 34, 172));

    fun next() = values()[(ordinal + 1) % values().size]
}