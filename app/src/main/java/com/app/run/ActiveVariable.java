package com.app.run;

public class ActiveVariable {
    private boolean variable = false;
    private ChangeListener listener;

    public boolean getValue() {
        return variable;
    }

    public void setValue(boolean boo) {
        this.variable = boo;
        if (listener != null) listener.onChange();
    }

    public ChangeListener getListener() {
        return listener;
    }

    public void setListener(ChangeListener listener) {
        this.listener = listener;
    }

    public interface ChangeListener {
        void onChange();
    }
}
