package id.wit.nfctest;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";

    private TextView mTextView;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.textView_explanation);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;

        }

        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText("NFC is disabled.");
        } else {
            mTextView.setText(R.string.explanation);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, mNfcAdapter);

        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500);

        mNfcAdapter.enableReaderMode(
                this,
                new NfcAdapter.ReaderCallback() {
                    @Override
                    public void onTagDiscovered(final Tag tag) {
//                        IsoDep isoDep = IsoDep.get(tag);
                        // Connect and perform rest of communication


                        byte[] APDUCommand = {
                                (byte) 0x00, // CLA Class
                                (byte) 0xA4, // INS Instruction
                                (byte) 0x04, // P1  Parameter 1
                                (byte) 0x00, // P2  Parameter 2
                                (byte) 0x07, // Length
                                (byte) 0xA0,0x00,0x00,0x00,0x03,0x10,0x10 // AID
                        };

//                        String item = "00 A4 04 00 0E 325041592E5359532E4444463031 00";
//                        String commandWithoutSpace = item.replace(" ", "");
//                        byte[] APDUCommand = DatatypeConverter.parseHexBinary(commandWithoutSpace);

//                        String hex = "00 A4 04 00 07 A0000000031010 00";
//                        String commandWithoutSpace = hex.replace(" ", "");
//                        byte[] APDUCommand = new byte[0];
//                        try {
//                            APDUCommand = Hex.decodeHex(commandWithoutSpace.toCharArray());
//                        } catch (DecoderException e) {
//                            e.printStackTrace();
//                        }

                        IsoDep iso = IsoDep.get(tag);
                        try {
                            iso.connect();
                            Log.d(TAG, "handleIntent: " + iso.isConnected());
                            byte[] result = iso.transceive(APDUCommand);
                            Log.d(TAG, "handleIntent result 1 : " + Arrays.toString(result));
                            Log.d(TAG, "handleIntent result 1 : " + Arrays.toString(Hex.encodeHex(result)));
                            Log.d(TAG, "handleIntent result 1 enc : " + Hex.encodeHexString(result));
                            Log.d(TAG, "handleIntent result 1 : " + new String(result, StandardCharsets.UTF_8));

                            int len = result.length;
                            byte[] data = new byte[len-2];
                            System.arraycopy(result, 0, data, 0, len-2);
                            String str = new String(data).trim();
                            Log.d(TAG, "handleIntent result 1 str : " + str);

                            byte[] GET_STRING = {
                                    (byte) 0x80, // CLA Class
                                    0x04, // INS Instruction
                                    0x00, // P1  Parameter 1
                                    0x00, // P2  Parameter 2
                                    0x10  // LE  maximal number of bytes expected in result
                            };

                            result = iso.transceive(GET_STRING);

                            Log.d(TAG, "handleIntent result 2 : " + Arrays.toString(result));
                            Log.d(TAG, "handleIntent result 2 : " + new String(result, StandardCharsets.UTF_8));
                            Log.d(TAG, "handleIntent result 2 enc : " + Hex.encodeHexString(result));

                            len = result.length;
//                            if (!(result[len-2]==(byte)0x90&&result[len-1]==(byte) 0x00))
//                                throw new RuntimeException("could not retrieve msisdn");

                            data = new byte[len-2];
                            System.arraycopy(result, 0, data, 0, len-2);
                            str = new String(data, StandardCharsets.UTF_8).trim();
                            Log.d(TAG, "handleIntent result 2 str : " + str);
                            Log.d(TAG, "handleIntent result 1 str enc : " + Hex.encodeHexString(data));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                },
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                options
        );
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        super.onNewIntent(intent);
        handleIntent(intent);
//        super.onNewIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                Log.d(TAG, "handleIntent tech list : " + tech);
//                if (searchedTech.equals(tech)) {
//                    new NdefReaderTask().execute(tag);
//                    break;
//                } else if (tech.equals(NdefFormatable.class.getName())) {
//                    NdefFormatable ndefFormatable = NdefFormatable.get(tag);
//
//                    if (ndefFormatable != null) {
//                        try {
//                            ndefFormatable.connect();
//                            Log.d(TAG, "handleIntent: " + ndefFormatable.isConnected());
//                            ndefFormatable.isConnected();
////                            ndefFormatable.format(new NdefMessage(NdefRecord.createTextRecord("en", "ABCD")));
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        } finally {
//                            try {
//                                ndefFormatable.close();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                    break;
//                }
                if (tech.equals(IsoDep.class.getName())) {

                    byte[] APDUCommand = {
                            (byte) 0x00, // CLA Class
                            (byte) 0xA4, // INS Instruction
                            (byte) 0x04, // P1  Parameter 1
                            (byte) 0x00, // P2  Parameter 2
                            (byte) 0x0A, // Length
                            0x63,0x64,0x63,0x00,0x00,0x00,0x00,0x32,0x32,0x31 // AID
                    };

                    IsoDep iso = IsoDep.get(tag);
                    try {
                        iso.connect();
                        Log.d(TAG, "handleIntent: " + iso.isConnected());
                        byte[] result = iso.transceive(APDUCommand);
                        Log.d(TAG, "handleIntent result : " + Arrays.toString(result));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
                }
            }
        }
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter  The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

//        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
    
        PendingIntent pendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity
                    (activity.getApplicationContext(), 0, intent, PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity
                    (activity.getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }
        
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
        adapter.disableReaderMode(activity);
    }

    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
            /*
             * See NFC forum specification for "Text Record Type Definition" at 3.2.1
             *
             * http://www.nfc-forum.org/specs/
             *
             * bit_7 defines encoding
             * bit_6 reserved for future use, must be 0
             * bit_5..0 length of IANA language code
             */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mTextView.setText("Read content: " + result);
            }
        }
    }
}