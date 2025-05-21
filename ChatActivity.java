package com.example.aichatbot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText editText;
    private Button buttonSend;
    private MessageAdapter adapter;
    private List<Message> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerViewChat);
        editText = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        messages = new ArrayList<>();
        adapter = new MessageAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonSend.setOnClickListener(v -> {
            String input = editText.getText().toString().trim();
            if (!input.isEmpty()) {
                addMessage(input, true);
                getBotResponse(input);
                editText.setText("");
            }
        });
    }

    private void addMessage(String text, boolean isUser) {
        messages.add(new Message(text, isUser));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
    }

    private void getBotResponse(String userInput) {
        new Thread(() -> {
            try {
                String response = callOpenAI(userInput);
                runOnUiThread(() -> addMessage(response, false));
            } catch (Exception e) {
                runOnUiThread(() -> addMessage("Error: " + e.getMessage(), false));
            }
        }).start();
    }

    private String callOpenAI(String userInput) throws Exception {
        OkHttpClient client = new OkHttpClient();
        String apiKey = "YOUR_OPENAI_API_KEY"; // Replace with your actual API key

        JSONObject body = new JSONObject();
        body.put("model", "gpt-3.5-turbo");

        JSONArray messagesArray = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userInput);
        messagesArray.put(userMessage);

        body.put("messages", messagesArray);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        Response response = client.newCall(request).execute();
        String json = response.body().string();
        JSONObject responseObject = new JSONObject(json);

        return responseObject.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
