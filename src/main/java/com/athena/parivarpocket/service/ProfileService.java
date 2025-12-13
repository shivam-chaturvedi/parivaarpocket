package com.athena.parivarpocket.service;

import com.athena.parivarpocket.model.UserRole;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ProfileService {
    private static final String PROFILES_TABLE = "profiles";
    private final SupabaseClient supabaseClient = new SupabaseClient();

    public UserRole fetchRoleByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String encoded = URLEncoder.encode(email.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8);
        JsonArray rows = supabaseClient.fetchTable(PROFILES_TABLE, "email=eq." + encoded, null);
        if (rows == null || rows.size() == 0) {
            return null;
        }
        JsonObject profile = rows.get(0).getAsJsonObject();
        String roleValue = safeString(profile, "role");
        return UserRole.fromMetadata(roleValue);
    }

    public void upsertProfile(String email, UserRole role) {
        if (email == null || email.isBlank() || role == null) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email.toLowerCase(Locale.ROOT));
        payload.addProperty("role", role.asMetadata());
        supabaseClient.insertRecord(PROFILES_TABLE, "on_conflict=email", payload, null);
    }

    private String safeString(JsonObject json, String key) {
        if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }
}
