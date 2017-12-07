package com.planetkershaw.heatingtest.restmethod;


import android.os.Handler;
import android.util.Log;

import com.planetkershaw.heatingtest.HeatingTestApp;
import com.planetkershaw.heatingtest.zwayservice.RoomList;
import com.planetkershaw.heatingtest.zwayservice.LightList;
import com.planetkershaw.heatingtest.zwayservice.Schedule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RestAPI
{
    public String cookie = null;
    private int roomId;
    HeatingTestApp app;

    //TODO: lots more strings need extracting from the code
    private final String apiURL = "/ZAutomation/api/v1/";

    public enum RequestType {LOGIN, GET_ROOMS, SET_MODE, SET_BOOST_TEMP, SET_BOOST_DURATION, SET_PUMP, SET_SCHEDULE, GET_LIGHTS, SET_LIGHT}

    public RestAPI (HeatingTestApp app)
    {
        //TODO: make singleton
        this.app = app;
    }

    private String getURLbase () {
        return app.zway_url + ":" + app.zway_port + apiURL;
    }

    public void loginRequest (String username,String password, Handler handler)
    {
        String payload = "{\"form\":true,\"login\":\"" + username + "\",\"password\":\""
                + password + "\",\"keepme\":false,\"default_ui\":1}";

        new RestTask(getURLbase()+"login", cookie, true, payload, RequestType.LOGIN.ordinal(), handler).execute((Void) null);
    }

    public void loginResponse (String responseBody)
    {
        // extract the cookie
        try
        {
            JSONObject jo = new JSONObject(responseBody);
            JSONObject data  = jo.getJSONObject("data");
            cookie = data.getString("sid");

        }
        catch (JSONException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void scheduleRequest (int id, Schedule schedule, Handler handler)
    {
        String urlParameters = schedule.toJSON();

        this.roomId = id;

        new RestTask(getURLbase()+"hillview/schedule/"+id,cookie,true,urlParameters,RequestType.SET_SCHEDULE.ordinal(),handler).execute((Void)null);
    }


    public static String getStatusText(int code) {

        switch(code) {
            case 200: return "OK";
            case 10: return "No route to server";

            default: return "Unknown Error";
        }
    }

    public void scheduleResponse (String responseBody)
    {
        if (responseBody != null)
        {
            String data = "";
            Schedule schedule = RoomList.get(roomId).schedule;
            schedule.clear();
            try {
                JSONObject jsonRootObject = new JSONObject(responseBody);
                JSONArray jsonArray = jsonRootObject.optJSONArray("data");
                for (int i=0; i<jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int minute = Integer.parseInt(jsonObject.optString("minute").toString());
                    int hour = Integer.parseInt(jsonObject.optString("hour").toString());
                    int day = Integer.parseInt(jsonObject.optString("day").toString());
                    double sp = Double.parseDouble(jsonObject.optString("sp").toString());
                    schedule.addTimer (day,hour,minute,sp);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            }
    }

    public void roomsRequest (Handler handler)
    {
        new RestTask(getURLbase()+"hillview/rooms", cookie, false, null, RequestType.GET_ROOMS.ordinal(),handler).execute((Void) null);
    }

    public void roomsResponse (String responseBody)
    {
        RoomList.clear();
        if (responseBody != null)
        {
            String data = "rooms response";
            try {
                JSONObject jsonRootObject = new JSONObject(responseBody);

                //Get the instance of JSONArray that contains JSONObjects
                JSONArray jsonArray = jsonRootObject.optJSONArray("data");

                //Iterate the jsonArray and print the info of JSONObjects
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    int id = Integer.parseInt(jsonObject.optString("id").toString());
                    double desiredTemp = Double.parseDouble(jsonObject.optString("desiredTemp").toString());
                    double currentTemp = Double.parseDouble(jsonObject.optString("currentTemp").toString());
                    double externalTemp;
                    boolean hasTempSensor = Boolean.parseBoolean(jsonObject.optString("hasTempSensor").toString());
                    boolean callForHeat = Boolean.parseBoolean(jsonObject.optString("callForHeat").toString());
                    String title = jsonObject.optString("title").toString();
                    String location = jsonObject.optString("location").toString();
                    RoomList.Type type = RoomList.Type.values()[Integer.parseInt(jsonObject.optString("type").toString())];
                    RoomList.Mode mode = RoomList.Mode.values()[Integer.parseInt(jsonObject.optString("mode").toString())];
                    RoomList.Mode base = RoomList.Mode.values()[Integer.parseInt(jsonObject.optString("baseMode").toString())];
                    double boostTemp = Double.parseDouble(jsonObject.optString("boostSP").toString());
                    int boostDuration = Integer.parseInt(jsonObject.optString("boostDuration").toString());
                    int boostTimeRemaining = Integer.parseInt(jsonObject.optString("boostTimeRemaining").toString());

                    // optional fields relating to hot water pump
                    boolean pumpStatus = false;
                    int pumpDuration = 0;
                    int pumpTimeRemaining = 0;
                    String pumpString = jsonObject.optString("pumpStatus");
                    String pumpDurationString = jsonObject.optString("pumpDuration");
                    String pumpTimeString = jsonObject.optString("pumpTimeRemaining");
                    if (!("".equals(pumpString) || "".equals(pumpDurationString) || "".equals(pumpTimeString))) {
                        pumpStatus = Boolean.parseBoolean(pumpString.toString());
                        pumpDuration = Integer.parseInt(pumpDurationString.toString());
                        pumpTimeRemaining = Integer.parseInt(pumpTimeString.toString());
                    }


                    Schedule schedule = new Schedule ();
                    String tempString = jsonObject.optString("externalTemp").toString();
                    externalTemp = (tempString!="") ? Double.parseDouble(tempString) : 0;

                    JSONArray jsonSchedule = jsonObject.optJSONArray("schedule");
                    int scheduleLength = jsonSchedule.length();
                    for (int j=0; j<scheduleLength; j++)
                    {
                        JSONObject jsonTimer = jsonSchedule.getJSONObject(j);
                        int day = Integer.parseInt(jsonTimer.optString("day").toString());
                        int hour = Integer.parseInt(jsonTimer.optString("hour").toString());
                        int minute = Integer.parseInt(jsonTimer.optString("minute").toString());
                        double sp = Double.parseDouble(jsonTimer.optString("sp").toString());
                        schedule.addTimer (day,hour,minute,sp);
                    }


//                    data += "Room" + i + " : \n id= " + id + " \n Name= " + title + " \n Pump Status " + pumpStatus + " \n";


                    RoomList.addRoom(id,title,desiredTemp,currentTemp,externalTemp,hasTempSensor,type,mode,base,schedule,
                            callForHeat,location,boostTemp,boostDuration,boostTimeRemaining,pumpStatus, pumpDuration, pumpTimeRemaining);

                }
                Log.d("RESTAPI",data);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    public void lightsRequest (Handler handler)
    {
        new RestTask(getURLbase()+"hillview/lights", cookie, false, null, RequestType.GET_LIGHTS.ordinal(),handler).execute((Void) null);
    }

    public void lightsResponse (String responseBody)
    {
        LightList.clear();
        if (responseBody != null)
        {
            String data = "lights response";
            try {
                JSONObject jsonRootObject = new JSONObject(responseBody);

                //Get the instance of JSONArray that contains JSONObjects
                JSONArray jsonArray = jsonRootObject.optJSONArray("data");

                //Iterate the jsonArray and print the info of JSONObjects
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String id = jsonObject.optString("id").toString();
                    JSONObject metricObject = jsonObject.optJSONObject("metrics");
                    String title = metricObject.optString("title").toString();
                    boolean status = metricObject.optString("level").toString().equals ("off") ? false : true;
                    String tag;
                    JSONArray tagsArray = jsonObject.optJSONArray("tags");
                    if (tagsArray.length() > 0)
                        tag = tagsArray.getString(0);
                    else
                        tag = "unknown";

//                    data += "Light" + i + " : \n id= " + id + " \n Name= " + title + " \n Status= " + status + " \n";

                    LightList.addLight(id,title,status);
                }
                Log.d("RESTAPI",data);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }


    public void modeRequest (Handler handler, int roomId, int mode)
    {
        String payload = "{\"data\":" + mode + "}";
        new RestTask(getURLbase()+"hillview/mode/"+roomId, cookie, true, payload, RequestType.SET_MODE.ordinal(), handler).execute((Void) null);
    }

    public void pumpStatusRequest (Handler handler, int roomId, boolean status)
    {
        String statusString = status ? "true" : "false";
        String payload = "{\"data\":" + "\""+statusString+"\"" + "}";
        new RestTask(getURLbase()+"hillview/pumpstatus/"+roomId, cookie, true, payload, RequestType.SET_PUMP.ordinal(), handler).execute((Void) null);
    }

    public void boostTempRequest (Handler handler, int roomId, double temp)
    {
        String payload = "{\"data\":" + temp + "}";
        new RestTask(getURLbase()+"hillview/boostsp/"+roomId, cookie, true, payload, RequestType.SET_BOOST_TEMP.ordinal(),handler).execute((Void) null);
    }

    public void boostDurationRequest (Handler handler, int roomId, int duration)
    {
        String payload = "{\"data\":" + duration + "}";
        new RestTask(getURLbase()+"hillview/boostduration/"+roomId, cookie, true, payload, RequestType.SET_BOOST_DURATION.ordinal(), handler).execute((Void) null);
    }

    public void lightStatusRequest (Handler handler, String lightId, boolean status)
    {
        String statusString = status ? "true" : "false";
        String payload = "{\"data\":" + "\""+statusString+"\"" + "}";
        new RestTask(getURLbase()+"hillview/lightstatus/"+lightId, cookie, true, payload, RequestType.SET_LIGHT.ordinal(), handler).execute((Void) null);
    }
}
