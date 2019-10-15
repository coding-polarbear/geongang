package com.example.geongang;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private String newline = "\r\n";

    private TextView receiveText;
    Button start;
    Button end;
    Button stopSave;

    private LineChart liveChart;

    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;

    private String sum = "";
    private ArrayList<Float> stream = new ArrayList<>();
    private ArrayList<Float> recent = new ArrayList<>();
    private int preSecond = 0;
    private int minStandard = 999;
    private View fragmentView;

    public TerminalFragment() {
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getContext(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service !=null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = fragmentView.findViewById(R.id.receive);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        TextView sendText = fragmentView.findViewById(R.id.send_text);
        View sendBtn = fragmentView.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));
        liveChart = fragmentView.findViewById(R.id.liveChart);
        start = (Button)fragmentView.findViewById(R.id.startA);
        start.setOnClickListener(v -> viewChange("A"));
        end = (Button)fragmentView.findViewById(R.id.endB);
        end.setOnClickListener(v -> {
            viewChange2("B");
            recent.addAll(stream);
            //submit data(stream) to server
            stream.clear();
            preSecond = 0;
            minStandard = 999;
        });
        stopSave = (Button)fragmentView.findViewById(R.id.stopSave);
        stopSave.setOnClickListener(v -> end());

        receiveText.append("연결이 완료될 때까지 기다려 주세요\n\n");
        receiveText.append("본 어플은 프로토타입 입니다\n\n");
        receiveText.append("측정을 위해 10초간 힘을 풀고 대기해주세요\n\n");
        initGraph();

        return fragmentView;
    }

    private void initGraph() {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        entries.add(new Entry(1f, 30f));
        entries.add(new Entry(2f, 60f));
        entries.add(new Entry(3f, 50f));

        LineDataSet dataset = new LineDataSet(entries, "");
        dataset.setLineWidth(5f);
        dataset.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        dataset.setDrawValues(false);
        dataset.setDrawCircles(false);
        dataset.setValueFormatter(new DefaultValueFormatter(0));
        liveChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        liveChart.getXAxis().setDrawGridLines(false);
        liveChart.getXAxis().setValueFormatter(new ValueFormatter() {
           @Override
           public String getFormattedValue(float value, AxisBase axis) {
               return labels.get((int) value);
           }
        });

        LineData data = new LineData(dataset);
        liveChart.setData(data);

    }

    private void refreshGraph(ArrayList<Float> list){
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int j = 0;
        if (list.size()>50) {j = list.size() - 50;}
        for(int i=j;i<list.size();i++){
            entries.add(new Entry((float)i, list.get(i)));
        }
        LineDataSet dataset = new LineDataSet(entries, "");
        dataset.setLineWidth(5f);
        dataset.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        dataset.setDrawValues(false);
        dataset.setDrawCircles(false);

        LineData data = new LineData(dataset);
        liveChart.setData(data);
        liveChart.invalidate();
    }

    public void viewChange(String a){
        send(a);
        start.setVisibility(View.GONE);
        end.setVisibility(View.VISIBLE);
    }

    public void viewChange2(String a){
        send(a);
        start.setVisibility(View.VISIBLE);
        end.setVisibility(View.GONE);
    }

    public void end(){
        ((SelectDeviceActivity)getActivity()).endactivity(recent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id ==R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();
            status("connecting...");
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, "Connected to " + deviceName);
            socket.connect(getContext(), service, device);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        socket.disconnect();
        socket = null;
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            byte[] data = (str + newline).getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] data) {
        if(new String(data).contains("A")){
            sum = sum + new String(data).replace("A", "");
            int a = Integer.parseInt(sum);
            if(preSecond<100){
                minStandard = a < minStandard ? a : minStandard;
                preSecond += 1;
                sum = "";
            } else {
                stream.add(((float)a-minStandard)/(1000-minStandard)*1000 < 0 ? 0 : ((float)a-minStandard)/(1000-minStandard)*1000);
                refreshGraph(stream);
                receiveText.append(Float.toString(((float)a-minStandard)/(1000-minStandard)*1000 < 0 ? 0 : ((float)a-minStandard)/(1000-minStandard)*1000));
                receiveText.append("\n");
                sum = "";
            }
        } else {
            sum = sum + new String(data);
        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
        stopSave.setEnabled(true);
        start.setEnabled(true);
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

}
