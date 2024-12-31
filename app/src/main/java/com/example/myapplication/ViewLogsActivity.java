package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Model.LogEntry;
import com.example.myapplication.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ViewLogsActivity extends AppCompatActivity {

    private Spinner actionTypeSpinner;
    private Button selectDateRangeButton;
    private SearchView userSearchView;
    private RecyclerView logsRecyclerView;
    private LogAdapter logAdapter;
    private List<LogEntry> logList = new ArrayList<>();
    private DatabaseReference logsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_logs);

        // Initialize views
        actionTypeSpinner = findViewById(R.id.actionTypeSpinner);
        selectDateRangeButton = findViewById(R.id.selectDateRangeButton);
        userSearchView = findViewById(R.id.userSearchView);
        logsRecyclerView = findViewById(R.id.logsRecyclerView);

        // Set RecyclerView
        logsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(logList);
        logsRecyclerView.setAdapter(logAdapter);

        // Firebase reference for logs
        logsRef = FirebaseDatabase.getInstance().getReference("inventory_logs");

        // Fetch logs from Firebase
        fetchLogs();

        // Set filter actions
        selectDateRangeButton.setOnClickListener(v -> {
            // Implement Date Range Picker (DatePickerDialog)
        });

        userSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterLogsByUser(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterLogsByUser(newText);
                return true;
            }
        });
    }

    private void fetchLogs() {
        logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                logList.clear(); // Clear existing logs

                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    LogEntry log = logSnapshot.getValue(LogEntry.class);
                    if (log != null) {
                        logList.add(log);
                    }
                }

                // Notify adapter of data changes
                logAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ViewLogsActivity.this, "Error fetching logs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterLogsByUser(String user) {
        List<LogEntry> filteredLogs = new ArrayList<>();
        for (LogEntry log : logList) {
            if (log.getUser().contains(user)) {
                filteredLogs.add(log);
            }
        }
        logAdapter = new LogAdapter(filteredLogs);
        logsRecyclerView.setAdapter(logAdapter);
    }
}
