package io.github.eyalshimshi.betterstips

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import io.github.eyalshimshi.betterstips.util.Links

/**
 * "Share → Stips+" (v0.18): a Stips question, profile or chat link shared from another app (WhatsApp, the browser) opens
 * in Stips+, in its own window as a phone notification's does, not inside the app it came from. Android doesn't let
 * Stips+ open Stips links by itself unless Stips vouches for it, or the user turns it on under "Open by default".
 * Nothing is shown here; without a Stips link in what was shared, a short message says so.
 */
class ShareLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)
        val link = Links.sharedLink(text)
        if (link != null) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(link)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
            )
        } else {
            Toast.makeText(applicationContext, R.string.share_no_link, Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
