/*
    1.리더기에 의해 TAG 가 읽혀지게 되면 어떤 액티비티에 읽혀진 정보를 가진 인텐트를 전달해야 하는가?
     (1)enableForgroundDispatch() 메서드를호출한 액티비티

     (2)ACTION_NDEF_DISCOVERED 액션을 인텐트 필터로 등록해놓은 액티비티
        - 이때 필터에는 mimetype 를 등록해야 함
        -  mimetype이 같은 경우 이 액티비티에게 인텐트 전달함

     (3)ACTION_TECH_DISCOVERED 액션을 인텐트 필터로 등록해놓은 액티비티
       - 이때 필터에는 메타데이터를 등록해야 함
       - 메타데이터가 같은 경우 이 액티비티에게 인텐트 전달함

     (4)ACTION_TAG_DISCOVERED 액션을 인텐트 필터로 등록해놓은 액티비티
*/

    /*
        2.태그 데이터의 구성
        NdefRecord < NdefMessage  < Tag
     */

package com.example.student.nfcapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    String TAG = this.getClass().getName();
    NfcAdapter nfcAdapter;
    int count;
    TextView txt_count;
    EditText edit_input;

    //태그정보
    Tag tag;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //txt_count = (TextView)findViewById(R.id.txt_count);
        edit_input = (EditText) findViewById(R.id.edit_input);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        //만일 이 주소값이 계속 변한 다면, 태그 인식에 의해 액티비티의 생성이 계속 일어나는 것임
        //해결책) FLAG_ACTIVITY_SINGLE_TOP 이용
        Log.d(TAG, "MainActivity is " + this);
    }

    public void setCount() {
        count++;
        //txt_count.setText(Integer.toString(count));
    }

    @Override
    protected void onResume() {
        setCount();
        registDetect();
        Log.d(TAG, "onResume 호출!!");
        super.onResume();
    }

    public void registDetect() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter[] intentFilter = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);
    }

    @Override
    protected void onPause() {
        nfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Toast.makeText(this, "읽혀진 데이터를 intent로 받았습니다.", Toast.LENGTH_SHORT).show();
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }

        super.onNewIntent(intent);
    }


    //메세지 생성하기
    public NdefMessage createNdefMessage(String payload,  boolean encodeInUtf8) {
        byte[] langBytes = Locale.getDefault().getLanguage().getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = payload.getBytes(utfEncoding);
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT, new byte[0], data);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{record});

        return ndefMessage;
    }

    //기존 Tag의 데이터 포맷하기
    public void formatTag(NdefMessage ndefMessage) {
        NdefFormatable ndefFormatable = NdefFormatable.get(tag);
        if (ndefFormatable == null) {
            Toast.makeText(this, "해당 태그는 Ndef 형식이 아닙니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
    }

    //메세지 작성하기
    public void writeNdefMessage(NdefMessage ndefMessage) {
        Ndef ndef = Ndef.get(tag);

        //형식이 맞지 않으면...
        if (ndef == null) {
            formatTag(ndefMessage);
        } else {
            try {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Toast.makeText(this, "this Tag is not writable !!", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }
                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeData(View view) {
        NdefMessage ndefMessage = createNdefMessage(edit_input.getText().toString(), true);

        writeNdefMessage(ndefMessage);
    }
}
