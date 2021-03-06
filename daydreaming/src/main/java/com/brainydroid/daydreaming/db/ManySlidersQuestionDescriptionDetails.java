package com.brainydroid.daydreaming.db;

import com.brainydroid.daydreaming.background.Logger;
import com.brainydroid.daydreaming.sequence.PreLoadCallback;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.ArrayList;

public class ManySlidersQuestionDescriptionDetails implements IQuestionDescriptionDetails {

    @SuppressWarnings("FieldCanBeLocal")
    private static String TAG = "ManySlidersQuestionDescriptionDetails";

    public static int DEFAULT_INITIAL_POSITION = -1;
    public static String TYPE = "ManySliders";

    @JsonView(Views.Internal.class)
    private String type = TYPE;
    @JsonView(Views.Internal.class)
    private String text = null;
    @JsonView(Views.Internal.class)
    private ArrayList<String> availableSliders = null;
    @JsonView(Views.Internal.class)
    private ArrayList<String> defaultSliders = null;
    @JsonView(Views.Internal.class)
    private ArrayList<String> hints = null;
    @JsonView(Views.Internal.class)
    private int initialPosition = DEFAULT_INITIAL_POSITION;
    @JsonView(Views.Internal.class)
    private boolean showLiveIndication = false;
    @JsonView(Views.Internal.class)
    private boolean alreadyValid = false;
    @JsonView(Views.Internal.class)
    private String addItemHint = null;
    @JsonView(Views.Internal.class)
    private String dialogText = null;

    @Override
    public synchronized boolean isPreLoaded() {
        return true;
    }

    @Override
    public synchronized void onPreLoaded(PreLoadCallback preLoadCallback) {
        // This question is always already loaded
        if (preLoadCallback != null) {
            preLoadCallback.onPreLoaded();
        }
    }

    @Override
    public synchronized Object getPreLoadedObject() {
        return null;
    }

    @Override
    public synchronized String getType() {
        return type;
    }

    public synchronized String getText() {
        return text;
    }

    public synchronized ArrayList<String> getAvailableSliders() {
        return availableSliders;
    }

    public synchronized ArrayList<String> getDefaultSliders() {
        return defaultSliders;
    }

    public synchronized ArrayList<String> getHints() {
        return hints;
    }

    public int getInitialPosition() {
        return initialPosition;
    }

    public boolean isShowLiveIndication() {
        return showLiveIndication;
    }

    public boolean isAlreadyValid() {
        return alreadyValid;
    }

    public String getAddItemHint() {
        return addItemHint;
    }

    public String getDialogText() {
        return dialogText;
    }

    public synchronized void validateInitialization() throws JsonParametersException {
        Logger.v(TAG, "Validating question details");

        if (text == null) {
            throw new JsonParametersException("text in " +
                    "ManySlidersQuestionDescriptionDetails can't be null");
        }

        if (availableSliders == null || availableSliders.size() == 0) {
            throw new JsonParametersException("availableSliders in " +
                    "ManySlidersQuestionDescriptionDetails can't by null or empty");
        }

        if (defaultSliders == null || defaultSliders.size() == 0) {
            throw new JsonParametersException("defaultSliders in " +
                    "ManySlidersQuestionDescriptionDetails can't by null or empty");
        }

        for (String slider : defaultSliders) {
            if (!availableSliders.contains(slider)) {
                throw new JsonParametersException("All sliders in defaultSliders must be defined " +
                        "in availableSliders too");
            }
        }

        if (hints == null || hints.size() < 2) {
            throw new JsonParametersException("There must be at least two hints in " +
                    "ManySlidersQuestionDescriptionDetails");
        }

        if (initialPosition != DEFAULT_INITIAL_POSITION &&
                (initialPosition < 0 || initialPosition > 100)) {
            throw new JsonParametersException("initialPosition must be between 0 and 100 in "
                    + "ManySlidersQuestionDescriptionDetails");
        }

        if (addItemHint == null) {
            throw new JsonParametersException("addItemHint can't be null in" +
                    " ManySlidersQuestionDescriptionDetails");
        }

        if (dialogText == null) {
            throw new JsonParametersException("dialogText can't be null in" +
                    " ManySlidersQuestionDescriptionDetails");
        }
    }
}
