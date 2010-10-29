package com.rackspace.apps;

import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.method.PasswordTransformationMethod;

public class SettingsPage extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        setPreferenceScreen(createPreferenceHierarchy());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root =
            getPreferenceManager().createPreferenceScreen(this);

        EditTextPreference editEmailPref = new EditTextPreference(this);
        editEmailPref.setDialogTitle("Email Address");
        editEmailPref.setKey("email");
        editEmailPref.setTitle("Email Address");
        root.addPreference(editEmailPref);

        EditTextPreference editPasswordPref = new EditTextPreference(this);
        editPasswordPref.setDialogTitle("Password");
        editPasswordPref.setKey("password");
        editPasswordPref.setTitle("Password");
        editPasswordPref.getEditText().setTransformationMethod(
            new PasswordTransformationMethod());
        root.addPreference(editPasswordPref);

        return root;
    }

}
