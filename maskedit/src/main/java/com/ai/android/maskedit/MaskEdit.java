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
            } finally {
                attrs.recycle();
            }
        }

        addTextChangedListener(new DelimiterInjector(delimiter, groupLength));
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

        private boolean isModificationInProgress = false;
        private int backspacePressedBeforeDelimiterAt;

        private char delimiter;
        private int groupLength;

        DelimiterInjector(char delimiter, int groupLength) {
            if (groupLength < 0)
                throw new InvalidParameterException("Group length must be >= 0");

            if (delimiter <= ' ')
                throw new InvalidParameterException("Delimiter must be alphabetic symbol");

            this.delimiter = delimiter;
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

            for (int i = s.length() - 1; i > 0; i--) {
                if (groupLength > 0 && i % groupLength == 0) {
                    s.insert(i, String.valueOf(delimiter));
                }
            }
        }
    }
}
