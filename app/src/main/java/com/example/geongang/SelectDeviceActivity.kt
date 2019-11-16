package com.example.geongang

import android.app.Activity
import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager

import java.util.ArrayList

class SelectDeviceActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportFragmentManager.addOnBackStackChangedListener(this)
        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction().add(
                R.id.fragment,
                DevicesFragment(),
                "devices"
            ).commit()
        else
            onBackStackChanged()
    }

    fun endactivity(list: ArrayList<Float>, time: Float?, money: Float?) {
        val integers = ArrayList<Int>()
        for (i in list.indices) {
            integers.add(Math.round(list[i]))
        }
        val resultintent = Intent()
        resultintent.putIntegerArrayListExtra("result", integers)
        resultintent.putExtra("time", time)
        resultintent.putExtra("money", money)
        setResult(Activity.RESULT_OK, resultintent)
        finish()
    }

    override fun onBackStackChanged() {
        supportActionBar!!.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
