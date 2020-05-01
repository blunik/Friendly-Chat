package com.example.friendlychat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 1000
        val  RC_SIGN_IN: Int = 1
        val RC_PHOTO_PICKER:Int = 2
        val FRIENDLY_MSG_LENGTH_KEY: String = "friendly_msg_length"
    }

    private var mMessageListView: ListView? = null
    private var mMessageAdapter: MessageAdapter? = null
    private var mProgressBar: ProgressBar? = null
    private var mPhotoPickerButton: ImageButton? = null
    private var mMessageEditText: EditText? = null
    private var mSendButton: Button? = null

    private var mUsername: String? = null

    private var mFirebaseDatabase: FirebaseDatabase? = null
    private var mMessagesDatabaseReference: DatabaseReference? = null
    private  var mChildEventListener: ChildEventListener? = null


    private var mFirebaseAuth: FirebaseAuth? = null
    private lateinit var mAuthStateListener: FirebaseAuth.AuthStateListener

    private var mFirebaseStorage: FirebaseStorage? = null
    private var mChatPhotosStorageReference: StorageReference? = null

    private var mFirebaseRemoteConfig: FirebaseRemoteConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUsername = ANONYMOUS

        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseStorage = FirebaseStorage.getInstance()
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        mChatPhotosStorageReference = mFirebaseStorage!!.reference.child("chat_photos")
        mMessagesDatabaseReference = mFirebaseDatabase!!.reference.child("messages")

        mProgressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        mMessageListView = findViewById<View>(R.id.messageListView) as ListView
        mPhotoPickerButton = findViewById<View>(R.id.photoPickerButton) as ImageButton
        mMessageEditText = findViewById<View>(R.id.messageEditText) as EditText
        mSendButton = findViewById<View>(R.id.sendButton) as Button

        val friendlyMessages: List<FriendlyMessage> = ArrayList()
        mMessageAdapter = MessageAdapter(this, R.layout.item_message, friendlyMessages)
        mMessageListView!!.adapter = mMessageAdapter

        mProgressBar!!.visibility = ProgressBar.INVISIBLE

        mPhotoPickerButton!!.setOnClickListener {

            val intent:Intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER)
        }

        mMessageEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (charSequence.toString().trim { it <= ' ' }.length > 0) {
                    mSendButton!!.isEnabled = true
                } else {
                    mSendButton!!.isEnabled = false
                }
            }

            override fun afterTextChanged(editable: Editable) {
            }
        })

        mMessageEditText!!.filters = arrayOf<InputFilter>(
            InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)
        )

        mSendButton!!.setOnClickListener {
            val friendlyMessage: FriendlyMessage = FriendlyMessage(
                mMessageEditText!!.text.toString(),
                mUsername!!, null
            )
            mMessagesDatabaseReference!!.push().setValue(friendlyMessage)
            mMessageEditText!!.setText("")
        }


        mAuthStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user: FirebaseUser? = firebaseAuth.currentUser
            if (user != null){
                onSignedInInitialize(user.displayName)
            } else {
                onSignedOutCleanup()
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )
                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        .build(),
                    RC_SIGN_IN)
            }
        }

        val configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
            .setDeveloperModeEnabled(BuildConfig.DEBUG)
            .build()
        mFirebaseRemoteConfig!!.setConfigSettings(configSettings)
        val defaultConfigMap: MutableMap<String, Any> = HashMap()
        defaultConfigMap[FRIENDLY_MSG_LENGTH_KEY] = DEFAULT_MSG_LENGTH_LIMIT
        mFirebaseRemoteConfig!!.setDefaults(defaultConfigMap)
        fetchConfig()


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show()
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Sign in canceled!", Toast.LENGTH_SHORT).show()
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data!!.data

            // Get a reference to store file at chat_photos/<FILENAME>
            val photoRef =
                mChatPhotosStorageReference!!.child(selectedImageUri!!.lastPathSegment!!)
            photoRef.putFile(selectedImageUri)
                .addOnSuccessListener(
                    this
                ) { //When the image has successfully uploaded, get its download URL
                    photoRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            val friendlyMessage =
                                FriendlyMessage(null, mUsername, uri.toString())
                            mMessagesDatabaseReference!!.push().setValue(friendlyMessage)
                        }
                }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.sign_out_menu -> {
                AuthUI.getInstance().signOut(this)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun onSignedInInitialize(username: String?){
        mUsername = username
        attachDatabaseReadListener()
    }
    private fun onSignedOutCleanup(){
        mUsername = ANONYMOUS
        mMessageAdapter!!.clear()
        detachDatabaseReadListener()
    }

    private fun attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val friendlyMessage: FriendlyMessage? =
                        dataSnapshot.getValue(FriendlyMessage::class.java)
                    mMessageAdapter!!.add(friendlyMessage)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onCancelled(databaseError: DatabaseError) {}
            }
            mMessagesDatabaseReference!!.addChildEventListener(this.mChildEventListener as ChildEventListener)
        }
    }

    private fun detachDatabaseReadListener() {
        if (mChildEventListener != null){
            mMessagesDatabaseReference!!.removeEventListener(mChildEventListener!!)
            mChildEventListener = null
        }
    }

    override fun onPause() {
        super.onPause()
        mFirebaseAuth!!.removeAuthStateListener(mAuthStateListener)
        mMessageAdapter!!.clear()
        detachDatabaseReadListener()
    }

    override fun onResume() {
        super.onResume()
        mFirebaseAuth!!.addAuthStateListener(mAuthStateListener)
    }

    fun fetchConfig() {
        var cacheExpiration: Long = 3600
        if (mFirebaseRemoteConfig!!.info.configSettings.isDeveloperModeEnabled) {
            cacheExpiration = 0
        }

        mFirebaseRemoteConfig!!.fetch(cacheExpiration)
            .addOnSuccessListener { // Make the fetched config available
                // via FirebaseRemoteConfig get<type> calls, e.g., getLong, getString.
                mFirebaseRemoteConfig!!.activateFetched()

                // Update the EditText length limit with
                // the newly retrieved values from Remote Config.
                applyRetrievedLengthLimit()
            }
            .addOnFailureListener { e -> // An error occurred when fetching the config.


                // Update the EditText length limit with
                // the newly retrieved values from Remote Config.
                applyRetrievedLengthLimit()
            }

    }
    private fun applyRetrievedLengthLimit() {
        val friendly_msg_length =
            mFirebaseRemoteConfig!!.getLong(FRIENDLY_MSG_LENGTH_KEY)
        mMessageEditText!!.filters = arrayOf<InputFilter>(LengthFilter(friendly_msg_length.toInt()))

    }
}