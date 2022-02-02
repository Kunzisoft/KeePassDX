package com.tokenautocomplete

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Layout
import android.text.NoCopySpan
import android.text.Selection
import android.text.SpanWatcher
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputMethodManager
import android.widget.ListView
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import java.io.Serializable
import java.lang.reflect.ParameterizedType
import java.util.*

/**
 * GMail style auto complete view with easy token customization
 * override getViewForObject to provide your token view
 * <br></br>
 * Created by mgod on 9/12/13.
 *
 * @author mgod
 */
abstract class TokenCompleteTextView<T: Any> : AppCompatAutoCompleteTextView, OnEditorActionListener,
    ViewSpan.Layout {
    //When the user clicks on a token...
    enum class TokenClickStyle(val isSelectable: Boolean) {
        None(false),  //...do nothing, but make sure the cursor is not in the token
        Delete(false),  //...delete the token
        Select(true),  //...select the token. A second click will delete it.
        SelectDeselect(true);

    }

    private var tokenizer: Tokenizer? = null
    private var selectedObject: T? = null
    private var listener: TokenListener<T>? = null
    private var spanWatcher: TokenSpanWatcher = TokenSpanWatcher()
    private var textWatcher: TokenTextWatcher = TokenTextWatcher()
    private var countSpan: CountSpan = CountSpan()
    private var hiddenContent: SpannableStringBuilder? = null
    private var tokenClickStyle: TokenClickStyle? = TokenClickStyle.None
    private var prefix: CharSequence? = null
    private var lastLayout: Layout? = null
    private var initialized = false
    private var performBestGuess = true
    private var preventFreeFormText = true
    private var savingState = false
    private var shouldFocusNext = false
    private var allowCollapse = true
    private var internalEditInProgress = false
    private var inBatchEditAPI26to29Workaround = false
    private var tokenLimit = -1

    /**
     * Android M/API 30 introduced a change to the SpannableStringBuilder that triggers additional
     * text change callbacks when we do our token replacement. It's supposed to report if it's a
     * recursive call to the callbacks to let the recipient handle nested calls differently, but
     * for some reason, in our case the first and second callbacks both report a depth of 1 and only
     * on the third callback do we get a depth of 2, so we need to track this ourselves.
     */
    private var ignoreNextTextCommit = false

    @Transient
    private var lastCompletionText: String? = null

    private val hintVisible: Boolean
        get() {
            return text.getSpans(0, text.length, HintSpan::class.java).isNotEmpty()
        }

    /**
     * Add the TextChangedListeners
     */
    protected open fun addListeners() {
        val text = text
        if (text != null) {
            text.setSpan(spanWatcher, 0, text.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            addTextChangedListener(textWatcher)
        }
    }

    /**
     * Remove the TextChangedListeners
     */
    protected open fun removeListeners() {
        val text = text
        if (text != null) {
            val spanWatchers = text.getSpans(0, text.length, TokenSpanWatcher::class.java)
            for (watcher in spanWatchers) {
                text.removeSpan(watcher)
            }
            removeTextChangedListener(textWatcher)
        }
    }

    /**
     * Initialise the variables and various listeners
     */
    private fun init() {
        if (initialized) return

        // Initialise variables
        setTokenizer(CharacterTokenizer(listOf(',', ';'), ","))

        // Initialise TextChangedListeners
        addListeners()
        setTextIsSelectable(false)
        isLongClickable = false

        //In theory, get the soft keyboard to not supply suggestions. very unreliable
        inputType = inputType or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        setHorizontallyScrolling(false)

        // Listen to IME action keys
        setOnEditorActionListener(this)

        // Initialise the text filter (listens for the split chars)
        filters =
            arrayOf(InputFilter { source, _, _, _, destinationStart, destinationEnd ->
                if (internalEditInProgress) {
                    return@InputFilter null
                }

                // Token limit check
                if (tokenLimit != -1 && objects.size == tokenLimit) {
                    return@InputFilter ""
                }

                //Detect split characters, remove them and complete the current token instead
                if (tokenizer!!.containsTokenTerminator(source)) {
                    //Only perform completion if we don't allow free form text, or if there's enough
                    //content to believe this should be a token
                    if (preventFreeFormText || currentCompletionText().isNotEmpty()) {
                        performCompletion()
                        return@InputFilter ""
                    }
                }

                //We need to not do anything when we would delete the prefix
                prefix?.also { prefix ->
                    if (destinationStart < prefix.length) {
                        //when setText is called, which should only be called during restoring,
                        //destinationStart and destinationEnd are 0. If not checked, it will clear out
                        //the prefix.
                        //This is why we need to return null in this if condition to preserve state.
                        if (destinationStart == 0 && destinationEnd == 0) {
                            return@InputFilter null
                        } else return@InputFilter if (destinationEnd <= prefix.length) {
                            //Don't do anything
                            prefix.subSequence(destinationStart, destinationEnd)
                        } else {
                            //Delete everything up to the prefix
                            prefix.subSequence(destinationStart, prefix.length)
                        }
                    }
                }
                null
            })
        initialized = true
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    override fun performFiltering(text: CharSequence, keyCode: Int) {
        val filter = filter
        filter?.filter(currentCompletionText(), this)
    }

    fun setTokenizer(t: Tokenizer) {
        tokenizer = t
    }

    /**
     * Set the action to be taken when a Token is clicked
     *
     * @param cStyle The TokenClickStyle
     */
    fun setTokenClickStyle(cStyle: TokenClickStyle) {
        tokenClickStyle = cStyle
    }

    /**
     * Set the listener that will be notified of changes in the Token list
     *
     * @param l The TokenListener
     */
    fun setTokenListener(l: TokenListener<T>?) {
        listener = l
    }

    /**
     * Override if you want to prevent a token from being added. Defaults to false.
     * @param token the token to check
     * @return true if the token should not be added, false if it's ok to add it.
     */
    open fun shouldIgnoreToken(token: T): Boolean {
        return false
    }

    /**
     * Override if you want to prevent a token from being removed. Defaults to true.
     * @param token the token to check
     * @return false if the token should not be removed, true if it's ok to remove it.
     */
    open fun isTokenRemovable(@Suppress("unused_parameter") token: T): Boolean {
        return true
    }

    /**
     * A String of text that is shown before all the tokens inside the EditText
     * (Think "To: " in an email address field. I would advise against this: use a label and a hint.
     *
     * @param p String with the hint
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setPrefix(p: CharSequence) {
        //Have to clear and set the actual text before saving the prefix to avoid the prefix filter
        val prevPrefix = prefix
        prefix = p
        val text = text
        if (text != null) {
            internalEditInProgress = true
            if (prevPrefix.isNullOrEmpty()) {
                text.insert(0, p)
            } else {
                text.replace(0, prevPrefix.length, p)
            }
            internalEditInProgress = false
        }
        //prefix = p;
        updateHint()
    }

    /**
     *
     * You can get a color integer either using
     * [androidx.core.content.ContextCompat.getColor]
     * or with [android.graphics.Color.parseColor].
     *
     * [android.graphics.Color.parseColor]
     * accepts these formats (copied from android.graphics.Color):
     * You can use: '#RRGGBB',  '#AARRGGBB'
     * or one of the following names: 'red', 'blue', 'green', 'black', 'white',
     * 'gray', 'cyan', 'magenta', 'yellow', 'lightgray', 'darkgray', 'grey',
     * 'lightgrey', 'darkgrey', 'aqua', 'fuchsia', 'lime', 'maroon', 'navy',
     * 'olive', 'purple', 'silver', 'teal'.
     *
     * @param prefix prefix
     * @param color A single color value in the form 0xAARRGGBB.
     */
    fun setPrefix(prefix: CharSequence, color: Int) {
        val spannablePrefix = SpannableString(prefix)
        spannablePrefix.setSpan(ForegroundColorSpan(color), 0, spannablePrefix.length, 0)
        setPrefix(spannablePrefix)
    }

    /**
     * Get the list of Tokens
     *
     * @return List of tokens
     */
    val objects: List<T>
        get() {
            val objects = ArrayList<T>()
            var text = text
            if (hiddenContent != null) {
                text = hiddenContent
            }
            for (span in text.getSpans(0, text.length, TokenImageSpan::class.java)) {
                @Suppress("unchecked_cast")
                objects.add(span.token as T)
            }
            return objects
        }

    /**
     * Get the content entered in the text field, including hidden text when ellipsized
     *
     * @return CharSequence of the entered content
     */
    val contentText: CharSequence
        get() = hiddenContent ?: text

    /**
     * Set whether we try to guess an entry from the autocomplete spinner or just use the
     * defaultObject implementation for inline token completion.
     *
     * @param guess true to enable guessing
     */
    fun performBestGuess(guess: Boolean) {
        performBestGuess = guess
    }

    /**
     * If set to true, the only content in this view will be the tokens and the current completion
     * text. Use this setting to create things like lists of email addresses. If false, it the view
     * will allow text in addition to tokens. Use this if you want to use the token search to find
     * things like user names or hash tags to put in with text.
     *
     * @param prevent true to prevent non-token text. Defaults to true.
     */
    fun preventFreeFormText(prevent: Boolean) {
        preventFreeFormText = prevent
    }

    /**
     * Set whether the view should collapse to a single line when it loses focus.
     *
     * @param allowCollapse true if it should collapse
     */
    fun allowCollapse(allowCollapse: Boolean) {
        this.allowCollapse = allowCollapse
    }

    /**
     * Set a number of tokens limit.
     *
     * @param tokenLimit The number of tokens permitted. -1 value disables limit.
     */
    @Suppress("unused")
    fun setTokenLimit(tokenLimit: Int) {
        this.tokenLimit = tokenLimit
    }

    /**
     * A token view for the object
     *
     * @param obj the object selected by the user from the list
     * @return a view to display a token in the text field for the object
     */
    protected abstract fun getViewForObject(obj: T): View?

    /**
     * Provides a default completion when the user hits , and there is no item in the completion
     * list
     *
     * @param completionText the current text we are completing against
     * @return a best guess for what the user meant to complete or null if you don't want a guess
     */
    protected abstract fun defaultObject(completionText: String): T?

    //Replace token spans
    //Need to take the existing tet buffer and
    // - replace all tokens with a decent string representation of the object
    // - set the selection span to the corresponding location in the new CharSequence
    /**
     * Correctly build accessibility string for token contents
     *
     * This seems to be a hidden API, but there doesn't seem to be another reasonable way
     * @return custom string for accessibility
     */
    @Suppress("MemberVisibilityCanBePrivate")
    open val textForAccessibility: CharSequence
        get() {
            if (objects.isEmpty()) {
                return text
            }
            var description = SpannableStringBuilder()
            val text = text
            var selectionStart = -1
            var selectionEnd = -1
            var i: Int
            //Need to take the existing tet buffer and
            // - replace all tokens with a decent string representation of the object
            // - set the selection span to the corresponding location in the new CharSequence
            i = 0
            while (i < text.length) {

                //See if this is where we should start the selection
                val origSelectionStart = Selection.getSelectionStart(text)
                if (i == origSelectionStart) {
                    selectionStart = description.length
                }
                val origSelectionEnd = Selection.getSelectionEnd(text)
                if (i == origSelectionEnd) {
                    selectionEnd = description.length
                }

                //Replace token spans
                val tokens = text.getSpans(i, i, TokenImageSpan::class.java)
                if (tokens.isNotEmpty()) {
                    val token = tokens[0]
                    description =
                        description.append(tokenizer!!.wrapTokenValue(token.token.toString()))
                    i = text.getSpanEnd(token)
                    ++i
                    continue
                }
                description = description.append(text.subSequence(i, i + 1))
                ++i
            }
            val origSelectionStart = Selection.getSelectionStart(text)
            if (i == origSelectionStart) {
                selectionStart = description.length
            }
            val origSelectionEnd = Selection.getSelectionEnd(text)
            if (i == origSelectionEnd) {
                selectionEnd = description.length
            }
            if (selectionStart >= 0 && selectionEnd >= 0) {
                Selection.setSelection(description, selectionStart, selectionEnd)
            }
            return description
        }

    /**
     * Clear the completion text only.
     */
    @Suppress("unused")
    fun clearCompletionText() {
        //Respect currentCompletionText in case hint is visible or if other checks are added.
        if (currentCompletionText().isEmpty()) {
            return
        }
        val currentRange = currentCandidateTokenRange
        internalEditInProgress = true
        text.delete(currentRange.start, currentRange.end)
        internalEditInProgress = false
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            val text = textForAccessibility
            event.fromIndex = Selection.getSelectionStart(text)
            event.toIndex = Selection.getSelectionEnd(text)
            event.itemCount = text.length
        }
    }
    //Don't try to search the hint for possible tokenizable strings

    //We want to find the largest string that contains the selection end that is not already tokenized
    private val currentCandidateTokenRange: Range
        get() {
            val editable = text
            val cursorEndPosition = selectionEnd
            var candidateStringStart = prefix?.length ?: 0
            var candidateStringEnd = editable.length
            if (hintVisible) {
                //Don't try to search the hint for possible tokenizable strings
                candidateStringEnd = candidateStringStart
            }

            //We want to find the largest string that contains the selection end that is not already tokenized
            val spans = editable.getSpans(prefix?.length ?: 0, editable.length, TokenImageSpan::class.java)
            for (span in spans) {
                val spanEnd = editable.getSpanEnd(span)
                if (spanEnd in (candidateStringStart + 1)..cursorEndPosition) {
                    candidateStringStart = spanEnd
                }
                val spanStart = editable.getSpanStart(span)
                if (candidateStringEnd > spanStart && cursorEndPosition <= spanEnd) {
                    candidateStringEnd = spanStart
                }
            }
            val tokenRanges =
                tokenizer!!.findTokenRanges(editable, candidateStringStart, candidateStringEnd)
            for (range in tokenRanges) {
                @Suppress("unused")
                if (range.start <= cursorEndPosition && cursorEndPosition <= range.end) {
                    return range
                }
            }
            return Range(cursorEndPosition, cursorEndPosition)
        }

    /**
     * Override if you need custom logic to provide a sting representation of a token
     * @param token the token to convert
     * @return the string representation of the token. Defaults to [Object.toString]
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun tokenToString(token: T): CharSequence {
        return token.toString()
    }

    protected open fun currentCompletionText(): String {
        if (hintVisible) return "" //Can't have any text if the hint is visible
        val editable = text
        val currentRange = currentCandidateTokenRange
        val result = TextUtils.substring(editable, currentRange.start, currentRange.end)
        Log.d(TAG, "Current completion text: $result")
        return result
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun maxTextWidth(): Float {
        return (width - paddingLeft - paddingRight).toFloat()
    }

    override val maxViewSpanWidth: Int
        get() = maxTextWidth().toInt()

    fun redrawTokens() {
        // There's no straight-forward way to convince the widget to redraw the text and spans. We trigger a redraw by
        // making an invisible change (either adding or removing a dummy span).
        val text = text ?: return
        val textLength = text.length
        val dummySpans = text.getSpans(0, textLength, DummySpan::class.java)
        if (dummySpans.isNotEmpty()) {
            text.removeSpan(DummySpan.INSTANCE)
        } else {
            text.setSpan(
                DummySpan.INSTANCE,
                0,
                textLength,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }
    }

    override fun enoughToFilter(): Boolean {
        if (tokenizer == null || hintVisible) {
            return false
        }
        val cursorPosition = selectionEnd
        if (cursorPosition < 0) {
            return false
        }
        val currentCandidateRange = currentCandidateTokenRange

        //Don't allow 0 length entries to filter
        @Suppress("MemberVisibilityCanBePrivate")
        return currentCandidateRange.length() >= threshold.coerceAtLeast(1)
    }

    override fun performCompletion() {
        if ((adapter == null || listSelection == ListView.INVALID_POSITION) && enoughToFilter()) {
            val bestGuess: Any? = if (adapter != null && adapter.count > 0 && performBestGuess) {
                adapter.getItem(0)
            } else {
                defaultObject(currentCompletionText())
            }
            replaceText(convertSelectionToString(bestGuess))
        } else {
            super.performCompletion()
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val superConn = super.onCreateInputConnection(outAttrs)
        val conn = TokenInputConnection(superConn, true)
        outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
        outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        return conn
    }

    /**
     * Create a token and hide the keyboard when the user sends the DONE IME action
     * Use IME_NEXT if you want to create a token and go to the next field
     */
    private fun handleDone() {
        // Attempt to complete the current token token
        performCompletion()

        // Hide the keyboard
        val imm = context.getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val handled = super.onKeyUp(keyCode, event)
        if (shouldFocusNext) {
            shouldFocusNext = false
            handleDone()
        }
        return handled
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        var handled = false
        when (keyCode) {
            KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> if (event?.hasNoModifiers() == true) {
                shouldFocusNext = true
                handled = true
            }
            KeyEvent.KEYCODE_DEL -> handled = !canDeleteSelection(1) || deleteSelectedObject()
        }
        return handled || super.onKeyDown(keyCode, event)
    }

    private fun deleteSelectedObject(): Boolean {
        if (tokenClickStyle?.isSelectable == true) {
            val text = text ?: return false
            @Suppress("unchecked_cast")
            val spans: Array<TokenImageSpan> =
                text.getSpans(0, text.length, TokenImageSpan::class.java) as Array<TokenImageSpan>
            for (span in spans) {
                if (span.view.isSelected) {
                    removeSpan(text, span)
                    return true
                }
            }
        }
        return false
    }

    override fun onEditorAction(view: TextView, action: Int, keyEvent: KeyEvent?): Boolean {
        if (action == EditorInfo.IME_ACTION_DONE) {
            handleDone()
            return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val text = text
        var handled = false
        if (tokenClickStyle == TokenClickStyle.None) {
            handled = super.onTouchEvent(event)
        }
        if (isFocused && text != null && lastLayout != null && action == MotionEvent.ACTION_UP) {
            val offset = getOffsetForPosition(event.x, event.y)
            if (offset != -1) {
                @Suppress("unchecked_cast")
                val links: Array<TokenImageSpan> =
                    text.getSpans(offset, offset, TokenImageSpan::class.java) as Array<TokenImageSpan>
                if (links.isNotEmpty()) {
                    links[0].onClick()
                    handled = true
                } else {
                    //We didn't click on a token, so if any are selected, we should clear that
                    clearSelections()
                }
            }
        }
        if (!handled && tokenClickStyle != TokenClickStyle.None) {
            handled = super.onTouchEvent(event)
        }
        return handled
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        var selectionStart = selStart
        if (hintVisible) {
            //Don't let users select the hint
            selectionStart = 0
        }
        //Never let users select text
        val selectionEnd = selectionStart
        if (tokenClickStyle?.isSelectable == true) {
            val text = text
            if (text != null) {
                clearSelections()
            }
        }
        if (selectionStart < prefix?.length ?: 0 || selectionEnd < prefix?.length ?: 0) {
            //Don't let users select the prefix
            setSelection(prefix?.length ?: 0)
        } else {
            val text = text
            if (text != null) {
                //Make sure if we are in a span, we select the spot 1 space after the span end
                @Suppress("unchecked_cast")
                val spans: Array<TokenImageSpan> =
                    text.getSpans(selectionStart, selectionEnd, TokenImageSpan::class.java) as Array<TokenImageSpan>
                for (span in spans) {
                    val spanEnd = text.getSpanEnd(span)
                    if (selectionStart <= spanEnd && text.getSpanStart(span) < selectionStart) {
                        if (spanEnd == text.length) setSelection(spanEnd) else setSelection(spanEnd + 1)
                        return
                    }
                }
            }
            super.onSelectionChanged(selectionStart, selectionEnd)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        lastLayout = layout //Used for checking text positions
    }

    /**
     * Collapse the view by removing all the tokens not on the first line. Displays a "+x" token.
     * Restores the hidden tokens when the view gains focus.
     *
     * @param hasFocus boolean indicating whether we have the focus or not.
     */
    open fun performCollapse(hasFocus: Boolean) {
        internalEditInProgress = true
        if (!hasFocus) {
            // Display +x thingy/ellipse if appropriate
            val text = text
            if (text != null && hiddenContent == null && lastLayout != null) {

                //Ellipsize copies spans, so we need to stop listening to span changes here
                text.removeSpan(spanWatcher)
                val temp = if (preventFreeFormText) countSpan else null
                val ellipsized = SpanUtils.ellipsizeWithSpans(
                    prefix, temp, objects.size,
                    lastLayout!!.paint, text, maxTextWidth()
                )
                if (ellipsized != null) {
                    hiddenContent = SpannableStringBuilder(text)
                    setText(ellipsized)
                    TextUtils.copySpansFrom(
                        ellipsized, 0, ellipsized.length,
                        TokenImageSpan::class.java, getText(), 0
                    )
                    TextUtils.copySpansFrom(
                        text, 0, hiddenContent!!.length,
                        TokenImageSpan::class.java, hiddenContent, 0
                    )
                    hiddenContent!!.setSpan(
                        spanWatcher,
                        0,
                        hiddenContent!!.length,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE
                    )
                } else {
                    getText().setSpan(
                        spanWatcher,
                        0,
                        getText().length,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
            }
        } else {
            if (hiddenContent != null) {
                text = hiddenContent
                TextUtils.copySpansFrom(
                    hiddenContent, 0, hiddenContent!!.length,
                    TokenImageSpan::class.java, text, 0
                )
                hiddenContent = null
                if (hintVisible) {
                    setSelection(prefix?.length ?: 0)
                } else {
                    post { setSelection(text.length) }
                }
                @Suppress("unchecked_cast")
                val watchers: Array<TokenSpanWatcher> =
                    text.getSpans(0, text.length, TokenSpanWatcher::class.java) as Array<TokenSpanWatcher>
                if (watchers.isEmpty()) {
                    //Span watchers can get removed in setText
                    text.setSpan(spanWatcher, 0, text.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }
            }
        }
        internalEditInProgress = false
    }

    public override fun onFocusChanged(hasFocus: Boolean, direction: Int, previous: Rect?) {
        super.onFocusChanged(hasFocus, direction, previous)

        // Clear sections when focus changes to avoid a token remaining selected
        clearSelections()

        // Collapse the view to a single line
        if (allowCollapse) performCollapse(hasFocus)
    }

    override fun convertSelectionToString(selectedObject: Any?): CharSequence {
        @Suppress("unchecked_cast")
        this.selectedObject = selectedObject as T?
        return ""
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun buildSpanForObject(obj: T?): TokenImageSpan? {
        if (obj == null) {
            return null
        }
        return getViewForObject(obj)?.let { TokenImageSpan(it, obj) }
    }

    override fun replaceText(ignore: CharSequence) {
        clearComposingText()

        // Don't build a token for an empty String
        if (selectedObject?.toString().isNullOrEmpty()) return
        val tokenSpan = buildSpanForObject(selectedObject)
        val editable = text
        val candidateRange = currentCandidateTokenRange
        val original = TextUtils.substring(editable, candidateRange.start, candidateRange.end)

        //Keep track of  replacements for a bug workaround
        if (original.isNotEmpty()) {
            lastCompletionText = original
        }
        if (editable != null) {
            internalEditInProgress = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ignoreNextTextCommit = true
            }
            if (tokenSpan == null) {
                editable.replace(candidateRange.start, candidateRange.end, "")
            } else if (shouldIgnoreToken(tokenSpan.token)) {
                editable.replace(candidateRange.start, candidateRange.end, "")
                if (listener != null) {
                    listener?.onTokenIgnored(tokenSpan.token)
                }
            } else {
                val ssb = SpannableStringBuilder(tokenizer!!.wrapTokenValue(tokenToString(tokenSpan.token)))
                editable.replace(candidateRange.start, candidateRange.end, ssb)
                editable.setSpan(
                    tokenSpan,
                    candidateRange.start,
                    candidateRange.start + ssb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                editable.insert(candidateRange.start + ssb.length, " ")
            }
            internalEditInProgress = false
        }
    }

    override fun extractText(request: ExtractedTextRequest, outText: ExtractedText): Boolean {
        return try {
            super.extractText(request, outText)
        } catch (ex: IndexOutOfBoundsException) {
            Log.d(TAG, "extractText hit IndexOutOfBoundsException. This may be normal.", ex)
            false
        }
    }

    /**
     * Append a token object to the object list. May only be called from the main thread.
     *
     * @param obj the object to add to the displayed tokens
     */
    @UiThread
    fun addObjectSync(obj: T) {
        if (shouldIgnoreToken(obj)) {
            if (listener != null) {
                listener?.onTokenIgnored(obj)
            }
            return
        }
        if (tokenLimit != -1 && objects.size == tokenLimit) return
        buildSpanForObject(obj)?.also { insertSpan(it) }
        if (text != null && isFocused) setSelection(text.length)
    }

    /**
     * Append a token object to the object list. Object will be added on the main thread.
     *
     * @param obj the object to add to the displayed tokens
     */
    fun addObjectAsync(obj: T) {
        post { addObjectSync(obj) }
    }

    /**
     * Remove an object from the token list. Will remove duplicates if present or do nothing if no
     * object is present in the view. Uses [Object.equals] to find objects. May only
     * be called from the main thread
     *
     * @param obj object to remove, may be null or not in the view
     */
    @UiThread
    fun removeObjectSync(obj: T) {
        //To make sure all the appropriate callbacks happen, we just want to piggyback on the
        //existing code that handles deleting spans when the text changes
        val texts = ArrayList<Editable>()
        //If there is hidden content, it's important that we update it first
        hiddenContent?.also { texts.add(it) }
        if (text != null) {
            texts.add(text)
        }

        // If the object is currently visible, remove it
        for (text in texts) {
            @Suppress("unchecked_cast")
            val spans: Array<TokenImageSpan> =
                text.getSpans(0, text.length, TokenImageSpan::class.java) as Array<TokenImageSpan>
            for (span in spans) {
                if (span.token == obj) {
                    removeSpan(text, span)
                }
            }
        }
        updateCountSpan()
    }

    /**
     * Remove an object from the token list. Will remove duplicates if present or do nothing if no
     * object is present in the view. Uses [Object.equals] to find objects. Object
     * will be added on the main thread
     *
     * @param obj object to remove, may be null or not in the view
     */
    fun removeObjectAsync(obj: T) {
        post { removeObjectSync(obj) }
    }

    /**
     * Remove all objects from the token list. Objects will be removed on the main thread.
     */
    fun clearAsync() {
        post {
            for (obj in objects) {
                removeObjectSync(obj)
            }
        }
    }

    /**
     * Set the count span the current number of hidden objects
     */
    private fun updateCountSpan() {
        //No count span with free form text
        if (!preventFreeFormText) {
            return
        }
        val text = text
        val visibleCount = getText().getSpans(0, getText().length, TokenImageSpan::class.java).size
        countSpan.setCount(objects.size - visibleCount)
        val spannedCountText = SpannableStringBuilder(countSpan.countText)
        spannedCountText.setSpan(
            countSpan,
            0,
            spannedCountText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        internalEditInProgress = true
        val countStart = text.getSpanStart(countSpan)
        if (countStart != -1) {
            //Span is in the text, replace existing text
            //This will also remove the span if the count is 0
            text.replace(countStart, text.getSpanEnd(countSpan), spannedCountText)
        } else {
            text.append(spannedCountText)
        }
        internalEditInProgress = false
    }

    /**
     * Remove a span from the current EditText and fire the appropriate callback
     *
     * @param text Editable to remove the span from
     * @param span TokenImageSpan to be removed
     */
    private fun removeSpan(text: Editable, span: TokenImageSpan) {
        //We usually add whitespace after a token, so let's try to remove it as well if it's present
        var end = text.getSpanEnd(span)
        if (end < text.length && text[end] == ' ') {
            end += 1
        }
        internalEditInProgress = true
        text.delete(text.getSpanStart(span), end)
        internalEditInProgress = false
        if (allowCollapse && !isFocused) {
            updateCountSpan()
        }
    }

    /**
     * Insert a new span for an Object
     *
     * @param tokenSpan span to insert
     */
    private fun insertSpan(tokenSpan: TokenImageSpan) {
        val ssb = tokenizer!!.wrapTokenValue(tokenToString(tokenSpan.token))
        val editable = text ?: return

        // If we haven't hidden any objects yet, we can try adding it
        if (hiddenContent == null) {
            internalEditInProgress = true
            var offset = editable.length
            //There might be a hint visible...
            if (hintVisible) {
                //...so we need to put the object in in front of the hint
                offset = prefix?.length ?: 0
            } else {
                val currentRange = currentCandidateTokenRange
                if (currentRange.length() > 0) {
                    // The user has entered some text that has not yet been tokenized.
                    // Find the beginning of this text and insert the new token there.
                    offset = currentRange.start
                }
            }
            editable.insert(offset, ssb)
            editable.insert(offset + ssb.length, " ")
            editable.setSpan(
                tokenSpan,
                offset,
                offset + ssb.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            internalEditInProgress = false
        } else {
            val tokenText = tokenizer!!.wrapTokenValue(
                tokenToString(
                    tokenSpan.token
                )
            )
            val start = hiddenContent!!.length
            hiddenContent!!.append(tokenText)
            hiddenContent!!.append(" ")
            hiddenContent!!.setSpan(
                tokenSpan,
                start,
                start + tokenText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            updateCountSpan()
        }
    }

    private fun updateHint() {
        val text = text
        val hintText = hint
        if (text == null || hintText == null) {
            return
        }

        //Show hint if we need to
        if (prefix?.isNotEmpty() == true) {
            val hints = text.getSpans(0, text.length, HintSpan::class.java)
            var hint: HintSpan? = null
            var testLength = prefix?.length ?: 0
            if (hints.isNotEmpty()) {
                hint = hints[0]
                testLength += text.getSpanEnd(hint) - text.getSpanStart(hint)
            }
            if (text.length == testLength) {
                if (hint != null) {
                    return  //hint already visible
                }

                //We need to display the hint manually
                val tf = typeface
                var style = Typeface.NORMAL
                if (tf != null) {
                    style = tf.style
                }
                val colors = hintTextColors
                val hintSpan = HintSpan(null, style, textSize.toInt(), colors, colors)
                internalEditInProgress = true
                val spannedHint = SpannableString(hintText)
                spannedHint.setSpan(hintSpan, 0, spannedHint.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text.insert(prefix?.length ?: 0, spannedHint)
                internalEditInProgress = false
                setSelection(prefix?.length ?: 0)
            } else {
                if (hint == null) {
                    return  //hint already removed
                }

                //Remove the hint. There should only ever be one
                val sStart = text.getSpanStart(hint)
                val sEnd = text.getSpanEnd(hint)
                internalEditInProgress = true
                text.removeSpan(hint)
                text.replace(sStart, sEnd, "")
                setSelection(sStart)
                internalEditInProgress = false
            }
        }
    }

    private fun clearSelections() {
        if (tokenClickStyle?.isSelectable != true) return
        val text = text ?: return
        @Suppress("unchecked_cast")
        val tokens: Array<TokenImageSpan> =
            text.getSpans(0, text.length, TokenImageSpan::class.java) as Array<TokenImageSpan>
        var shouldRedrawTokens = false
        for (token in tokens) {
            if (token.view.isSelected) {
                token.view.isSelected = false
                shouldRedrawTokens = true
            }
        }
        if (shouldRedrawTokens) {
            redrawTokens()
        }
    }

    inner class TokenImageSpan(d: View, val token: T) : ViewSpan(d, this@TokenCompleteTextView),
        NoCopySpan {
        fun onClick() {
            val text = text ?: return
            when (tokenClickStyle) {
                TokenClickStyle.Select, TokenClickStyle.SelectDeselect -> {
                    if (!view.isSelected) {
                        clearSelections()
                        view.isSelected = true
                        redrawTokens()
                    } else if (tokenClickStyle == TokenClickStyle.SelectDeselect || !isTokenRemovable(token)) {
                        view.isSelected = false
                        redrawTokens()
                    } else if (isTokenRemovable(token)) {
                        removeSpan(text, this)
                    }
                }
                TokenClickStyle.Delete -> if (isTokenRemovable(token)) {
                    removeSpan(text, this)
                }
                TokenClickStyle.None -> if (selectionStart != text.getSpanEnd(this)) {
                    //Make sure the selection is not in the middle of the span
                    setSelection(text.getSpanEnd(this))
                }
                else -> {}
            }
        }
    }

    interface TokenListener<T> {
        fun onTokenAdded(token: T)
        fun onTokenRemoved(token: T)
        fun onTokenIgnored(token: T)
    }

    private inner class TokenSpanWatcher : SpanWatcher {
        override fun onSpanAdded(text: Spannable, what: Any, start: Int, end: Int) {
            if (what is TokenCompleteTextView<*>.TokenImageSpan && !savingState) {

                // If we're not focused: collapse the view if necessary
                if (!isFocused && allowCollapse) performCollapse(false)
                @Suppress("unchecked_cast")
                if (listener != null) listener?.onTokenAdded(what.token as T)
            }
        }

        override fun onSpanRemoved(text: Spannable, what: Any, start: Int, end: Int) {
            if (what is TokenCompleteTextView<*>.TokenImageSpan && !savingState) {
                @Suppress("unchecked_cast")
                if (listener != null) listener?.onTokenRemoved(what.token as T)
            }
        }

        override fun onSpanChanged(
            text: Spannable, what: Any,
            oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int
        ) {
        }
    }

    private inner class TokenTextWatcher : TextWatcher {
        var spansToRemove = ArrayList<TokenImageSpan>()
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (internalEditInProgress || ignoreNextTextCommit) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (s is SpannableStringBuilder && s.textWatcherDepth > 1) return
            }

            // count > 0 means something will be deleted
            if (count > 0 && text != null) {
                val text = text
                val end = start + count
                @Suppress("unchecked_cast")
                val spans = text.getSpans(start, end, TokenImageSpan::class.java) as Array<TokenCompleteTextView<T>.TokenImageSpan>

                //NOTE: I'm not completely sure this won't cause problems if we get stuck in a text changed loop
                //but it appears to work fine. Spans will stop getting removed if this breaks.
                val spansToRemove = ArrayList<TokenImageSpan>()
                for (token in spans) {
                    if (text.getSpanStart(token) < end && start < text.getSpanEnd(token)) {
                        spansToRemove.add(token)
                    }
                }
                this.spansToRemove = spansToRemove
            }
        }

        override fun afterTextChanged(text: Editable) {
            if (!internalEditInProgress) {
                val spansCopy = ArrayList(spansToRemove)
                spansToRemove.clear()
                for (token in spansCopy) {
                    //Only remove it if it's still present
                    if (text.getSpanStart(token) != -1 && text.getSpanEnd(token) != -1) {
                        removeSpan(text, token)
                    }
                }
                ignoreNextTextCommit = false
            }

            clearSelections()

            if (!inBatchEditAPI26to29Workaround) {
                updateHint()
            }
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun getSerializableObjects(): List<Serializable> {
        val serializables = ArrayList<Serializable>()
        for (obj in objects) {
            if (obj is Serializable) {
                serializables.add(obj as Serializable)
            } else {
                Log.e(TAG, "Unable to save '$obj'")
            }
        }
        if (serializables.size != objects.size) {
            val message = """
            You should make your objects Serializable or Parcelable or
            override getSerializableObjects and convertSerializableArrayToObjectArray
            """.trimIndent()
            Log.e(TAG, message)
        }
        return serializables
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun convertSerializableObjectsToTypedObjects(s: List<*>?): List<T>? {
        @Suppress("unchecked_cast")
        return s as List<T>?
    }

    //Used to determine if we can use the Parcelable interface
    private fun reifyParameterizedTypeClass(): Class<*> {
        //Borrowed from http://codyaray.com/2013/01/finding-generic-type-parameters-with-guava

        //Figure out what class of objects we have
        var viewClass: Class<*> = javaClass
        while (viewClass.superclass != TokenCompleteTextView::class.java) {
            viewClass = viewClass.superclass as Class<*>
        }

        // This operation is safe. Because viewClass is a direct sub-class, getGenericSuperclass() will
        // always return the Type of this class. Because this class is parameterized, the cast is safe
        val superclass = viewClass.genericSuperclass as ParameterizedType
        val type = superclass.actualTypeArguments[0]
        return type as Class<*>
    }

    override fun onSaveInstanceState(): Parcelable {
        //We don't want to save the listeners as part of the parent
        //onSaveInstanceState, so remove them first
        removeListeners()

        //Apparently, saving the parent state on 2.3 mutates the spannable
        //prevent this mutation from triggering add or removes of token objects ~mgod
        savingState = true
        val superState = super.onSaveInstanceState()
        savingState = false
        val state = SavedState(superState)
        state.prefix = prefix
        state.allowCollapse = allowCollapse
        state.performBestGuess = performBestGuess
        state.preventFreeFormText = preventFreeFormText
        state.tokenClickStyle = tokenClickStyle
        val parameterizedClass = reifyParameterizedTypeClass()
        //Our core array is Parcelable, so use that interface
        if (Parcelable::class.java.isAssignableFrom(parameterizedClass)) {
            state.parcelableClassName = parameterizedClass.name
            state.baseObjects = objects
        } else {
            //Fallback on Serializable
            state.parcelableClassName = SavedState.SERIALIZABLE_PLACEHOLDER
            state.baseObjects = getSerializableObjects()
        }
        state.tokenizer = tokenizer

        //So, when the screen is locked or some other system event pauses execution,
        //onSaveInstanceState gets called, but it won't restore state later because the
        //activity is still in memory, so make sure we add the listeners again
        //They should not be restored in onInstanceState if the app is actually killed
        //as we removed them before the parent saved instance state, so our adding them in
        //onRestoreInstanceState is good.
        addListeners()
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        internalEditInProgress = true
        setText(state.prefix)
        prefix = state.prefix
        internalEditInProgress = false
        updateHint()
        allowCollapse = state.allowCollapse
        performBestGuess = state.performBestGuess
        preventFreeFormText = state.preventFreeFormText
        tokenClickStyle = state.tokenClickStyle
        tokenizer = state.tokenizer
        addListeners()
        val objects: List<T>? = if (SavedState.SERIALIZABLE_PLACEHOLDER == state.parcelableClassName) {
            convertSerializableObjectsToTypedObjects(state.baseObjects)
        } else {
            @Suppress("unchecked_cast")
            state.baseObjects as List<T>?
        }

        //TODO: change this to keep object spans in the correct locations based on ranges.
        if (objects != null) {
            for (obj in objects) {
                addObjectSync(obj)
            }
        }

        // Collapse the view if necessary
        if (!isFocused && allowCollapse) {
            post { //Resize the view and display the +x if appropriate
                performCollapse(isFocused)
            }
        }
    }

    /**
     * Handle saving the token state
     */
    private class SavedState : BaseSavedState {
        var prefix: CharSequence? = null
        var allowCollapse = false
        var performBestGuess = false
        var preventFreeFormText = false
        var tokenClickStyle: TokenClickStyle? = null
        var parcelableClassName: String = SERIALIZABLE_PLACEHOLDER
        var baseObjects: List<*>? = null
        var tokenizerClassName: String? = null
        var tokenizer: Tokenizer? = null

        constructor(parcel: Parcel) : super(parcel) {
            prefix = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
            allowCollapse = parcel.readInt() != 0
            performBestGuess = parcel.readInt() != 0
            preventFreeFormText = parcel.readInt() != 0
            tokenClickStyle = TokenClickStyle.values()[parcel.readInt()]
            parcelableClassName = parcel.readString() ?: SERIALIZABLE_PLACEHOLDER
            baseObjects = if (SERIALIZABLE_PLACEHOLDER == parcelableClassName) {
                parcel.readSerializable() as ArrayList<*>
            } else {
                try {
                    val loader = Class.forName(parcelableClassName).classLoader
                    parcel.readArrayList(loader)
                } catch (ex: ClassNotFoundException) {
                    //This should really never happen, class had to be available to get here
                    throw RuntimeException(ex)
                }
            }
            tokenizerClassName = parcel.readString()
            tokenizer = try {
                val loader = Class.forName(tokenizerClassName!!).classLoader
                parcel.readParcelable(loader)
            } catch (ex: ClassNotFoundException) {
                //This should really never happen, class had to be available to get here
                throw RuntimeException(ex)
            }
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            TextUtils.writeToParcel(prefix, out, 0)
            out.writeInt(if (allowCollapse) 1 else 0)
            out.writeInt(if (performBestGuess) 1 else 0)
            out.writeInt(if (preventFreeFormText) 1 else 0)
            out.writeInt((tokenClickStyle ?: TokenClickStyle.None).ordinal)
            if (SERIALIZABLE_PLACEHOLDER == parcelableClassName) {
                out.writeString(SERIALIZABLE_PLACEHOLDER)
                out.writeSerializable(baseObjects as Serializable?)
            } else {
                out.writeString(parcelableClassName)
                out.writeList(baseObjects)
            }
            out.writeString(tokenizer!!.javaClass.canonicalName)
            out.writeParcelable(tokenizer, 0)
        }

        override fun toString(): String {
            val str = ("TokenCompleteTextView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " tokens=" + baseObjects)
            return "$str}"
        }

        companion object {
            const val SERIALIZABLE_PLACEHOLDER = "Serializable"
            @Suppress("unused")
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> = object : Parcelable.Creator<SavedState?> {
                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    /**
     * Checks if selection can be deleted. This method is called from TokenInputConnection .
     * @param beforeLength the number of characters before the current selection end to check
     * @return true if there are no non-deletable pieces of the section
     */
    fun canDeleteSelection(beforeLength: Int): Boolean {
        if (objects.isEmpty()) return true

        // if beforeLength is 1, we either have no selection or the call is coming from OnKey Event.
        // In these scenarios, getSelectionStart() will return the correct value.
        val endSelection = selectionEnd
        val startSelection = if (beforeLength == 1) selectionStart else endSelection - beforeLength
        val text = text
        val spans = text.getSpans(0, text.length, TokenImageSpan::class.java)

        // Iterate over all tokens and allow the deletion
        // if there are no tokens not removable in the selection
        for (span in spans) {
            val startTokenSelection = text.getSpanStart(span)
            val endTokenSelection = text.getSpanEnd(span)

            // moving on, no need to check this token
            @Suppress("unchecked_cast")
            if (isTokenRemovable(span.token as T)) continue
            if (startSelection == endSelection) {
                // Delete single
                if (endTokenSelection + 1 == endSelection) {
                    return false
                }
            } else {
                // Delete range
                // Don't delete if a non removable token is in range
                if (startSelection <= startTokenSelection
                    && endTokenSelection + 1 <= endSelection
                ) {
                    return false
                }
            }
        }
        return true
    }

    private inner class TokenInputConnection(
        target: InputConnection?,
        mutable: Boolean
    ) : InputConnectionWrapper(target, mutable) {

        private val needsWorkaround: Boolean
            get() {
                return Build.VERSION_CODES.O <= Build.VERSION.SDK_INT  &&
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q

            }

        override fun beginBatchEdit(): Boolean {
            if (needsWorkaround) {
                inBatchEditAPI26to29Workaround = true
            }
            return super.beginBatchEdit()
        }

        override fun endBatchEdit(): Boolean {
            val result = super.endBatchEdit()
            if (needsWorkaround) {
                inBatchEditAPI26to29Workaround = false
                post { updateHint() }
            }
            return result
        }

        // This will fire if the soft keyboard delete key is pressed.
        // The onKeyPressed method does not always do this.
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            // Shouldn't be able to delete any text with tokens that are not removable
            var fixedBeforeLength = beforeLength
            if (!canDeleteSelection(fixedBeforeLength)) return false

            //Shouldn't be able to delete prefix, so don't do anything
            if (selectionStart <= prefix?.length ?: 0) {
                fixedBeforeLength = 0
                return deleteSelectedObject() || super.deleteSurroundingText(
                    fixedBeforeLength,
                    afterLength
                )
            }
            return super.deleteSurroundingText(fixedBeforeLength, afterLength)
        }

        override fun setComposingRegion(start: Int, end: Int): Boolean {
            //The hint is displayed inline as regular text, but we want to disable normal compose
            //functionality on it, so if we attempt to set a composing region on the hint, set the
            //composing region to have length of 0, which indicates there is no composing region
            //Without this, on many software keyboards, the first word of the hint will be underlined
            var fixedStart = start
            var fixedEnd = end
            if (hintVisible) {
                fixedEnd = 0
                fixedStart = fixedEnd
            }
            return super.setComposingRegion(fixedStart, fixedEnd)
        }

        override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
            //There's an issue with some keyboards where they will try to insert the first word
            //of the prefix as the composing text
            var fixedText: CharSequence? = text
            val hint = hint
            if (hint != null && fixedText != null) {
                val firstWord = hint.toString().trim { it <= ' ' }.split(" ").toTypedArray()[0]
                if (firstWord.isNotEmpty() && firstWord == fixedText.toString()) {
                    fixedText = "" //It was trying to use th hint, so clear that text
                }
            }

            //Also, some keyboards don't correctly respect the replacement if the replacement
            //is the same number of characters as the replacement span
            //We need to ignore this value if it's available
            lastCompletionText?.also { lastCompletion ->
                fixedText?.also { fixed ->
                    if (fixed.length == lastCompletion.length + 1 && fixed.toString().startsWith(lastCompletion)) {
                        fixedText = fixed.subSequence(fixed.length - 1, fixed.length)
                        lastCompletionText = null
                    }
                }
            }
            return super.setComposingText(fixedText, newCursorPosition)
        }
    }

    companion object {
        //Logging
        const val TAG = "TokenAutoComplete"
    }
}