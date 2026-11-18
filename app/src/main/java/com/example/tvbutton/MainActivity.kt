package com.example.tvbutton

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // کدهای رایج LG (پروتکل NEC): آدرس، دستور
    // اگه اولی کار نکرد، دکمه‌ی پایین رو بزن تا بره سراغ بعدی
    private val codes = listOf(
        Pair(0x04, 0x0B), // INPUT / AV  (رایج‌ترین)
        Pair(0x04, 0x0F), // TV/AV قدیمی
        Pair(0x04, 0x38), // AV بعضی مدل‌ها
        Pair(0x00, 0x0B)
    )
    private var idx = 0
    private lateinit var ir: ConsumerIrManager
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ir = getSystemService(Context.CONSUMER_IR_SERVICE) as ConsumerIrManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#101820"))
            setPadding(32, 32, 32, 32)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val big = Button(this).apply {
            text = "تغییر حالت\nآنتن ⇄ ستاپ‌باکس"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#E63946"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            setOnClickListener { send() }
        }

        status = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
            text = if (ir.hasIrEmitter()) "آماده — گوشی رو سمت تلویزیون بگیر" else "این گوشی مادون قرمز نداره!"
        }

        val next = Button(this).apply {
            text = "کار نکرد؟ کد بعدی (فعلی: ۱)"
            setOnClickListener {
                idx = (idx + 1) % codes.size
                text = "کار نکرد؟ کد بعدی (فعلی: ${idx + 1})"
                send()
            }
        }

        root.addView(big)
        root.addView(status)
        root.addView(next)
        setContentView(root)
    }

    private fun send() {
        if (!ir.hasIrEmitter()) return
        val (addr, cmd) = codes[idx]
        ir.transmit(38000, necPattern(addr, cmd))
        status.text = "فرستاده شد ✓"
    }

    // ساخت سیگنال NEC (واحد: میکروثانیه)
    private fun necPattern(addr: Int, cmd: Int): IntArray {
        val p = ArrayList<Int>()
        p.add(9000); p.add(4500)
        val data = (addr and 0xFF) or
                ((addr.inv() and 0xFF) shl 8) or
                ((cmd and 0xFF) shl 16) or
                ((cmd.inv() and 0xFF) shl 24)
        for (i in 0 until 32) {
            p.add(560)
            p.add(if ((data shr i) and 1 == 1) 1690 else 560)
        }
        p.add(560)
        return p.toIntArray()
    }
}
