package com.example.geongang

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.geongang.entity.EmgRequest
import com.example.geongang.bluetooth.SerialListener
import com.example.geongang.bluetooth.SerialService
import com.example.geongang.bluetooth.SerialSocket
import com.example.geongang.utils.RetrofitUtil

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

import java.util.ArrayList

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlin.math.roundToInt

class TerminalFragment : Fragment(), ServiceConnection,
    SerialListener {

    private var deviceAddress: String? = null
    private var newline = "\r\n"

    private var receiveText: TextView? = null
    private lateinit var start: Button
    private lateinit var end: Button
    private lateinit var stopSave: Button

    private var liveChart: LineChart? = null

    private var socket: SerialSocket? = null
    private var service: SerialService? = null
    private var initialStart = true
    private var connected = Connected.False

    private var sum = ""
    private val stream = ArrayList<Float>()
    private val recent = ArrayList<Float>()
    private var preSecond = 0
    private var minStandard = 999
    private var fragmentView: View? = null

    private enum class Connected {
        False, Pending, True
    }

    /*
     * Lifecycle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        deviceAddress = arguments!!.getString("device")
    }

    override fun onDestroy() {
        if (connected != Connected.False)
            disconnect()
        activity!!.stopService(Intent(activity, SerialService::class.java))
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (service != null)
            service!!.attach(this)
        else
            activity!!.startService(
                Intent(
                    context,
                    SerialService::class.java
                )
            ) // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    override fun onStop() {
        if (service != null && !activity!!.isChangingConfigurations)
            service!!.detach()
        super.onStop()
    }

    override// onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    fun onAttach(activity: Activity) {
        super.onAttach(activity)
        getActivity()!!.bindService(
            Intent(getActivity(), SerialService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDetach() {
        try {
            activity!!.unbindService(this)
        } catch (ignored: Exception) {
        }

        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        if (initialStart && service != null) {
            initialStart = false
            activity!!.runOnUiThread { this.connect() }
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        service = (binder as SerialService.SerialBinder).service
        if (initialStart && isResumed) {
            initialStart = false
            activity!!.runOnUiThread { this.connect() }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        service = null
    }

    /*
     * UI
     */
    @SuppressLint("CheckResult")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = inflater.inflate(R.layout.fragment_terminal, container, false)
        receiveText =
            fragmentView!!.findViewById(R.id.receive)                          // TextView performance decreases with number of spans
        receiveText!!.setTextColor(resources.getColor(R.color.colorRecieveText)) // set as default color to reduce number of spans
        receiveText!!.movementMethod = ScrollingMovementMethod.getInstance()
        val sendText = fragmentView!!.findViewById<TextView>(R.id.send_text)
        val sendBtn = fragmentView!!.findViewById<View>(R.id.send_btn)
        sendBtn.setOnClickListener { send(sendText.text.toString()) }
        liveChart = fragmentView!!.findViewById(R.id.liveChart)
        start = fragmentView!!.findViewById<View>(R.id.startA) as Button
        start.setOnClickListener {
            viewChange("A")
            recent.clear()
        }
        end = fragmentView!!.findViewById<View>(R.id.endB) as Button
        end.setOnClickListener {
            viewChange2("B")
            recent.addAll(stream)
            //submit data(stream) to server
            stream.clear()
            preSecond = 0
            minStandard = 999
        }
        stopSave = fragmentView!!.findViewById<View>(R.id.stopSave) as Button
        stopSave.setOnClickListener {
            val emgService = RetrofitUtil.retrofit.create(EmgService::class.java)
            val result = ArrayList<Int>()
            for (i in recent.indices) {
                result.add(recent[i].roundToInt())
            }
            emgService.getEmg(EmgRequest(result, 0.1f))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { Emg -> end(Emg.exerciseTime, Emg.monthlyPremium) },
                    { t -> Log.e("error", t.localizedMessage!!) })
        }

        receiveText!!.append("연결이 완료될 때까지 기다려 주세요\n\n")
        receiveText!!.append("본 어플은 프로토타입 입니다\n\n")
        receiveText!!.append("측정을 위해 10초간 힘을 풀고 대기해주세요\n\n")
        initGraph()

        return fragmentView
    }

    private fun initGraph() {
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        entries.add(Entry(1f, 30f))
        entries.add(Entry(2f, 60f))
        entries.add(Entry(3f, 50f))

        val dataset = LineDataSet(entries, "")
        dataset.lineWidth = 5f
        dataset.color = ContextCompat.getColor(context!!, R.color.colorAccent)
        dataset.setDrawValues(false)
        dataset.setDrawCircles(false)
        dataset.valueFormatter = DefaultValueFormatter(0)
        liveChart!!.xAxis.position = XAxis.XAxisPosition.BOTTOM
        liveChart!!.xAxis.setDrawGridLines(false)
        liveChart!!.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float, axis: AxisBase?): String {
                return labels[value.toInt()]
            }
        }

        val data = LineData(dataset)
        liveChart!!.data = data

    }

    private fun refreshGraph(list: ArrayList<Float>) {
        val entries = ArrayList<Entry>()
        var j = 0
        if (list.size > 50) {
            j = list.size - 50
        }
        for (i in j until list.size) {
            entries.add(Entry(i.toFloat(), list[i]))
        }
        val dataSet = LineDataSet(entries, "")
        dataSet.lineWidth = 5f
        dataSet.color = ContextCompat.getColor(context!!, R.color.colorAccent)
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)

        val data = LineData(dataSet)
        liveChart!!.data = data
        liveChart!!.invalidate()
    }

    private fun viewChange(a: String) {
        send(a)
        start.visibility = View.GONE
        end.visibility = View.VISIBLE
    }

    private fun viewChange2(a: String) {
        send(a)
        start.visibility = View.VISIBLE
        end.visibility = View.GONE
    }

    private fun end(time: Float?, money: Float?) {
        (activity as SelectDeviceActivity).endactivity(recent, time, money)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_terminal, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.clear) {
            receiveText!!.text = ""
            return true
        } else if (id == R.id.newline) {
            val newlineNames = resources.getStringArray(R.array.newline_names)
            val newlineValues = resources.getStringArray(R.array.newline_values)
            val pos = java.util.Arrays.asList(*newlineValues).indexOf(newline)
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Newline")
            builder.setSingleChoiceItems(newlineNames, pos) { dialog, item1 ->
                newline = newlineValues[item1]
                dialog.dismiss()
            }
            builder.create().show()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    /*
     * Serial + UI
     */
    private fun connect() {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            val deviceName = if (device.name != null) device.name else device.address
            status("connecting...")
            connected = Connected.Pending
            socket = SerialSocket()
            service!!.connect(this, "Connected to $deviceName")
            socket!!.connect(context!!, service!!, device)
        } catch (e: Exception) {
            onSerialConnectError(e)
        }

    }

    private fun disconnect() {
        connected = Connected.False
        service!!.disconnect()
        socket!!.disconnect()
        socket = null
    }

    private fun send(str: String) {
        if (connected != Connected.True) {
            Toast.makeText(activity, "not connected", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val spn = SpannableStringBuilder(str + '\n')
            spn.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.colorSendText)),
                0,
                spn.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            receiveText!!.append(spn)
            val data = (str + newline).toByteArray()
            socket!!.write(data)
        } catch (e: Exception) {
            onSerialIoError(e)
        }

    }

    private fun receive(data: ByteArray) {
        if (String(data).contains("A")) {
            sum += String(data).replace("A", "")
            val a = Integer.parseInt(sum)
            if (preSecond < 100) {
                minStandard = if (a < minStandard) a else minStandard
                preSecond += 1
                sum = ""
            } else {
                stream.add((if ((a.toFloat() - minStandard) / (1000 - minStandard) * 1000 < 0) 0 else (a.toFloat() - minStandard) / (1000 - minStandard) * 1000) as Float)
                refreshGraph(stream)
                receiveText!!.append(((if ((a.toFloat() - minStandard) / (1000 - minStandard) * 1000 < 0) 0 else (a.toFloat() - minStandard) / (1000 - minStandard) * 1000) as Float).toString())
                receiveText!!.append("\n")
                sum = ""
            }
        } else {
            sum += String(data)
        }
    }

    private fun status(str: String) {
        val spn = SpannableStringBuilder(str + '\n')
        spn.setSpan(
            ForegroundColorSpan(resources.getColor(R.color.colorStatusText)),
            0,
            spn.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        receiveText!!.append(spn)
    }

    /*
     * SerialListener
     */
    override fun onSerialConnect() {
        status("connected")
        connected = Connected.True
        stopSave.isEnabled = true
        start.isEnabled = true
    }

    override fun onSerialConnectError(e: Exception) {
        status("connection failed: " + e.message)
        disconnect()
    }

    override fun onSerialRead(data: ByteArray) {
        receive(data)
    }

    override fun onSerialIoError(e: Exception) {
        status("connection lost: " + e.message)
        disconnect()
    }

}
