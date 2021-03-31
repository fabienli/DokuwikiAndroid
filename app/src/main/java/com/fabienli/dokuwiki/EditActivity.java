package com.fabienli.dokuwiki;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class EditActivity extends AppCompatActivity {
    static String TAG="EditActivity";
    private String _pagename;
    EditText _EditTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        _EditTextView = (EditText) findViewById(R.id.edit_text);
        _pagename = getIntent().getStringExtra("pagename");
        Log.d(TAG, "edit page: "+ _pagename);
        _EditTextView.setText("");

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePage();
            }
        });

        setTitle("Edit: "+_pagename);
        resetPage(false);

        _EditTextView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                if(count==1 && cs.charAt(start)=='\n') {
                    // get the previous line
                    String[] lines = cs.subSequence(0, start).toString().split("\n");
                    if(lines.length > 0){
                        //Log.d("keycode","previous line:"+lines[lines.length-1]);
                        String previousLine = lines[lines.length-1];
                        // check if the line starts with a list char:
                        if (previousLine.startsWith("  * ") && previousLine.length()>4) {
                            int index = _EditTextView.getSelectionStart();
                            Editable editable = _EditTextView.getText();
                            editable.insert(index, "  * ");
                        }
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

            @Override
            public void afterTextChanged(Editable arg0) { }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            savePage();
            return true;
        }
        else if (id == R.id.action_reset) {
            resetPage(false);
            return true;
        }
        else if (id == R.id.action_force_reset) {
            resetPage(true);
            return true;
        }
        else if (id == R.id.action_cancel) {
            cancelEdit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void savePage()
    {
        String new_text = _EditTextView.getText().toString();
        WikiCacheUiOrchestrator.instance().updateTextPage(_pagename, new_text);
        Snackbar.make(this.findViewById(android.R.id.content),
                "Saved !", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        onBackPressed();
    }

    void resetPage(Boolean force)
    {
        WikiCacheUiOrchestrator.instance().retrievePageEdit(_pagename, _EditTextView, force);
    }

    void cancelEdit()
    {
        onBackPressed();
    }
}
