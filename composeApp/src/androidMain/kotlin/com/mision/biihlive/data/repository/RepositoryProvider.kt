package com.mision.biihlive.data.repository

import android.content.Context

object RepositoryProvider {

    private var firestoreRepository: FirestoreRepository? = null

    fun initialize(context: Context) {
        // Firebase se inicializa automáticamente con google-services.json
    }

    fun getFirestoreRepository(): FirestoreRepository {
        if (firestoreRepository == null) {
            firestoreRepository = FirestoreRepository()
        }
        return firestoreRepository!!
    }

}