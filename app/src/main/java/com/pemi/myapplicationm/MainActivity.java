package com.pemi.myapplicationm;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Rotation";
    private final String FILE_NAME = "example.txt";
    public static final String DATE_FORMAT_1 = "hh:mm a";
    public String loadedContents;
    private NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    RelativeLayout relativeLayout;
    int mLastRotation = 0;
    TextView mTextView, mTextView2;
    EditText mEditText;
    Button mButton;
    StringBuilder hex;
    String filename = "";
    String filepath = "";
    String fileContent = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        relativeLayout = findViewById(R.id.relativeLayout);
        mTextView = findViewById(R.id.textView1);
        filename = "myFile.txt";
        filepath = "MyFileDir";

        load();
        if (!isExternalStorageAvailableForRW()) {
            mButton.setEnabled(false);
        }


        if (
                Build.VERSION.SDK_INT >= 23
                        && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
                        && checkSelfPermission(WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
                    , Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 0);
        } else {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        getSupportActionBar().hide();

        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        OrientationEventListener orientationEventListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {

                Display display = mWindowManager.getDefaultDisplay();
                int rotation = display.getRotation();
                if (rotation == Surface.ROTATION_0) {
//                    Toast.makeText(MainActivity.this, "Portrait!", Toast.LENGTH_SHORT).show();
//                    Log.i(TAG, "changed >>> " + rotation);
                    relativeLayout.setBackgroundColor(Color.GREEN);
                    // do something
                    mLastRotation = rotation;
                } else if (rotation == Surface.ROTATION_180) {
//                    Toast.makeText(MainActivity.this, "Rotation!", Toast.LENGTH_SHORT).show();
//                    Log.i(TAG, "changed >>> " + rotation);
                    relativeLayout.setBackgroundColor(Color.RED);
                    // do something
                    mLastRotation = rotation;
                }
            }
        };

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }

        //Initialize NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        //if no NfcAdapter, display that the device has no NFC
        if (nfcAdapter == null) {
            Toast.makeText(this, "NO NFC Capabilities", Toast.LENGTH_SHORT).show();
            finish();
        }
        //create a pendingIntent
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

    }

    @Override
    protected void onResume() {
        super.onResume();
        assert nfcAdapter != null;
        //nfcAdapter.enableForegroundDispatch(context,pendingIntent,
        //                                    intentFilterArray,
        //                                    techListsArray)
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    protected void onPause() {
        super.onPause();
        //Onpause stop listening
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            assert tag != null;
            byte[] payload = detectTagData(tag).getBytes();

        }
    }

    private String detectTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        String data = tag.getId().toString();
        hex = sb.append("ID : ").append((data)).append('\n');
        sb.append("ID (hex): ").append(toHex(id)).append('\n');
//        sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n');
        sb.append("ID (dec): ").append(toDec(id)).append('\n');
//        sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n');
//        sb.append("ID (string): ").append(toString()).append('\n');

        Toast.makeText(this, "NFC SCANNED", Toast.LENGTH_SHORT).show();
        mTextView.setText(hex);
        fileContent = mTextView.getText().toString().trim();
        // Check for Storage Permission
        if (isStoragePermissionGranted()) {
            // If input is not empty, we'll proceed
            if (!fileContent.equals("")) {
                // To access app-specific files from external storage, you can call
                // getExternalFilesDir() method. It returns the path to
                // storage > emulated > 0 > Android > data > [package_name] > files > MyFileDir
                // or,
                // storage > self > Android > data > [package_name] > files > MyFileDir
                // directory on the SD card. Once the app is uninstalled files here also get
                // deleted.
                // Create a File object like this.
//                File myExternalFile = new File(getExternalFilesDir(filepath), filename);
                File folderFile = new File(Environment.getExternalStorageDirectory().getPath() + filepath);
                if (!folderFile.exists()) {
                    folderFile.mkdirs();
                }
                File myExternalFile = new File(getExternalFilesDir(filepath), filename);
                // Create an object of FileOutputStream for writing data to myFile.txt
                FileOutputStream fos = null;
                try {
                    // Instantiate the FileOutputStream object and pass myExternalFile in constructor
                    fos = new FileOutputStream(myExternalFile);
                    // Write to the file
                    String merge = fileContent+loadedContents;
                    fos.write(merge.getBytes());
                    // Close the stream
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                load();
                // Clear the EditText
                // Show a Toast message to inform the user that the operation has been successfully completed.
                Toast.makeText(MainActivity.this, "Information saved to SD card.", Toast.LENGTH_SHORT).show();
            } else {
                // If the Text field is empty show corresponding Toast message
                Toast.makeText(MainActivity.this, "Text field can not be empty.", Toast.LENGTH_SHORT).show();
            }
        }

        String prefix = "android.nfc.tech";
        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.substring(prefix.length()));
            sb.append(", ");
        }

        sb.delete(sb.length() - 2, sb.length());

        for (String tech : tag.getTechList()) {

            if (tech.equals(MifareClassic.class.getName())) {
                sb.append('\n');
                String type = "Unknown";

                try {
                    MifareClassic mifareTag = MifareClassic.get(tag);

                    switch (mifareTag.getType()) {
                        case MifareClassic.TYPE_CLASSIC:
                            type = "Classic";
                            break;
                        case MifareClassic.TYPE_PLUS:
                            type = "Plus";
                            break;
                        case MifareClassic.TYPE_PRO:
                            type = "Pro";
                            break;
                    }
                    sb.append("Mifare Classic type: ");
                    sb.append(type);
                    sb.append('\n');

                    sb.append("Mifare size: ");
                    sb.append(mifareTag.getSize() + " bytes");
                    sb.append('\n');

                    sb.append("Mifare sectors: ");
                    sb.append(mifareTag.getSectorCount());
                    sb.append('\n');

                    sb.append("Mifare blocks: ");
                    sb.append(mifareTag.getBlockCount());
                } catch (Exception e) {
                    sb.append("Mifare classic error: " + e.getMessage());
                }
            }

            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                sb.append("Mifare Ultralight type: ");
                sb.append(type);
            }
        }
        Log.v(TAG, sb.toString());
        return sb.toString();
    }

//    For reading and writing
//    private String detectTagData(Tag tag) {
//        StringBuilder sb = new StringBuilder();
//        byte[] id = tag.getId();
//        sb.append("NFC ID (dec): ").append(toDec(id)).append('\n');
//        for (String tech : tag.getTechList()) {
//            if (tech.equals(MifareUltralight.class.getName())) {
//                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
//                String payload;
//                payload = readTag(mifareUlTag);
//                sb.append("payload: ");
//                sb.append(payload);
//                writeTag(mifareUlTag);
//            }
//        }
//    Log.v("test",sb.toString());
//    return sb.toString();
//}

    //    private void readFromIntent(Intent intent) {
//        String action = intent.getAction();
//        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
//                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
//                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
//            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
//            NdefMessage[] msgs = null;
//            if (rawMsgs != null) {
//                msgs = new NdefMessage[rawMsgs.length];
//                for (int i = 0; i < rawMsgs.length; i++) {
//                    msgs[i] = (NdefMessage) rawMsgs[i];
//                }
//            }
//            buildTagViews(msgs);
//        }
//    }
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        mTextView.setText("NFC Content: " + text);
        Toast.makeText(this, "Buildtags!", Toast.LENGTH_SHORT).show();
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");

            }
        }
        return sb.toString();
    }

    private String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    private long toDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    private long toReversedDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = bytes.length - 1; i >= 0; --i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    public void writeTag(MifareUltralight mifareUlTag) {
        try {
            mifareUlTag.connect();
            mifareUlTag.writePage(4, "get ".getBytes(Charset.forName("US-ASCII")));
            mifareUlTag.writePage(5, "fast".getBytes(Charset.forName("US-ASCII")));
            mifareUlTag.writePage(6, " NFC".getBytes(Charset.forName("US-ASCII")));
            mifareUlTag.writePage(7, " now".getBytes(Charset.forName("US-ASCII")));
        } catch (IOException e) {
            Log.e(TAG, "IOException while writing MifareUltralight...", e);
        } finally {
            try {
                mifareUlTag.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException while closing MifareUltralight...", e);
            }
        }
    }

    public String readTag(MifareUltralight mifareUlTag) {
        try {
            mifareUlTag.connect();
            byte[] payload = mifareUlTag.readPages(4);
            return new String(payload, Charset.forName("US-ASCII"));
        } catch (IOException e) {
            Log.e(TAG, "IOException while reading MifareUltralight message...", e);
        } finally {
            if (mifareUlTag != null) {
                try {
                    mifareUlTag.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing tag...", e);
                }
            }
        }
        return null;
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //Permission is granted
                return true;
            } else {
                //Permission is revoked
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            //permission is automatically granted on sdk<23 upon installation
            //Permission is granted
            return true;
        }
    }

    private boolean isExternalStorageAvailableForRW() {
        // Check if the external storage is available for read and write by calling
        // Environment.getExternalStorageState() method. If the returned state is MEDIA_MOUNTED,
        // then you can read and write files. So, return true in that case, otherwise, false.
        String extStorageState = Environment.getExternalStorageState();
        if (extStorageState.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    private void load() {
        FileReader fr = null;
        File myExternalFile = new File(getExternalFilesDir(filepath), filename);


        StringBuilder stringBuilder = new StringBuilder();
        try {
            // Instantiate the FileReader object and pass myExternalFile in the constructor
            fr = new FileReader(myExternalFile);
            // Instantiate a BufferedReader object and pass FileReader object in constructor.
            // The BufferedReader maintains an internal buffer and can be used with different
            // types of readers to read text from an Input stream more efficiently.
            BufferedReader br = new BufferedReader(fr);
            // Next, call readLine() method on BufferedReader object to read a line of text.
            String line = br.readLine();
            // Use a while loop to read the entire file
            while (line != null) {
                // Append the line read to StringBuilder object. Also, append a new-line
                stringBuilder.append(line).append('\n');
                // Again read the next line and store in variable line
                line = br.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Date currentTime = Calendar.getInstance().getTime();

            // Convert the StringBuilder content into String and add text "File contents\n"
            // at the beginning.
            loadedContents = "\n" + currentTime + "\n" + stringBuilder.toString().trim();
            // Set the TextView with fileContents
//            mTextView2.setText(loadedContents);
        }
    }
}