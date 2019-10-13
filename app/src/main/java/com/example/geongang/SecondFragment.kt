package com.example.geongang

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.android.synthetic.main.fragment_second.view.*

class SecondFragment : Fragment() {
    private lateinit var fragmentView: View
    var value: Int = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_second, container, false)
        initBarChart()
        initCircleProgressbar()
        return fragmentView
    }

    fun initCircleProgressbar() {
        fragmentView.staticMaxMuscular.bringToFront()
        fragmentView.maxMuscularStrength.bringToFront()

        fragmentView.circularProgressBar.progressMax = 1000f
        fragmentView.circularProgressBar.progress = 0f
        fragmentView.circularProgressBar.progressBarColor = ContextCompat.getColor(context!!, R.color.colorAccent)
        value = 1200
    }

    fun initBarChart() {
        var chartList: ArrayList<BarEntry> = ArrayList()
        chartList.add(BarEntry(0f, 300f))
        chartList.add(BarEntry(1f, 500f))
        chartList.add(BarEntry(2f, 700f))
        chartList.add(BarEntry(3f, 600f))
        chartList.add(BarEntry(4f, 1000f))
        chartList.add(BarEntry(5f, 400f))
        chartList.add(BarEntry(6f, 200f))
        chartList.add(BarEntry(7f, 500f))
        chartList.add(BarEntry(8f, 800f))
        chartList.add(BarEntry(9f, 400f))
        chartList.add(BarEntry(10f, 500f))
        chartList.add(BarEntry(11f, 900f))
        chartList.add(BarEntry(12f, 300f))
        chartList.add(BarEntry(13f, 800f))
        chartList.add(BarEntry(14f, 1000f))
        chartList.add(BarEntry(15f, 300f))
        chartList.add(BarEntry(16f, 400f))

        var dataSet = BarDataSet(chartList, "")
        dataSet.color = ContextCompat.getColor(context!!, R.color.colorAccent)
        var data = BarData(dataSet)
        fragmentView.barChart.data = data
        fragmentView.barChart.animateXY(1000, 1000)
    }

    override fun onResume() {
        super.onResume()
        fragmentView.circularProgressBar.progressMax = 1000f
        fragmentView.circularProgressBar.progress = 0f
        fragmentView.barChart.animateXY(1000, 1000)
        if(value > 1000) {
            fragmentView.circularProgressBar.setProgressWithAnimation(1000f, 3000L)
        } else {
            fragmentView.circularProgressBar.setProgressWithAnimation(value.toFloat(), 3000L)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SecondFragment()
    }
}
