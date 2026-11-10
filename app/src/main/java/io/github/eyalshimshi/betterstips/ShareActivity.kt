package io.github.eyalshimshi.betterstips

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import io.github.eyalshimshi.betterstips.util.ShareRules

/**
 * "Share → Stips+" (v0.19): text shared from another app (WhatsApp, the browser) goes to up to five chats, picked in
 * Stips+'s own window, as WhatsApp does. Only plain text: Stips' messages are text, so pictures and files aren't
 * offered to Stips+ at all (the manifest takes text/plain only), and formatting is dropped. Nothing is shown here.
 */
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = intent?.takeIf { it.action == Intent.ACTION_SEND }?.getCharSequenceExtra(Intent.EXTRA_TEXT)
        val text = ShareRules.clean(shared)
        if (text != null) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    action = ACTION_SEND_TO_CHATS
                    putExtra(Intent.EXTRA_TEXT, text)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
            )
        } else {
            Toast.makeText(applicationContext, R.string.share_no_text, Toast.LENGTH_LONG).show()
        }
        finish()
    }

    companion object {
        /** MainActivity opens the chats picker with the text in [Intent.EXTRA_TEXT]. */
        const val ACTION_SEND_TO_CHATS = "io.github.eyalshimshi.betterstips.action.SEND_TO_CHATS"
    }
}