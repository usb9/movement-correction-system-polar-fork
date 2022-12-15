package com.example.mobile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseHandler {

        fun getCurrentUser(): FirebaseUser? {
            val mFirebaseAuth = FirebaseAuth.getInstance();

            val mFirebaseUser = mFirebaseAuth.currentUser;
            if (mFirebaseUser != null) {
                return mFirebaseUser
            } else
            {
                return null
            }
        }
}