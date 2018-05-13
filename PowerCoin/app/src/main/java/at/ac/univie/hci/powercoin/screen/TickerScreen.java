package at.ac.univie.hci.powercoin.screen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;

import at.ac.univie.hci.powercoin.functionality.Graph;
import at.ac.univie.hci.powercoin.R;


public class TickerScreen extends AppCompatActivity implements View.OnClickListener {

    /**HAMBURGER-MENU RELATED
     *mDrawerLayout: Links to Layout for Hamburger Menu
     * mToggle: makes the Hamburger Button clickable
     */
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;

    /**API RELATED
     * upQueue: creates queue for API
     * upUrl: saves API-Url
     * upVal: saves latest price
     * upTime: saves latest time
     *
     * sinceQueue: creates queue for API
     * sinceUrl: saves API-Url
     * sinceVal: saves array of prices since now
     * sinceTime: saves array of times for the values
     */
    private RequestQueue upQueue;
    private String upUrl;
    private double upVal;
    private long upTime = 0;

    private RequestQueue sinceQueue;
    private String sinceUrl;
    private double [] sinceVal;
    private long [] sinceTime;


    /**GRAPH RELATED
     * mHandler: https://developer.android.com/reference/android/os/Handler
     * mTimer: https://developer.android.com/reference/java/lang/Runnable
     * graph: creates Graph to fill
     * currency: displays different currencies
     */
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private Graph graph;
    private String currency = " $ ";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticker_screen);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        //HAMBURGER-RELATED STUFF THERE
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close );

        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        NavigationView nv = findViewById(R.id.hamburger);
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case(R.id.nav_ticker):
                        startTicker();
                        break;
                    case(R.id.nav_calc):
                        startCalculator();
                        break;
                    case(R.id.nav_portfolio):
                        startPortfolio();
                        break;
                    case(R.id.nav_notification):
                        startNotification();
                        break;
                    case(R.id.nav_settings):
                        startSettings();
                        break;
                }
                return false;
            }
        });

        //API-RELATED STUFF HERE

        //TODO: make expandable
        upQueue = Volley.newRequestQueue(this);
        upUrl = "https://api.cryptowat.ch/markets/kraken/btcusd/price";

        sinceQueue = Volley.newRequestQueue(this);
        //every hour for 24 hours
        sinceUrl = "https://api.cryptowat.ch/markets/gdax/btcusd/trades?limit=24&since=" + (System.currentTimeMillis() / 1000L - 86400);


        JsonRequest sinceReq = new JsonObjectRequest(

                Request.Method.GET, sinceUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("API_RESPONSE", response.toString());

                        sinceProcessResult(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(TickerScreen.this,
                                "API is not responding!",
                                Toast.LENGTH_LONG).show();
                        if(error.getMessage() != null) Log.e("API_ERROR", error.getMessage());
                    }
                });
        sinceQueue.add(sinceReq);

    }



    //fills graph with intel
    private void sinceProcessResult (JSONObject apiResponse) {
        try {

            JSONArray data = apiResponse.getJSONArray("result");
            sinceVal = new double[data.length()];
            sinceTime = new long[data.length()];

            for(int i = 0; i < sinceVal.length; i++) {
                sinceVal[i] = data.getJSONArray(i).getDouble(2) * 1000;
                sinceTime[i] = data.getJSONArray(i).getLong(1) * 1000;
            }

            for(int i = 0; i < sinceVal.length; i++) {
                System.out.println(sinceTime[i]);
            }

            createGraph();

        } catch(JSONException e){
            Toast.makeText(TickerScreen.this,
                    "Could not parse API response for Creation!",
                    Toast.LENGTH_LONG).show();
            Log.e("PARSER_ERROR", e.getMessage());
        }
    }

    private void createGraph() {
        //GRAPH-RELATED STUFF HERE (NO TOUCH)
        GraphView graphView = findViewById(R.id.graph);
        graph = new Graph();
        graphView.addSeries(graph.newGraph(sinceVal, sinceTime));

        graphView.getViewport().setScalable(true);
        graphView.getViewport().setScalableY(true);

        NumberFormat currency = NumberFormat.getCurrencyInstance();
        NumberFormat time = NumberFormat.getInstance();
        graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(time, currency));


        Button buttonUpdateGraph = findViewById(R.id.graphManualUpdate);
        buttonUpdateGraph.setOnClickListener(this);
        //END OF GRAPH-RELATED STUFF

    }

    //updates the graph
    private void UpProcessResult (JSONObject apiResponse) {
        try {

            JSONObject data = apiResponse.getJSONObject("result");
            upVal = data.getDouble("price");
            upTime = System.currentTimeMillis();
            graph.updateGraph(upVal, upTime);

        } catch(JSONException e){
            Toast.makeText(TickerScreen.this,
                    "Could not parse API response!",
                    Toast.LENGTH_LONG).show();
            Log.e("PARSER_ERROR", e.getMessage());
        }
    }

    //AUTO UPDATE STUFF HERE
    @Override
    public void onResume() {
        super.onResume();


        mTimer = new Runnable() {
            @Override
            public void run() {

                JsonRequest upRequest = new JsonObjectRequest(
                        Request.Method.GET, upUrl, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.i("API_RESPONSE", response.toString());
                                UpProcessResult(response);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(TickerScreen.this,
                                        "Please try again!",
                                        Toast.LENGTH_LONG).show();
                                if(error.getMessage() != null) Log.e("API_ERROR", error.getMessage());
                            }
                        }
                );
                upQueue.add(upRequest);
                mHandler.postDelayed(this, 5000);
            }
        };
        mHandler.postDelayed(mTimer, 1000);
        Log.d("GRAPH", "Successfully updated automatically!");
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(mTimer);
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId())
        {
            case R.id.graphManualUpdate:

                Log.d("GRAPH", "Update Button was clicked!");
                JsonRequest upReq = new JsonObjectRequest(
                        Request.Method.GET, upUrl, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.i("API_RESPONSE", response.toString());
                                UpProcessResult(response);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(TickerScreen.this,
                                        "Please try again!",
                                        Toast.LENGTH_LONG).show();
                                if(error.getMessage() != null) Log.e("API_ERROR", error.getMessage());
                            }
                        });
                upQueue.add(upReq);
                graph.updateGraph(upVal, System.currentTimeMillis());
                Log.d("GRAPH", "Successfully updated!");
                break;
            default:
                throw new RuntimeException("Unknown button ID");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ticker_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /* kept for future reference
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startPortfolio();
            //return true;
        }
        */

        //enables Hamburger-Menu to be opened by pressing the button
        if(mToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startTicker() {
        Intent intent = new Intent(this, TickerScreen.class);
        startActivity(intent);
    }
    public void startCalculator() {
        Intent intent = new Intent(this, CalculatorScreen.class);
        startActivity(intent);
    }
    public void startNotification() {
        Intent intent = new Intent(this, NotificationScreen.class);
        startActivity(intent);
    }
    public void startSettings() {
        Intent intent = new Intent(this, SettingsScreen.class);
        startActivity(intent);
    }
    public void startPortfolio(){
        Intent intent = new Intent(this, PortfolioScreen.class);
        startActivity(intent);
    }



}
