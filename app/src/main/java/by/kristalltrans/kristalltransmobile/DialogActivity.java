package by.kristalltrans.kristalltransmobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DialogActivity extends AppCompatActivity {

    private DatabaseReference mSimpleFirechatDatabaseReference;
    private FirebaseRecyclerAdapter<ChatMessage, FirechatMsgViewHolder>
            mFirebaseAdapter;
    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMsgEditText;
    private String mUsername;
    private String mUseremail;
    private String mPhotoUrl;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    public static class FirechatMsgViewHolder extends RecyclerView.ViewHolder {
        TextView userTextView;
        TextView emailUserTextView;
        TextView msgTextView;
        CircleImageView userImageView;

        public FirechatMsgViewHolder(View v) {
            super(v);
            userTextView = (TextView) itemView.findViewById(R.id.userTextView);
            emailUserTextView = (TextView) itemView.findViewById(R.id.emailUserTextView);
            msgTextView = (TextView) itemView.findViewById(R.id.msgTextView);
            userImageView = (CircleImageView) itemView.findViewById(R.id.userImageView);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    if (user.getDisplayName() != null)
                        mUsername = user.getDisplayName();
                    mUseremail = user.getEmail();
                    if (user.getPhotoUrl() != null)
                        mPhotoUrl = user.getPhotoUrl().toString();
                } else {
                    startActivity(new Intent(DialogActivity.this, EmailPasswordActivity.class));
                }
            }
        };

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        mMsgEditText = (EditText) findViewById(R.id.msgEditText);

        mMsgEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatMessage friendlyMessage = new
                        ChatMessage(mUsername,
                        mUseremail,
                        mMsgEditText.getText().toString(),
                        mPhotoUrl);
                mSimpleFirechatDatabaseReference.child("dialogs/" + FirebaseAuth.getInstance().getCurrentUser().getEmail().replace('.', ' '))
                        .push().setValue(friendlyMessage);
                mMsgEditText.setText("");
                if (mUsername == null)
                    notification(mUseremail);
                else
                    notification(mUsername);
            }
        });

        mSimpleFirechatDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatMessage,
                FirechatMsgViewHolder>(
                ChatMessage.class,
                R.layout.chat_message,
                FirechatMsgViewHolder.class,
                mSimpleFirechatDatabaseReference.child("dialogs/" + FirebaseAuth.getInstance().getCurrentUser().getEmail().replace('.', ' '))) {

            @Override
            protected void populateViewHolder(FirechatMsgViewHolder viewHolder, ChatMessage friendlyMessage, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.userTextView.setText(friendlyMessage.getName());
                viewHolder.emailUserTextView.setText(friendlyMessage.getEmail());
                viewHolder.msgTextView.setText(friendlyMessage.getText());
                if (friendlyMessage.getPhotoUrl() == null) {
                    viewHolder.userImageView
                            .setImageDrawable(ContextCompat
                                    .getDrawable(DialogActivity.this,
                                            R.drawable.profile));
                } else {
                    Glide.with(DialogActivity.this)
                            .load(friendlyMessage.getPhotoUrl())
                            .into(viewHolder.userImageView);
                }
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int chatMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (chatMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void notification(final String user) {

        final ArrayList<String> adminTokens = new ArrayList<>();
        final ArrayList<String> arrayList = new ArrayList<>();
        arrayList.clear();
        arrayList.add("1");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("adminTokens");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (arrayList.size() > 0) {
                    HashMap<String, String> value = (HashMap<String, String>) dataSnapshot.getValue();
                    adminTokens.clear();
                    for (int i = 0; i < value.size(); i++)
                        adminTokens.add(value.values().toArray()[i].toString());

                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    OkHttpClient client = new OkHttpClient();

                    String keyFromConsole = "AAAAK4hHGz0:APA91bGXJ3-cCVUdF9YpuScopTOG3AYIvG3SUyLVy2QBj7GKIEj8ZQWbg4jx8NTSfJQMpDSgQUu1B16QvkUMpuPeVGPJZqzcyVzznJJ0GOlgW6GHzfjTxIjbZqGS-QGvlffbJa8ASZwY82Shxakn3O7vGjfWMMKCIg";//тут ключ который можно взять из консоли (Настройки-настройки проекта-CLOUD MESSAGING-ключ сервера)

                    for (int i = 0; i < value.size(); i++) {

                        String json = "{ \"notification\": { \"text\": \"" + user + " прислал сообщение\", \"sound\": \"notification_sound\"}, \"to\" : \"" + adminTokens.get(i) + "\"}";

                        RequestBody body = RequestBody.create(JSON, json);
                        Request request = new Request.Builder()
                                .url("https://fcm.googleapis.com/fcm/send")
                                .addHeader("Authorization", "key=" + keyFromConsole)
                                .addHeader("ContentType", "application/json")
                                .post(body)
                                .build();
                        client.newCall(request).enqueue(new Callback() {

                            @Override
                            public void onFailure(Call call, IOException e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                System.out.println(response.body().string());
                            }
                        });
                    }
                    arrayList.clear();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });

    }
}

