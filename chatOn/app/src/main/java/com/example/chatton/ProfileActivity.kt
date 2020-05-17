@file:Suppress("DEPRECATION")

package com.example.chatton

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.GenericTransitionOptions.with
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.with
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_profile.*
import java.io.File
import java.io.IOException
import java.net.URL


@Suppress("DEPRECATION")
class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var RootRef: DatabaseReference
    private lateinit var profileRef: DatabaseReference
    private lateinit var UserProfileImage: StorageReference
    private lateinit var currentUserID:String
    private val GalleryPick = 1;
    private lateinit var profile_image: CircleImageView
    private lateinit var progressBar:ProgressDialog
    private lateinit var resultUri:Uri
    private lateinit var filePath:StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setTitle("Profile")
        //DEFINITION
        val updateAccountProfile= findViewById<Button>(R.id.update_settings_button)
      //  val userName = findViewById<EditText>(R.id.set_user_name)
      //  val userStatus = findViewById<EditText>(R.id.set_profile_status)
        profile_image = findViewById<CircleImageView>(R.id.set_profile_image)
        progressBar = ProgressDialog(this)

        auth = FirebaseAuth.getInstance()
        currentUserID = auth.currentUser?.uid.toString()
        RootRef = FirebaseDatabase.getInstance().reference
        profileRef = FirebaseDatabase.getInstance().reference.child("Users").child(currentUserID).child("image")
        UserProfileImage = FirebaseStorage.getInstance().reference.child("Profile Image")

        if (profile_image == null) {
            profile_image = CircleImageView(this)
        }

        //PICK PHOTO FROM GALLERY
        profile_image.setOnClickListener{
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, GalleryPick)
        }

        updateAccountProfile.setOnClickListener{
            UpdateProfile()
        }

        RetrieveUserInfo(this@ProfileActivity)

    }
    //save user information in the firebase.
    //These information under the User Root.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GalleryPick && resultCode == Activity.RESULT_OK && data != null){
          //  val uri = data.data
            CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(this)

    }
        if (requestCode === CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

            if (resultCode === Activity.RESULT_OK) {
                progressBar.setTitle("Set Profile Image")
                progressBar.setMessage("Please wait your profile image is updating..")
                progressBar.setCanceledOnTouchOutside(false)
                progressBar.show()
                resultUri = result.uri
                //profile_image.setImageURI(resultUri)
                //task.result.storage?.downloadUrl.toString()
                val filePath: StorageReference = UserProfileImage.child(currentUserID)
                filePath.putFile(resultUri).addOnCompleteListener(this, OnCompleteListener<UploadTask.TaskSnapshot> {    task ->
                    if (task.isSuccessful) {
                        //Toast.makeText(applicationContext, task.result.toString(), Toast.LENGTH_LONG).show()
                        //val downloadUrl= task.result.toString()

                        val value = task.result?.storage?.toString()
                        val downloadUrl= "https://firebasestorage.googleapis.com/v0/b/chattondatabase.appspot.com/o/Profile%20Image%2F"+
                                currentUserID.toString() + "?alt=media&"
                        Toast.makeText(applicationContext, downloadUrl, Toast.LENGTH_LONG).show()
                        RootRef.child("Users").child(currentUserID).child("image")
                            .setValue(downloadUrl)
                            .addOnCompleteListener(this, OnCompleteListener<Void> { task ->
                                if (task.isSuccessful) {
                                   // Toast.makeText(applicationContext, "Image Saved Successfully", Toast.LENGTH_LONG).show()
                                    progressBar.dismiss()
                                }else{
                                    Toast.makeText(applicationContext, "Error", Toast.LENGTH_LONG).show()
                                    progressBar.dismiss()
                                }
                            })

                    }
                    else{
                        Toast.makeText(applicationContext, "Error", Toast.LENGTH_LONG).show()
                        progressBar.dismiss()
                    }
                })
            }

        }

        }



    private fun RetrieveUserInfo(context: Context) {
        RootRef.child("Users").child(currentUserID)
            .addValueEventListener(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    TODO("Not yet implemented")
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                        if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name") && (dataSnapshot.hasChild("image"))))
                        {
                            val retrieveUserName = dataSnapshot.child("name").getValue().toString();
                            val retrievesStatus = dataSnapshot.child("status").getValue().toString();
                            val retrieveProfileImage = dataSnapshot.child("image").getValue().toString();

                            set_user_name.setText(retrieveUserName);
                            set_profile_status.setText(retrievesStatus);
                            Picasso.get().load(retrieveProfileImage).into(set_profile_image);
                        }
                        else if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name")))
                        {
                            val retrieveUserName = dataSnapshot.child("name").getValue().toString();
                            val retrievesStatus = dataSnapshot.child("status").getValue().toString();

                            set_user_name.setText(retrieveUserName);
                            set_profile_status.setText(retrievesStatus);
                        }
                        else
                        {
                            set_user_name.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Please set & update your profile information...", Toast.LENGTH_SHORT).show();
                        }
                    }

            })
    }

    private fun getBitmapFromAssets(fileName: String): Bitmap? {
        return try {
            BitmapFactory.decodeStream(assets.open(fileName))
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun UpdateProfile() {
        val username = set_user_name.text.toString()
        val setStatus = set_profile_status.text.toString()
        val profile = resultUri.toString() + ".png"
       // val profile =
        if (TextUtils.isEmpty(username)) {
            val toast = Toast.makeText(applicationContext, "Please Write Your Name ", Toast.LENGTH_LONG)
            toast.show()
        }
        if (TextUtils.isEmpty(setStatus)) {
            val toast = Toast.makeText(applicationContext, "Please Write Your Status ", Toast.LENGTH_LONG)
            toast.show()
        }
        else{
            var hashMap : HashMap<String, String> = HashMap<String, String> ()
                hashMap.put("uid",currentUserID)
                hashMap.put("name",username)
                hashMap.put("status",setStatus)
              //  hashMap.put("image" , profile )
            RootRef.child("Users").child(currentUserID).setValue(hashMap)
                .addOnCompleteListener(
                OnCompleteListener { task ->
                    when {
                        task.isSuccessful -> {
                            val intent = Intent(this, MainActivity::class.java)
                            //TODO: Can be redirected to ProfileActivity. But bug appears while pressed back button.
                            startActivity(intent)
                            val toast = Toast.makeText(applicationContext, "Profile Updated Successfully ", Toast.LENGTH_LONG)
                            toast.show()
                        }
                        else -> {
                            val toast = Toast.makeText(applicationContext, "Error ", Toast.LENGTH_LONG)
                            toast.show()
                        }
                    }
                })
        }
    }


}
