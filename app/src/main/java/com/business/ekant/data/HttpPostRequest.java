package com.business.ekant.data;

import android.content.ContentValues;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

public class HttpPostRequest {
    public JSONObject POST(String _url, ContentValues _params) throws ParseException, IOException {
        HttpURLConnection urlConn = null;
        StringBuffer sbParams = new StringBuffer();

        boolean isAnd = false;

        String key;
        String value;

        for (Map.Entry<String, Object> parameter : _params.valueSet()) {
            key = parameter.getKey();
            value = parameter.getValue().toString();

            if (isAnd)
                sbParams.append("&");

            sbParams.append(key).append("=").append(value);

            if (!isAnd)
                if (_params.size() >= 2)
                    isAnd = true;
        }

        try {
            URL url = new URL(_url);
            urlConn = (HttpURLConnection) url.openConnection();

            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Accept-Charset", "UTF-8");
            urlConn.setRequestProperty("Context_Type", "application/x-www-form-urlencoded;charset=UTF-8");

            String strParams = sbParams.toString();
            OutputStream os = urlConn.getOutputStream();
            os.write(strParams.getBytes("UTF-8"));
            os.flush();
            os.close();

            if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK)
                return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

            String line;
            String page = "";

            while ((line = reader.readLine()) != null) {
                page += line;
            }

            return new JSONObject(page);

        } catch (MalformedURLException e) { // for URL.
            e.printStackTrace();
        } catch (IOException e) { // for openConnection().
            e.printStackTrace();
        } catch (JSONException e) { // for openConnection().
            e.printStackTrace();
        } finally {
            if (urlConn != null)
                urlConn.disconnect();
        }

        return null;
    }

    public JSONObject POST(String _url, ContentValues _params, String token) throws ParseException, IOException {
        HttpURLConnection urlConn = null;
        StringBuffer sbParams = new StringBuffer();

        boolean isAnd = false;

        String key;
        String value;

        for (Map.Entry<String, Object> parameter : _params.valueSet()) {
            key = parameter.getKey();
            value = parameter.getValue().toString();

            if (isAnd)
                sbParams.append("&");

            sbParams.append(key).append("=").append(value);

            if (!isAnd)
                if (_params.size() >= 2)
                    isAnd = true;
        }

        try {
            URL url = new URL(_url);
            urlConn = (HttpURLConnection) url.openConnection();

            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Accept-Charset", "UTF-8");
            urlConn.setRequestProperty("Context_Type", "application/x-www-form-urlencoded;charset=UTF-8");
            urlConn.setRequestProperty("X-Requested-With","XMLHttpRequest");
            urlConn.setRequestProperty("Authorization","Bearer "+ token);


            String strParams = sbParams.toString();
            OutputStream os = urlConn.getOutputStream();
            os.write(strParams.getBytes("UTF-8"));
            os.flush();
            os.close();



            int status = 0;
            try {
                status = urlConn.getResponseCode();
            } catch (IOException e) {
                // HttpUrlConnection will throw an IOException if any 4XX
                // response is sent. If we request the status again, this
                // time the internal status will be properly set, and we'll be
                // able to retrieve it.
                //status = urlConn.getResponseCode();
            }
            if (status == 401) {
                // ...
                JSONObject result = new JSONObject();
                result.put("status", "error");
                result.put("message", "ERR_INVALID_TOKEN");
                return result;
            }
            else if (status != HttpURLConnection.HTTP_OK)
                return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

            String line;
            String page = "";

            while ((line = reader.readLine()) != null) {
                page += line;
            }

            return new JSONObject(page);

        } catch (MalformedURLException e) { // for URL.
            e.printStackTrace();
        } catch (IOException e) { // for openConnection().
            e.printStackTrace();
        } catch (JSONException e) { // for openConnection().
            e.printStackTrace();
        } finally {
            if (urlConn != null)
                urlConn.disconnect();
        }

        return null;
    }

    public JSONObject GET(String _url, ContentValues _params) throws ParseException, IOException {
        HttpURLConnection urlConn = null;

        boolean isAnd = false;

        String key;
        String value;
        _url += "?";

        for (Map.Entry<String, Object> parameter : _params.valueSet()) {
            key = parameter.getKey();
            value = parameter.getValue().toString();

            if (isAnd)
                _url +=  "&";

            _url += key + "=" + value;

            if (!isAnd)
                if (_params.size() >= 2)
                    isAnd = true;
        }

        try {
            URL url = new URL(_url);
            urlConn = (HttpURLConnection) url.openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

            String line;
            String page = "";

            while ((line = reader.readLine()) != null) {
                page += line;
            }

            return new JSONObject(page);

        } catch (MalformedURLException e) { // for URL.
            e.printStackTrace();
        } catch (IOException e) { // for openConnection().
            e.printStackTrace();
        } catch (JSONException e) { // for openConnection().
            e.printStackTrace();
        } finally {
            if (urlConn != null)
                urlConn.disconnect();
        }

        return null;
    }

    public JSONObject GET(String _url, ContentValues _params, String token) throws ParseException, IOException {
        HttpURLConnection urlConn = null;

        boolean isAnd = false;

        String key;
        String value;
        _url += "?";

        for (Map.Entry<String, Object> parameter : _params.valueSet()) {
            key = parameter.getKey();
            value = parameter.getValue().toString();

            if (isAnd)
                _url +=  "&";

            _url += key + "=" + value;

            if (!isAnd)
                if (_params.size() >= 2)
                    isAnd = true;
        }

        try {
            URL url = new URL(_url);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            urlConn.setRequestProperty("Authorization","Bearer "+ token);

            int status = 0;
            try {
                status = urlConn.getResponseCode();
            } catch (IOException e) {
                // HttpUrlConnection will throw an IOException if any 4XX
                // response is sent. If we request the status again, this
                // time the internal status will be properly set, and we'll be
                // able to retrieve it.
                status = urlConn.getResponseCode();
            }
            if (status == 401) {
                // ...
                JSONObject result = new JSONObject();
                result.put("status", "error");
                result.put("message", "ERR_INVALID_TOKEN");
                return result;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));


            String line;
            String page = "";

            while ((line = reader.readLine()) != null) {
                page += line;
            }

            return new JSONObject(page);

        } catch (MalformedURLException e) { // for URL.
            e.printStackTrace();
        } catch (IOException e) { // for openConnection().
            e.printStackTrace();
        } catch (JSONException e) { // for openConnection().
            e.printStackTrace();
        } finally {
            if (urlConn != null)
                urlConn.disconnect();
        }

        return null;
    }
}
