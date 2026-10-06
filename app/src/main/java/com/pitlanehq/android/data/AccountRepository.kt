package com.pitlanehq.android.data
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class AccountState(val signedIn:Boolean=false,val email:String="",val display:String="",val syncing:Boolean=false,val syncedFiles:Int=0,val syncVersion:Long=0,val error:String?=null)

class AccountRepository(private val context:Context){
 private val base="https://pitlanehq.app"
 private val client=OkHttpClient.Builder().callTimeout(30,TimeUnit.SECONDS).build()
 private val prefs=context.getSharedPreferences("pitlane-account",Context.MODE_PRIVATE)
 fun storedState()=AccountState(prefs.getString("token",null)!=null,prefs.getString("email","")?:"",prefs.getString("display","")?:"",false,prefs.getInt("syncedFiles",0),prefs.getLong("syncVersion",0))
 private fun hkdf(k:ByteArray,info:String):ByteArray{val m=Mac.getInstance("HmacSHA256");m.init(SecretKeySpec(ByteArray(32),"HmacSHA256"));val p=m.doFinal(k);m.init(SecretKeySpec(p,"HmacSHA256"));return m.doFinal(info.toByteArray()+byteArrayOf(1)).copyOf(32)}
 private fun keys(email:String,password:String):Pair<String,ByteArray>{val salt=java.security.MessageDigest.getInstance("SHA-256").digest(("pitlanehq-account-v1:"+email).toByteArray());val spec=PBEKeySpec(password.toCharArray(),salt,600000,256);val master=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded;return hkdf(master,"pitlanehq auth").joinToString(""){"%02x".format(it)} to hkdf(master,"pitlanehq wrap")}
 private fun deviceKey():SecretKey{val ks=java.security.KeyStore.getInstance("AndroidKeyStore").apply{load(null)};(ks.getEntry("pitlanehq-account",null) as? java.security.KeyStore.SecretKeyEntry)?.let{return it.secretKey};val g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(KeyGenParameterSpec.Builder("pitlanehq-account",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());return g.generateKey()}
 private fun protect(v:String):String{val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,deviceKey());return Base64.encodeToString(c.iv+c.doFinal(v.toByteArray()),Base64.NO_WRAP)}
 private fun unprotect(v:String):String{val b=Base64.decode(v,Base64.NO_WRAP);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,deviceKey(),GCMParameterSpec(128,b,0,12));return String(c.doFinal(b,12,b.size-12))}
 private fun openWrapped(wrap:ByteArray,s:String):ByteArray{val b=Base64.decode(s,Base64.DEFAULT);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,SecretKeySpec(wrap,"AES"),GCMParameterSpec(128,b,0,12));return c.doFinal(b,12,b.size-12)}
 private fun request(method:String,path:String,body:String?=null,token:String?=null):JSONObject{val rb=body?.toRequestBody("application/json".toMediaType());val q=Request.Builder().url(base+path).method(method,rb);if(token!=null)q.header("Authorization","Bearer $token");client.newCall(q.build()).execute().use{response->val txt=response.body?.string()?:"{}";val j=JSONObject(txt);if(!response.isSuccessful)throw IllegalStateException(j.optString("error","Request failed"));return j}}
 fun login(email0:String,password:String):AccountState{val email=email0.trim().lowercase();require(email.contains("@")){"Enter a valid email"};require(password.length>=10){"Password must be at least 10 characters"};val (auth,wrap)=keys(email,password);val j=request("POST","/account/login",JSONObject().apply{put("email",email);put("auth",auth);put("device","Android")}.toString());prefs.edit().putString("token",protect(j.getString("token"))).putString("dataKey",protect(Base64.encodeToString(openWrapped(wrap,j.getString("wrappedKey")),Base64.NO_WRAP))).putString("email",email).putString("display",j.optString("display","")).apply();return sync()}
 fun sync():AccountState{val token=unprotect(prefs.getString("token",null)?:throw IllegalStateException("Sign in required"));val key=Base64.decode(unprotect(prefs.getString("dataKey",null)!!),Base64.NO_WRAP);val j=request("GET","/account/sync",token=token);val blob=j.optString("blob","");if(blob.isEmpty())return storedState().copy(syncVersion=j.optLong("version",0));val s=Base64.decode(blob,Base64.DEFAULT);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,s,0,12));val zipped=c.doFinal(s,12,s.size-12);val out=ByteArrayOutputStream();GZIPInputStream(ByteArrayInputStream(zipped)).use{it.copyTo(out)};val files=JSONObject(String(out.toByteArray()));val count=files.length();prefs.edit().putInt("syncedFiles",count).putLong("syncVersion",j.optLong("version",0)).apply();return storedState().copy(syncVersion=j.optLong("version",0))}
 fun logout(){runCatching{val t=unprotect(prefs.getString("token",null)!!);request("POST","/account/logout",token=t)};prefs.edit().clear().apply()}
}