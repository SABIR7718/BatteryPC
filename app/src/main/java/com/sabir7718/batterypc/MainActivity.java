package com.sabir7718.batterypc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class MainActivity extends Activity {

    private TextView tvStatus;
    private TextView tvBatteryVal;
    private SeekBar sbBattery;
    private EditText etCustomBattery;
    private Button btnSet;
    private Button btnReset;
    private Button btnCharging;
    private boolean chargingConnected = false;

    private int selectedLevel = 50;
    private static final int SHIZUKU_CODE = 1001;

    private final Shizuku.OnBinderReceivedListener binderReceivedListener = () -> {
        runOnUiThread(this::checkShizukuPermission);
    };

    private final Shizuku.OnBinderDeadListener binderDeadListener = () -> {
        runOnUiThread(() -> updateStatus("Status: Shizuku Disconnected"));
    };

    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> {
        try {
            if (requestCode == SHIZUKU_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    updateStatus("Status: Permission Granted!");
                    checkChargingState();
                } else {
                    showErrorDialog("Permission Denied", "Shizuku permission was not granted.");
                }
            }
        } catch (Throwable t) {
            showErrorDialog("Permission Listener Error", t.getMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            runOnUiThread(() -> showErrorDialog("Uncaught Exception", throwable.getLocalizedMessage()));
        });

        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            getWindow().setStatusBarColor(android.graphics.Color.rgb(3, 5, 4));
            getWindow().setNavigationBarColor(android.graphics.Color.rgb(3, 5, 4));

            Toast.makeText(this, new String(Base64.decode("4qS3IPCdmbHwnZqiIPCdmojwnZqY8J2anvCdmpsg8J2agvCdmbDwnZmx8J2ZuPCdmoHigbfigbfCueKBuCDipLY=", Base64.DEFAULT), StandardCharsets.UTF_8), Toast.LENGTH_LONG).show();

            tvStatus = findViewById(R.id.tvStatus);
            tvBatteryVal = findViewById(R.id.tvBatteryVal);
            sbBattery = findViewById(R.id.sbBattery);
            etCustomBattery = findViewById(R.id.etCustomBattery);
            btnSet = findViewById(R.id.btnSet);
            btnReset = findViewById(R.id.btnReset);
            btnCharging = findViewById(R.id.btnCharging);

            try {
                Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
                Shizuku.addBinderDeadListener(binderDeadListener);
                Shizuku.addRequestPermissionResultListener(permissionListener);
            } catch (Throwable ignored) {
            }

            checkShizukuPermission();

            sbBattery.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        try {
                            selectedLevel = progress;
                            tvBatteryVal.setText("Battery: " + selectedLevel + "%");
                            etCustomBattery.setText(String.valueOf(selectedLevel));
                        } catch (Throwable t) {
                            showErrorDialog("UI Error", t.getMessage());
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            etCustomBattery.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (etCustomBattery.hasFocus()) {
                        String input = s.toString().trim();
                        if (!input.isEmpty()) {
                            try {
                                int val = Integer.parseInt(input);
                                if (val > 9999) {
                                    val = 9999;
                                    etCustomBattery.setText("9999");
                                    etCustomBattery.setSelection(4);
                                }
                                selectedLevel = val;
                                tvBatteryVal.setText("Battery: " + selectedLevel + "%");
                                if (val <= 100) {
                                    sbBattery.setProgress(val);
                                }
                            } catch (NumberFormatException ignored) {
                            } catch (Throwable t) {
                                showErrorDialog("Input Error", t.getMessage());
                            }
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            btnSet.setOnClickListener(v -> handleSetBattery());
            btnReset.setOnClickListener(v -> executeShellCmd("dumpsys battery reset", true));
            btnCharging.setOnClickListener(v -> toggleChargingState());

        } catch (Throwable t) {
            showErrorDialog("Init Error", t.getMessage());
        }
    }

    private void handleSetBattery() {
        try {
            String customStr = etCustomBattery.getText().toString().trim();
            if (!customStr.isEmpty()) {
                int customVal = Integer.parseInt(customStr);
                if (customVal > 9999) customVal = 9999;
                selectedLevel = customVal;
            }
            executeShellCmd("dumpsys battery set level " + selectedLevel, false);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number entered!", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            showErrorDialog("Set Battery Error", t.getMessage());
        }
    }

    private void checkChargingState() {
        executeShellCmd("dumpsys battery", false, true);
    }

    private void toggleChargingState() {
        String command = chargingConnected
                ? "dumpsys battery set ac 0"
                : "dumpsys battery set ac 1";

        executeShellCmd(command, false, true);
    }

    private void checkShizukuPermission() {
        try {
            if (!isShizukuAvailable()) {
                updateStatus("Status: Shizuku Not Running");
                return;
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                updateStatus("Status: Ready");
                checkChargingState();
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                showErrorDialog("Permission Needed", "Please enable permission for BatteryPC in Shizuku App.");
            } else {
                Shizuku.requestPermission(SHIZUKU_CODE);
            }
        } catch (Throwable t) {
            updateStatus("Shizuku Check Failed");
            showErrorDialog("Shizuku Error", t.getLocalizedMessage());
        }
    }

    private boolean isShizukuAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    private void executeShellCmd(String command, boolean isReset) {
        executeShellCmd(command, isReset, false);
    }

    private void executeShellCmd(String command, boolean isReset, boolean checkCharging) {
        try {
            if (!isShizukuAvailable()) {
                showErrorDialog("Shizuku Error", "Shizuku service is not running!");
                return;
            }

            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                updateStatus("Status: Permission Required");
                Shizuku.requestPermission(SHIZUKU_CODE);
                return;
            }

            new Thread(() -> {
                Process process = null;

                try {
                    Method newProcessMethod = Shizuku.class.getDeclaredMethod(
                            "newProcess",
                            String[].class,
                            String[].class,
                            String.class
                    );

                    newProcessMethod.setAccessible(true);

                    process = (Process) newProcessMethod.invoke(
                                    null,
                                    new String[]{"sh", "-c", command},
                                    null,
                                    null
                            );

                    BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                    );

                    BufferedReader stderr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
                    );

                    StringBuilder output = new StringBuilder();
                    StringBuilder errorLog = new StringBuilder();

                    String line;

                    while ((line = stdout.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    while ((line = stderr.readLine()) != null) {
                        errorLog.append(line).append("\n");
                    }

                    int exitCode = process.waitFor();

                    String result = output.toString();

                    runOnUiThread(() -> {
                        if (exitCode == 0) {

                            if (checkCharging) {
                                updateChargingState(result);
                            }

                        } else {
                            showErrorDialog(
                            "Command Execution Failed",
                            errorLog.toString().trim()
                            );
                        }
                    });

                } catch (Throwable t) {
                    runOnUiThread(() ->
                            showErrorDialog(
                            "Execution Exception",
                            t.getLocalizedMessage()
                            )
                    );

                } finally {
                    if (process != null) {
                        process.destroy();
                    }
                }
            }).start();

        } catch (Throwable t) {
            showErrorDialog(
            "Command Setup Error",
            t.getLocalizedMessage()
            );
        }
    }

    private void updateChargingState(String batteryOutput) {
        try {
            chargingConnected = batteryOutput.contains("AC powered: true");

            if (btnCharging != null) {
                if (chargingConnected) {
                    btnCharging.setText("CHARGING ON");
                } else {
                    btnCharging.setText("CHARGING OFF");
                }
            }

            updateStatus(
            chargingConnected
                    ? "Status: Charging Connected"
                    : "Status: Charging Disconnected"
            );

        } catch (Throwable t) {
            showErrorDialog(
            "Charging State Error",
            t.getLocalizedMessage()
            );
        }
    }

    private void updateStatus(String msg) {
        try {
            if (tvStatus != null) {
                tvStatus.setText(msg);
            }
        } catch (Throwable ignored) {
        }
    }

    private void showErrorDialog(String title, String message) {
        try {
            if (isFinishing() || isDestroyed()) return;

            new AlertDialog.Builder(this)
                    .setTitle("⚠️ " + title)
                    .setMessage(message != null && !message.isEmpty() ? message : "Unknown System Error")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener);
            Shizuku.removeBinderDeadListener(binderDeadListener);
            Shizuku.removeRequestPermissionResultListener(permissionListener);
        } catch (Throwable ignored) {
        }
    }
}