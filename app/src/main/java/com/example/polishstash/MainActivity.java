package com.example.polishstash;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.vision.barcode.Barcode;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Search API key:  AIzaSyAmS3l6_iHAQgtwY5ZI2-3M68XkPctqC3A
 */

public class MainActivity extends AppCompatActivity {

    List<String> links;
    Button scanbtn;
    TextView result;
    TableLayout table;

    private RequestQueue mRequestQueue;
    private StringRequest stringRequest;

    private static final String TAG = MainActivity.class.getName();
    private static final int NUM_COLS = 1;
    public static final int REQUEST_CODE = 100;
    public static final int PERMISSION_REQUEST = 200;
    private String API_NAME_GOOGLE = "Google";
    private String API_NAME_UPC = "UPC";

    // API stuff for Google
    private String GOOGLE_KEY = "AIzaSyAmS3l6_iHAQgtwY5ZI2-3M68XkPctqC3A";
    private String GOOGLE_CX = "010560061732309351313:-cyaupp_hty";
    private String GOOGLE_URL = "https://www.googleapis.com/customsearch/v1?key=" + GOOGLE_KEY + "&cx=" + GOOGLE_CX + "&q=";

    // API stuff for UPC Item DB
    private String UPC_URL = "https://api.upcitemdb.com/prod/trial/lookup?upc=";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanbtn = (Button) findViewById(R.id.scanbtn);
        result = (TextView) findViewById(R.id.result);
        table = (TableLayout) findViewById(R.id.tableForImages);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
        }
        scanbtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                                           startActivityForResult(intent, REQUEST_CODE);
                                       }
                                   }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                final Barcode barcode = data.getParcelableExtra("barcode");
                result.post(new Runnable() {
                    @Override
                    public void run() {
                        table.removeAllViews();
                        String str = barcode.displayValue;
                        sendGetRequest("UPC", str, "");

                    }
                });
            }
        }
    }

    /**
     * Send GET request.
     *
     * @param barcodeValue
     */
    private void sendGetRequest(final String apiType, String barcodeValue, String prName) {
        String getUrl;

        if (apiType == API_NAME_UPC) {
            getUrl = UPC_URL + barcodeValue;
        } else {
            getUrl = GOOGLE_URL + prName + " swatch" + "&searchType=image";
        }

        Log.i(TAG, "URL : " + getUrl);

        mRequestQueue = Volley.newRequestQueue(this);
        stringRequest = new StringRequest(Request.Method.GET, getUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TAG, "Response : " + response.toString());

                try {
                    if (apiType == API_NAME_UPC) {
                        getProductName(response);
                    } else {
                        getImageUrl(response);
                    }
                } catch (JSONException j) {
                    Log.i(TAG, "JSON Error : " + j);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error : " + error.toString());
            }
        });
        mRequestQueue.add(stringRequest);
    }

    /**
     * Parse the JSON for product name.
     *
     * @param response
     * @return
     * @throws JSONException
     */
    private void getProductName(String response) throws JSONException {
        JSONObject reader = new JSONObject(response);
        JSONArray items = reader.getJSONArray("items");
        JSONObject product = items.getJSONObject(0);
        String productName = product.getString("title");
        result.setText(productName);
        sendGetRequest(API_NAME_GOOGLE, "", productName);
    }

    /**
     * Parse JSON for search result image links.
     * Returns an array of image links.
     *
     * @param response
     * @throws JSONException
     */
    private void getImageUrl(String response) throws JSONException {
        JSONObject reader = new JSONObject(response);
        JSONArray items = reader.getJSONArray("items");
        links = new ArrayList();

        for (int i = 0; i < items.length(); i++) {
            String link = items.getJSONObject(i).getString("link");
            links.add(link);
        }

        Log.i(TAG, "links : " + links.toString());
        Log.i(TAG, "size : " + links.size());

        populateImages();
    }

    private void populateImages() {
        int linksSize = links.size();
        int count = 0;

        Log.i(TAG, "links size : " + linksSize);

        for (int row = 0; row < (linksSize); row++) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.MATCH_PARENT,
                    1.0f

            ));

            table.addView(tableRow);
            for (int col = 0; col < NUM_COLS; col++) {

                if (count < linksSize) {
                    Log.i(TAG, "populating ... " + count);
                    Log.i(TAG, "populating url ... " + links.get(count));
                    ImageView img = new ImageView(this);
                    Picasso.with(this).load(links.get(count)).placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .into(img, new com.squareup.picasso.Callback() {

                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError() {

                                }
                            });
                    tableRow.addView(img);
                    count++;

                }
            }
        }
    }


}
