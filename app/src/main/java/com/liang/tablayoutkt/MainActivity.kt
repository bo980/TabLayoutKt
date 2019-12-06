package com.liang.tablayoutkt

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.liang.tabs.BadgeView
import com.liang.tabs.TabImp
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var badgeView: BadgeView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.button).setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    TabActivity::class.java
                )
            )
        }
        findViewById<View>(R.id.button1).setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    OperationActivity::class.java
                )
            )
        }

        findViewById<View>(R.id.button2).setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    Tab2Activity::class.java
                )
            )
        }

        badgeView = findViewById(R.id.badgeView)

        initTab()
    }

    private fun initTab() {
//        repeat(10) {
//            tabLayout.addTab(TabImp(this).apply {
//                parent = tabLayout
////                setBackgroundColor(resources.getColor(R.color.colorPrimary))
//                setTitle("tab$it")
//                this.setTitleColor(
//                    resources.getColor(R.color.colorPrimary),
//                    resources.getColor(R.color.holo_red)
//                )
//                this.setIcon(R.mipmap.icon_qipaishi_normal, R.mipmap.icon_qipaishi_press)
//                this.setTabRippleColor(Color.YELLOW)
//                this.setTabTabBackground(R.color.colorAccent)
//                itemDecoration = TabImp.DefItemDecoration().apply {
//                    color = Color.GREEN
//                }
//            })
//        }

    }

    fun show(view: View?) {
        badgeView!!.show("8")
        Log.e("show", "getPaddingLeft: " + badgeView!!.paddingLeft)
        Log.e("show", "getPaddingTop: " + badgeView!!.paddingTop)
    }
}