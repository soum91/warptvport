package com.soum91.tunertv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Called by Sony's Live TV channel-sources setup, when exposed by OEM firmware. */
public final class TvInputSetupActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        column.setPadding(80, 40, 80, 40);
        column.setBackgroundColor(0xFF07131B);
        TextView title = new TextView(this);
        title.setText("TunerTV · Sony TV input");
        title.setTextColor(0xFF45DAA4);
        title.setTextSize(28);
        column.addView(title);
        TextView status = new TextView(this);
        status.setText("Publish your Xtream live channels into Sony's TV source list (experimental).\nThe built-in tuner remains untouched.");
        status.setTextColor(0xFFFFFFFF);
        status.setTextSize(18);
        status.setPadding(0, 30, 0, 30);
        column.addView(status);
        Button publish = new Button(this);
        publish.setText("Publish live channels");
        column.addView(publish);
        publish.setOnClickListener(v -> {
            publish.setEnabled(false);
            status.setText("Loading live channels…");
            new Thread(() -> {
                try {
                    SourceSettings source = new SecureStore(this).source();
                    if (source == null || source.type != SourceSettings.Type.XTREAM)
                        throw new IllegalStateException("Configure an Xtream source in TunerTV first");
                    int count = TvPublisher.publish(this, new IpTvRepository(source).live());
                    runOnUiThread(() -> {
                        status.setText("Published " + count + " channels. Enable TunerTV in Sony's channel sources.");
                        setResult(RESULT_OK);
                        publish.setText("Done");
                        publish.setEnabled(true);
                        publish.setOnClickListener(done -> finish());
                    });
                } catch (Exception error) {
                    runOnUiThread(() -> {
                        status.setText("Sony TV input is unavailable, or account not configured: "
                                + error.getClass().getSimpleName());
                        publish.setEnabled(true);
                    });
                }
            }).start();
        });
        Button open = new Button(this);
        open.setText("Open TunerTV setup");
        column.addView(open);
        open.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        setContentView(column);
    }
}

