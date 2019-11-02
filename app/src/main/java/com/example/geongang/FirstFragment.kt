package com.example.geongang


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlinx.android.synthetic.main.fragment_first.*
import kotlinx.android.synthetic.main.fragment_first.view.*

class FirstFragment : Fragment() {
    private lateinit var fragmentView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_first, container, false)
        initGraph()
        fragmentView.startBluetooth.setOnClickListener {
            val intent = Intent(context,SelectDeviceActivity::class.java)
            startActivityForResult(intent,1)
        }
        return fragmentView
    }

    private fun initGraph() {
        var entries: ArrayList<Entry> = ArrayList()
        entries.add(Entry(0f, 0f))
        entries.add(Entry(1f, 0f))
        entries.add(Entry(2f, 0f))
        entries.add(Entry(3f, 0f))
        entries.add(Entry(4f, 0f))
        entries.add(Entry(5f, 300f))
        entries.add(Entry(6f, 500f))
        entries.add(Entry(7f, 200f))
        entries.add(Entry(8f, 300f))
        entries.add(Entry(9f, 600f))
        entries.add(Entry(10f, 800f))
        entries.add(Entry(11f, 700f))
        entries.add(Entry(12f, 300f))
        entries.add(Entry(13f, 500f))
        entries.add(Entry(14f, 700f))
        entries.add(Entry(15f, 1000f))
        entries.add(Entry(16f, 400f))

        var labels = arrayListOf<String>()
        var dataset = LineDataSet(entries, "")
        dataset.lineWidth = 5f
        dataset.color = ContextCompat.getColor(context!!, R.color.colorAccent)
        dataset.setDrawValues(false)
        dataset.setDrawCircles(false)
        dataset.valueFormatter = DefaultValueFormatter(0)
        fragmentView.lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        fragmentView.lineChart.xAxis.setDrawGridLines(false)

        var data = LineData(dataset)
        fragmentView.lineChart.data = data
        fragmentView.lineChart.animateXY(1000, 1000)
    }

    fun refreshGraph(list: java.util.ArrayList<Float>) {
        val entries = java.util.ArrayList<Entry>()
        val labels = java.util.ArrayList<String>()
        var j = 0
        if (list.size > 50) {
            j = list.size - 50
        }
        for (i in j until list.size) {
            entries.add(Entry(i.toFloat(), list[i]))
        }
        val dataset = LineDataSet(entries, "")
        dataset.lineWidth = 5f
        dataset.color = ContextCompat.getColor(context!!, R.color.colorAccent)
        dataset.setDrawValues(false)
        dataset.setDrawCircles(false)

        val data = LineData(dataset)
        fragmentView.lineChart.data = data
        fragmentView.lineChart.invalidate()
    }

    override fun onResume() {
        super.onResume()
        fragmentView.lineChart.animateXY(1000, 1000)
    }
    companion object {
        @JvmStatic
        fun newInstance() = FirstFragment()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==1&&resultCode==-1){
            val entries = java.util.ArrayList<Entry>()
            val labels = java.util.ArrayList<String>()
            var j = 0
            var sum = 0
            val list = data?.extras!!.getIntegerArrayList("result")
            var time = data?.extras!!.getFloat("time");
            for (i in j until list!!.size) {
                entries.add(Entry(i.toFloat(), list[i].toFloat()))
                sum +=list[i]
            }
            val dataset = LineDataSet(entries, "")
            dataset.lineWidth = 5f
            dataset.color = ContextCompat.getColor(context!!, R.color.colorAccent)
            dataset.setDrawValues(false)
            dataset.setDrawCircles(false)

            val data = LineData(dataset)
            fragmentView.lineChart.data = data
            fragmentView.lineChart.invalidate()
            fragmentView.textView.text = (sum.toFloat()/list.size).toString()
            fragmentView.todayExerciseTime.text = (time).toString() + "분"
            //  보험료 api call
            // fragmentView.discountPrice.text
        }
    }
}
