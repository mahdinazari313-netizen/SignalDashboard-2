package com.example.signaldashboard.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A single parsed trading signal for one symbol + timeframe.
 */
public class Signal {

    public String symbol;
    public String timeframe;
    public String direction; // "Buy" or "Sell"
    public long receiveTime; // millis, from the MT notification timestamp
    public long expiryTime;  // millis, receiveTime + (candleCount * timeframeDurationMs)

    public Signal() {
    }

    public Signal(String symbol, String timeframe, String direction, long receiveTime, long expiryTime) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.direction = direction;
        this.receiveTime = receiveTime;
        this.expiryTime = expiryTime;
    }

    public boolean isExpired(long now) {
        return expiryTime <= now;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("symbol", symbol);
            o.put("timeframe", timeframe);
            o.put("direction", direction);
            o.put("receiveTime", receiveTime);
            o.put("expiryTime", expiryTime);
        } catch (JSONException ignored) {
            // fields are simple primitives/strings, cannot fail
        }
        return o;
    }

    public static Signal fromJson(JSONObject o) throws JSONException {
        Signal s = new Signal();
        s.symbol = o.getString("symbol");
        s.timeframe = o.getString("timeframe");
        s.direction = o.getString("direction");
        s.receiveTime = o.getLong("receiveTime");
        s.expiryTime = o.getLong("expiryTime");
        return s;
    }
}
