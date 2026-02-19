package com.smithSophiav.ethereum.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun copyToClipboard(context: Context, label: String, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
        ClipData.newPlainText(label, text)
    )
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}
