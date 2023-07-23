package com.pnj.bus_firebase.penumpang

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.pnj.bus_firebase.MainActivity
import com.pnj.bus_firebase.databinding.ActivityEditPenumpangBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class EditPenumpangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPenumpangBinding
    private val db = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUri : Uri
    private var dataGambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPenumpangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val (year, month, day, curr_penumpang) = setDefaultValue()

        binding.TxtEditTglBerangkat.setOnClickListener {
            val dpd = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    binding.TxtEditTglBerangkat.setText(
                        "" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth
                    )
                }, year.toString().toInt(), month.toString().toInt(), day.toString().toInt()
            )
            dpd.show()
        }

        binding.BtnEditPenumpang.setOnClickListener {
            val new_data_penumpang = newPenumpang()
            updatePenumpang(curr_penumpang as Penumpang, new_data_penumpang)

            val intentMain = Intent(this, MainActivity::class.java)
            startActivity(intentMain)
            finish()
        }

        showFoto()

        binding.BtnImgPenumpang.setOnClickListener {
            openCamera()
        }
    }

    fun setDefaultValue(): Array<Any> {
        val intent = intent
        val nik = intent.getStringExtra("nik").toString()
        val nama = intent.getStringExtra("nama").toString()
        val tgl_berangkat = intent.getStringExtra("tgl_berangkat").toString()
        val tujuan = intent.getStringExtra("tujuan").toString()
        val jenis_kelamin = intent.getStringExtra("jenis_kelamin").toString()

        binding.TxtEditNIK.setText(nik)
        binding.TxtEditNama.setText(nama)
        binding.TxtEditTglBerangkat.setText(tgl_berangkat)
        binding.TxtEditTujuan.setText(tujuan)

        val tgl_split = intent.getStringExtra("tgl_berangkat")
            .toString().split("-").toTypedArray()
        val year = tgl_split[0].toInt()
        val month = tgl_split[1].toInt() - 1
        val day = tgl_split[2].toInt()
        if (jenis_kelamin == "Laki-Laki") {
            binding.RdnEditJKL.isChecked = true
        } else if (jenis_kelamin == "Perempuan") {
            binding.RdnEditJKP.isChecked = true
        }

        val curr_penumpang = Penumpang(nik, nama, tgl_berangkat, tujuan, jenis_kelamin)
        return arrayOf(year, month, day, curr_penumpang)
    }

    fun newPenumpang(): Map<String, Any> {
        var nik: String = binding.TxtEditNIK.text.toString()
        var nama: String = binding.TxtEditNama.text.toString()
        var tgl_berangkat: String = binding.TxtEditTglBerangkat.text.toString()
        var tujuan: String = binding.TxtEditTujuan.text.toString()

        var jk: String = ""
        if (binding.RdnEditJKL.isChecked) {
            jk = "Laki-laki"
        } else if (binding.RdnEditJKP.isChecked) {
            jk = "Perempuan"
        }
        if (dataGambar != null) {
            uploadPictFirebase(dataGambar!!, "${nik}_${nama}")
        }

        val penumpang = mutableMapOf<String, Any>()
        penumpang["nik"] = nik
        penumpang["nama"] = nama
        penumpang["tgl_berangkat"] = tgl_berangkat
        penumpang["tujuan"] = tujuan
        penumpang["jenis_kelamin"] = jk

        return penumpang
    }

    private fun updatePenumpang(penumpang: Penumpang, newPenumpangMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personaQuery = db.collection("penumpang")
                .whereEqualTo("nik", penumpang.nik)
                .whereEqualTo("nama", penumpang.nama)
                .whereEqualTo("jenis_kelamin", penumpang.jenis_kelamin)
                .whereEqualTo("tgl_berangkat", penumpang.tgl_berangkat)
                .whereEqualTo("tujuan", penumpang.tujuan)
                .get()
                .await()
            if (personaQuery.documents.isNotEmpty()) {
                for (document in personaQuery) {
                    try {
                        db.collection("penumpang").document(document.id).set(
                            newPenumpangMap,
                            SetOptions.merge()
                        )
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@EditPenumpangActivity,
                                e.message, Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditPenumpangActivity,
                        "No persons matched the query.", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    fun showFoto() {
        val intent = intent
        val nik = intent.getStringExtra("nik").toString()
        val nama = intent.getStringExtra("nama").toString()
        val tujuan = intent.getStringExtra("tujuan").toString()

        val storageRef = FirebaseStorage.getInstance().reference.child("img_penumpang/${nik}_${nama}.jpg")
        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.BtnImgPenumpang.setImageBitmap(bitmap)
        }.addOnFailureListener {
            Log.e("foto ?", "gagal")
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.packageManager?.let {
                intent?.resolveActivity(it).also{
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
            .addOnCompleteListener{
                if(it.isSuccessful) {
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