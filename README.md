<h1>Credit Card - Android NFC Reader, Card.IO Camera Scanner, BrainTree Credit Card Form</h1>

<table>
<tr>

<td><img width="500px" alt="main activity" src="https://cloud.githubusercontent.com/assets/11988924/25058120/49e60a5e-2144-11e7-8c8f-d7eb2e5334c3.jpg">
</td>

<td>
<img width="500px" alt="card io" src="https://cloud.githubusercontent.com/assets/11988924/25058134/66d60286-2144-11e7-870f-4b297412deba.jpg">
</td>

</tr>
</table>


As long as your Android device supports NFC and has a working camera you are good to go.

<b>Key features of this app:</b>

<li>NFC Card Reader (Tap Card Behind Device)</li>
<li>Camera Card Scanner (Card.IO by PayPal)</li>
<li>Credit Card Form (BrainTree)</li>

<br>

<p>Works with <b>Visa</b> &amp; <b>MasterCard</b></p>
<img width="500px" alt="card io" src="https://cloud.githubusercontent.com/assets/11988924/25058488/e72ffea2-2147-11e7-8379-0695d7691510.png">


When you tap your card behind your device (NFC Reader) or scan your card with the camera (Card IO). The credit card number will appear in the Card Number field in the Credit Card form.

<h4>Android Manifest:</h4>

First thing you want to do is set the NFC & Camera Permissions in the <b>AndroidManifest.xml</b>

```xml
   <uses-permission android:name="android.permission.NFC"/>
   <uses-permission android:name="android.permission.CAMERA"/>
       
   <!-- More Code Below... -->     

```

<h4>Dependencies:</h4>

Compile these 4 dependencies in the <b>build.gradle</b>

```xml
    // Animation
    compile 'com.skyfishjy.ripplebackground:library:1.0.1'

    // Credit Card Form
    compile 'com.braintreepayments:card-form:3.0.0'

    // NFC
    compile 'com.github.pro100svitlo:creditCardNfcReader:1.0.2'

    // Card IO
    compile 'io.card:android-sdk:5.4.2'

```
<br>

The NFC Reader, Card Scanner, & Card Form fields will be handled in the <b>MainActivity.class</b>
```java 
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

```
<br>

Of course, below we have the app user interface. This can be found in <b>activity_main.xml</b>
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fadingEdgeLength="0dp"
    android:fillViewport="true"
    android:overScrollMode="never"
    android:scrollbars="none">

    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        style="@style/linearLayoutStyle">

        <com.skyfishjy.library.RippleBackground
            android:id="@+id/animation"
            android:layout_width="match_parent"
            android:layout_height="169dp"
            android:layout_weight="0.54"
            app:rb_color="#cacfd1"
            app:rb_duration="3000"
            app:rb_radius="32dp"
            app:rb_rippleAmount="4"
            app:rb_scale="6">

            <ImageView
                android:id="@+id/nfcIcon"
                android:layout_width="64dp"
                android:layout_height="64dp"
                android:layout_centerInParent="true"
                android:src="@drawable/nfc_icon_pay" />

            <TextView
                android:id="@+id/nfcMsg"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_below="@+id/nfcIcon"
                android:layout_centerHorizontal="true"
                android:layout_gravity="center"
                android:layout_marginTop="50dp"
                android:text="@string/nfc_msg"
                android:textAlignment="center" />

        </com.skyfishjy.library.RippleBackground>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/stroke_top"
            android:orientation="vertical"
            android:padding="10dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="@dimen/default_margin_item_separator"
                android:gravity="center"
                android:orientation="horizontal">

                <ImageView
                    android:id="@+id/cameraIcon"
                    android:layout_width="25dp"
                    android:layout_height="25dp"
                    android:layout_gravity="center"
                    android:layout_marginRight="10dp"
                    android:background="@drawable/ic_camera_alt_24"
                    android:backgroundTint="@color/link_blue" />

                <TextView
                    android:id="@+id/cardCameraScan"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:text="@string/scan_card_link"
                    android:textSize="18dp" />

            </LinearLayout>

            <com.braintreepayments.cardform.view.CardForm
                android:id="@+id/cardForm"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_margin="@dimen/bt_margin" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="@dimen/default_margin_item_separator"
                android:layout_marginTop="20dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnCancel"
                    style="@style/btnRadiusWhite"
                    android:layout_width="wrap_content"
                    android:layout_marginRight="10dp"
                    android:layout_weight="1"
                    android:background="@drawable/border_radius_dark_grey"
                    android:text="@string/cancel"
                    android:textColor="@color/main_darkgrey" />

                <Button
                    android:id="@+id/cardSubmitBtn"
                    style="@style/btnRadiusWhite"
                    android:layout_width="wrap_content"
                    android:layout_marginLeft="10dp"
                    android:layout_weight="1"
                    android:background="@drawable/border_radius_blue"
                    android:text="@string/submit" />

            </LinearLayout>

            <TextView
                android:id="@+id/cardSupportedCards"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:layout_marginTop="@dimen/default_margin_item_separator"
                android:text="@string/banks_cards_supported" />

        </LinearLayout>

    </LinearLayout>

</ScrollView>
```
<br>

<h4 style="color: red;">All other supporting files can be found in this repository</h4>

<br>

Cheers, 
<br>Let me know if you have any further questions.
