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

import org.apache.commons.codec.DecoderException;
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

                        Log.d(TAG, "tag : " + tag.getClass().getName());
                        Log.d(TAG, "tag : " + Arrays.toString(tag.getTechList()));

                        // REGION SELECT eMoney

//                        String AID = "0000000000000001";
                        String hex = "00A40400080000000000000001";
                        String commandWithoutSpace = hex.replace(" ", "");
                        byte[] APDUCommand = new byte[0];
                        try {
                            APDUCommand = Hex.decodeHex(commandWithoutSpace.toCharArray());
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        }

//                        byte[] APDUCommand = {
//                                (byte) 0x00, // CLA Class
//                                (byte) 0xA4, // INS Instruction
//                                (byte) 0x04, // P1  Parameter 1
//                                (byte) 0x00, // P2  Parameter 2and?
//                                (byte) 0x08, // Length
//                                (byte) 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01,// AID
//                        };

                        IsoDep iso = IsoDep.get(tag);
                        try {
                            iso.connect();
                            Log.d(TAG, "handleIntent: " + iso.isConnected());
                            byte[] result = iso.transceive(APDUCommand);
                            Log.d(TAG, "handleIntent result Select eMoney Bytes: " + Arrays.toString(result));
                            Log.d(TAG, "handleIntent result Select eMoney Hex : " + Hex.encodeHexString(result));
                            Log.d(TAG, "handleIntent result Select eMoney Hex : " + bytesToHex(result));
                            Log.d(TAG, "handleIntent result Select eMoney String : " + new String(result, StandardCharsets.UTF_8));


                            // REGION READ LAST BALANCE

                            String hex2 = "00B500000A";
                            String commandWithoutSpace2 = hex2.replace(" ", "");
                            byte[] GET_LAST_BALANCE = new byte[0];
                            try {
                                GET_LAST_BALANCE = Hex.decodeHex(commandWithoutSpace2.toCharArray());
                            } catch (DecoderException e) {
                                e.printStackTrace();
                            }

//                            byte[] GET_LAST_BALANCE = {
//                                    (byte) 0x00, // CLA Class
//                                    (byte) 0xb5, // INS Instruction
//                                    (byte) 0x00, // P1  Parameter 1
//                                    (byte) 0x00, // P2  Parameter 2
//                                    (byte) 0x5a  // LE  maximal number of bytes expected in result
//                            };

                            result = iso.transceive(GET_LAST_BALANCE);

                            Log.d(TAG, "handleIntent result Last Balance Bytes : " + Arrays.toString(result));
                            Log.d(TAG, "handleIntent result Last Balance Hex : " + Hex.encodeHexString(result));
                            Log.d(TAG, "handleIntent result Last Balance Hex : " + bytesToHex(result));
                            Log.d(TAG, "handleIntent result Last Balance String : " + new String(result, StandardCharsets.UTF_8));

                            int len = result.length;
                            if (!(result[len-2]==(byte)0x90&&result[len-1]==(byte) 0x00))
                                throw new RuntimeException("could not retrieve msisdn");

                            byte[] dataR = new byte[len-2];
                            System.arraycopy(result, 0, dataR, 0, len-2);
                            String str = new String(dataR).trim();
                            Log.d(TAG, "handleIntent result Last Balance String : " + str);

//                            StringBuilder sb = new StringBuilder();
//                            final char[] xxxx = Hex.encodeHex(result);
//                            for (char value : xxxx) {
////                                System.out.println(xxxx[i]);
//                                sb.append(value);
//                            }
//                            System.out.println("----------------------------------------------");
//                            System.out.println(sb);
//                            System.out.println("----------------------------------------------");
//                            sb = new StringBuilder();
//                            for (char c : xxxx) {
//                                sb.append(Long.decode("0x" + c));
////                                System.out.println(Long.decode("0x" + xxxx[i]));
//                            }
//                            System.out.println("----------------------------------------------");
//                            System.out.println(sb);
//                            System.out.println("----------------------------------------------");

//                            for (int sfi = 1; sfi < 10; ++sfi ) {
//                                for (int record = 1; record < 10; ++record) {
//                                    byte[] cmd = new byte[0];
//                                    cmd = GET_LAST_BALANCE;
//                                    cmd[2] = (byte)(record & 0x0FF);
//                                    cmd[3] |= (byte)((sfi << 3) & 0x0F8);
//                                    result = iso.transceive(cmd);
//                                    if ((result != null) && (result.length >=2)) {
//                                        if ((result[result.length - 2] == (byte)0x90) && (result[result.length - 1] == (byte)0x00)) {
//                                            // file exists and contains data
//                                            byte[] data = Arrays.copyOf(result, result.length - 2);
////                                                Log.d(TAG, "handleIntent result 1 : " + sfi + Arrays.toString(data));
////                                                Log.d(TAG, "handleIntent result 1 : " + sfi  + Arrays.toString(Hex.encodeHex(data)));
//                                            Log.d(TAG, "handleIntent result 1 enc : " + sfi + " : "  + Hex.encodeHexString(data));
//                                            Log.d(TAG, "handleIntent result 1 : " + sfi + " : "  + new String(data, StandardCharsets.UTF_8));
////                                                Log.d(TAG, "handleIntent result 1 : " + data[0] + "|" + data[1]);
////                                                Log.d(TAG, "handleIntent result 1 : " + data[data.length - 2] + "|" + data[data.length - 1]);
//                                            Log.d(TAG, "handleIntent result 1 bth : " + sfi + " : "  + bytesToHex(data));
//                                        }
//                                    }
//                                }
//                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                },
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                options
        );
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
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