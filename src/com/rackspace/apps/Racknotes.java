package com.rackspace.apps;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Racknotes extends ListActivity
{
    String email = "";
    String password = "";
    Vector notesVector = null;
    ArrayList<NoteForListView> boundNotes = new ArrayList<NoteForListView>();
    ArrayAdapter<NoteForListView> listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getApplicationContext();
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(context);
        email = prefs.getString("email", "email-missing");
        password = prefs.getString("password", "password-missing");

        //List<String> notes = new ArrayList<String>();
        try {
            for (JSONObject note: getNotes()) {
                String subject = note.getString("subject");
                String content = note.getString("content");
                boundNotes.add(new NoteForListView(subject, content));
            }
        } catch (Exception e) {
            Log.d("Racknotes", e.toString());
        }

        //int layout = android.R.layout.simple_list_item_1;
        int layout = R.layout.list_item;
        listAdapter = new ArrayAdapter<NoteForListView>(    
            this, layout, boundNotes);
        setListAdapter(listAdapter);

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                int position, long id) {
                String text = ((TextView) view).getText().toString();
                text = boundNotes.get(position).content;
                Toast.makeText(getApplicationContext(), text,Toast.LENGTH_SHORT)
                .show();
                //boundNotes.add(new NoteForListView("aaa", "bbb"));
                //listAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.settings:
            Intent intent = new Intent(getApplicationContext(), Foo.class);
            //startActivity[ForResult](i);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private List<JSONObject> getNotes() throws org.json.JSONException {
        List<JSONObject> notes = new ArrayList<JSONObject>();
        String json = requestJSON(
            "http://apps.rackspace.com/api/0.9.0/" + email
            + "/notes");
        Log.d("Racknotes", json);
        JSONObject jsonObject = (JSONObject)
            new JSONTokener(json).nextValue();
        JSONArray jsonArray = jsonObject.getJSONArray("notes");
        ArrayList<String> uris = new ArrayList<String>();
        Log.d("Racknotes", jsonArray.length() + "");
        for (int i = 0; i < jsonArray.length(); i++) {
            String uri =
                jsonArray.getJSONObject(i).getString("uri");
            uris.add(uri);
            json = requestJSON(uri);
            jsonObject = (JSONObject)
                new JSONTokener(json).nextValue();
            jsonObject = jsonObject.getJSONObject("note");
            notes.add(jsonObject);
            String subject = jsonObject.getString("subject");
            Log.d("Racknotes", subject);
        }
        return notes;
    }

    private String requestJSON(String uri) {
        HttpGet request = new HttpGet(uri);
        request.addHeader("Accept", "application/json");
        DefaultHttpClient client = makeHttpClient();
        StringBuffer sb = new StringBuffer();
        try {
            HttpResponse res = client.execute(request);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(res.getEntity().getContent()), 10000);
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            Log.d("Racknotes", e.toString());
        }
        return sb.toString();
    }

    private DefaultHttpClient makeHttpClient() {
        Log.d("Racknotes", "makeHttpClient email: " + email);
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(
            email, password);
        AuthScope authScope = new AuthScope(
            "apps.rackspace.com", 80, "webmail");
        BasicCredentialsProvider credProvider = new BasicCredentialsProvider();
        credProvider.setCredentials(authScope, creds);
        DefaultHttpClient client = new DefaultHttpClient();
        client.setCredentialsProvider(credProvider);
        return client;
    }

    class NoteForListView {
        String subject;
        String content;
        public NoteForListView(String subject, String content) {
            this.subject = subject;
            this.content = content;
        }
        public String toString() {
            return subject;
        }
    }
}

// vim:fdm=indent:fdn=2:
