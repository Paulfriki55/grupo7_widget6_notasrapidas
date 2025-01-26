package com.example.quicknoteswidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EditNoteActivity extends AppCompatActivity {
    private int widgetId;
    private EditText editText;
    private FloatingActionButton fabAddTodo;
    private FloatingActionButton fabSave;
    private RecyclerView recyclerTodo;
    private TodoAdapter todoAdapter;
    private List<TodoItem> todoItems;
    private Handler autoSaveHandler;
    private static final long AUTO_SAVE_DELAY = 1000; // 1 second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if(widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "Error al cargar el widget", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initUI();
        loadNoteContent();
        loadTodoItems();
        setupAutoSave();
        setupAddTodoButton();
        setupSaveButton();
    }

    private void initUI() {
        editText = findViewById(R.id.edit_note);
        fabAddTodo = findViewById(R.id.fab_add_todo);
        fabSave = findViewById(R.id.fab_save);
        recyclerTodo = findViewById(R.id.recycler_todo);

        recyclerTodo.setLayoutManager(new LinearLayoutManager(this));
        todoItems = new ArrayList<>();
        todoAdapter = new TodoAdapter(todoItems);
        recyclerTodo.setAdapter(todoAdapter);
    }

    private void loadNoteContent() {
        SharedPreferences prefs = getSharedPreferences("NOTES", MODE_PRIVATE);
        String noteContent = prefs.getString("note_" + widgetId, "");
        editText.setText(noteContent);
    }

    private void loadTodoItems() {
        SharedPreferences prefs = getSharedPreferences("TODOS", MODE_PRIVATE);
        String json = prefs.getString("todos_" + widgetId, "");
        Gson gson = new Gson();
        Type type = new TypeToken<List<TodoItem>>(){}.getType();
        todoItems = gson.fromJson(json, type);
        if (todoItems == null) {
            todoItems = new ArrayList<>();
        }
        todoAdapter.updateItems(todoItems);
    }

    private void setupAutoSave() {
        autoSaveHandler = new Handler();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                autoSaveHandler.removeCallbacks(autoSaveRunnable);
                autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_DELAY);
            }
        });
    }

    private Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            saveNoteContent(false);
        }
    };

    private void setupAddTodoButton() {
        fabAddTodo.setOnClickListener(v -> {
            TodoItem newItem = new TodoItem("Nueva tarea");
            todoItems.add(newItem);
            todoAdapter.notifyItemInserted(todoItems.size() - 1);
            saveTodoItems();
        });
    }

    private void setupSaveButton() {
        fabSave.setOnClickListener(v -> {
            saveNoteContent(true);
            removeCompletedTodos();
            Toast.makeText(EditNoteActivity.this, "Nota guardada y tareas completadas eliminadas", Toast.LENGTH_SHORT).show();
            finish(); // Cerrar la actividad después de guardar
        });
    }

    private void saveNoteContent(boolean showToast) {
        String noteContent = editText.getText().toString().trim();
        SharedPreferences.Editor editor = getSharedPreferences("NOTES", MODE_PRIVATE).edit();
        editor.putString("note_" + widgetId, noteContent);
        editor.putLong("timestamp_" + widgetId, System.currentTimeMillis());
        editor.apply();

        updateWidget();
        if (showToast) {
            Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeCompletedTodos() {
        List<TodoItem> completedItems = new ArrayList<>();
        for (TodoItem item : todoItems) {
            if (item.isCompleted()) {
                completedItems.add(item);
            }
        }
        todoItems.removeAll(completedItems);
        todoAdapter.notifyDataSetChanged();
        saveTodoItems();
    }

    private void saveTodoItems() {
        SharedPreferences.Editor editor = getSharedPreferences("TODOS", MODE_PRIVATE).edit();
        Gson gson = new Gson();
        String json = gson.toJson(todoItems);
        editor.putString("todos_" + widgetId, json);
        editor.apply();
        updateWidget();
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        QuickNoteWidget.updateWidget(this, appWidgetManager, widgetId);
    }

    @Override
    public void onBackPressed() {
        // No guardamos automáticamente al presionar el botón de retroceso
        super.onBackPressed();
    }

    private class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {
        private List<TodoItem> items;

        TodoAdapter(List<TodoItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false);
            return new TodoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
            TodoItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        void updateItems(List<TodoItem> newItems) {
            items = newItems;
            notifyDataSetChanged();
        }

        class TodoViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            EditText editText;

            TodoViewHolder(@NonNull View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.checkbox_todo);
                editText = itemView.findViewById(R.id.edit_todo);
            }

            void bind(TodoItem item) {
                checkBox.setChecked(item.isCompleted());
                editText.setText(item.getText());

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.setCompleted(isChecked);
                    saveTodoItems();
                });

                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        item.setText(s.toString());
                        saveTodoItems();
                    }
                });
            }
        }
    }

    private static class TodoItem {
        private String text;
        private boolean completed;

        TodoItem(String text) {
            this.text = text;
            this.completed = false;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }
}

