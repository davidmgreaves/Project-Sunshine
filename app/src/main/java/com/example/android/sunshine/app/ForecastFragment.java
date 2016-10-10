package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ForecastFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ForecastFragment#\newInstance} factory method to
 * create an instance of this fragment.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;
    private ListView mListView;

    public ForecastFragment() {

    }

    // mandatory interface
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_refresh){
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("94043");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] forecastArray = new String[] {"Today - Sunny - 89 / 63",
                "Tomorrow - Foggy - 70 / 46", "Weds - Cloudy - 72 / 63", "Thurs - Rainy - 64 / 51",
                "Fri - Foggy - 70 / 46", "Sat - Sunny - 76 / 58"};

        List<String> weekForecast = new ArrayList<>(Arrays.asList(forecastArray));
        mForecastAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);

        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask <String, Void, String[]>{

        String mForecastJsonStr = null;
        String mFormat = "json";
        String mUnits = "metric";
        int mNumDays = 7;
        Uri mWeatherApiUri;
        URL mWeatherRequestURL;

        private final String TAG = FetchWeatherTask.class.getSimpleName();
        private final String FORECAST_BASE_URL = "http://api.openweathermap." +
                "org/data/2.5/forecast/daily?";
        private final String QUERY_PARAM = "q";
        private final String FORMAT_PARAM = "mode";
        private final String UNITS_PARAM = "units";
        private final String DAYS_PARAM = "cnt";
        private final String API_KEY = "&appid=704e20d0067771864bf360687e8e3a1f";


        @Override
        protected void onPostExecute(String[] result) {
            if(result != null){
                mForecastAdapter.clear();
                for(String dayForecastStr : result){
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }

        @Override
        protected String[] doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;



            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                mWeatherApiUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, mFormat)
                        .appendQueryParameter(UNITS_PARAM, mUnits)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(mNumDays))
                        .build();

                mWeatherRequestURL = new URL(mWeatherApiUri.toString() + API_KEY);
                Log.v(TAG, "Weather URL: " + mWeatherRequestURL);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) mWeatherRequestURL.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                mForecastJsonStr = buffer.toString();
                Log.v(TAG, "Forecast Json String: " + mForecastJsonStr);
            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in
                // attemping to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            try{
                return getWeatherDataFromJson(mForecastJsonStr, mNumDays);
            } catch(JSONException e){
                Log.e(TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;


        }


        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(),
                    dayTime.gmtoff);

            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++){
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);
                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + "-" + description + "-" + highAndLow;
            }

            for(String s : resultStrs){
                Log.v(TAG, "Forecast Entry: " + s);
            }

            return resultStrs;
        }

        // strip decimals for presentation purposes
        private String formatHighLows(double high, double low) {

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        // converts the unix timestamp returned by the API (measured in seconds) into a value of
        // milliseconds so that it can be converted to a valid date using the format(time_ method
        private String getReadableDateString(long time) {

            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return  shortenedDateFormat.format(time);
        }
    }
    
}
