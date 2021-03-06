package tango.rajantechie.us.scorpiontango.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import tango.rajantechie.us.scorpiontango.R;

/**
 * Created by rajan on 4/23/2017.
 */

public class LogcatView extends ScrollView implements LogcatListener {

private int verboseColor, debugColor, errorColor, infoColor, warningColor, consoleColor;
private TextView textView;

public LogcatView(Context context) {
    super(context);
    init(null);
}

public LogcatView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
}

public LogcatView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs);
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public LogcatView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(attrs);
}

private void init(AttributeSet attrs) {

    textView = new TextView(getContext());
    textView.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
    textView.setPadding(20, 20, 20, 20);
    addView(textView);

    textView.setTextColor(getContext().getResources().getColor(R.color.defaultTextColor));

    if (attrs != null) {
        TypedArray a = getContext().getTheme()
                .obtainStyledAttributes(attrs, R.styleable.LogcatTextView, 0, 0);

        try {
            verboseColor = a.getColor(R.styleable.LogcatTextView_verboseColor, getContext().getResources()
                    .getColor(R.color.defaultVerboseColor));
            debugColor = a.getColor(R.styleable.LogcatTextView_debugColor, getContext().getResources()
                    .getColor(R.color.defaultDebugColor));
            errorColor = a.getColor(R.styleable.LogcatTextView_errorColor, getContext().getResources()
                    .getColor(R.color.defaultErrorColor));
            infoColor = a.getColor(R.styleable.LogcatTextView_infoColor, getContext().getResources()
                    .getColor(R.color.defaultInfoColor));
            warningColor = a.getColor(R.styleable.LogcatTextView_warningColor, getContext().getResources()
                    .getColor(R.color.defaultWarningColor));

            consoleColor = a.getColor(R.styleable.LogcatTextView_consoleColor, getContext().getResources()
                    .getColor(R.color.defaultConsoleColor));
        } finally {
            a.recycle();
        }
    } else {
        verboseColor = getContext().getResources().getColor(R.color.defaultVerboseColor);
        debugColor = getContext().getResources().getColor(R.color.defaultDebugColor);
        errorColor = getContext().getResources().getColor(R.color.defaultErrorColor);
        infoColor = getContext().getResources().getColor(R.color.defaultInfoColor);
        warningColor = getContext().getResources().getColor(R.color.defaultWarningColor);
        consoleColor = getContext().getResources().getColor(R.color.defaultConsoleColor);
    }

    setBackgroundColor(consoleColor);
    textView.setBackgroundColor(consoleColor);
    refreshLogcat();
}

@Override
protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);
    View view = getChildAt(getChildCount() - 1);
    int diff = (view.getBottom() - (getHeight() + getScrollY()));

    // if diff is zero, then the bottom has been reached
    if (diff == 0) {
        refreshLogcat();
    }
}

public void refreshLogcat() {
    getLogcat(this);
}

@Override
public void onLogcatCaptured(final String logcat) {
    post(new Runnable() {
        @Override
        public void run() {
            textView.setText(Html.fromHtml(logcat));
        }
    });
}

private void getLogcat(final LogcatListener listener) {

    new Thread() {
        @Override
        public void run() {
            try {
                String processId = Integer.toString(android.os.Process.myPid());
                String[] command = new String[]{
                        "logcat",
                        "-d",
                        "-v",
                        "threadtime"
                };
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process
                        .getInputStream()));

                StringBuilder log = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains(processId)) {
                        int lineColor = verboseColor;

                        if (line.contains(" I ")) {
                            lineColor = infoColor;
                        } else if (line.contains(" E ")) {
                            lineColor = errorColor;
                        } else if (line.contains(" D ")) {
                            lineColor = debugColor;
                        } else if (line.contains(" W ")) {
                            lineColor = warningColor;
                        }

                        log.append("<font color=\"#" + Integer.toHexString(lineColor)
                                .toUpperCase()
                                .substring(2) + "\">" + line + "</font><br><br>");
                    }
                }
                listener.onLogcatCaptured(log.toString());
            } catch (Exception e) {
                Log.e("onlogcatCaptured",e.getMessage());
            }
        }
    }.start();
}
}