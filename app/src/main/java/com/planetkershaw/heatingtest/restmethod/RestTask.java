package com.planetkershaw.heatingtest.restmethod;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**************************************************************************************************

 RestTask is an AsyncTask that is created each time an API request is made.
 The constructor takes the following parameters:

 urlstring - the url and specific end point of the API,
             eg http://<ip addr>:<port>/ZAutomation/API/v1/login
 sid - session identifier ie the cookie returned after logging in
 doOutput - true for POST, false for GET
 payload
 reqid - used to match up the original request
 handler - called from onPostExecute to tell the original caller we have a response

 To use, create an instance of RestTask and then call execute()
 This invokes the following steps:

 1. A call to onPreExecute() on the UI thread - not implemented
 2. A call to doInBackground() on the background thread
 3. Possible calls to onProgressUpdate on the UI thread (only if publishProgress is called)
 4. A call to onPostExecute() on the UI thread once doInBackground completes

 **************************************************************************************************/

public class RestTask extends AsyncTask<Void, Void, RestTask.APIResponse>
{
    private String sid=null;
    private boolean doOutput;
    private String payload;
    private String urlString;
    private Handler handler;
    private int reqid;

    public static final String PAYLOAD = "payload";
    public static final String REQID = "reqid";
    public static final String ERROR = "error";

    // an instance of this class is returned from doInBackground
    public class APIResponse
    {
        public String response;
        public int    error;
        APIResponse (String response, int error) {this.response = response; this.error = error;}
    }

    // constructor
    RestTask(String urlString, String sid, boolean doOutput, String payload, int reqid, Handler handler)
    {
        this.urlString = urlString;
        this.doOutput = doOutput;
        this.payload = payload;
        this.handler = handler;
        this.sid = sid;
        this.reqid = reqid;
    }

    @Override
    protected APIResponse doInBackground(Void... params)
    {
        HttpURLConnection urlConnection = null;
        String responseText="";
        int error = 0;
        APIResponse apiResponse;
        try {
            URL url = new URL(urlString);

            String cookieStr = "ZWAYSession="+sid;

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setDoOutput(doOutput);
            urlConnection.setRequestMethod(doOutput ? "POST" : "GET");
            urlConnection.setRequestProperty("Cookie", cookieStr);
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Content-Type", "application/json");

            if (payload != null) {
                byte[] outputBytes = payload.getBytes("UTF-8");
                OutputStream out = urlConnection.getOutputStream();
                out.write(outputBytes);
                out.close();
            }

            urlConnection.connect();
            int statusCode = urlConnection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                Log.d("RESTTASK", "doInBackground(): connection failed: statusCode: " + statusCode);
                error = statusCode;
            }
            else {
                // create JSON object from content
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                responseText = getResponseText(in);
            }
        }
        catch (IOException e)
        {
            Log.d("RESTTASK","IO Exception"+e.getMessage());
            e.printStackTrace();
            error = 10;
        }
        finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
        }

        // package up the results into a single object
        apiResponse = new APIResponse (responseText,error);
        return apiResponse;
    }

    private void processResponse (APIResponse response){
        Message message = new Message();
        Bundle data = new Bundle();
        data.putString(PAYLOAD, response.response);
        data.putInt(ERROR,response.error);
        data.putInt(REQID,reqid);
        message.setData(data);
        handler.sendMessage(message);
    }

    @Override
    protected void onPostExecute(APIResponse output)
    {
        processResponse (output);
    }

    @Override
    // TODO: this is going to cause an exception in processResponse, but I must have put it here
    // TODO: for a reason?
    protected void onCancelled()
    {
        processResponse (null);
    }

    private String getResponseText(InputStream inStream)
    {
        // very nice trick from
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        return new Scanner(inStream).useDelimiter("\\A").next();
    }

}
