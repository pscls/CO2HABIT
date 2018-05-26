package com.co2habit.hackhpi.hackhpi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

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

    private final String[] beaconArray = {"C3:FC:67:A9:54:C7", "CE:92:BD:85:DF:44", "C9:C8:7C:99:3E:FD", "CD:A2:4C:60:37:C8", "F4:F0:22:16:FD:C3"}; /*"C2:0D:F5:3D:BE:72",*/
    private final ArrayList<String> beacons = new ArrayList<>(Arrays.asList(beaconArray));
    ArrayList<String> scannedBeacons = new ArrayList<>();
    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;


    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final String deviceAddress = device.getAddress();
                            if (beacons.contains(deviceAddress) && rssi > -80) {
                                if (scannedBeacons.size() == 0 || !scannedBeacons.get(scannedBeacons.size() - 1).equals(deviceAddress)) {
                                    if (deviceAddress.equals(elevator)) {
                                        // add Elevator
                                        Toast.makeText(context, "Elevator! :(", Toast.LENGTH_LONG).show();
                                    } else if (deviceAddress.equals(bikestation)) {
                                        // add bikestation
                                        Toast.makeText(context, "Bikestation! :)", Toast.LENGTH_LONG).show();
                                    } else if (deviceAddress.equals(carport)) {
                                        // add Carport
                                        Toast.makeText(context, "Carport! :(", Toast.LENGTH_LONG).show();
                                    } else if (scannedBeacons.size() > 0 && (scannedBeacons.get(scannedBeacons.size() - 1).equals(downstairs) && deviceAddress.equals(upstairs) || scannedBeacons.get(scannedBeacons.size() - 1).equals(upstairs) && deviceAddress.equals(downstairs))) {
                                        // add Stairs
                                        Toast.makeText(context, "Stairs! :)", Toast.LENGTH_LONG).show();
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
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "Pause", Toast.LENGTH_SHORT).show();
        scanLeDevice(false);
    }

    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();

        if(intent != null)
            Log.d("1", " " + intent.toString());

        if (intent != null && NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages =
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                NdefMessage[] messages = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                }

                if (messages[0].getRecords()[0].getPayload()[3] == 65) {
                    // Add Essen A
                } else if (messages[0].getRecords()[0].getPayload()[3] == 66) {
                    // Add Essen B
                }
                // Process the messages array.
            }
        }



        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }

        //Toast.makeText(this, "Start LE Scan", Toast.LENGTH_SHORT).show();

        scanLeDevice(false);

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

        if (id == R.id.nav_overview) {
            fragment = new OverviewFragment();
            title  = "Overview";
        } else if (id == R.id.nav_addEntry) {
            fragment = new AddEntryFragment();
            title  = "Add Entry";
        } else if (id == R.id.nav_history) {
            fragment = new HistoryFragment();
            title  = "History";
        } else if (id == R.id.nav_statistics) {
            fragment = new StatisticsFragment();
            title  = "Statistics";
        }

        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.drawer_layout2, fragment);
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
        setIntent(intent);
    }

}
