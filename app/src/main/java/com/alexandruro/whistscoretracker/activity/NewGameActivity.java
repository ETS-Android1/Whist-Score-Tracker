package com.alexandruro.whistscoretracker.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import com.alexandruro.whistscoretracker.BuildConfig;
import com.alexandruro.whistscoretracker.config.DevelopmentFlags;
import com.alexandruro.whistscoretracker.adapter.EditListAdapter;
import com.alexandruro.whistscoretracker.view.EditListItem;
import com.alexandruro.whistscoretracker.R;
import com.alexandruro.whistscoretracker.view.WrappedListView;
import com.alexandruro.whistscoretracker.model.Game;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity used for configuring a new game
 */
public class NewGameActivity extends AppCompatActivity {

    private List<EditListItem> names;
    private EditListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if(ab!=null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(R.string.title_new_game);
        }

        names = new ArrayList<>();

        if(DevelopmentFlags.PREPOPULATE_PLAYER_NAMES) {
            names.add(new EditListItem("Alex"));
            names.add(new EditListItem("Ana"));
        }

        adapter = new EditListAdapter(this, names);

        WrappedListView listView = findViewById(R.id.nameListView);
        listView.setAdapter(adapter);

    }

    /**
     * Starts the game with the current settings
     * @param view The view that calls the method
     */
    public void startGame(View view){

        if(!BuildConfig.DEBUG && names.size()<4) {
            Toast.makeText(this, R.string.new_game_minimum, Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton gameTypeOneEightOne = findViewById(R.id.radioGameTypeOneEightOne);
        Game.Type type;
        if(gameTypeOneEightOne.isChecked())
            type = Game.Type.ONE_EIGHT_ONE;
        else
            type = Game.Type.EIGHT_ONE_EIGHT;

        RadioButton prize0 = findViewById(R.id.radioPrizeNone);
        RadioButton prize5 = findViewById(R.id.radioPrize5);
        int prize;
        if(prize0.isChecked())
            prize = 0;
        else if(prize5.isChecked())
            prize = 5;
        else prize= 10;

        adapter.notifyDataSetChanged();

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("type", type);
        intent.putExtra("prize", prize);
        ArrayList<String> stringNames = new ArrayList<>();
        for(EditListItem item: names) {
            stringNames.add(item.toString());
            if(!BuildConfig.DEBUG && item.toString().isEmpty()) {
                Toast.makeText(this, R.string.new_game_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        intent.putStringArrayListExtra("playerNames", stringNames);
        startActivity(intent);
        finish();
    }

    /**
     * Removes the last EditText element in the list of names
     * @param view The view that calls the method
     */
    public void removeLastName(View view){
        names.remove(names.size()-1);
        adapter.notifyDataSetChanged();
    }

    /**
     * Adds an empty EditText element in the list of names
     * @param view The view that calls the method
     */
    public void addName(View view) {
        adapter.add(new EditListItem());
    }

    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.discard_prompt);
        builder.setPositiveButton(R.string.discard, (dialog, which) -> NewGameActivity.super.onBackPressed());
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
