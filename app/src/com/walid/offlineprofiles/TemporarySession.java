package com.walid.offlineprofiles;

import org.json.JSONException;
import org.json.JSONObject;

final class TemporarySession {
    final int temporaryProfileId;
    final int previousProfileId;
    final long untilMillis;
    final Profile previousSound;

    TemporarySession(int temporaryProfileId, int previousProfileId, long untilMillis,
            Profile previousSound) {
        this.temporaryProfileId = temporaryProfileId;
        this.previousProfileId = previousProfileId;
        this.untilMillis = untilMillis;
        this.previousSound = previousSound;
    }

    JSONObject toJson() throws JSONException {
        JSONObject value = new JSONObject();
        value.put("temporaryProfileId", temporaryProfileId);
        value.put("previousProfileId", previousProfileId);
        value.put("untilMillis", untilMillis);
        value.put("previousSound", previousSound.toJson());
        return value;
    }

    static TemporarySession fromJson(JSONObject value) {
        JSONObject sound = value.optJSONObject("previousSound");
        if (sound == null) return null;
        return new TemporarySession(value.optInt("temporaryProfileId", -1),
                value.optInt("previousProfileId", -1), value.optLong("untilMillis", 0),
                Profile.fromJson(sound));
    }
}
