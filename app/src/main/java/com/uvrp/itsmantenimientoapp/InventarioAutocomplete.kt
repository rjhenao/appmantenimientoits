package com.uvrp.itsmantenimientoapp

import android.text.TextWatcher
import com.google.android.material.textfield.MaterialAutoCompleteTextView

/**
 * Asigna texto sin disparar [TextWatcher]: MaterialAutoCompleteTextView puede emitir
 * [afterTextChanged] varias veces (filtro, dropdown); los flags con post/postDelayed no bastan.
 */
internal fun MaterialAutoCompleteTextView.setTextWithoutWatcher(
    value: String,
    watcher: TextWatcher,
    delayBeforeReattachMs: Long = 160L
) {
    removeTextChangedListener(watcher)
    setText(value)
    dismissDropDown()
    if (delayBeforeReattachMs <= 0L) {
        post { addTextChangedListener(watcher) }
    } else {
        postDelayed({ addTextChangedListener(watcher) }, delayBeforeReattachMs)
    }
}
