package com.example.howltalk.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.howltalk.R;
import com.example.howltalk.model.ChatModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class MessageActivity extends AppCompatActivity {
    private FirebaseRemoteConfig firebaseRemoteConfig;

    private String destinationUid;
    private Button button;
    private EditText edittext;

    private String uid;
    private String chatRoomUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        firebaseRemoteConfig = firebaseRemoteConfig.getInstance();

        String splash_background = firebaseRemoteConfig.getString(getString(R.string.rc_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(splash_background));
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); //채팅을 요구하는 아이디
        destinationUid = getIntent().getStringExtra("destinationUid"); //채팅을 당하는 아이디
        button = findViewById(R.id.messageActivity_button);
        edittext = findViewById(R.id.messageActivity_editText);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatModel chatModel = new ChatModel();
                chatModel.users.put(uid,true);
                chatModel.users.put(destinationUid,true);


                if(chatRoomUid == null) {
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel);
                } else {

                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = edittext.getText().toString();
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").push().setValue(comment);
                }
            }
        });

        checkChatRoom();

    }

    void checkChatRoom() {
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot item : dataSnapshot.getChildren()) {
                    ChatModel chatModel = item.getValue(ChatModel.class);
                    if(chatModel.users.containsKey(destinationUid)) {
                        chatRoomUid = item.getKey();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}