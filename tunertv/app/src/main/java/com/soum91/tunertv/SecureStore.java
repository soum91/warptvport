package com.soum91.tunertv;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts IPTV account details using a device-local Android Keystore key. */
public final class SecureStore {
    private static final String ALIAS = "tunertv.private.v1";
    private final SharedPreferences prefs;

    public SecureStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("tuner_settings", Context.MODE_PRIVATE);
    }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey existing = (SecretKey) store.getKey(ALIAS, null);
        if (existing != null) return existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }

    public void putSecret(String name, String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] payload = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
        if (!prefs.edit().putString(name, Base64.encodeToString(payload, Base64.NO_WRAP)).commit())
            throw new IllegalStateException("Failed to store encrypted settings");
    }

    public String secret(String name) throws Exception {
        String stored = prefs.getString(name, null);
        if (stored == null) return "";
        byte[] payload = Base64.decode(stored, Base64.DEFAULT);
        if (payload.length < 29) throw new IllegalStateException("Encrypted settings are damaged");
        byte[] iv = new byte[12];
        System.arraycopy(payload, 0, iv, 0, 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(payload, 12, payload.length - 12), StandardCharsets.UTF_8);
    }

    public void saveSource(SourceSettings source) throws Exception {
        JSONObject object = new JSONObject();
        object.put("type", source.type.name());
        object.put("server", source.server);
        object.put("username", source.username);
        object.put("password", source.password);
        object.put("epg", source.epgUrl);
        object.put("hls", source.useHls);
        putSecret("source", object.toString());
    }

    public SourceSettings source() throws Exception {
        String data = secret("source");
        if (data.isEmpty()) return null;
        JSONObject obj = new JSONObject(data);
        return new SourceSettings(SourceSettings.Type.valueOf(obj.getString("type")),
                obj.getString("server"), obj.optString("username"), obj.optString("password"),
                obj.optString("epg"), obj.optBoolean("hls"));
    }

    public String lastChannel() { return prefs.getString("last_channel", ""); }
    public void setLastChannel(String id) { prefs.edit().putString("last_channel", id).apply(); }
    public String favorites() { return prefs.getString("favorites", ""); }
    public void setFavorites(String ids) { prefs.edit().putString("favorites", ids).apply(); }
}

