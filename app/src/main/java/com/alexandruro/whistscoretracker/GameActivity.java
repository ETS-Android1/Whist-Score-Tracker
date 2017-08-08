package com.alexandruro.whistscoretracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Main game activity, where the game table is displayed
 */
public class GameActivity extends AppCompatActivity {

    static final int RESULT_REQUEST = 1;
    static final int BET_REQUEST = 2;

    private ArrayList<String> names;
    private boolean betsPlaced;
    private int roundCount;
    private ArrayList<PlayerRecord> scoreTable;
    private int nrOfPlayers;
    private boolean gameType1;
    private boolean gameOver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle("Game Table");

        initialiseGameVariables();

        Intent intent = getIntent();
        names = intent.getStringArrayListExtra("names");
        nrOfPlayers = names.size();
        gameType1 = intent.getBooleanExtra("gameType1", true);
        int prize = intent.getIntExtra("prize", 0);
        initRoundInfo();

        TableRow header = (TableRow) findViewById(R.id.header);
        for(int i=0; i<names.size(); i++) {
            scoreTable.add(new PlayerRecord(names.get(i), prize));
            LayoutInflater.from(this).inflate(R.layout.divider, header, true);
            LayoutInflater.from(this).inflate(R.layout.name_header_item, header, true);
            ((TextView)header.getChildAt(getViewIndexOfName(i))).setText(names.get(i));
        }


        View bottomSheet = findViewById(R.id.bottom_sheet);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if(!bottomSheetBehavior.isHideable())
                    findViewById(R.id.floatingActionButton).animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset).setDuration(0).start();
            }
        });

    }

    /**
     * Initialises the game variables like the round and the table
     */
    private void initialiseGameVariables() {
        gameOver = false;
        roundCount = 1;
        betsPlaced = false;
        scoreTable = new ArrayList<>();
    }

    /**
     * Resets the game to the beginning
     */
    private void restartGame() {
        initialiseGameVariables();
        ((TableLayout) findViewById(R.id.tableBody)).removeAllViews();
        int prize = getIntent().getIntExtra("prize", 0);
        for(int i=0; i<names.size(); i++)
            scoreTable.add(new PlayerRecord(names.get(i), prize));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_undo:
                if(roundCount==0) {
                    Toast.makeText(this, "No rounds were played to undo!", Toast.LENGTH_SHORT).show();
                    return true;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Undo last input?");
                builder.setPositiveButton("Undo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TableLayout body = (TableLayout) findViewById(R.id.tableBody);
                        TableRow lastRow = (TableRow) body.getChildAt(body.getChildCount()-1);
                        if(betsPlaced) {
                            for(int i=0; i<names.size(); i++) {
                                scoreTable.get(i).undoBet();
                                ((TextView) lastRow.getChildAt(getViewIndexOfBet(i))).setText("");
                            }
                            roundCount--;
                            initRoundInfo();
                            body.removeView(lastRow);
                        }
                        else {
                            for(int i=0; i<names.size(); i++) {
                                scoreTable.get(i).undoResult();
                                ((TextView) lastRow.getChildAt(getViewIndexOfScore(i))).setText("");
                                TextView bet = (TextView) lastRow.getChildAt(getViewIndexOfBet(i));
                                bet.setTextColor(ContextCompat.getColor(GameActivity.this, android.R.color.primary_text_light));
                                bet.setPaintFlags(bet.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                            }
                            if(roundCount>=3*nrOfPlayers+12) { // if game was ended, undo that
                                findViewById(R.id.floatingActionButton).setVisibility(View.VISIBLE);
                                View bottomSheet = findViewById(R.id.bottom_sheet);
                                BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
                                bottomSheetBehavior.setHideable(false);
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                                initRoundInfo();
                            }
                        }
                        betsPlaced = !betsPlaced;
                        Snackbar.make(findViewById(R.id.game_coord_layout), "Results undone", Snackbar.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancel", null);
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;

            case R.id.action_restart:
                if(roundCount==0) {
                    Toast.makeText(this, "No rounds were played yet!", Toast.LENGTH_SHORT).show();
                    return true;
                }
                builder = new AlertDialog.Builder(this);
                builder.setTitle("Restart game?");
                if(!gameOver)
                    builder.setMessage("The game is not over, so the current scores will be discarded.");
                builder.setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        restartGame();
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
                return true;

            case R.id.action_settings:
                Toast.makeText(getApplicationContext(), "Not implemented yet \uD83D\uDE1E", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_quit:
                builder = new AlertDialog.Builder(this);
                builder.setMessage("Quit to main menu?");
                builder.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BET_REQUEST) {
            if (resultCode == RESULT_OK) {
                addBets(data.getIntArrayExtra("inputs"));
            }
        }
        else if(requestCode == RESULT_REQUEST) {
            if (resultCode == RESULT_OK) {
                addResults(data.getIntArrayExtra("inputs"));
            }
        }
    }

    /**
     * Method called by pressing the add button. Redirects the user to the AddToGameTableActivity
     * @param view The view that calls the method
     */
    public void addScore(View view) {

        int nrOfHands;
        int firstPlayerDelay;
        if(betsPlaced) {
            nrOfHands = getNrOfHands();
            firstPlayerDelay = (roundCount-1) % nrOfPlayers;
        }
        else {
            nrOfHands = getNrOfHands(roundCount + 1);
            firstPlayerDelay = roundCount % nrOfPlayers;
        }

        int requestCode;
        if (betsPlaced) {
            requestCode = RESULT_REQUEST;
        }
        else requestCode = BET_REQUEST;

        Intent intent = new Intent(this, AddToGameTableActivity.class);
        intent.putExtra("betsPlaced", betsPlaced);
        intent.putExtra("nrOfHands", nrOfHands);
        intent.putStringArrayListExtra("names", names);
        intent.putExtra("requestCode", requestCode);
        intent.putExtra("firstPlayerDelay", firstPlayerDelay);

        startActivityForResult(intent, requestCode);
    }

    /**
     * Adds the bets of the current round to the table
     * @param bets The bets of the players
     */
    private void addBets(int[] bets) {
        TableLayout body = (TableLayout) findViewById(R.id.tableBody);
        TableRow newRow = new TableRow(this);
        LayoutInflater.from(this).inflate(R.layout.score_number, newRow, true);
        roundCount++;
        ((TextView)newRow.getChildAt(0)).setText(String.valueOf(getNrOfHands()));

        for(int i=0; i<names.size(); i++) {
            scoreTable.get(i).addBet(bets[i]);
            LayoutInflater.from(this).inflate(R.layout.divider, newRow, true);
            LayoutInflater.from(this).inflate(R.layout.score_item_short, newRow, true);
            LayoutInflater.from(this).inflate(R.layout.score_item_long, newRow, true);
            ((TextView)newRow.getChildAt(getViewIndexOfBet(i))).setText(String.valueOf(bets[i]));
            ((TextView)newRow.getChildAt(getViewIndexOfScore(i))).setText("");
        }

        body.addView(newRow);

        betsPlaced = true;

    }

    private int getViewIndexOfName(int playerIndex) {
        return 2*playerIndex+2;
    }

    private int getViewIndexOfBet(int playerIndex) {
        return 3*playerIndex+2;
    }

    private int getViewIndexOfScore(int playerIndex) {
        return 3*playerIndex+3;
    }

    /**
     * Adds the results of the current round to the table
     * @param results The results of the players
     */
    private void addResults(int[] results) {
        TableLayout body = (TableLayout) findViewById(R.id.tableBody);
        TableRow lastRow = (TableRow) body.getChildAt(body.getChildCount()-1);

        for(int i=0; i<names.size(); i++) {
            scoreTable.get(i).addResult(results[i]);
            ((TextView) lastRow.getChildAt(getViewIndexOfScore(i))).setText(String.valueOf(scoreTable.get(i).getScore()));
            if(scoreTable.get(i).lastResult())
                ((TextView) lastRow.getChildAt(getViewIndexOfBet(i))).setTextColor(ContextCompat.getColor(this, R.color.colorPositiveResult));
            else {
                TextView textView = (TextView) lastRow.getChildAt(3 * i + 2);
                textView.setTextColor(ContextCompat.getColor(this, R.color.colorNegativeResult));
                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
        betsPlaced = false;

        if(roundCount>=3*nrOfPlayers+12)
            endGame();
        else
            initRoundInfo();
    }

    private void initRoundInfo() {
        int nrOfHands = getNrOfHands(roundCount+1);
        String currentRoundText = "Current round: " + getResources().getQuantityString(R.plurals.numberOfHands, nrOfHands, nrOfHands);
        TextView currentRound = (TextView) findViewById(R.id.currentRound);
        currentRound.setText(currentRoundText);
        ((TextView)findViewById(R.id.firstPlayer)).setText("First player: " + names.get((roundCount-1) % nrOfPlayers));
        ((TextView)findViewById(R.id.dealer)).setText("Dealer: " + names.get((roundCount+nrOfPlayers-2) % nrOfPlayers));
    }

    /**
     * Ends the current game
     */
    private void endGame() {
        gameOver = true;

        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        findViewById(R.id.floatingActionButton).setVisibility(View.INVISIBLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game over!");

        Collections.sort(scoreTable);

        ListView playerScores = new ListView(this);

        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (16*scale + 0.5f);
        playerScores.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);

        EndPlayerListAdapter adapter = new EndPlayerListAdapter(this, scoreTable);
        playerScores.setAdapter(adapter);

        builder.setView(playerScores);
        builder.setPositiveButton("Return to menu", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNegativeButton("Dismiss", null);
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /**
     * Computes the number of hands in a certain round
     * @param roundCount The round number
     * @return The number of hands
     */
    private int getNrOfHands(int roundCount) {
        if(roundCount>3*nrOfPlayers+12)
            return -1;
        if(gameType1)
            if(roundCount<=nrOfPlayers)
                return 1;
            else if(roundCount<=nrOfPlayers+6)
                return roundCount-nrOfPlayers+1;
            else if(roundCount<=2*nrOfPlayers+6)
                return 8;
            else if(roundCount<=2*nrOfPlayers+12)
                return 2*nrOfPlayers+14-roundCount;
            else return 1;
        else
            if(roundCount<=nrOfPlayers)
                return 8;
            else if(roundCount<=nrOfPlayers+6)
                return nrOfPlayers+8-roundCount;
            else if(roundCount<=2*nrOfPlayers+6)
                return 1;
            else if(roundCount<=2*nrOfPlayers+12)
                return roundCount-2*nrOfPlayers-5;
            else return 8;
    }

    /**
     * Computes the number of hands in the current round
     * @return The number of hands
     */
    private int getNrOfHands() {
        return getNrOfHands(roundCount);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Discard game?");
        builder.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GameActivity.super.onBackPressed();
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
