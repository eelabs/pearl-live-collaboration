import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.markup.*
import java.awt.Color

object Highlighter {

    fun highlight(caret: Caret, color: Color): RangeHighlighter? {
        val caretOffset = caret.getOffset()
        val atr = TextAttributes()
        atr.backgroundColor = color
        atr.foregroundColor = color
        atr.effectColor = color
        val markup = caret.editor.markupModel
        return Utility.suppressException {
            markup.addRangeHighlighter(caretOffset, caretOffset, HighlighterLayer.CARET_ROW,
                    atr, HighlighterTargetArea.EXACT_RANGE)
        }
    }

    fun highlightSelection(markup: MarkupModel, color: Color, startOffset: Int, endOffset: Int): RangeHighlighter? {
        val atr = TextAttributes()
        atr.backgroundColor = color
        atr.effectColor = color
        return Utility.suppressException {
            markup.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION, atr, HighlighterTargetArea.EXACT_RANGE)
        }

    }


}