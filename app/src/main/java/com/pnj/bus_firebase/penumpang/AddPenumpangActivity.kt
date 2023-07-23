package com.pnj.bus_firebase.penumpang

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.pnj.bus_firebase.MainActivity
import com.pnj.bus_firebase.R
import com.pnj.bus_firebase.databinding.ActivityAddPenumpangBinding
import java.io.ByteArrayOutputStream
import java.util.*

class AddPenumpangActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPenumpangBinding
    private val firestoreDatabase = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUri : Uri
    private var dataGambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPenumpangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.TxtAddTglBerangkatan.setOnClickListener{
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { view, year, monthOfyear, dayOfMonth ->
                    binding.TxtAddTglBerangkatan.setText("" + year + "-" + monthOfyear + "-" + dayOfMonth)
                }, year, month, day)

            dpd.show()
        }

        binding.BtnAddPenumpang.setOnClickListener {
            addPenumpang()
        }

        binding.BtnImgPenumpang.setOnClickListener {
            openCamera()
        }
    }

    fun addPenumpang() {
        var nik : String = binding.TxtAddNIK.text.toString()
        var nama : String = binding.TxtAddNama.text.toString()
        var tgl_berangkat : String = binding.TxtAddTglBerangkatan.text.toString()
        var tujuan : String = binding.TxtAddTujuan.text.toString()

        var jk : String = ""
        if(binding.RdnEditJKL.isChecked) {
            jk = "Laki-Laki"
        }
        else if (binding.RdnEditJKP.isChecked) {
            jk = "Perempuan"
        }

        val penumpang : MutableMap<String, Any> = HashMap()
        penumpang["nik"] = nik
        penumpang["nama"] = nama
        penumpang["tgl_berangkat"] = tgl_berangkat
        penumpang["tujuan"] = tujuan
        penumpang["jenis_kelamin"] = jk

        if(dataGambar != null) {
            uploadPictFirebase(dataGambar!!, "${nik}_${nama}")

            firestoreDatabase.collection("penumpang").add(penumpang)
                .addOnSuccessListener {
                    val intentMain = Intent (this, MainActivity::class.java)
                    startActivity(intentMain)
                }
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.packageManager?.let {
                intent?.resolveActivity(it).also {
                    startActivityForResult(intent, REQ_CAM)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == RESULT_OK) {
            dataGambar = data?.extras?.get("data") as Bitmap
            binding.BtnImgPenumpang.setImageBitmap(dataGambar)
        }
    }

    private fun uploadPictFirebase(img_bitmap: Bitmap, file_name: String) {
        val baos = ByteArrayOutputStream()
        val ref = FirebaseStorage.getInstance().reference.child("img_penumpang/${file_name}.jpg")
        img_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val img = baos.toByteArray()
        ref.putBytes(img)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    ref.downloadUrl.addOnCompleteListener { Task ->
                        Task.result.let {Uri ->
                            imgUri = Uri
                            binding.BtnImgPenumpang.setImageBitmap(img_bitmap)
                        }
                    }
                }
            }
    }
}