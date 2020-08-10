package com.example.howltalk.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.howltalk.R;
import com.example.howltalk.model.ChatModel;
import com.example.howltalk.model.NotificationModel;
import com.example.howltalk.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {
    private FirebaseRemoteConfig firebaseRemoteConfig;

    private String destinationUid;
    private Button button;
    private EditText edittext;

    private String uid;
    private String chatRoomUid;

    private RecyclerView recyclerView;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private UserModel destinationuserModel;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        /* 원격으로 상태바 바꿈*/
        firebaseRemoteConfig = firebaseRemoteConfig.getInstance();

        String splash_background = firebaseRemoteConfig.getString(getString(R.string.rc_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(splash_background));
        }
        /* -- */

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); //채팅을 요구하는 아이디 즉 로그인한 아이디
        destinationUid = getIntent().getStringExtra("destinationUid"); //채팅을 당하는 아이디
        button = findViewById(R.id.messageActivity_button);
        edittext = findViewById(R.id.messageActivity_editText);

        recyclerView = findViewById(R.id.messageActivity_recyclerview);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatModel chatModel = new ChatModel();
                chatModel.users.put(uid,true);
                chatModel.users.put(destinationUid,true);


                if(chatRoomUid == null) {
                    button.setEnabled(false);
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkChatRoom();
                        }
                    });
                } else {

                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = edittext.getText().toString();
                    comment.timestamp = ServerValue.TIMESTAMP;
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            sendFcm();
                            edittext.setText("");
                        }
                    });
                }
            }
        });
        checkChatRoom();
    }

    void sendFcm() {

        Gson gson = new Gson();

        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();

        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = destinationuserModel.pushToken;
        notificationModel.notification.title = userName;
        notificationModel.notification.text = edittext.getText().toString();
        notificationModel.data.title = userName;
        notificationModel.data.text = edittext.getText().toString();

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf8"),gson.toJson(notificationModel));

        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .addHeader("Authorization", "key=AAAA-yJ1V5U:APA91bFjYxEXYWVHFXY8sDL20GsEqOs8l-M2AQxBDrJgJWZjBkTIhtTi46O51neSSy92uFuWkxJ75YFGLQlOvw4T8lWnH-svP1Hg7HL8epDEOdOUyZAwEg3MOxqyj0yW9KQDyXR7rETn")
                .url("https://fcm.googleapis.com/fcm/send")
                .post(requestBody)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

            }
        });

    }

    void checkChatRoom() {
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot item : dataSnapshot.getChildren()) {
                    ChatModel chatModel = item.getValue(ChatModel.class);
                    if(chatModel.users.containsKey(destinationUid)) {
                        chatRoomUid = item.getKey();
                        button.setEnabled(true);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
                        recyclerView.setAdapter(new RecyclerViewAdapter());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        List<ChatModel.Comment> comments;
        public RecyclerViewAdapter() {
            comments = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    destinationuserModel = dataSnapshot.getValue(UserModel.class);
                    getMessageList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        void getMessageList() {
            databaseReference = FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments");
           valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    comments.clear();
                    Map<String, Object> readUsersMap = new HashMap<>();
                    for(DataSnapshot item : dataSnapshot.getChildren()) {
                        String key = item.getKey();
                        ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);
                        comment.readUsers.put(uid, true);

                        readUsersMap.put(key,comment);
                        comments.add(item.getValue(ChatModel.Comment.class));
                    }

                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            notifyDataSetChanged();
                            recyclerView.scrollToPosition(comments.size()-1);
                        }
                    });
                    //메세지가 갱신

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message,parent,false);

            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MessageViewHolder messageViewHolder = ((MessageViewHolder)holder);

            //내가보낸 메시지
            if(comments.get(position).uid.equals(uid)) {
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);
                setReadCounter(position, messageViewHolder.textView_readCounter_left);
                //상대방이 보낸 메시지
            }else {
                Glide.with(holder.itemView.getContext())
                        .load(destinationuserModel.profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);
                messageViewHolder.textView_name.setText(destinationuserModel.userName);
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);
                setReadCounter(position, messageViewHolder.textView_readCounter_right);

            }

            long unixTime = (long)comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            messageViewHolder.textView_timestamp.setText(time);

        }
        void setReadCounter(final int position, final TextView textView) {
            FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Map<String, Boolean> users =(Map<String, Boolean>) dataSnapshot.getValue();

                    int count = users.size() - comments.get(position).readUsers.size();
                    if(count > 0) {
                        textView.setVisibility(View.VISIBLE);
                        textView.setText(String.valueOf(count));
                    }else {
                        textView.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class MessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textView_message;
            public TextView textView_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;
            public TextView textView_readCounter_left;
            public TextView textView_readCounter_right;

            public MessageViewHolder(View view) {
                super(view);
                textView_message = view.findViewById(R.id.messageItem_textView_message);
                textView_name = view.findViewById(R.id.messageItem_textview_name);
                imageView_profile = view.findViewById(R.id.messageItem_imageview_profile);
                linearLayout_destination = view.findViewById(R.id.messageItem_linearlayout_destination);
                linearLayout_main = view.findViewById(R.id.messageItem_linearlayout_main);
                textView_timestamp = view.findViewById(R.id.messageItem_textView_timestamp);
                textView_readCounter_left = view.findViewById(R.id.messageItem_textView_readCounter_left);
                textView_readCounter_right = view.findViewById(R.id.messageItem_textView_readCounter_right);
            }
        }
    }

    @Override
    public void onBackPressed() {
        databaseReference.removeEventListener(valueEventListener);
        finish();
        overridePendingTransition(R.anim.fromleft, R.anim.toright);
    }
}