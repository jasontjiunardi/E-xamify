package com.example.e_xamify;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class QuizInterface extends AppCompatActivity {

    private Button createMCQButton;
    private EditText titleInput;
    private EditText durationInput;
    private EditText instructionInput;
    private Spinner attemptsSpinner;
    private Spinner quizTypeSpinner;
    private Spinner moduleSpinner; // New module spinner
    private Switch navigableSwitch;
    private Switch tabRestrictSwitch;
    private Switch randomizeSwitch;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private int user_id = 1;
    private int quiz_id = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_interface);

        titleInput = findViewById(R.id.titleInput);
        durationInput = findViewById(R.id.durationInput);
        instructionInput = findViewById(R.id.instructionInput);
        attemptsSpinner = findViewById(R.id.attemptsSpinner);
        quizTypeSpinner = findViewById(R.id.quizTypeSpinner);
        moduleSpinner = findViewById(R.id.moduleSpinner); // Initialize module spinner
        navigableSwitch = findViewById(R.id.navigableSwitch);
        tabRestrictSwitch = findViewById(R.id.tabRestrictSwitch);
        randomizeSwitch = findViewById(R.id.randomizeSwitch);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        // Populate spinners
        populateQuizTypeSpinner();
        setupAttemptsSpinner();
        populateModuleSpinner();  // Populate module spinner

        // "Create MCQ" Button
        createMCQButton = findViewById(R.id.createMCQButton);
        createMCQButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createMCQ();
            }
        });
    }

    private void populateQuizTypeSpinner() {
        Cursor cursor = db.rawQuery("SELECT type_name FROM quiz_type", null);
        ArrayList<String> quizTypes = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                quizTypes.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, quizTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        quizTypeSpinner.setAdapter(adapter);
    }

    private void setupAttemptsSpinner() {
        ArrayAdapter<String> attemptsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"1", "2", "3", "Infinite"});
        attemptsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        attemptsSpinner.setAdapter(attemptsAdapter);
    }

    private void populateModuleSpinner() {
        // Retrieve module names from the database
        Cursor cursor = db.rawQuery("SELECT module_name FROM module", null);
        ArrayList<String> modules = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                modules.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Set up the Spinner with module data
        ArrayAdapter<String> moduleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modules);
        moduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moduleSpinner.setAdapter(moduleAdapter);
    }

    private void createMCQ() {
        String quizTitle = titleInput.getText().toString();
        String durationStr = durationInput.getText().toString();
        String instructions = instructionInput.getText().toString();
        String attemptsStr = attemptsSpinner.getSelectedItem().toString();

        if (quizTitle.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int quizDuration = Integer.parseInt(durationStr);
        int quizAttempts = attemptsStr.equals("Infinite") ? -1 : Integer.parseInt(attemptsStr);
        int quizNavigable = navigableSwitch.isChecked() ? 1 : 0;
        int quizTabRestrictor = tabRestrictSwitch.isChecked() ? 1 : 0;
        int questionRandomize = randomizeSwitch.isChecked() ? 1 : 0;
        String type_name = quizTypeSpinner.getSelectedItem().toString();
        String moduleName = moduleSpinner.getSelectedItem().toString();

        // Get the selected quiz type ID from the database
        int quizTypeId = -1;
        try (Cursor cursorQuizType = db.rawQuery("SELECT quiz_type_id FROM quiz_type WHERE type_name = ?", new String[]{type_name})) {
            if (cursorQuizType.moveToFirst()) {
                quizTypeId = cursorQuizType.getInt(0);
            }
        }
        if (quizTypeId == -1) {
            Toast.makeText(this, "Invalid quiz type selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the selected module ID from the database
        int moduleId = -1;
        try (Cursor cursorModule = db.rawQuery("SELECT module_id FROM module WHERE module_name = ?", new String[]{moduleName})) {
            if (cursorModule.moveToFirst()) {
                moduleId = cursorModule.getInt(0);
            }
        }
        if (moduleId == -1) {
            Toast.makeText(this, "Invalid module selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user exists (important if user_id is a foreign key)
        Cursor cursorUser = db.rawQuery("SELECT user_id FROM user WHERE user_id = ?", new String[]{String.valueOf(user_id)});
        if (!cursorUser.moveToFirst()) {
            Toast.makeText(this, "User ID does not exist", Toast.LENGTH_SHORT).show();
            cursorUser.close();
            return;
        }
        cursorUser.close();

        // Insert quiz details into the database
        ContentValues quizValues = new ContentValues();
        quizValues.put("quiz_title", quizTitle);
        quizValues.put("quiz_duration", quizDuration);
        quizValues.put("instructions", instructions);
        quizValues.put("quiz_attempts", quizAttempts);
        quizValues.put("quiz_navigable", quizNavigable);
        quizValues.put("quiz_tab_restrictor", quizTabRestrictor);
        quizValues.put("question_randomize", questionRandomize);
        quizValues.put("quiz_type_id", quizTypeId);
        quizValues.put("module_id", moduleId);
        quizValues.put("user_id", user_id);
        quizValues.put("quiz_id", quiz_id);

        db.insert("quiz", null, quizValues);
        Cursor cursor = db.rawQuery("SELECT quiz_id FROM quiz WHERE quiz_id = ?", new String[]{String.valueOf(quiz_id)});
        if (cursor.getCount() != 0) {
            Log.e("DatabaseHelper", "quiz_id " + quiz_id + " does exist in quiz table");
        }
        Toast.makeText(this, "Quiz Created Successfully with ID: " + quiz_id, Toast.LENGTH_SHORT).show();
        // Pass the quiz ID and title to MCQEditorActivity
        Intent intent = new Intent(QuizInterface.this, MCQEditorActivity.class);
        intent.putExtra("quiz_id", quiz_id);
        intent.putExtra("quiz_title", quizTitle);
        intent.putExtra("quiz_type_id", quizTypeId);
        startActivity(intent);
        quiz_id++;
    }

}
