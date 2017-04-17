package com.example.apple.android_nfc_reader_cardio_braintree;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.view.CardEditText;
import com.braintreepayments.cardform.view.CardForm;
import com.pro100svitlo.creditCardNfcReader.CardNfcAsyncTask;
import com.pro100svitlo.creditCardNfcReader.utils.CardNfcUtils;
import com.skyfishjy.library.RippleBackground;

import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;

public class MainActivity extends BaseActivity implements CardNfcAsyncTask.CardNfcInterface, OnCardFormSubmitListener {

    private CardNfcAsyncTask mCardNfcAsyncTask;

    private NfcAdapter mNfcAdapter;
    private AlertDialog mTurnNfcDialog;
    private ProgressDialog mProgressDialog;
    private String mDoNotMoveCardMessage;
    private String mUnknownEmvCardMessage;
    private String mCardWithLockedNfcMessage;
    boolean mIsScanNow;
    private boolean mIntentFromCreate;
    private CardNfcUtils mCardNfcUtils;

    int MY_SCAN_REQUEST_CODE = 100;

    TextView mCardCameraScan, mCardSupportedCards;
    protected CardForm mCardForm;
    private CardEditText mCardNumber;
    Button mSubmitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupBaseToolbar();

        mCardCameraScan = (TextView) findViewById(R.id.cardCameraScan);
        mCardSupportedCards = (TextView) findViewById(R.id.cardSupportedCards);

        mCardForm = (CardForm) findViewById(R.id.cardForm);
        mSubmitButton = (Button) findViewById(R.id.cardSubmitBtn);

        mCardNumber = (CardEditText) findViewById(com.braintreepayments.cardform.R.id.bt_card_form_card_number);


        // underline the following text
        mCardCameraScan.setPaintFlags(mCardCameraScan.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mCardSupportedCards.setPaintFlags(mCardSupportedCards.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // animation
        final RippleBackground rippleBackground = (RippleBackground) findViewById(R.id.animation);
        rippleBackground.setVisibility(View.VISIBLE);
        rippleBackground.startRippleAnimation();

        // is NFC available on this device?
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            TextView noNfc = (TextView) findViewById(android.R.id.candidatesArea);
            noNfc.setVisibility(View.VISIBLE);
        } else {
            mCardNfcUtils = new CardNfcUtils(this);
            createProgressDialog();
            initNfcMessages();
            mIntentFromCreate = true;
            onNewIntent(getIntent());
        }

        // set the credit card form fields
        mCardForm.cardRequired(true)
                .expirationRequired(true)
                .cvvRequired(true)
                .setup(this);

        mCardCameraScan.setOnClickListener(this);
        mSubmitButton.setOnClickListener(this);
        mCardForm.setOnCardFormSubmitListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cardCameraScan:
                onScanPress();
                break;
            case R.id.cardSubmitBtn:
                onCardFormSubmit();
                break;
            default:
                break;
        }
    }

    // when the submit button is clicked
    @Override
    public void onCardFormSubmit() {

        // check to see if all Card Form fields are valid & complete
        if (mCardForm.isValid()) {
            Toast.makeText(this, "Your card has been added", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Your card is invalid", Toast.LENGTH_SHORT).show();
        }
    }


    /******************************
     * BEGIN NFC READER
     ******************************/

    @Override
    protected void onResume() {
        super.onResume();
        mIntentFromCreate = false;
        if (mNfcAdapter != null && !mNfcAdapter.isEnabled()) {
            showTurnOnNfcDialog();
        } else if (mNfcAdapter != null) {
            mCardNfcUtils.enableDispatch();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mCardNfcUtils.disableDispatch();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            mCardNfcAsyncTask = new CardNfcAsyncTask.Builder(this, intent, mIntentFromCreate)
                    .build();
        }
    }

    @Override
    public void startNfcReadCard() {
        mIsScanNow = true;
        mProgressDialog.show();
    }

    @Override
    public void cardIsReadyToRead() {
        String card = mCardNfcAsyncTask.getCardNumber().trim();

        // set card number to Card Number field
        mCardNumber.setText(card);
    }


    // while your card is being read by the NFC reader, do not move your card away from the back of the device
    @Override
    public void doNotMoveCardSoFast() {
        showSnackBar(mDoNotMoveCardMessage);
    }

    @Override
    public void unknownEmvCard() {
        showSnackBar(mUnknownEmvCardMessage);
    }

    @Override
    public void cardWithLockedNfc() {
        showSnackBar(mCardWithLockedNfcMessage);
    }

    @Override
    public void finishNfcReadCard() {
        mProgressDialog.dismiss();
        mCardNfcAsyncTask = null;
        mIsScanNow = false;
    }

    // you will see this progress dialog when you tap the credit card behind your device
    private void createProgressDialog() {
        String title = getString(R.string.ad_progressBar_title);
        String mess = getString(R.string.ad_progressBar_mess);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(mess);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
    }

    private void showSnackBar(String message) {
        Snackbar.make(toolbar, message, Snackbar.LENGTH_SHORT).show();
    }

    // show the turn on NFC dialog
    private void showTurnOnNfcDialog() {
        if (mTurnNfcDialog == null) {
            String title = getString(R.string.ad_nfcTurnOn_title);
            String mess = getString(R.string.ad_nfcTurnOn_message);
            String pos = getString(R.string.ad_nfcTurnOn_pos);
            String neg = getString(R.string.ad_nfcTurnOn_neg);
            mTurnNfcDialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(mess)
                    .setPositiveButton(pos, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Send the user to the settings page and hope they turn it on
                            if (android.os.Build.VERSION.SDK_INT >= 16) {
                                startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                            } else {
                                startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                            }
                        }
                    })
                    .setNegativeButton(neg, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            onBackPressed();
                        }
                    }).create();
        }
        mTurnNfcDialog.show();
    }

    private void initNfcMessages() {
        mDoNotMoveCardMessage = getString(R.string.snack_doNotMoveCard);
        mCardWithLockedNfcMessage = getString(R.string.snack_lockedNfcCard);
        mUnknownEmvCardMessage = getString(R.string.snack_unknownEmv);
    }

    /******************************
     * END NFC READER
     ******************************/


    /******************************
     * BEGIN CARD.IO CAMERA SCANNER
     ******************************/

    public void onScanPress() {
        // This method is set up as an onClick handler in the layout xml
        // e.g. android:onClick="onScanPress"

        Intent scanIntent = new Intent(this, CardIOActivity.class);

        // setting this to true will remove the PayPal logo
        scanIntent.putExtra(CardIOActivity.EXTRA_HIDE_CARDIO_LOGO, true); // default: false

        scanIntent.putExtra(CardIOActivity.EXTRA_USE_PAYPAL_ACTIONBAR_ICON, false); // default: true

        // customize these values to suit your needs.
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_RESTRICT_POSTAL_CODE_TO_NUMERIC_ONLY, false); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CARDHOLDER_NAME, false); // default: false

        // hides the manual entry button
        // if set, developers should provide their own manual entry mechanism in the app
        scanIntent.putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, true); // default: false

        // matches the theme of your application
        scanIntent.putExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, false); // default: false

        // MY_SCAN_REQUEST_CODE is arbitrary and is only used within this activity.
        startActivityForResult(scanIntent, MY_SCAN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String resultStr;
        if (data != null && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
            CreditCard scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);

            // Never log a raw card number. Avoid displaying it, but if necessary use getFormattedCardNumber()
            resultStr = scanResult.cardNumber; // returns the card number

//            // Do something with the raw number, e.g.:
//            // myService.setCardNumber( scanResult.cardNumber );
//
//            if (scanResult.isExpiryValid()) {
//                resultStr += "Expiration Date: " + scanResult.expiryMonth + "/" + scanResult.expiryYear + "\n";
//            }
//
//            if (scanResult.cvv != null) {
//                // Never log or display a CVV
//                resultStr += "CVV has " + scanResult.cvv.length() + " digits.\n";
//            }
//
//            if (scanResult.postalCode != null) {
//                resultStr += "Postal Code: " + scanResult.postalCode + "\n";
//            }
//
//            if (scanResult.cardholderName != null) {
//                resultStr += "Cardholder Name : " + scanResult.cardholderName + "\n";
//            }
        } else {
            resultStr = null;
        }

        // set credit card number to Card Number field
        mCardNumber.setText(resultStr);
    }

    /******************************
     * END CARD.IO CAMERA SCANNER
     ******************************/
}
