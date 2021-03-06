package com.brainydroid.daydreaming.ui.dashboard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.brainydroid.daydreaming.R;
import com.brainydroid.daydreaming.background.Logger;
import com.brainydroid.daydreaming.background.StatusManager;
import com.brainydroid.daydreaming.db.ParametersStorage;
import com.brainydroid.daydreaming.ui.FontUtils;
import com.google.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import roboguice.activity.RoboFragmentActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectResource;

@ContentView(R.layout.activity_glossary)
public class GlossaryActivity extends RoboFragmentActivity {

    private static String TAG = "GlossaryActivity";

    @Inject HashMap<String, View> glossaryPairsViews;
    @Inject ParametersStorage parametersStorage;
    @Inject StatusManager statusManager;

    @InjectResource(R.string.glossary_explanation_title) String explanationTitle;
    @InjectResource(R.string.glossary_explanation_text) String explanationText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.v(TAG, "Creating");
        super.onCreate(savedInstanceState);

        ViewGroup godfatherView = (ViewGroup) this.getWindow().getDecorView();
        populateGlossary();
        FontUtils.setRobotoFont(this, godfatherView);
    }

    @Override
    public void onResume() {
        Logger.v(TAG, "Resuming");
        super.onResume();

        if (!statusManager.is(StatusManager.GLOSSARY_EXPLAINED)) {
            Logger.d(TAG, "Glossary not explained yet, showing popup");
            statusManager.set(StatusManager.GLOSSARY_EXPLAINED);

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setCancelable(false)
                    .setTitle(explanationTitle)
                    .setMessage(explanationText)
                    .setPositiveButton("Got it", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
            alertBuilder.show();
        }
    }

    public void populateGlossary() {
        Logger.v(TAG, "Populating glossary");
        HashMap<String,String> dictionary = parametersStorage.getGlossary();
        for (Map.Entry<String,String> glossaryPair : dictionary.entrySet()) {
            glossaryPairsViews.put(glossaryPair.getKey(), inflateView(glossaryPair));
        }
    }

    @Override
    public void onBackPressed() {
        Logger.v(TAG, "Back pressed, slide transition");
        super.onBackPressed();
        overridePendingTransition(R.anim.push_bottom_in, R.anim.push_bottom_out);
    }

    public void onClick_backToDashboard(@SuppressWarnings("UnusedParameters") View v) {
        Logger.v(TAG, "Back to dashboard button clicked");
        onBackPressed();
    }

    private LinearLayout inflateView(Map.Entry glossaryPair) {
        Logger.v(TAG, "Inflating view for glossary");

        LinearLayout glossary_items_layout =
                (LinearLayout)findViewById(R.id.glossary_items_layout);

        LinearLayout linearLayout = (LinearLayout)getLayoutInflater().inflate(
                R.layout.glossary_item_layout, glossary_items_layout, false);

        TextView glossary_key =
                (TextView)linearLayout.findViewById(R.id.glossary_item_key);
        TextView glossary_value =
                (TextView)linearLayout.findViewById(R.id.glossary_item_value);

        Logger.d(TAG, "Glossary key: {}", glossaryPair.getKey().toString());
        Logger.d(TAG, "Glossary value: {}", glossaryPair.getValue().toString());

        glossary_key.setText(glossaryPair.getKey().toString());
        glossary_value.setText(glossaryPair.getValue().toString());

        glossary_items_layout.addView(linearLayout);
        return linearLayout;
    }

}
