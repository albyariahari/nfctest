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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";

    private TextView mTextView, mTvInfo, mTvAttr;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.textView_explanation);
        mTvInfo = (TextView) findViewById(R.id.textView_info);
        mTvAttr = (TextView) findViewById(R.id.textView_attr);

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
                this::handleTag,
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                options
        );
    }

    private void handleTag(Tag tag) {

        IsoDep iso = IsoDep.get(tag);
        try {
            iso.connect();
            String cardId = Hex.encodeHexString(iso.getTag().getId());
            Log.d(TAG, "handleIntent result cardId : " + cardId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // REGION SELECT eMoney
//        String AID = "0000000000000001";
        String hex = "00A40400080000000000000001";
        String commandWithoutSpace = hex.replace(" ", "");
        byte[] APDUCommand = new byte[0];
        try {
            APDUCommand = Hex.decodeHex(commandWithoutSpace.toCharArray());
        } catch (DecoderException e) {
            e.printStackTrace();
        }

//        byte[] APDUCommand = {
//                (byte) 0x00, // CLA Class
//                (byte) 0xA4, // INS Instruction
//                (byte) 0x04, // P1  Parameter 1
//                (byte) 0x00, // P2  Parameter 2and?
//                (byte) 0x08, // Length
//                (byte) 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01,// AID
//        };

        try {
            byte[] result = iso.transceive(APDUCommand);
            Log.d(TAG, "handleIntent result Select eMoney Bytes: " + printBytes(result));
            Log.d(TAG, "handleIntent result Select eMoney Hex : " + Hex.encodeHexString(result));
            // ENDREGION





            // REGION READ LAST BALANCE
            String hex2 = "00B500000A";
            String commandWithoutSpace2 = hex2.replace(" ", "");
            byte[] GET_LAST_BALANCE = new byte[0];
            try {
                GET_LAST_BALANCE = Hex.decodeHex(commandWithoutSpace2.toCharArray());
            } catch (DecoderException e) {
                e.printStackTrace();
            }

            result = iso.transceive(GET_LAST_BALANCE);

            int len = result.length;
            if (!(result[len - 2] == (byte) 0x90 && result[len - 1] == (byte) 0x00)) {
                runOnUiThread(() -> mTextView.setText("Gagal membaca saldo"));
                throw new RuntimeException("could not retrieve msisdn");
            }

            String hexString = Hex.encodeHexString(result);
            int lastBalance = ParseHelper.fromLittleEndian(hexString.substring(0, 8));
            runOnUiThread(() -> mTextView.setText("Saldo Anda : " + formatRupiah(lastBalance)));
            Log.d(TAG, "handleIntent result GET_LAST_BALANCE Bytes : " + printBytes(result));
            Log.d(TAG, "handleIntent result GET_LAST_BALANCE Hex : " + hexString);
            Log.d(TAG, "handleIntent result GET_LAST_BALANCE Hex substring : " + hexString.substring(0, 8));
            Log.d(TAG, "handleIntent result GET_LAST_BALANCE lastBalance : " + lastBalance);
//            Log.d(TAG, "handleIntent result GET_LAST_BALANCE String : " + new String(result, StandardCharsets.UTF_8));
            // ENDREGION






            // REGION CARD INFO
            String hex3 = "00b300003F";
            String commandWithoutSpace3 = hex3.replace(" ", "");
            byte[] GET_CARD_INFO = new byte[0];
            try {
                GET_CARD_INFO = Hex.decodeHex(commandWithoutSpace3.toCharArray());
            } catch (DecoderException e) {
                e.printStackTrace();
            }

            result = iso.transceive(GET_CARD_INFO);

            len = result.length;
            if (!(result[len - 2] == (byte) 0x90 && result[len - 1] == (byte) 0x00)) {
                runOnUiThread(() -> mTvInfo.setText("Gagal membaca info"));
                throw new RuntimeException("could not retrieve msisdn");
            }

            hexString = Hex.encodeHexString(result);
            String info = hexString.substring(0, 16);
            String formatedInfo = info.replaceAll("....", "$0 ");
            runOnUiThread(() -> mTvInfo.setText(String.format("No Kartu : %s", formatedInfo)));
            Log.d(TAG, "handleIntent result GET_CARD_INFO Bytes : " + printBytes(result));
            Log.d(TAG, "handleIntent result GET_CARD_INFO Hex : " + hexString);
            Log.d(TAG, "handleIntent result GET_CARD_INFO Card Number : " + info);
            Log.d(TAG, "handleIntent result GET_CARD_INFO Card Number Formatted : " + formatedInfo);
            Log.d(TAG, "handleIntent result GET_CARD_INFO String : " + new String(result, StandardCharsets.UTF_8));
            // ENDREGION










            // REGION CARD ATTR
            String hex4 = "00F210000B";
            String commandWithoutSpace4 = hex4.replace(" ", "");
            byte[] GET_CARD_ATTR = new byte[0];
            try {
                GET_CARD_ATTR = Hex.decodeHex(commandWithoutSpace4.toCharArray());
            } catch (DecoderException e) {
                e.printStackTrace();
            }

            result = iso.transceive(GET_CARD_ATTR);

            len = result.length;
            if (!(result[len - 2] == (byte) 0x90 && result[len - 1] == (byte) 0x00)) {
                runOnUiThread(() -> mTvAttr.setText("Gagal membaca attr"));
                throw new RuntimeException("could not retrieve msisdn");
            }

            hexString = Hex.encodeHexString(result);
            String finalHexString = hexString;
//            runOnUiThread(() -> mTvAttr.setText(String.format("Attr Hex : %s", finalHexString)));
            Log.d(TAG, "handleIntent result GET_CARD_ATTR Bytes : " + printBytes(result));
            Log.d(TAG, "handleIntent result GET_CARD_ATTR Hex : " + hexString);
            Log.d(TAG, "handleIntent result GET_CARD_ATTR String : " + new String(result, StandardCharsets.UTF_8));
            // ENDREGION

            iso.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String printBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte b : bytes) {
            sb.append(String.format("0x%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String formatRupiah(int data) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        formatRupiah.setMaximumFractionDigits(0);
        return formatRupiah.format(data);
    }


    private String getHex(byte[] bytes) {
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

    private String getHex2(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
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

    private long getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    public static byte[] decode(String hex) {
        String[] list = hex.split("(?<=\\G\\w{2})(?:\\s*)");
        ByteBuffer buffer = ByteBuffer.allocate(list.length);
        System.out.println(list.length);
        for (String str : list) {
            buffer.put(Byte.parseByte(str, 16));
            System.out.println(str);
        }

        return buffer.array();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
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
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            handleTag(tag);
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