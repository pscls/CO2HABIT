package com.co2habit.hackhpi.hackhpi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


//ToDo:
//
// App im Background --> Daten gehen verloren :(
// Refactor Overview Fragment --> Use Main Activity
//

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, HistoryFragment.OnFragmentInteractionListener, AddEntryFragment.OnFragmentInteractionListener,
OverviewFragment.OnFragmentInteractionListener, StatisticsFragment.OnFragmentInteractionListener {

    Context context;
    private BluetoothAdapter mBluetoothAdapter;

    // "CD:A2:4C:60:37:C8" = JTUJ
    // "F4:F0:22:16:FD:C3" = Sihf
    // "C9:C8:7C:99:3E:FD" = R10J
    // "CE:92:BD:85:DF:44" = i1tv
    // "C3:FC:67:A9:54:C7" = qLBl

    private final String elevator = "C3:FC:67:A9:54:C7"; // qLbl
    private final String downstairs = "CE:92:BD:85:DF:44"; // i1tv
    private final String upstairs = "C9:C8:7C:99:3E:FD"; // R10J
    private final String bikestation = "F4:F0:22:16:FD:C3"; // Sihf
    private final String carport = "CD:A2:4C:60:37:C8"; // JTUJ

    int y_0 = 0;
    int y_u = 0;

    NfcAdapter mNfcAdapter;

    private final String[] beaconArray = {"C3:FC:67:A9:54:C7", "CE:92:BD:85:DF:44", "C9:C8:7C:99:3E:FD", "CD:A2:4C:60:37:C8", "F4:F0:22:16:FD:C3"}; /*"C2:0D:F5:3D:BE:72",*/
    private final ArrayList<String> beacons = new ArrayList<>(Arrays.asList(beaconArray));
    ArrayList<String> scannedBeacons = new ArrayList<>();
    private boolean mScanning;
    private Handler mHandler;

    int smileUnicode = 0x1F60A;
    int sadUnicode = 0x1F641;


    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;

    //Public Fields for Fragments!
    public ArrayList<HashMap<String, Object>> fillMaps = new ArrayList<>();

    List<DataPoint> mockPoints = new ArrayList<DataPoint>();
    BarGraphSeries<DataPoint> series;
    int number = 4;


    public void appendLastDataSet(boolean isPositive, int range){


        if(isPositive){
            y_0 += range;
        }else{
            y_u -= range;
        }
        if(this.number == 4) {
            series.appendData(new DataPoint(10, y_0), false, 30);
            series.appendData(new DataPoint(10, y_u), false, 30);
            this.number++;
        }else{

            //copy global mockPoints
            List<DataPoint> localMockPoints = new ArrayList<DataPoint>(this.mockPoints);
            localMockPoints.add(new DataPoint(10, y_0));
            localMockPoints.add(new DataPoint(10, y_u));
            DataPoint[] points = new DataPoint[localMockPoints.size()];
            points = localMockPoints.toArray(points);

            series.resetData(points);
        }

        OverviewFragment overview  = (OverviewFragment) getSupportFragmentManager().findFragmentByTag("overview");

        if(overview != null) {
            overview.graph.getViewport().setMinX(0);
            overview.graph.getViewport().setMaxX(12);
            overview.graph.getViewport().setMinY(-4);
            overview.graph.getViewport().setMaxY(8);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("message", fillMaps);
    }

    private void restore(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            fillMaps = (ArrayList<HashMap<String, Object>>) savedInstanceState.getSerializable("message");
        }
    }


    //public method to add new elements to the list
    public void addToList(String title, String fDescr, String sDescri, boolean status){

        HashMap<String, Object> map = new HashMap<>();

        map.put("title", title); // This will be shown in R.id.title
        map.put("firstDescription", fDescr); // And this in R.id.description
        map.put("secondDescription", sDescri);

        if(status){
            map.put("colorStatus", new String(Character.toChars(smileUnicode)));
        }else{
            map.put("colorStatus", new String(Character.toChars(sadUnicode)));

        }

        fillMaps.add(0,map);

        HistoryFragment history  = (HistoryFragment) getSupportFragmentManager().findFragmentByTag("history");

        if(history != null){
            history.mListView.invalidateViews();
        }
    }

    public void executeFragmentCode(String text){


    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final String deviceAddress = device.getAddress();
                            if (beacons.contains(deviceAddress) && rssi > -80) {
                                Date c = Calendar.getInstance().getTime();
                                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                                String formattedDate = df.format(c);

                                NotificationCompat.Builder mBuilder = null;
                                if (scannedBeacons.size() == 0 || !scannedBeacons.get(scannedBeacons.size() - 1).equals(deviceAddress)) {
                                    if (deviceAddress.equals(elevator)) {
                                        // add Elevator
                                        //Toast.makeText(context, "Elevator! :(", Toast.LENGTH_LONG).show();
                                        addToList("Used Elevator", formattedDate, "20g", false);
                                        mBuilder = new NotificationCompat.Builder(context, "Channel 1")
                                                .setSmallIcon(R.drawable.ic_add_sap_24dp)
                                                .setContentTitle("Oh no!")
                                                .setContentText("You emitted 20 g CO2 by using the elevator.")
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                    } else if (deviceAddress.equals(bikestation)) {
                                        // add bikestation
                                        //Toast.makeText(context, "Bikestation! :)", Toast.LENGTH_LONG).show();
                                        addToList("Used Bike", formattedDate, "-20g", true);
                                        mBuilder = new NotificationCompat.Builder(context, "Channel 1")
                                                .setSmallIcon(R.drawable.ic_add_sap_24dp)
                                                .setContentTitle("Congratulations")
                                                .setContentText("You saved 20 g CO2 by driving by bike.")
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                    } else if (deviceAddress.equals(carport)) {
                                        // add Carport
                                        //Toast.makeText(context, "Carport! :(", Toast.LENGTH_LONG).show();
                                        addToList("Used Car", formattedDate, "140g", false);
                                        mBuilder = new NotificationCompat.Builder(context, "Channel 1")
                                                .setSmallIcon(R.drawable.ic_add_sap_24dp)
                                                .setContentTitle("Oh no!")
                                                .setContentText("You emitted 140 g CO2 by using the car.")
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                    } else if (scannedBeacons.size() > 0 && (scannedBeacons.get(scannedBeacons.size() - 1).equals(downstairs) && deviceAddress.equals(upstairs) || scannedBeacons.get(scannedBeacons.size() - 1).equals(upstairs) && deviceAddress.equals(downstairs))) {
                                        // add Stairs
                                        //Toast.makeText(context, "Stairs! :)", Toast.LENGTH_LONG).show();
                                        addToList("Used Stairs", formattedDate, "-20g", true);
                                        mBuilder = new NotificationCompat.Builder(context, "Channel 1")
                                                .setSmallIcon(R.drawable.ic_add_sap_24dp)
                                                .setContentTitle("Congratulations")
                                                .setContentText("You saved 20 g CO2 walking the stairs.")
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                    }

                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                                    if (mBuilder != null) {
                                        notificationManager.notify(1, mBuilder.build());
                                    }
                                    scannedBeacons.add(deviceAddress);
                                }


                                if (!scannedBeacons.contains(deviceAddress)) {
                                    TextView text = (TextView) findViewById(R.id.bluetoothView);
                                    text.setText(text.getText() + " ? " + rssi + " " + deviceAddress);
                                }
                            }

                        }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restore(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            //finish();
            return;

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        switchFragmentBasedOnId(R.id.nav_overview);

        mHandler = new Handler();
        context=this;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

            // Ensures Bluetooth is available on the device and it is enabled. If not,
            // displays a dialog requesting user permission to enable Bluetooth.
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }

        //setup history data
        addToList("Used Stairs", "26.04.2018 18:30", "-5 g" , true);
        addToList("Used Car", "28.04.2018 7:30", "+25 g" , false);

        //add mock data
        this.mockPoints.add(new DataPoint(2, 3));
        this.mockPoints.add(new DataPoint(2, -1));
        this.mockPoints.add(new DataPoint(4, 5));
        this.mockPoints.add(new DataPoint(4, -3));
        this.mockPoints.add(new DataPoint(6, 3));
        this.mockPoints.add(new DataPoint(6, -1));
        this.mockPoints.add(new DataPoint(8, 6));
        this.mockPoints.add(new DataPoint(8, -3));

        this.series = getBarGraphMockData();




        Intent intent = new Intent(this, AlertDetails.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);



        createNotificationChannel();
        handleIntent(getIntent());

    }


    public BarGraphSeries<DataPoint>  getBarGraphMockData(){

        DataPoint[] points = new DataPoint[this.mockPoints.size()];
        points = this.mockPoints.toArray(points);
        return new BarGraphSeries<>(points);

    }



    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Main Channel";
            String description = "Everything";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("Channel 1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("HackHPI 2018");
            alertDialog.setMessage("This App was hacked at Hackathon 2018 for the SAP Challenge from: \n\nTill, Pascal, Nick, David.\n\n" +
                    "With a lot of support from Coffein and Melina. ");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        scanLeDevice(true);
        stopForegroundDispatch(this, mNfcAdapter);
    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity, activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    protected void onResume() {
        super.onResume();

        setupForegroundDispatch(this, mNfcAdapter);

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }

        //Toast.makeText(this, "Start LE Scan", Toast.LENGTH_SHORT).show();

        scanLeDevice(true);

    }

    public void handleIntent(Intent intent){

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages =
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {

                NdefMessage[] messages = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                }

                Date c = Calendar.getInstance().getTime();
                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                String formattedDate = df.format(c);

                if (messages[0].getRecords()[0].getPayload()[3] == 65) {
                    // Add Essen A
                    addToList("Meal A (Steak)", formattedDate, "140g", false);
                    appendLastDataSet(false, 2);
                } else if (messages[0].getRecords()[0].getPayload()[3] == 66) {
                    // Add Essen B
                    addToList("Meal B (Veggie)", formattedDate, "140g", false);
                    appendLastDataSet(true, 3);
                }
                // Process the messages array.
            }
        }

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        switchFragmentBasedOnId(item.getItemId());
        return true;
    }

    public void switchFragmentBasedOnId(int id) {
        // Handle navigation view item clicks here.

        Fragment fragment = null;
        String title = getString(R.string.app_name);
        String tag = "";

        //skip if overview fragment is already there...

        if (id == R.id.nav_overview) {


            Fragment currentlyActive = (OverviewFragment) getSupportFragmentManager().findFragmentByTag("overview");


            fragment = new OverviewFragment();
            title  = "Overview";
            tag = "overview";



        } else if (id == R.id.nav_history) {
            fragment = new HistoryFragment();
            title  = "History";
            tag = "history";
        } else if (id == R.id.nav_statistics) {
            fragment = new StatisticsFragment();
            title  = "Statistics";
            tag = "statics";
        }


        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.drawer_layout2, fragment, tag);
            ft.addToBackStack(tag);

            ft.commit();
        }



        // set the toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            //finish();
        }


    }

    public void onFragmentInteraction(Uri uri) { }

    @Override
    public void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private class AlertDetails {
    }
}
