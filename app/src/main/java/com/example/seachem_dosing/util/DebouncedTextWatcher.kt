package com.example.seachem_dosing.util

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

/**
 * A TextWatcher that debounces input to prevent rapid-fire calculations.
 * Automatically cleans up when the lifecycle is destroyed.
 *
 * @param debounceMs Delay in milliseconds before triggering the callback
 * @param lifecycleOwner Optional lifecycle owner for automatic cleanup
 * @param onTextChanged Callback with the parsed Double value (0.0 if parsing fails)
 */
class DebouncedTextWatcher(
    private val debounceMs: Long = 300L,
    lifecycleOwner: LifecycleOwner? = null,
    private val onTextChanged: (Double) -> Unit
) : TextWatcher, DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null
    private var isDestroyed = false

    init {
        lifecycleOwner?.lifecycle?.addObserver(this)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isDestroyed) return

        pendingRunnable?.let { handler.removeCallbacks(it) }

        pendingRunnable = Runnable {
            if (!isDestroyed) {
                val value = s?.toString()?.toDoubleOrNull() ?: 0.0
                onTextChanged(value)
            }
        }
        handler.postDelayed(pendingRunnable!!, debounceMs)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cleanup()
        owner.lifecycle.removeObserver(this)
    }

    fun cleanup() {
        isDestroyed = true
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
    }
}

/**
 * A TextWatcher that debounces and provides the raw string value.
 */
class DebouncedStringTextWatcher(
    private val debounceMs: Long = 300L,
    lifecycleOwner: LifecycleOwner? = null,
    private val onTextChanged: (String) -> Unit
) : TextWatcher, DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null
    private var isDestroyed = false

    init {
        lifecycleOwner?.lifecycle?.addObserver(this)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isDestroyed) return

        pendingRunnable?.let { handler.removeCallbacks(it) }

        pendingRunnable = Runnable {
            if (!isDestroyed) {
                onTextChanged(s?.toString() ?: "")
            }
        }
        handler.postDelayed(pendingRunnable!!, debounceMs)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cleanup()
        owner.lifecycle.removeObserver(this)
    }

    fun cleanup() {
        isDestroyed = true
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
    }
}

/**
 * Extension function to easily add debounced text watching to an EditText.
 */
fun EditText.addDebouncedTextListener(
    debounceMs: Long = 300L,
    lifecycleOwner: LifecycleOwner? = null,
    onTextChanged: (Double) -> Unit
): DebouncedTextWatcher {
    val watcher = DebouncedTextWatcher(debounceMs, lifecycleOwner, onTextChanged)
    addTextChangedListener(watcher)
    return watcher
}

/**
 * Manager class to track and cleanup multiple TextWatchers.
 * Use this in Fragments to ensure all watchers are cleaned up in onDestroyView.
 */
class TextWatcherManager {
    private val watchers = mutableListOf<Pair<WeakReference<EditText>, TextWatcher>>()

    fun addWatcher(editText: EditText, watcher: TextWatcher) {
        editText.addTextChangedListener(watcher)
        watchers.add(WeakReference(editText) to watcher)
    }

    fun createDebouncedWatcher(
        editText: EditText,
        debounceMs: Long = 300L,
        onTextChanged: (Double) -> Unit
    ): DebouncedTextWatcher {
        val watcher = DebouncedTextWatcher(debounceMs, null, onTextChanged)
        addWatcher(editText, watcher)
        return watcher
    }

    fun cleanup() {
        watchers.forEach { (editTextRef, watcher) ->
            editTextRef.get()?.removeTextChangedListener(watcher)
            if (watcher is DebouncedTextWatcher) {
                watcher.cleanup()
            } else if (watcher is DebouncedStringTextWatcher) {
                watcher.cleanup()
            }
        }
        watchers.clear()
    }
}
