package com.hannapapova.clientandroid;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int SERVERPORT = 3003;

    public static final String SERVER_IP = "10.160.92.145";
    private ClientThread clientThread;
    private Thread thread;
    private Handler handler;

    private TextInputEditText login;
    private TextInputEditText password;
    private TextInputEditText userInfoEdit, secondPasswordEditController;
    private Context context;
    private TextInputLayout userInfoLayout;
    private TextInputLayout secondPasswordLayout;
    private Button editButton, sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Client");
        context = this;
        handler = new Handler();
        login = findViewById(R.id.userNameEditController);
        password = findViewById(R.id.passwordEditController);
        userInfoEdit = findViewById(R.id.userInfoController);
        userInfoLayout = findViewById(R.id.userInfoLayout);
        editButton = findViewById(R.id.editButton);
        secondPasswordLayout = findViewById(R.id.secondPasswordEdit);
        sendButton = findViewById(R.id.sendSecondPassword);
        secondPasswordEditController = findViewById(R.id.secondPasswordEditController);
    }

    public void showToast(String message) {
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.connect_server) {
            clientThread = new ClientThread();
            thread = new Thread(clientThread);
            thread.start();
            return;
        }

        if (view.getId() == R.id.logInButton) {
            String clientLogin = login.getText().toString().trim();
            String clientPassword = password.getText().toString().trim();
            if (clientThread != null) {
                clientThread.sendMessage(authString(clientLogin, clientPassword));
            }
        }

        if (view.getId() == R.id.editButton) {
            String clientLogin = login.getText().toString().trim();
            String clientEditInfo = userInfoEdit.getText().toString().trim();
            if (clientThread != null) {
                clientThread.sendMessage(editString(clientLogin, clientEditInfo));
            }
        }
        if (view.getId() == R.id.sendSecondPassword) {
            secondPasswordLayout.setVisibility(View.GONE);
            sendButton.setVisibility(View.GONE);
            //send
            String secondPass = "secondPassword {" + secondPasswordEditController.getText().toString().trim() + "}";
            clientThread.sendMessage(secondPass);
        }
    }

    class ClientThread implements Runnable {
        private Socket socket;
        private BufferedReader input;

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);

                while (!Thread.currentThread().isInterrupted()) {
                    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = input.readLine();
                    if (null == message || "Disconnect".contentEquals(message)) {
                        handler.post(() -> {
                            userInfoLayout.setVisibility(View.GONE);
                            editButton.setVisibility(View.GONE);
                        });
                        Thread.interrupted();
                        break;
                    }
                    if (message.equals("Enter second password")) {
                        handler.post(() -> {
                            secondPasswordLayout.setVisibility(View.VISIBLE);
                            sendButton.setVisibility(View.VISIBLE);
                        });
                    } else if (message.equals("login error")) {
                        showToast("This user doesn't exist");
                        handler.post(() -> {
                            userInfoLayout.setVisibility(View.GONE);
                            editButton.setVisibility(View.GONE);
                        });
                    } else {
                        if (message.equals("password error")) {
                            showToast("Wrong password");
                            handler.post(() -> {
                                userInfoLayout.setVisibility(View.GONE);
                                editButton.setVisibility(View.GONE);
                            });
                        } else {
                            String operationWord = message.substring(0, message.indexOf(" "));
                            if (operationWord.equals("auth")) {
                                String userInfo = message.substring(message.lastIndexOf('{') + 1, message.lastIndexOf('}'));
                                showToast("Successfully logged in");
                                handler.post(() -> {
                                    userInfoLayout.setVisibility(View.VISIBLE);
                                    editButton.setVisibility(View.VISIBLE);
                                    userInfoEdit.setText(userInfo);
                                });

                            } else if (operationWord.equals("edit")) {
                                showToast("Info saved");
                            }
                        }
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        void sendMessage(String message) {
            new Thread(() -> {
                try {
                    if (null != socket) {
                        PrintWriter out = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(socket.getOutputStream())),
                                true);
                        out.println(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != clientThread) {
            clientThread.sendMessage("Disconnect");
            clientThread = null;
        }
    }

    public static String authString(String username, String password) {
        return "auth {" + username + "}{" + password + "}";
    }

    public static String editString(String username, String editInfo) {
        return "edit {" + username + "}{" + editInfo + "}";
    }
}

