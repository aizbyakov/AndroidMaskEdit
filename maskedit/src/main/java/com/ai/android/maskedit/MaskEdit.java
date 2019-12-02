package com.ai.android.maskedit;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

import java.security.InvalidParameterException;
import java.util.ArrayList;

public class MaskEdit extends EditText {
    private static final String DEFAULT_DELIMITER = "-";
    private static final int DEFAULT_GROUP_LENGTH = 4;
    private static final boolean DEFAULT_SHOW_DELIMITER_BEFORE_NEXT_CHARACTER = true;
    private static final boolean DEFAULT_REMOVE_DELIMITER_IN_LAST_POSITION = true;

    private ArrayList<TextWatcher> textWatchers;

    public MaskEdit(Context context) {
        super(context);

        initDelimiterInjectorTextWatcher(null);
    }

    public MaskEdit(Context context, AttributeSet attrs) {
        super(context, attrs);

        initDelimiterInjectorTextWatcher(attrs);
    }

    public MaskEdit(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initDelimiterInjectorTextWatcher(attrs);
    }

    public MaskEdit(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initDelimiterInjectorTextWatcher(attrs);
    }

    protected void initDelimiterInjectorTextWatcher(AttributeSet attributeSet) {
        char delimiter = DEFAULT_DELIMITER.charAt(0);
        int groupLength = DEFAULT_GROUP_LENGTH;
        boolean showDelimiterBeforeNextCharacter = DEFAULT_SHOW_DELIMITER_BEFORE_NEXT_CHARACTER;
        boolean removeDelimiterInLastPosition = DEFAULT_REMOVE_DELIMITER_IN_LAST_POSITION;


        if (attributeSet != null) {
            TypedArray attrs = getContext().getTheme().obtainStyledAttributes(attributeSet, R.styleable.MaskEdit, 0, 0);
            try {
                String delimiterAttr = attrs.getString(R.styleable.MaskEdit_key_delimiter);

                if (delimiterAttr != null) {
                    if (delimiterAttr.length() != 1)
                        throw new InvalidParameterException("Delimiter length must be 1");

                    delimiter = delimiterAttr.charAt(0);
                }

                groupLength = attrs.getInt(R.styleable.MaskEdit_key_group_length, DEFAULT_GROUP_LENGTH);
                showDelimiterBeforeNextCharacter = attrs.getBoolean(R.styleable.MaskEdit_key_show_delimiter_before_next_char, DEFAULT_SHOW_DELIMITER_BEFORE_NEXT_CHARACTER);
                removeDelimiterInLastPosition = attrs.getBoolean(R.styleable.MaskEdit_key_remove_delimiter_in_last_position, DEFAULT_REMOVE_DELIMITER_IN_LAST_POSITION);
            } finally {
                attrs.recycle();
            }
        }

        addTextChangedListener(new DelimiterInjector(delimiter, groupLength, showDelimiterBeforeNextCharacter, removeDelimiterInLastPosition));
    }

    @Override
    public void addTextChangedListener(TextWatcher watcher) {
        //First text watcher is always delimiter injector
        if (textWatchers == null) {
            super.addTextChangedListener(watcher);

            textWatchers = new ArrayList<>();
        } else {
            textWatchers.add(watcher);
        }
    }

    @Override
    public void removeTextChangedListener(TextWatcher watcher) {
        if (textWatchers != null) {
            textWatchers.remove(watcher);
        }
    }

    private class DelimiterInjector implements TextWatcher {
        private final String TAG = DelimiterInjector.class.getName();

        private static final String TEST_CHAR_AS_STRING = "a";
        private static final char MIN_CHAR = ' ';

        private boolean showDelimiterBeforeNextCharacter;
        private boolean removeDelimiterInLastPosition;
        private boolean isModificationInProgress = false;
        private boolean isLengthExceedMaxLength = true;
        private int backspacePressedBeforeDelimiterAt;

        private char delimiter;
        private String delimiterAsString;
        private int groupLength;

        DelimiterInjector(char delimiter, int groupLength, boolean showDelimiterBeforeNextCharacter, boolean removeDelimiterInLastPosition) {
            if (groupLength < 0)
                throw new InvalidParameterException("Group length must be >= 0");

            if (delimiter <= MIN_CHAR)
                throw new InvalidParameterException("Delimiter must be alphabetic symbol");

            this.showDelimiterBeforeNextCharacter = showDelimiterBeforeNextCharacter;
            this.removeDelimiterInLastPosition = removeDelimiterInLastPosition;
            this.delimiter = delimiter;
            this.delimiterAsString = String.valueOf(this.delimiter);
            this.groupLength = groupLength;

            if (BuildConfig.DEBUG)
                Log.d(TAG, String.format("DelimiterInjector: delimiter = %s, groupLength = %d", delimiter, groupLength));
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (isModificationInProgress)
                return;

            if (BuildConfig.DEBUG)
                Log.d(TAG, String.format("beforeTextChanged: s = %s, start = %d, count = % d, after = %d", s, start, count, after));

            if (count == 1 && after == 0 && delimiter == s.charAt(start))
                backspacePressedBeforeDelimiterAt = start - 1;
            else
                backspacePressedBeforeDelimiterAt = -1;

            if (textWatchers != null) {
                for (int i = 0; i < textWatchers.size(); i++) {
                    textWatchers.get(i).beforeTextChanged(s, start, count, after);
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            isLengthExceedMaxLength = false;

            if (isModificationInProgress)
                return;

            if (BuildConfig.DEBUG)
                Log.d(TAG, String.format("onTextChanged: s = %s, start = %d, count = % d", s, start, count));

            if (textWatchers != null) {
                for (int i = 0; i < textWatchers.size(); i++) {
                    textWatchers.get(i).onTextChanged(s, start, before, count);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isModificationInProgress)
                return;

            if (BuildConfig.DEBUG)
                Log.d(TAG, String.format("afterTextChanged: s = %s", s));

            isModificationInProgress = true;

            modifyText(s);

            isModificationInProgress = false;

            if (textWatchers != null) {
                for (int i = 0; i < textWatchers.size(); i++) {
                    textWatchers.get(i).afterTextChanged(s);
                }
            }
        }

        private void modifyText(Editable s) {
            for (int i = s.length() - 1; i >= 0; i--) {
                if (s.charAt(i) == delimiter)
                    s.replace(i, i + 1, "");

                if (backspacePressedBeforeDelimiterAt == i)
                    s.replace(i, i + 1, "");
            }

            if (groupLength < 1)
                return;

            int maxDelimiterPosition = s.length() - (showDelimiterBeforeNextCharacter ? 1 : 0);

            for (int i = maxDelimiterPosition; i > 0; i--) {
                if (i % groupLength == 0) {
                    isLengthExceedMaxLength = true;

                    s.insert(i, delimiterAsString);

                    //There is no ability to get current maxLength. If it is exceeded, beforeTextChanged and onTextChanged are not invoked.
                    //If after inserting the onTextChanged has not been called, we have to reduce length until delimiter is inserted successfully.
                    while(isLengthExceedMaxLength) {
                        s.replace(s.length() - 1, s.length(),"");
                        s.insert(i, delimiterAsString);
                    }
                }
            }

            //There is no ability to get current maxLength. If it is exceeded, beforeTextChanged and onTextChanged are not invoked.
            //If after inserting the onTextChanged has not been called, we have to reduce length until delimiter is inserted successfully.
            //So we need to test if it is possible to insert one symbol after last delimiter in case we show it after a char is added and we
            //need to remove last delimiter.
            if (removeDelimiterInLastPosition && s.length() > 0 && s.charAt(s.length() - 1) == delimiter) {
                s.append(TEST_CHAR_AS_STRING);
                //If max length exceed test character is not added, so the last delimiter is removed, if max length is not exceed test character is removed.
                s.replace(s.length() - 1, s.length(), "");
            }
        }
    }
}
