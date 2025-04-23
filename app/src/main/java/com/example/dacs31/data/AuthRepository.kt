package com.example.dacs31.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    companion object {
        private const val TAG = "AuthRepository"
    }

    suspend fun register(email: String, password: String, fullName: String, role: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user == null) {
                Log.e(TAG, "Đăng ký thất bại: User is null")
                return Result.failure(Exception("Đăng ký thất bại: Không thể tạo người dùng"))
            }

            val userData = User(
                uid = user.uid,
                email = email,
                fullName = fullName,
                phoneNumber = "",
                gender = "",
                address = "",
                role = role
            )
            usersCollection.document(user.uid).set(userData).await()
            Log.d(TAG, "Đăng ký thành công: ${user.uid}")
            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(handleAuthError(e, "Đăng ký thất bại"))
        }
    }

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user == null) {
                Log.e(TAG, "Đăng nhập thất bại: User is null")
                return Result.failure(Exception("Đăng nhập thất bại: Không tìm thấy người dùng"))
            }

            Log.d(TAG, "Đăng nhập thành công: ${user.uid}")
            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(handleAuthError(e, "Đăng nhập thất bại"))
        }
    }

    suspend fun getUserRole(): String? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val document = usersCollection.document(firebaseUser.uid).get().await()
            val role = document.getString("role")
            if (role != null) {
                Log.d(TAG, "Lấy vai trò thành công: $role")
                role
            } else {
                Log.e(TAG, "Vai trò người dùng không tồn tại")
                throw Exception("Vai trò người dùng không tồn tại")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy vai trò: ${e.message}")
            throw e
        }
    }

    suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.w(TAG, "Không có người dùng hiện tại")
            return null
        }

        return try {
            getUserFromFirestore(firebaseUser.uid) ?: User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                role = "unknown"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy thông tin người dùng: ${e.message}")
            User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                role = "unknown"
            )
        }
    }

    suspend fun getUserFromFirestore(uid: String): User? {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)?.copy(uid = uid)
                Log.d(TAG, "Lấy thông tin người dùng thành công: $uid")
                user
            } else {
                Log.w(TAG, "Tài liệu người dùng không tồn tại: $uid")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy thông tin người dùng từ Firestore: ${e.message}")
            null
        }
    }

    fun signOut() {
        auth.signOut()
        if (auth.currentUser == null) {
            Log.d(TAG, "Đăng xuất thành công")
        } else {
            Log.w(TAG, "Đăng xuất thất bại: Người dùng vẫn tồn tại")
        }
    }

    private fun handleAuthError(e: Exception, defaultMessage: String): Exception {
        val errorMessage = when {
            e.message?.contains("EMAIL_EXISTS") == true -> "Email đã được sử dụng"
            e.message?.contains("INVALID_EMAIL") == true -> "Email không hợp lệ"
            e.message?.contains("WEAK_PASSWORD") == true -> "Mật khẩu quá yếu (ít nhất 6 ký tự)"
            e.message?.contains("INVALID_PASSWORD") == true -> "Mật khẩu không đúng"
            e.message?.contains("USER_NOT_FOUND") == true -> "Tài khoản không tồn tại"
            e.message?.contains("USER_DISABLED") == true -> "Tài khoản đã bị khóa"
            e.message?.contains("PERMISSION_DENIED") == true -> "Không có quyền truy cập Firestore"
            else -> "$defaultMessage: ${e.message}"
        }
        Log.e(TAG, errorMessage)
        return Exception(errorMessage)
    }
}