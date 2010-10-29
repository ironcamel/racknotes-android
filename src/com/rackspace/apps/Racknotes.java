package com.rackspace.apps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
    ArrayAdapter<NoteForListView> listAdapter;
    NotesDB notesDB;
    ArrayList<NoteForListView> boundNotes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int layout = android.R.layout.simple_list_item_1;
        //int layout = R.layout.list_item;

        notesDB = new NotesDB(getApplicationContext());
        boundNotes = notesDB.getNotes();
        listAdapter = new ArrayAdapter<NoteForListView>(    
            this, layout, boundNotes);
        setListAdapter(listAdapter);

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                int position, long id) {
                //String text = ((TextView) view).getText().toString();
                String content  = boundNotes.get(position).content;
                //Toast.makeText(getApplicationContext(),
                //  text,Toast.LENGTH_SHORT).show();
                showAlert(content);
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
            Intent intent =
                new Intent(getApplicationContext(), SettingsPage.class);
            //startActivity[ForResult](i);
            startActivity(intent);
            return true;
        case R.id.refresh:
            refreshNotes();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showAlert(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(
                "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private List<JSONObject> getNotesFromNet() throws org.json.JSONException {
        Context context = getApplicationContext();
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(context);
        String email = prefs.getString("email", "email-missing").trim();
        String password = prefs.getString("password","passwd-missing").trim();
        DefaultHttpClient client = makeHttpClient(email, password);
        List<JSONObject> notes = new ArrayList<JSONObject>();
        String json = requestJSON(client,
            "http://apps.rackspace.com/api/0.9.0/" + email
            + "/notes");
        Log.d("Racknotes json:", json);
        JSONObject jsonObject = (JSONObject)
            new JSONTokener(json).nextValue();
        JSONArray jsonArray = jsonObject.getJSONArray("notes");
        ArrayList<String> uris = new ArrayList<String>();
        Log.d("Racknotes", jsonArray.length() + "");
        for (int i = 0; i < jsonArray.length(); i++) {
            String uri =
                jsonArray.getJSONObject(i).getString("uri");
            uris.add(uri);
            json = requestJSON(client, uri);
            jsonObject = (JSONObject)
                new JSONTokener(json).nextValue();
            jsonObject = jsonObject.getJSONObject("note");
            notes.add(jsonObject);
            String subject = jsonObject.getString("subject");
            Log.d("Racknotes", "subject: " + subject);
        }
        return notes;
    }

    private String requestJSON(DefaultHttpClient client, String uri) {
        HttpGet request = new HttpGet(uri);
        request.addHeader("Accept", "application/json");
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
            Log.d("Racknotes", "requestJSON: " + e.toString());
        }
        return sb.toString();
    }

    private DefaultHttpClient makeHttpClient(String email, String password) {
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

    private void refreshNotes() {
        boundNotes.clear();
        ProgressDialog progress = ProgressDialog.show(Racknotes.this, "", 
            "Loading. Please wait...", true);
        NotesRefresher refresher = new NotesRefresher(this, progress);
        Thread thread = new Thread(refresher);
        Log.d("Racknotes", "refreshNotes starting thread");
        thread.start();
    }

    class NotesRefresher implements Runnable {
        Activity act;
        ProgressDialog progress;
        public NotesRefresher(Activity act, ProgressDialog progress) {
            this.act = act;
            this.progress = progress;
        }
        public void run() {
            try {
                for (JSONObject note: getNotesFromNet()) {
                    int id = note.getInt("id");
                    String subject = note.getString("subject");
                    String content = note.getString("content");
                    boundNotes.add(new NoteForListView(id, subject, content));
                }
            } catch (Exception e) {
                Log.d("Racknotes", "NotesRefresher: " + e.toString());
            } finally {
                act.runOnUiThread(new Runnable() {
                    public void run() {
                        progress.dismiss();
                        listAdapter.notifyDataSetChanged();
                    }
                });
            }
            NotesDB db = new NotesDB(getApplicationContext());
            db.resetNotes(boundNotes);
        }
    }

    class NoteForListView {
        int id;
        String subject;
        String content;
        public NoteForListView(int id, String subject, String content) {
            this.id = id;
            this.subject = subject;
            this.content = content;
        }
        public String toString() {
            return subject;
            //return id + "";
        }
    }

    public class NotesDB extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        NotesDB(Context context) {
            super(context, "notes", null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql =
                "CREATE TABLE notes ("
                + " id INT PRIMARY KEY,"
                + " subject TEXT,"
                + " content TEXT,"
                + " last_modified TEXT,"
                + " created TEXT"
                + ");";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(
                SQLiteDatabase db, int oldVersion, int newVersion) {}

        public ArrayList getNotes() {
            SQLiteDatabase db = getReadableDatabase();
            String sql = "SELECT id, subject, content FROM notes";
            Cursor c = db.rawQuery(sql, new String[0]);
            ArrayList<NoteForListView> notes = new ArrayList<NoteForListView>();
            while (c.moveToNext()) {
                int id = c.getInt(0);
                String subject = c.getString(1);
                String content = c.getString(2);
                NoteForListView note =
                    new NoteForListView(id, subject, content);
                notes.add(note);
                Log.d("Racknotes", "NotesDB.getNotes: " + note.toString());
            }
            c.close();
            db.close();
            return notes;
        }

        public void resetNotes(List<NoteForListView> notes)
                throws SQLException {
            SQLiteDatabase db = getWritableDatabase();
            String sql = "DELETE FROM notes;";
            db.execSQL(sql);
            sql = "INSERT INTO notes (id, subject, content) values (?,?,?);";
            for (NoteForListView note : notes) {
                db.execSQL(sql,
                    new Object[]{note.id, note.subject, note.content});
            }
            db.close();
        }
    }
}

// vim:fdm=indent:fdn=2:
