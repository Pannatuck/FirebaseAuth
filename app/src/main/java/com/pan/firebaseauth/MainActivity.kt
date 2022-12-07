package com.pan.firebaseauth

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.pan.firebaseauth.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // треба не забувати про inflate
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        // робимо запит на вихід з аккаунту, при запуску нашої проги
        auth.signOut()

        // прості виклики при натисканні на кнопку
        binding.btnRegister.setOnClickListener{
            registerUser()
        }
        binding.btnLogin.setOnClickListener{
            loginUser()
        }
        binding.btnUpdateProfile.setOnClickListener{
            updateProfile()
        }
    }

    /* метод для оновлення аккаунта, на сервері Firebase. Для прикладу, тут оновлюється нікнейм та фото користувача */
    private fun updateProfile(){
        auth.currentUser?.let { user ->
            val username = binding.etUsername.text.toString()
            // примерно такой формат всегда используется, когда нужно достать файл, из файлов программьі
            val photoURI = Uri.parse("android.resource://$packageName/${R.drawable.avatar}")
            // через UserProfileChangeRequest робимо запит на сервер, для оновлення данних
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .setPhotoUri(photoURI)
                .build() // не забьівать билдить

            // з задачами Firebase потрібно працювати через корутіни, тому що процес може затягнутим
            // бути і викличе зависання нашої апки
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    user.updateProfile(profileUpdates).await()
                    // для виклику з Toast, потрібно повернутись назад до MAIN треду
                    withContext(Dispatchers.Main){
                        checkLoggedInState()
                        // треба робити явний виклик this на Актівіті, тому що інакше this передає виклик
                        // на CoroutineScope
                        Toast.makeText(this@MainActivity,
                            "Successfully updated userprofile",
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (e: Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkLoggedInState()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    //
    private fun registerUser(){
        val email = binding.etEmailRegister.text.toString()
        val password = binding.etPasswordRegister.text.toString()
        // перевіряємо, чи ввів користувач щось в поля
        if (email.isNotEmpty() && password.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // await() makes our app to wait, while creating process is not completed
                    auth.createUserWithEmailAndPassword(email, password).await()
                    withContext(Dispatchers.Main){
                        checkLoggedInState()
                    }
                } catch (e: Exception){

                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // якщо вже існує аккаунт на сервері firebase, то можна просто увійти в нього
    private fun loginUser(){
        val email = binding.etEmailLogin.text.toString()
        val password = binding.etPasswordLogin.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // await() заставляє прогу чекати, доки не виконається процес, до якого під'єднаний цей самий await()
                    // взагалі, у Firebase є методи на будь-який випадок, треба просто знайти потрібний
                    auth.signInWithEmailAndPassword(email, password).await()
                    withContext(Dispatchers.Main){
                        checkLoggedInState()
                    }
                } catch (e: Exception){
                    // cant work with UI in IO thread, need to switch to MAIN thread
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // для перевірки авторизації на сервері. Тут просто змінює напис logged/not logged
    private fun checkLoggedInState() {
        val user = auth.currentUser
        with(binding){
            if(user == null)
                tvLoggedIn.text = "You are not logged in"
            else {
                tvLoggedIn.text = "You are logged in"
                // при первой загрузке, будет показано то имя, которое у пользователя в Firebase
                etUsername.setText(user.displayName)
                ivAvatar.setImageURI(user.photoUrl)
            }
        }

    }


}