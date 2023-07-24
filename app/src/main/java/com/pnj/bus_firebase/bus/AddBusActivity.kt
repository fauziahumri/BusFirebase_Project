package com.pnj.bus_firebase.bus

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.pnj.bus_firebase.databinding.ActivityAddBusBinding
import java.io.ByteArrayOutputStream
import java.util.*

class AddBusActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddBusBinding
    private val firestoreDatabase = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUri : Uri
    private var dataGambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.TxtAddTglProduksi.setOnClickListener{
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { view, year, monthOfyear, dayOfMonth ->
                    binding.TxtAddTglProduksi.setText("" + year + "-" + monthOfyear + "-" + dayOfMonth)
                }, year, month, day)

            dpd.show()
        }

        binding.BtnAddBus.setOnClickListener {
            addBus()
        }

        binding.BtnImgBus.setOnClickListener {
            openCamera()
        }
    }

    fun addBus() {
        var plat : String = binding.TxtAddPlat.text.toString()
        var nama : String = binding.TxtAddNama.text.toString()
        var tgl_produksi : String = binding.TxtAddTglProduksi.text.toString()

        var jb : String = ""
        if(binding.RdnEditJBUHD.isChecked) {
            jb = "Ultra High Decker"
        }
        else if (binding.RdnEditJBDD.isChecked) {
            jb = "Double Decker"
        }

        var service = ArrayList<String>()
        if(binding.ChkSelimut.isChecked) {
            service.add("selimut")
        }
        if(binding.ChkBantal.isChecked) {
            service.add("bantal")
        }
        if(binding.ChkCamilan.isChecked) {
            service.add("camilan")
        }

        val service_string = service.joinToString("|")

        val bus : MutableMap<String, Any> = HashMap()
        bus["plat"] = plat
        bus["nama"] = nama
        bus["tgl_produksi"] = tgl_produksi
        bus["jenis_bus"] = jb
        bus["service"] = service_string

        if(dataGambar != null) {
            uploadPictFirebase(dataGambar!!, "${plat}_${nama}")

            firestoreDatabase.collection("bus").add(bus)
                .addOnSuccessListener {
                    val intentMain = Intent (this, BusActivity::class.java)
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

    private fun uploadPictFirebase(img_bitmap: Bitmap, file_name: String) {
        val baos = ByteArrayOutputStream()
        val ref = FirebaseStorage.getInstance().reference.child("img_bus/${file_name}.jpg")
        img_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val img = baos.toByteArray()
        ref.putBytes(img)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    ref.downloadUrl.addOnCompleteListener { Task ->
                        Task.result.let {Uri ->
                            imgUri = Uri
                            binding.BtnImgBus.setImageBitmap(img_bitmap)
                        }
                    }
                }
            }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            dataGambar = imageBitmap
            binding.BtnImgBus.setImageBitmap(imageBitmap)
        }
    }


}