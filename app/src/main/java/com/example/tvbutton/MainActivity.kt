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
    private val codes = listOf(
        Pair(0x04, 0x0B),
        Pair(0x04, 0x0F),
        Pair(0x04, 0x38),
        Pair(0x00, 0x0B)
    )
    private var idx = 0
    private var ir: ConsumerIrManager? = null
    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Color.parseColor("#101820"))
        root.setPadding(32, 32, 32, 32)
        setContentView(root)

        val st = TextView(this)
        st.textSize = 18f
        st.setTextColor(Color.LTGRAY)
        st.gravity = Gravity.CENTER
        st.setPadding(0, 24, 0, 24)
        status = st

        try {
            ir = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        } catch (e: Throwable) {
            st.text = "خطا در IR: " + e.toString()
        }

        val big = Button(this)
        big.text = "تغییر حالت\nآنتن ⇄ ستاپ‌باکس"
        big.textSize = 34f
        big.typeface = Typeface.DEFAULT_BOLD
        big.setTextColor(Color.WHITE)
        big.setBackgroundColor(Color.parseColor("#E63946"))
        big.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        big.setOnClickListener { send() }

        val next = Button(this)
        next.text = "کار نکرد؟ کد بعدی (فعلی: 1)"
        next.setOnClickListener {
            idx = (idx + 1) % codes.size
            next.text = "کار نکرد؟ کد بعدی (فعلی: ${idx + 1})"
            send()
        }

        root.addView(big)
        root.addView(st)
        root.addView(next)

        val m = ir
        if (m == null) {
            st.text = "این گوشی مادون قرمز (IR) نداره یا اجازه نمیده"
        } else if (!m.hasIrEmitter()) {
            st.text = "این گوشی LED مادون قرمز نداره!"
        } else if (st.text.isEmpty()) {
            st.text = "آماده — گوشی رو سمت تلویزیون بگیر"
        }
    }

    private fun send() {
        try {
            val m = ir
            if (m == null || !m.hasIrEmitter()) {
                status?.text = "IR در دسترس نیست"
                return
            }
            val (addr, cmd) = codes[idx]
            m.transmit(38000, necPattern(addr, cmd))
            status?.text = "فرستاده شد ✓ (کد ${idx + 1})"
        } catch (e: Throwable) {
            status?.text = "خطا: " + e.toString()
        }
    }

    private fun necPattern(addr: Int, cmd: Int): IntArray {
        val p = ArrayList<Int>()
        p.add(9000); p.add(4500)
        val data = (addr and 0xFF) or
                ((addr.inv() and 0xFF) shl 8) or
                ((cmd and 0xFF) shl 16) or
                ((cmd.inv() and 0xFF) shl 24)
        for (i in 0 until 32) {
            p.add(560)
            p.add(if (((data shr i) and 1) == 1) 1690 else 560)
        }
        p.add(560)
        return p.toIntArray()
    }
}
