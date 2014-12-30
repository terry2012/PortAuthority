package com.aaronjwood.portauthority.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.aaronjwood.portauthority.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Host {

    private static final String TAG = "Host";

    private Activity activity;
    private String ip;
    private ProgressDialog scanProgressDialog;

    public Host(Activity activity, String ip) {
        this.activity = activity;
        this.ip = ip;
    }

    public void getMacAddress() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
            reader.readLine();
            String line;

            while((line = reader.readLine()) != null) {
                String[] l = line.split("\\s+");

                String ip = l[0];
                String macAddress = l[3];

                if(ip.equals(this.ip)) {
                    TextView hostMac = (TextView) activity.findViewById(R.id.hostMac);
                    hostMac.setText(macAddress);
                    return;
                }
            }
        }
        catch(FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        catch(IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void scanSystemPorts() {
        this.scanProgressDialog = new ProgressDialog(this.activity, AlertDialog.THEME_HOLO_DARK);
        scanProgressDialog.setCancelable(false);
        scanProgressDialog.setTitle("Scanning Well Known Ports");
        scanProgressDialog.setMessage(""); //For some reason we need to set a blank message here if we want to change it later on
        scanProgressDialog.setProgressStyle(scanProgressDialog.STYLE_HORIZONTAL);
        scanProgressDialog.setProgress(0);
        scanProgressDialog.setMax(1024);
        scanProgressDialog.show();

        new AsyncTask<Void, Void, ArrayList<Integer>>() {

            @Override
            protected ArrayList<Integer> doInBackground(Void... params) {
                ExecutorService executor = Executors.newFixedThreadPool(8);
                Future<ArrayList<Integer>> ports1 = executor.submit(new ScanPortsCallable(ip, 1, 128));
                Future<ArrayList<Integer>> ports2 = executor.submit(new ScanPortsCallable(ip, 129, 257));
                Future<ArrayList<Integer>> ports3 = executor.submit(new ScanPortsCallable(ip, 258, 386));
                Future<ArrayList<Integer>> ports4 = executor.submit(new ScanPortsCallable(ip, 387, 515));
                Future<ArrayList<Integer>> ports5 = executor.submit(new ScanPortsCallable(ip, 516, 644));
                Future<ArrayList<Integer>> ports6 = executor.submit(new ScanPortsCallable(ip, 645, 773));
                Future<ArrayList<Integer>> ports7 = executor.submit(new ScanPortsCallable(ip, 774, 902));
                Future<ArrayList<Integer>> ports8 = executor.submit(new ScanPortsCallable(ip, 903, 1024));
                try {
                    ArrayList<Integer> ports = new ArrayList<>();
                    ports.addAll(ports1.get());
                    ports.addAll(ports2.get());
                    ports.addAll(ports3.get());
                    ports.addAll(ports4.get());
                    ports.addAll(ports5.get());
                    ports.addAll(ports6.get());
                    ports.addAll(ports7.get());
                    ports.addAll(ports8.get());
                    return ports;
                }
                catch(InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                }
                catch(ExecutionException e) {
                    Log.e(TAG, e.getMessage());
                }
                finally {
                    executor.shutdown();
                    scanProgressDialog.dismiss();
                }
                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<Integer> result) {
                ListView portList = (ListView) activity.findViewById(R.id.portList);
                ArrayAdapter<Integer> adapter = (ArrayAdapter<Integer>) portList.getAdapter();
                adapter.addAll(result);
                adapter.notifyDataSetChanged();
            }

        }.execute();
    }

    private class ScanPortsCallable implements Callable<ArrayList<Integer>> {

        private static final String TAG = "ScanPortsCallable";

        private String ip;
        private int startPort;
        private int stopPort;

        public ScanPortsCallable(String ip, int startPort, int stopPort) {
            this.ip = ip;
            this.startPort = startPort;
            this.stopPort = stopPort;
        }

        @Override
        public ArrayList<Integer> call() {
            ArrayList<Integer> ports = new ArrayList<>();
            for(int i = this.startPort; i <= this.stopPort; i++) {
                try {
                    final int port = i;
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(this.ip, port), 50);
                    socket.close();
                    ports.add(port);
                    activity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            scanProgressDialog.setMessage("Port " + port + " is open!");
                        }
                    });

                }
                catch(IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                finally {
                    scanProgressDialog.incrementProgressBy(1);
                }
            }
            return ports;
        }
    }

}
